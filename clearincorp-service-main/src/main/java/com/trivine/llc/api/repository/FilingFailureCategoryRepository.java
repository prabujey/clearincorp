package com.trivine.llc.api.repository;

import com.trivine.llc.api.entity.FilingFailureCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FilingFailureCategoryRepository extends JpaRepository<FilingFailureCategory, Integer> {
    // Custom queries can go here if needed
    FilingFailureCategory findByFilingFailureCategoryId(Integer filingFailureCategoryId);
}

