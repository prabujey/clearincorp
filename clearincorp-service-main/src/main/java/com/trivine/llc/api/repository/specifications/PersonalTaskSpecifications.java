package com.trivine.llc.api.repository.specifications;

import com.trivine.llc.api.dto.PersonalTaskFilterDto;
import com.trivine.llc.api.entity.PersonalTask;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class PersonalTaskSpecifications {

    public static Specification<PersonalTask> byFilter(PersonalTaskFilterDto f) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<Predicate>();

            if (f.getLoginUserId() != null) {
                predicates.add(cb.equal(root.get("loginUserId"), f.getLoginUserId()));
            }
            if (f.getPriority() != null) {
                predicates.add(cb.equal(root.get("priority"), f.getPriority()));
            }
            if (f.getCompleted() != null) {
                predicates.add(cb.equal(root.get("completed"), f.getCompleted()));
            }
            if (f.getDueDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dueDate"), f.getDueDateFrom()));
            }
            if (f.getDueDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dueDate"), f.getDueDateTo()));
            }
            if (f.getSearch() != null && !f.getSearch().isBlank()) {
                var like = "%" + f.getSearch().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("taskTitle")), like));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

