package com.trivine.llc.api.repository.specifications;

// src/main/java/com/trivine/llc/api/repository/CompanySpecifications.java

import com.trivine.llc.api.dto.CompanyFilterDto;
import com.trivine.llc.api.entity.Company;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public final class CompanySpecifications {

    private CompanySpecifications() {}

    public static Specification<Company> byFilter(CompanyFilterDto f) {
        return (root, query, cb) -> {
            var preds = new java.util.ArrayList<Predicate>();

            if (f.getPrincipalActivity() != null && !f.getPrincipalActivity().isBlank()) {
                String like = "%" + f.getPrincipalActivity().trim().toLowerCase() + "%";
                preds.add(cb.like(cb.lower(root.get("principalActivity")), like));
            }

            if (f.getStates() != null && !f.getStates().isEmpty()) {
                preds.add(root.get("state").in(f.getStates()));
            }

            if (f.getEffectiveFrom() != null) {
                preds.add(cb.greaterThanOrEqualTo(root.get("companyEffectiveDate"), f.getEffectiveFrom()));
            }
            if (f.getEffectiveTo() != null) {
                preds.add(cb.lessThanOrEqualTo(root.get("companyEffectiveDate"), f.getEffectiveTo()));
            }

            if (f.getSearch() != null && !f.getSearch().isBlank()) {
                String like = "%" + f.getSearch().trim().toLowerCase() + "%";
                preds.add(cb.or(
                        cb.like(cb.lower(root.get("companyName")), like),
                        cb.like(cb.lower(root.get("tradeName")), like),
                        cb.like(cb.lower(root.get("city")), like),
                        cb.like(cb.lower(root.get("principalActivity")), like)
                ));
            }

            return cb.and(preds.toArray(new Predicate[0]));
        };
    }
}
