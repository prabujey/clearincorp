package com.trivine.llc.api.service;

import com.trivine.llc.api.dto.NlpQuery;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Lazy + quiet NLP query parser.
 *
 * Notes:
 *  - Make your pipeline bean lazy:
 *      @Bean @Lazy public StanfordCoreNLP stanfordCoreNLP() { ... }
 *  - Mute CoreNLP logs in application.yml:
 *      logging:
 *        level:
 *          root: WARN
 *          org.springframework: INFO
 *          edu.stanford.nlp: ERROR
 *          e.stanford: ERROR
 */
@Component
@RequiredArgsConstructor
public class NlpQueryParser {

    /** Inject pipeline lazily: either use ObjectFactory or @Lazy on the field. */
    private final @Lazy StanfordCoreNLP nlp; // created on first use
    private final LocationLexicon lexicon;

    // US ZIP (12345 or 12345-6789) OR India PIN (560001)
    private static final Pattern ZIP = Pattern.compile("(\\b\\d{5}(?:-\\d{4})?\\b)|(\\b\\d{6}\\b)");

    // caps to avoid OR explosion later
    private static final int MAX_SUGGESTIONS_PER_WORD = 3;
    private static final int MAX_KEYWORDS_TOTAL       = 12;

    public NlpQuery parse(String query) {
        // ----- NLP annotate (pipeline instantiates here the first time) -----
        Annotation ann = new Annotation(query == null ? "" : query);
        nlp.annotate(ann);

        final List<CoreLabel> labels = new ArrayList<>(ann.get(CoreAnnotations.TokensAnnotation.class));
        final int n = labels.size();

        // ----- ZIP -----
        String zip = labels.stream()
                .map(CoreLabel::originalText)
                .filter(t -> ZIP.matcher(t).matches())
                .findFirst().orElse(null);

        // ----- NER spans (aggregate multi-word locations) -----
        String cityRaw  = firstEntitySpan(labels, Set.of("CITY", "LOCATION"));
        String stateRaw = firstEntitySpan(labels, Set.of("STATE_OR_PROVINCE", "LOCATION"));

        // Lexicon normalize/snap (lazy-loaded DB index)
        String lexCity  = firstNonBlank(lexicon.bestCity(cityRaw).orElse(null),  lexicon.suggestCity(cityRaw).orElse(null));
        String lexState = firstNonBlank(lexicon.bestState(stateRaw).orElse(null), lexicon.suggestState(stateRaw).orElse(null));

        String city  = preferLonger(cityRaw,  lexCity);
        String state = preferLonger(stateRaw, lexState);

        // If NER gave something not in DB at all, snap to nearest DB entry
        if (city != null  && !lexicon.hasCity(city))   city  = lexicon.suggestCity(city).orElse(city);
        if (state != null && !lexicon.hasState(state)) state = lexicon.suggestState(state).orElse(state);

        // ----- Keywords (unigrams) by POS: nouns/proper nouns/adjectives/gerunds -----
        List<String> unigrams = labels.stream()
                .map(t -> {
                    String pos = t.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                    if (pos == null) return null;
                    if (!(pos.startsWith("NN") || pos.startsWith("JJ") || "VBG".equals(pos))) return null;
                    String lemma = t.lemma();
                    String w = (lemma == null ? t.originalText() : lemma).toLowerCase();
                    return (w.length() >= 3 && !w.matches("\\d+")) ? w : null;
                })
                .filter(Objects::nonNull)
                .toList();

        // Remove tokens that are clearly location/zip strings to avoid overweighting
        Set<String> drop = new HashSet<>();
        if (city  != null) drop.add(city.toLowerCase());
        if (state != null) drop.add(state.toLowerCase());
        if (zip   != null) drop.add(zip.toLowerCase());

        // ----- Phrases (bi/tri-grams) from contiguous JJ/NN/NNP/VBG sequences -----
        List<String> surface = labels.stream().map(CoreLabel::originalText).toList();
        List<String> posTags = labels.stream().map(t -> t.get(CoreAnnotations.PartOfSpeechAnnotation.class)).toList();
        List<String> phrases = nounishPhrases(surface, posTags, 3);

        // Merge keywords (phrases first), dedupe, and drop location terms
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        for (String p : phrases) if (!shouldDrop(p, drop)) merged.add(p.toLowerCase());
        for (String u : unigrams) if (!shouldDrop(u, drop)) merged.add(u.toLowerCase());
        List<String> keywords = new ArrayList<>(merged);

        // ----- Expand keywords with nearest DB terms (service first, then generic), capped -----
        LinkedHashSet<String> expanded = new LinkedHashSet<>(keywords);
        for (String w : keywords) {
            for (String s : lexicon.suggestService(w, MAX_SUGGESTIONS_PER_WORD)) expanded.add(s);
            for (String s : lexicon.suggestKeyword(w, MAX_SUGGESTIONS_PER_WORD)) expanded.add(s);
            if (expanded.size() >= MAX_KEYWORDS_TOTAL) break;
        }
        List<String> expandedKeywords = expanded.stream().limit(MAX_KEYWORDS_TOTAL).toList();

        return NlpQuery.builder()
                .city(city)
                .state(state)
                .zip(zip)
                .keywords(expandedKeywords)
                .build();
    }

