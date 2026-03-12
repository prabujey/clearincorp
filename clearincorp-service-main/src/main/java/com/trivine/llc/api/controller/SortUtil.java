package com.trivine.llc.api.controller;

import org.springframework.data.domain.Sort;

import java.util.*;

public final class SortUtil {
    private SortUtil() {}

    /** Parse multi-value sort params like ["field-asc","other-desc"]. */
    public static Sort buildSort(
            List<String> sortParams,
            Set<String> allowedFields,
            Sort defaultSort
    ) {
        if (defaultSort == null) defaultSort = Sort.unsorted();
        if (sortParams == null || sortParams.isEmpty()) return defaultSort;

        List<Sort.Order> orders = new ArrayList<>();
        for (String token : sortParams) {
            if (token == null || token.isBlank()) continue;

            String[] parts = token.split("-", 2);
            String field = parts[0].trim();
            String dir = parts.length > 1 ? parts[1].trim() : "asc";

            System.out.printf("field :%s\n direction: %s\n", field, dir);

            if (field.isEmpty()
                    || field.equalsIgnoreCase("string")
                    || field.equalsIgnoreCase("asc")
                    || field.equalsIgnoreCase("desc")) {
                continue; // ignore swagger noise / invalid
            }
            if (!allowedFields.contains(field)) continue;

            orders.add("desc".equalsIgnoreCase(dir)
                    ? Sort.Order.desc(field)
                    : Sort.Order.asc(field));
        }
        return orders.isEmpty() ? defaultSort : Sort.by(orders);
    }

    /**
     * Map parsed Sort (entity fields) to JPQL expressions (e.g., join aliases).
     * Example: "taskTitle" -> "m.taskTitle", "createdOn" -> "t.createdOn".
     *
     * Only fields present in exprMap will be converted to JpaSort. Others are kept as-is.
     */
    public static Sort mapToAliases(Sort parsed, Map<String, String> exprMap) {
        if (parsed == null || parsed.isUnsorted() || exprMap == null || exprMap.isEmpty()) {
            return parsed;
        }
        Sort result = Sort.unsorted();
        for (Sort.Order o : parsed) {
            String prop = o.getProperty();
            String expr = exprMap.get(prop);
            if (expr != null && !expr.isBlank()) {
                // Use alias-based expression (supports joins, functions, etc.)
                result = result.and(org.springframework.data.jpa.domain.JpaSort.unsafe(o.getDirection(), expr));
            } else {
                // Keep normal entity field
                result = result.and(Sort.by(o));
            }
        }
        return result;
    }
}


