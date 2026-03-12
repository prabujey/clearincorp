package com.trivine.llc.api.repository;

import com.trivine.llc.api.entity.CompanyFiling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyFilingRepository extends JpaRepository<CompanyFiling, Long> {

        @Query("""
    SELECT cf FROM CompanyFiling cf  WHERE cf.company.companyId = :companyId  AND cf.isActive = true  ORDER BY cf.createdOn DESC
    """)
        Optional<CompanyFiling> findLatestActiveByCompanyId(@Param("companyId") Long companyId);
    }

