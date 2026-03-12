package com.trivine.llc.api.service;

import com.trivine.llc.api.dto.NlpQuery;
import com.trivine.llc.api.dto.WeightedBusinessHitDto;
import com.trivine.llc.api.entity.Business;
import com.trivine.llc.api.entity.BusinessService;
import com.trivine.llc.api.mapper.BusinessMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class BusinessWeightedSearchService {

    private final EntityManager em;
    private final NlpQueryParser parser;
    private final BusinessMapper businessMapper;

    // Decide admin-only filter inside the service (no controller param).
    // You can override in application.yml: llc.search.admin-only: true/false
    @Value("${llc.search.admin-only:true}")
    private boolean adminOnlyDefault;

    // Field weights / boosts
    private static final double W_BUSINESS_NAME = 5.0;
    private static final double W_SERVICE_NAME  = 4.0;
    private static final double W_CITY          = 2.5;
    private static final double W_STATE         = 1.5;
    private static final double BOOST_ZIP_EQ    = 8.0;
    private static final double BOOST_LOC_ANY   = 3.0;

    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}]+");

    // cache of distinct states from DB (all lowercased)
    private volatile Set<String> stateCache;

    // ---------- Pageable search (adminOnly decided here) ----------
    @Transactional(readOnly = true)
    public Page<WeightedBusinessHitDto> searchWeighted(String q, Pageable pageable) {
        // 1) cap query to 50 words (as requested)
        String capped = capWords(q, 50);

        // 2) parse
        NlpQuery nq = parser.parse(capped == null ? "" : capped);

        // 3) admin-only decision (from property)
        final boolean adminOnly = adminOnlyDefault;

        // 4) build candidate query
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Business> cq = cb.createQuery(Business.class);
        Root<Business> root = cq.from(Business.class);
        root.fetch("service", JoinType.LEFT);
        root.fetch("owner", JoinType.LEFT);
        Join<Business, BusinessService> svc = root.join("service", JoinType.LEFT);

        List<Predicate> must = new ArrayList<>();
        boolean hasLocation = false;

        if (adminOnly) {
            must.add(cb.equal(cb.lower(root.get("updatedBy")), "admin"));
        }
        if (nq.getZip() != null)   { must.add(cb.equal(cb.lower(root.get("zipCode")), nq.getZip().toLowerCase())); hasLocation = true; }
        if (nq.getCity() != null)  { must.add(cb.like(cb.lower(root.get("city")), "%" + nq.getCity().toLowerCase() + "%")); hasLocation = true; }
        if (nq.getState() != null) { addStatePredicate(cb, root.get("state"), must, nq.getState()); hasLocation = true; }

        Predicate kwOr = null;
        if (nq.getKeywords() != null && !nq.getKeywords().isEmpty()) {
            List<Predicate> ors = new ArrayList<>();
            for (String kw : nq.getKeywords()) {
                String like = "%" + kw + "%";
                ors.add(cb.like(cb.lower(root.get("businessName")), like));
                ors.add(cb.like(cb.lower(svc.get("serviceName")), like));
                ors.add(cb.like(cb.lower(root.get("city")), like));
                ors.add(cb.like(cb.lower(root.get("state")), like));
            }
            kwOr = cb.or(ors.toArray(new Predicate[0]));
        }

        // If no location, require keywords; if location present, keywords optional
        if (kwOr != null && !hasLocation) {
            must.add(kwOr);
        }

        long total = countByFilters(nq, hasLocation, adminOnly);

        int candidateCap = Math.max(200, Math.min(2000, pageable.getPageSize() * 20));
        cq.select(root).where(must.toArray(new Predicate[0])).distinct(true);
        List<Business> candidates = em.createQuery(cq)
                .setMaxResults(candidateCap)
                .getResultList();

        boolean usedFallback = false;
        if (candidates.isEmpty() && kwOr != null) {
            candidates = fallbackCandidates(nq, adminOnly, candidateCap);
            usedFallback = true;
            total = candidates.size();
        }

        // score + sort
        boolean locPresent = nq.getZip()!=null || nq.getCity()!=null || nq.getState()!=null;
        List<WeightedBusinessHitDto> hits = new ArrayList<>(candidates.size());
        for (Business b : candidates) {
            var sb = score(b, nq, locPresent);
            if (sb.total <= 0) continue;
            hits.add(WeightedBusinessHitDto.builder()
                    .business(businessMapper.toDto(b))
                    .score(round2(sb.total))
                    .wordWeights(order(sb.wordContrib))
                    .fieldWeights(order(sb.fieldContrib))
                    .build());
        }
        hits.sort(Comparator.comparingDouble(WeightedBusinessHitDto::getScore).reversed());

        int from = (int) pageable.getOffset();
        int to = Math.min(from + pageable.getPageSize(), hits.size());
        List<WeightedBusinessHitDto> pageContent = from >= hits.size() ? List.of() : hits.subList(from, to);
        long pageTotal = usedFallback ? hits.size() : total;

        return new PageImpl<>(pageContent, pageable, pageTotal);
    }

    // Back-compat if you still need the old signature
    @Transactional(readOnly = true)
    public List<WeightedBusinessHitDto> searchWeighted(String q, int limit) {
        return searchWeighted(q, org.springframework.data.domain.PageRequest.of(0, limit)).getContent();
    }

    // -------- COUNT mirrors search filters (recomputes keyword OR when no location) --------
    private long countByFilters(NlpQuery nq, boolean hasLocation, boolean adminOnly) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> countCq = cb.createQuery(Long.class);
        Root<Business> r = countCq.from(Business.class);
        Join<Business, BusinessService> svc = r.join("service", JoinType.LEFT);

        List<Predicate> where = new ArrayList<>();
        if (adminOnly) where.add(cb.equal(cb.lower(r.get("updatedBy")), "admin"));
        if (nq.getZip() != null)   where.add(cb.equal(cb.lower(r.get("zipCode")), nq.getZip().toLowerCase()));
        if (nq.getCity() != null)  where.add(cb.like(cb.lower(r.get("city")), "%" + nq.getCity().toLowerCase() + "%"));
        if (nq.getState() != null) addStatePredicate(cb, r.get("state"), where, nq.getState());

        if (!hasLocation && nq.getKeywords() != null && !nq.getKeywords().isEmpty()) {
            List<Predicate> ors = new ArrayList<>();
            for (String kw : nq.getKeywords()) {
                String like = "%" + kw + "%";
                ors.add(cb.like(cb.lower(r.get("businessName")), like));
                ors.add(cb.like(cb.lower(svc.get("serviceName")), like));
                ors.add(cb.like(cb.lower(r.get("city")), like));
                ors.add(cb.like(cb.lower(r.get("state")), like));
            }
            where.add(cb.or(ors.toArray(new Predicate[0])));
        }
        countCq.select(cb.countDistinct(r)).where(where.toArray(new Predicate[0]));
        return Optional.ofNullable(em.createQuery(countCq).getSingleResult()).orElse(0L);
    }

    // -------- fallback respects adminOnly --------
    private List<Business> fallbackCandidates(NlpQuery nq, boolean adminOnly, int limit) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Business> cq = cb.createQuery(Business.class);
        Root<Business> root = cq.from(Business.class);
        root.fetch("service", JoinType.LEFT);
        root.fetch("owner", JoinType.LEFT);

        List<Predicate> where = new ArrayList<>();
        if (adminOnly) where.add(cb.equal(cb.lower(root.get("updatedBy")), "admin"));
        if (nq.getZip() != null)   where.add(cb.equal(cb.lower(root.get("zipCode")), nq.getZip().toLowerCase()));
        if (nq.getCity() != null)  where.add(cb.like(cb.lower(root.get("city")), "%" + nq.getCity().toLowerCase() + "%"));
        if (nq.getState() != null) addStatePredicate(cb, root.get("state"), where, nq.getState());

        cq.select(root).distinct(true);
        if (!where.isEmpty()) {
            cq.where(where.toArray(new Predicate[0]));
        } else {
            cq.orderBy(cb.desc(root.get("clickTotalCount")));
        }
        return em.createQuery(cq).setMaxResults(Math.max(50, Math.min(2000, limit))).getResultList();
    }

    // -------- state variants from DB (no hard-coded maps) --------
    private void addStatePredicate(CriteriaBuilder cb, Expression<String> stateExpr,
                                   List<Predicate> where, String inputState) {
        Set<String> variants = deriveStateVariants(inputState);
        if (variants.isEmpty()) {
            where.add(cb.like(cb.lower(stateExpr), "%" + inputState.toLowerCase() + "%"));
            return;
        }
        List<Predicate> ors = new ArrayList<>();
        for (String v : variants) {
            if (v.length() <= 3) { // likely code like "tx", "ny"
                ors.add(cb.equal(cb.lower(stateExpr), v));
            }
            ors.add(cb.like(cb.lower(stateExpr), "%" + v + "%"));
        }
        where.add(cb.or(ors.toArray(new Predicate[0])));
    }

    private Set<String> deriveStateVariants(String raw) {
        if (raw == null || raw.isBlank()) return Set.of();
        String s = normState(raw);
        Set<String> pool = getStatePool();

        Set<String> out = new LinkedHashSet<>();
        out.add(s);
        boolean twoLetter = s.length() == 2;

        for (String p : pool) {
            if (p == null || p.isBlank()) continue;
            String v = normState(p);

            if (v.equals(s) || v.contains(s) || s.contains(v) || lev(s, v) <= 3) {
                out.add(v);
                continue;
            }
            // generic relation: 2-letter code <-> full/multi-word
            if (twoLetter && codeMatchesName(s, v)) {
                out.add(v);
            } else if (!twoLetter && v.length() == 2 && codeMatchesName(v, s)) {
                out.add(v);
            }
        }
        return out;
    }

    private Set<String> getStatePool() {
        Set<String> local = stateCache;
        if (local != null) return local;
        synchronized (this) {
            if (stateCache == null) {
                List<String> rows = em.createQuery(
                        "select distinct lower(b.state) from Business b " +
                                "where b.state is not null and trim(b.state) <> ''", String.class
                ).getResultList();
                stateCache = new HashSet<>(rows);
            }
            return stateCache;
        }
    }

    private static String normState(String x) {
        return x.toLowerCase().trim().replaceAll("\\s+"," ");
    }

    /** Heuristic to relate a 2-letter code with a full state name (generic, no tables). */
    private static boolean codeMatchesName(String code, String name) {
        if (code == null || name == null) return false;
        String c = code.toLowerCase();
        if (c.length() != 2) return false;

        String n = name.toLowerCase().trim();
        String[] parts = n.split("\\s+");
        if (parts.length >= 2) {
            String initials = "" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0);
            if (c.equals(initials)) return true;
            if (parts.length >= 2) {
                String firstTwo = "" + parts[0].charAt(0) + parts[1].charAt(0);
                if (c.equals(firstTwo)) return true;
            }
        } else {
            char first = n.charAt(0);
            char last  = n.charAt(n.length()-1);
            if (c.equals("" + first + last)) return true;
            for (int i = 1; i < n.length(); i++) {
                char ch = n.charAt(i);
                if (isConsonant(ch)) {
                    if (c.equals("" + first + ch)) return true;
                    break;
                }
            }
            if (n.length() >= 2) {
                String firstTwo = n.substring(0, 2);
                if (c.equals(firstTwo)) return true;
            }
        }
        return false;
    }

    private static boolean isConsonant(char ch) {
        return ch >= 'a' && ch <= 'z' && "aeiou".indexOf(ch) < 0;
    }

    // ---------- scoring ----------
    private record ScoreBreakdown(double total, Map<String, Double> wordContrib, Map<String, Double> fieldContrib) {}

    private ScoreBreakdown score(Business b, NlpQuery nq, boolean hasLocation) {
        Map<String, Double> wordContrib = new HashMap<>();
        Map<String, Double> fieldContrib = new HashMap<>();

        String bn = nz(b.getBusinessName());
        String sn = b.getService() != null ? nz(b.getService().getServiceName()) : "";
        String city = nz(b.getCity());
        String state = nz(b.getState());
        String zip = nz(b.getZipCode());

        double total = 0.0;
        if (nq.getZip() != null && zip.equalsIgnoreCase(nq.getZip())) total += BOOST_ZIP_EQ;
        if (hasLocation) total += BOOST_LOC_ANY;

        List<String> words = nq.getKeywords() == null ? List.of() : nq.getKeywords();
        for (String w : words) {
            double base = baseWordWeight(w);
            double wScore = 0.0;

            double sBN = matchStrength(bn, w);
            double sSN = matchStrength(sn, w);
            double sCT = matchStrength(city, w);
            double sST = matchStrength(state, w);

            if (sBN > 0) { double v = base * W_BUSINESS_NAME * sBN; wScore += v; fieldContrib.merge("businessName", v, Double::sum); }
            if (sSN > 0) { double v = base * W_SERVICE_NAME  * sSN; wScore += v; fieldContrib.merge("serviceName",  v, Double::sum); }
            if (sCT > 0) { double v = base * W_CITY          * sCT; wScore += v; fieldContrib.merge("city",         v, Double::sum); }
            if (sST > 0) { double v = base * W_STATE         * sST; wScore += v; fieldContrib.merge("state",        v, Double::sum); }

            if (wScore > 0) { total += wScore; wordContrib.merge(w, wScore, Double::sum); }
        }

        if (b.getClickTotalCount() != null) {
            double pop = Math.log1p(Math.max(0L, b.getClickTotalCount()));
            total += pop; fieldContrib.merge("popularity", pop, Double::sum);
        }

        return new ScoreBreakdown(total, wordContrib, fieldContrib);
    }

    private static double matchStrength(String hay, String word) {
        if (hay == null || word == null || hay.isEmpty() || word.isEmpty()) return 0.0;
        String h = hay.toLowerCase(); String w = word.toLowerCase();
        if (h.contains(w)) return 1.0;
        for (String tok : TOKEN_SPLIT.split(h)) { if (lev1(tok, w)) return 0.5; }
        return 0.0;
    }

    private static boolean lev1(String a, String b) {
        int la = a.length(), lb = b.length();
        if (Math.abs(la - lb) > 1) return false;
        int i=0, j=0, edits=0;
        while (i<la && j<lb) {
            if (a.charAt(i)==b.charAt(j)) { i++; j++; continue; }
            if (++edits>1) return false;
            if (la>lb) i++; else if (lb>la) j++; else { i++; j++; }
        }
        return edits + (la-i) + (lb-j) <= 1;
    }

    private static int lev(String a, String b) {
        int m = a.length(), n = b.length();
        if (m == 0) return n; if (n == 0) return m;
        int[] prev = new int[n+1], cur = new int[n+1];
        for (int j=0;j<=n;j++) prev[j]=j;
        for (int i=1;i<=m;i++) {
            cur[0]=i; char ca=a.charAt(i-1);
            for (int j=1;j<=n;j++) {
                int cost = (ca==b.charAt(j-1))?0:1;
                cur[j]=Math.min(Math.min(cur[j-1]+1, prev[j]+1), prev[j-1]+cost);
            }
            int[] tmp=prev; prev=cur; cur=tmp;
        }
        return prev[n];
    }

    private static String nz(String s) { return s == null ? "" : s; }
    private static double baseWordWeight(String w) {
        int len = (w == null) ? 0 : w.length();
        if (len <= 2) return 0.0;
        return Math.min(3.0, 0.75 + Math.log1p(len)); // longer words => slightly higher base
    }
    private static Map<String, Double> order(Map<String, Double> m) {
        var out = new LinkedHashMap<String, Double>();
        m.entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEach(e -> out.put(e.getKey(), Math.round(e.getValue() * 100.0) / 100.0));
        return out;
    }
    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }

    private static String capWords(String q, int maxWords) {
        if (q == null) return null;
        String[] parts = q.trim().split("\\s+");
        if (parts.length <= maxWords) return q.trim();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxWords; i++) {
            if (i > 0) sb.append(' ');
            sb.append(parts[i]);
        }
        return sb.toString();
    }
}
