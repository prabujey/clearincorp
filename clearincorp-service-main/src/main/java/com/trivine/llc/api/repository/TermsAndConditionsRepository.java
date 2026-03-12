package com.trivine.llc.api.repository;

import com.trivine.llc.api.entity.TermsAndConditions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TermsAndConditionsRepository extends JpaRepository<TermsAndConditions, Long> {
    Optional<TermsAndConditions> findByFormType(String formType);
}
