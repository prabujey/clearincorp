package com.trivine.llc.api.repository.specifications;

import com.trivine.llc.api.dto.request.AssignedTaskMasterSearchDto;
import com.trivine.llc.api.entity.AssignedTaskMaster;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public final class AssignedTaskMasterSpecs {

    private AssignedTaskMasterSpecs() {}

    public static Specification<AssignedTaskMaster> withFilter(AssignedTaskMasterSearchDto f) {
        return (root, query, cb) -> {
            var preds = new java.util.ArrayList<Predicate>();

            if (f.getCreatedById() != null) {
                // join createdBy (LoginUser) → compare loginUserId
                preds.add(cb.equal(root.join("createdBy").get("loginUserId"), f.getCreatedById()));
            }

            if (f.getPriority() != null) {
                preds.add(cb.equal(root.get("priority"), f.getPriority()));
            }

            if (f.getDueFrom() != null) {
                preds.add(cb.greaterThanOrEqualTo(root.get("dueDate"), f.getDueFrom()));
            }
            if (f.getDueTo() != null) {
                preds.add(cb.lessThanOrEqualTo(root.get("dueDate"), f.getDueTo()));
            }

            if (hasText(f.getQuery())) {
                String like = "%" + f.getQuery().toLowerCase() + "%";
                preds.add(cb.or(
                        cb.like(cb.lower(root.get("taskTitle")), like),
                        cb.like(cb.lower(root.get("description")), like)
                ));
            }

            if (f.getCreatedFrom() != null) {
                preds.add(cb.greaterThanOrEqualTo(root.get("createdOn"), f.getCreatedFrom()));
            }
            if (f.getCreatedTo() != null) {
                preds.add(cb.lessThanOrEqualTo(root.get("createdOn"), f.getCreatedTo()));
            }

            if (f.getUpdatedFrom() != null) {
                preds.add(cb.greaterThanOrEqualTo(root.get("updatedOn"), f.getUpdatedFrom()));
            }
            if (f.getUpdatedTo() != null) {
                preds.add(cb.lessThanOrEqualTo(root.get("updatedOn"), f.getUpdatedTo()));
            }

            return cb.and(preds.toArray(Predicate[]::new));
        };
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