    // ===================== helpers (NER span aggregation, phrases, guards) =====================

    private static String firstEntitySpan(List<CoreLabel> toks, Set<String> tags) {
        StringBuilder span = new StringBuilder();
        boolean open = false;
        for (int i = 0; i < toks.size(); i++) {
            CoreLabel t = toks.get(i);
            String ner = t.get(CoreAnnotations.NamedEntityTagAnnotation.class);
            if (ner != null && tags.contains(ner)) {
                if (!open) { span.setLength(0); open = true; }
                if (span.length() > 0) span.append(' ');
                span.append(t.originalText());
            } else if (open) {
                break; // take first contiguous span only
            }
        }
        String s = span.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static boolean shouldDrop(String candidate, Set<String> drop) {
        if (candidate == null) return true;
        String c = candidate.toLowerCase().trim();
        if (c.isBlank()) return true;
        // if phrase contains a dropped location token entirely, drop it
        for (String d : drop) { if (!d.isBlank() && c.contains(d)) return true; }
        return false;
    }

    /** Extract nounish phrases (up to maxLen) from contiguous JJ/NN/NNP/VBG sequences. */
    private static List<String> nounishPhrases(List<String> surface, List<String> pos, int maxLen) {
        int n = surface.size();
        List<String> out = new ArrayList<>();
        int i = 0;
        while (i < n) {
            int j = i;
            while (j < n && isNounish(pos.get(j))) j++;
            int len = j - i;
            if (len > 0) {
                for (int L = Math.min(maxLen, len); L >= 2; L--) {
                    for (int k = i; k + L <= j; k++) {
                        String ph = join(surface, k, k + L);
                        if (ph.length() >= 3) out.add(ph.toLowerCase());
                    }
                }
                i = j;
            } else i++;
        }
        return out;
    }

    private static boolean isNounish(String p) {
        if (p == null) return false;
        return p.startsWith("NN") || p.startsWith("JJ") || "VBG".equals(p);
    }

    private static String join(List<String> arr, int a, int b) {
        return String.join(" ", arr.subList(a, b));
    }

    private static String firstNonBlank(String... xs) {
        for (String x : xs) if (x != null && !x.isBlank()) return x;
        return null;
    }

    /** Prefer the longer surface (e.g., "texas") over short codes (e.g., "tx"). */
    private static String preferLonger(String human, String normalized) {
        if (human == null) return normalized;
        if (normalized == null) return human;
        String a = human.trim();
        String b = normalized.trim();
        return (a.length() >= b.length()) ? a : b;
    }

    // ================================== Lexicon (lazy DB load) ==================================

    @Component
    @RequiredArgsConstructor
    static class LocationLexicon {
        private final JdbcTemplate jdbc;

        private volatile boolean ready = false;

        private Set<String> cities; // lower
        private Set<String> states;
        private Set<String> zips;

        // BK-trees
        private BKTree tokenTree;    // tokens from business/service names
        private BKTree cityTree;     // full city strings
        private BKTree stateTree;    // full state strings
        private BKTree serviceTree;  // service tokens + full service names

        private static final Pattern SPLIT = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}]+");

