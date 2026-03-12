package com.trivine.llc.api.service;

import com.trivine.llc.api.dto.NlpQuery;
import com.trivine.llc.api.dto.PostDto;
import com.trivine.llc.api.entity.ForumPost;
import com.trivine.llc.api.entity.ForumTitle;
import com.trivine.llc.api.entity.ForumTopic;
import com.trivine.llc.api.entity.LoginUser;
import com.trivine.llc.api.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ForumSearchService {

    private final NlpQueryParser nlpParser;
    private final JdbcTemplate jdbc;
    private final PostMapper postMapper;

    public List<PostDto> search(String rawQuery, int page, int size) {
        if (rawQuery == null || rawQuery.isBlank()) return List.of();

        // 1) NLP keywords
        NlpQuery nlp = nlpParser.parse(rawQuery);
        List<String> keywords = nlp.getKeywords();
        if (keywords.isEmpty()) {
            keywords = List.of(rawQuery.toLowerCase().trim());
        }

        // 2) LIKE block — use ft.title now (not p.title)
        String likeBlock = keywords.stream()
                .map(k -> """
                    (LOWER(ft.title) LIKE ?
                     OR LOWER(p.description_md) LIKE ?
                     OR LOWER(t.topic_name) LIKE ?
                     OR LOWER(r.content_md) LIKE ?
                     OR LOWER(u.first_name) LIKE ?
                     OR LOWER(u.last_name)  LIKE ?)
                    """)
                .collect(Collectors.joining(" OR "));

        // 3) SQL joins forum_title as ft (note p.title_id)
        String sql = """
            SELECT DISTINCT
                   p.post_id,
                   p.description_md,
                   p.is_pinned,
                   p.likes_count,
                   p.views_count,
                   p.reply_count,
                   p.created_at,
                   p.last_activity_at,
                   p.login_user_id,
                   u.first_name, u.last_name,
                   p.topic_id,
                   t.topic_name,
                   ft.title_id      AS ft_title_id,
                   ft.title         AS ft_title,
                   COUNT(DISTINCT r.reply_id) AS reply_hits
            FROM llc.forum_post p
            JOIN llc.forum_topic_master t ON p.topic_id = t.topic_id
            JOIN llc.login_user u         ON p.login_user_id = u.login_user_id
            JOIN llc.forum_title ft       ON p.title_id = ft.title_id
            LEFT JOIN llc.forum_reply r   ON r.post_id = p.post_id AND r.is_deleted = 0
            WHERE p.is_deleted = 0
              AND (%s)
            GROUP BY p.post_id
            ORDER BY reply_hits DESC, p.last_activity_at DESC
            LIMIT ? OFFSET ?
            """.formatted(likeBlock);

        // 4) params
        List<Object> params = new ArrayList<>();
        for (String k : keywords) {
            String pat = "%" + k.toLowerCase() + "%";
            params.add(pat); // ft.title
            params.add(pat); // p.description_md
            params.add(pat); // t.topic_name
            params.add(pat); // r.content_md
            params.add(pat); // u.first_name
            params.add(pat); // u.last_name
        }
        params.add(size);
        params.add(page * size);

        // 5) map
        List<ForumPost> posts = jdbc.query(sql, params.toArray(), (rs, i) -> {
            LoginUser user = new LoginUser();
            user.setLoginUserId(rs.getLong("login_user_id"));
            user.setFirstName(rs.getString("first_name"));
            user.setLastName(rs.getString("last_name"));

            ForumTopic topic = new ForumTopic();
            topic.setTopicId(rs.getLong("topic_id"));
            topic.setTopicName(rs.getString("topic_name"));

            ForumTitle title = new ForumTitle();
            title.setTitleId(rs.getLong("ft_title_id"));
            title.setTitle(rs.getString("ft_title"));

            ForumPost post = new ForumPost();
            post.setPostId(rs.getLong("post_id"));
            post.setLoginUser(user);
            post.setTopic(topic);
            post.setTitle(title); // <-- set relation (ForumTitle)
            post.setDescriptionMd(rs.getString("description_md"));
            post.setPinned(rs.getBoolean("is_pinned"));
            post.setLikesCount(rs.getInt("likes_count"));
            post.setViewsCount(rs.getInt("views_count"));
            post.setReplyCount(rs.getInt("reply_count"));
            post.setCreatedAt(toInstant(rs.getTimestamp("created_at")));
            post.setLastActivityAt(toInstant(rs.getTimestamp("last_activity_at")));
            return post;
        });

        if (posts.isEmpty()) {
            posts = fuzzyBackupSearch(keywords, size);
        }

        final String q = rawQuery;
        final List<String> kw = List.copyOf(keywords);
        posts.sort(Comparator.comparingDouble(p -> -computeRelevance(p, q, kw)));

        return posts.stream()
                .map(postMapper::toDto)
                .limit(size)
                .toList();
    }

    private static Instant toInstant(Timestamp ts) {
        return ts != null ? ts.toInstant() : null;
    }

    /** Fuzzy fallback (also join forum_title and set relation) */
    private List<ForumPost> fuzzyBackupSearch(List<String> keywords, int limit) {
        String sql = """
            SELECT p.post_id,
                   p.description_md,
                   p.is_pinned,
                   p.likes_count,
                   p.views_count,
                   p.reply_count,
                   p.login_user_id,
                   u.first_name, u.last_name,
                   p.topic_id,
                   t.topic_name,
                   ft.title_id AS ft_title_id,
                   ft.title    AS ft_title,
                   p.created_at,
                   p.last_activity_at
            FROM llc.forum_post p
            JOIN llc.forum_topic_master t ON p.topic_id = t.topic_id
            JOIN llc.login_user u         ON p.login_user_id = u.login_user_id
            JOIN llc.forum_title ft       ON p.title_id = ft.title_id
            WHERE p.is_deleted = 0
            """;

        List<ForumPost> all = jdbc.query(sql, (rs, i) -> {
            LoginUser user = new LoginUser();
            user.setLoginUserId(rs.getLong("login_user_id"));
            user.setFirstName(rs.getString("first_name"));
            user.setLastName(rs.getString("last_name"));

            ForumTopic topic = new ForumTopic();
            topic.setTopicId(rs.getLong("topic_id"));
            topic.setTopicName(rs.getString("topic_name"));

            ForumTitle title = new ForumTitle();
            title.setTitleId(rs.getLong("ft_title_id"));
            title.setTitle(rs.getString("ft_title"));

            ForumPost post = new ForumPost();
            post.setPostId(rs.getLong("post_id"));
            post.setLoginUser(user);
            post.setTopic(topic);
            post.setTitle(title); // <-- relation
            post.setDescriptionMd(rs.getString("description_md"));
            post.setPinned(rs.getBoolean("is_pinned"));
            post.setLikesCount(rs.getInt("likes_count"));
            post.setViewsCount(rs.getInt("views_count"));
            post.setReplyCount(rs.getInt("reply_count"));
            post.setCreatedAt(toInstant(rs.getTimestamp("created_at")));
            post.setLastActivityAt(toInstant(rs.getTimestamp("last_activity_at")));
            return post;
        });

        return all.stream()
                .sorted(Comparator.comparingDouble(p -> avgDistance(p, keywords)))
                .limit(limit)
                .toList();
    }

    /** --- relevance helpers --- */

    private double computeRelevance(ForumPost p, String query, List<String> keywords) {
        String titleText = getTitleText(p);
        String text = (titleText + " " + nullToEmpty(p.getDescriptionMd())).toLowerCase();
        double sim = 1.0 / (1 + levenshtein(query.toLowerCase(), text));
        double keywordBoost = keywords.stream()
                .filter(text::contains)
                .count() * 0.3;
        return sim + keywordBoost;
    }

    private double avgDistance(ForumPost p, List<String> keywords) {
        String titleText = getTitleText(p);
        String text = (titleText + " " + nullToEmpty(p.getDescriptionMd())).toLowerCase();
        return keywords.stream()
                .mapToInt(k -> levenshtein(k, text))
                .average()
                .orElse(Double.MAX_VALUE);
    }

    private static String getTitleText(ForumPost p) {
        return p != null && p.getTitle() != null && p.getTitle().getTitle() != null
                ? p.getTitle().getTitle()
                : "";
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private int levenshtein(String a, String b) {
        int m = a.length(), n = b.length();
        int[] prev = new int[n + 1], cur = new int[n + 1];
        for (int j = 0; j <= n; j++) prev[j] = j;
        for (int i = 1; i <= m; i++) {
            cur[0] = i;
            for (int j = 1; j <= n; j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                cur[j] = Math.min(Math.min(prev[j] + 1, cur[j - 1] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev; prev = cur; cur = tmp;
        }
        return prev[n];
    }
}
