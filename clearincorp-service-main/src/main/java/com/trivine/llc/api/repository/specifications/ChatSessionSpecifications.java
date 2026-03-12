package com.trivine.llc.api.repository.specifications;


import com.trivine.llc.api.dto.request.ChatSessionFilter;
import com.trivine.llc.api.entity.ChatSession;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class ChatSessionSpecifications {

    public static Specification<ChatSession> withFilter(ChatSessionFilter f) {
        return Specification.where(minRating(f.getMinRating()))
                .and(maxRating(f.getMaxRating()))
                .and(isIssue(f.getIssue()))
                .and(isResolved(f.getResolved()))
                .and(userEmailLike(f.getUserEmail()))
                .and(createdBetween(f.getFrom(), f.getTo()));
    }

    private static Specification<ChatSession> minRating(Byte min) {
        return (root, query, cb) ->
                min == null ? null : cb.greaterThanOrEqualTo(root.get("rating"), min);
    }

    private static Specification<ChatSession> maxRating(Byte max) {
        return (root, query, cb) ->
                max == null ? null : cb.lessThanOrEqualTo(root.get("rating"), max);
    }

    private static Specification<ChatSession> isIssue(Boolean issue) {
        return (root, query, cb) ->
                issue == null ? null : cb.equal(root.get("issue"), issue);
    }

    private static Specification<ChatSession> isResolved(Boolean resolved) {
        return (root, query, cb) ->
                resolved == null ? null : cb.equal(root.get("resolved"), resolved);
    }

    private static Specification<ChatSession> userEmailLike(String email) {
        return (root, query, cb) ->
                (email == null || email.isBlank())
                        ? null
                        : cb.like(cb.lower(root.get("userEmail")), "%" + email.toLowerCase() + "%");
    }

    private static Specification<ChatSession> createdBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return null;
            if (from != null && to != null) {
                return cb.between(root.get("createdAt"), from, to);
            } else if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            } else {
                return cb.lessThanOrEqualTo(root.get("createdAt"), to);
            }
        };
    }
}