        /** Lazy, thread-safe loader. No DB work happens at startup. */
        private void ensureLoaded() {
            if (ready) return;
            synchronized (this) {
                if (ready) return;

                cities = new HashSet<>(jdbc.queryForList(
                        "SELECT DISTINCT LOWER(city) FROM llc.business WHERE city IS NOT NULL AND TRIM(city)<>''", String.class));
                states = new HashSet<>(jdbc.queryForList(
                        "SELECT DISTINCT LOWER(state) FROM llc.business WHERE state IS NOT NULL AND TRIM(state)<>''", String.class));
                zips = new HashSet<>(jdbc.queryForList(
                        "SELECT DISTINCT LOWER(zip_code) FROM llc.business WHERE zip_code IS NOT NULL AND TRIM(zip_code)<>''", String.class));

                List<String> businessNames = jdbc.queryForList(
                        "SELECT DISTINCT business_name FROM llc.business WHERE business_name IS NOT NULL AND TRIM(business_name)<>''",
                        String.class);
                List<String> serviceNames = jdbc.queryForList(
                        "SELECT DISTINCT s.service_name FROM llc.business b JOIN llc.business_service s ON s.service_id=b.service_id " +
                                "WHERE s.service_name IS NOT NULL AND TRIM(s.service_name)<>''",
                        String.class);

                tokenTree   = new BKTree();
                serviceTree = new BKTree();
                cityTree    = new BKTree();
                stateTree   = new BKTree();

                Set<String> tokens = new HashSet<>();
                tokens.addAll(tokens(businessNames));
                tokens.addAll(tokens(serviceNames));
                tokens.stream().filter(t -> t.length() >= 3).forEach(tokenTree::add);

                tokens(serviceNames).stream().filter(t -> t.length() >= 3).forEach(serviceTree::add);
                serviceNames.stream().filter(Objects::nonNull)
                        .map(String::toLowerCase).forEach(serviceTree::add);

                cities.forEach(cityTree::add);
                states.forEach(stateTree::add);

                ready = true;
            }
        }

        private Set<String> tokens(List<String> values) {
            Set<String> out = new HashSet<>();
            for (String v : values) {
                if (v == null) continue;
                for (String t : SPLIT.split(v.toLowerCase())) {
                    if (t.length() >= 3) out.add(t);
                }
            }
            return out;
        }

        // --------- exact + fuzzy helpers (public API ensures lazy load) ---------
        public Optional<String> bestCity(String phrase)  { ensureLoaded(); return bestFromPool(phrase, cities); }
        public Optional<String> bestState(String phrase) { ensureLoaded(); return bestFromPool(phrase, states); }

        public boolean hasCity(String c)  { ensureLoaded(); return c != null && cities.contains(c.toLowerCase()); }
        public boolean hasState(String s) { ensureLoaded(); return s != null && states.contains(s.toLowerCase()); }

        public Optional<String> suggestCity(String like)   { ensureLoaded(); return nearest(cityTree, like); }
        public Optional<String> suggestState(String like)  { ensureLoaded(); return nearest(stateTree, like); }

        public List<String> suggestService(String w, int k) { ensureLoaded(); return nearestK(serviceTree, w, k); }
        public List<String> suggestKeyword(String w, int k) { ensureLoaded(); return nearestK(tokenTree,   w, k); }

        private Optional<String> bestFromPool(String phrase, Set<String> pool) {
            if (phrase == null || phrase.isBlank() || pool == null || pool.isEmpty()) return Optional.empty();
            String k = n(phrase);
            if (pool.contains(k)) return Optional.of(k);
            var sw = pool.stream().filter(p -> p.startsWith(k)).findFirst();
            if (sw.isPresent()) return sw;
            var ct = pool.stream().filter(p -> p.contains(k)).findFirst();
            if (ct.isPresent()) return ct;
            int max = k.length() <= 5 ? 1 : 2;
            String best = null; int bestD = Integer.MAX_VALUE;
            for (String p : pool) { int d = BKTree.ld(k, p); if (d < bestD && d <= max) { bestD = d; best = p; } }
            return Optional.ofNullable(best);
        }

        private Optional<String> nearest(BKTree tree, String q) {
            if (tree == null || q == null || q.isBlank()) return Optional.empty();
            int max = (q.length() <= 5) ? 1 : 2;
            List<String> cands = tree.search(q, max);
            return cands.stream()
                    .min(Comparator.comparingInt(a -> BKTree.ld(q.toLowerCase(), a.toLowerCase())));
        }

        private List<String> nearestK(BKTree tree, String q, int k) {
            if (tree == null || q == null || q.isBlank()) return List.of();
            int max = (q.length() <= 5) ? 1 : 2;
            List<String> cands = tree.search(q, max);
            return cands.stream()
                    .sorted(Comparator.comparingInt(a -> BKTree.ld(q.toLowerCase(), a.toLowerCase())))
                    .limit(Math.max(1, k))
                    .toList();
        }

        private String n(String s) { return s == null ? "" : s.toLowerCase().trim().replaceAll("\\s+"," "); }

        // ---------- Minimal BK-tree (Levenshtein) ----------
        private static class BKTree {
            private static class Node {
                final String term;
                final Map<Integer, Node> children = new HashMap<>();
                Node(String term) { this.term = term; }
            }
            private Node root;

            void add(String term) {
                if (term == null || term.isBlank()) return;
                String t = norm(term);
                if (root == null) { root = new Node(t); return; }
                Node cur = root;
                while (true) {
                    int d = ld(t, cur.term);
                    Node nxt = cur.children.get(d);
                    if (nxt == null) { cur.children.put(d, new Node(t)); return; }
                    cur = nxt;
                }
            }

            List<String> search(String query, int maxDist) {
                List<String> out = new ArrayList<>();
                if (root == null || query == null) return out;
                String q = norm(query);
                Deque<Node> stack = new ArrayDeque<>();
                stack.push(root);
                while (!stack.isEmpty()) {
                    Node n = stack.pop();
                    int d = ld(q, n.term);
                    if (d <= maxDist) out.add(n.term);
                    int from = Math.max(1, d - maxDist), to = d + maxDist;
                    for (Map.Entry<Integer, Node> e : n.children.entrySet()) {
                        int edge = e.getKey();
                        if (edge >= from && edge <= to) stack.push(e.getValue());
                    }
                }
                return out;
            }

            private static String norm(String s) { return s.toLowerCase().trim().replaceAll("\\s+", " "); }

            static int ld(String a, String b) {
                int m = a.length(), n = b.length();
                if (m == 0) return n; if (n == 0) return m;
                int[] prev = new int[n+1], cur = new int[n+1];
                for (int j=0;j<=n;j++) prev[j]=j;
                for (int i=1;i<=m;i++) {
                    cur[0]=i;
                    char ca=a.charAt(i-1);
                    for (int j=1;j<=n;j++) {
                        int cost = (ca==b.charAt(j-1))?0:1;
                        cur[j]=Math.min(Math.min(cur[j-1]+1, prev[j]+1), prev[j-1]+cost);
                    }
                    int[] tmp=prev; prev=cur; cur=tmp;
                }
                return prev[n];
            }
        }
    }
}
