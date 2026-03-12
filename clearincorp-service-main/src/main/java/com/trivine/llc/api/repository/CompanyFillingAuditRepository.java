package com.trivine.llc.api.repository;

import com.trivine.llc.api.entity.CompanyFillingAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyFillingAuditRepository extends JpaRepository<CompanyFillingAudit, Long> {
    @Modifying
    @Query("UPDATE CompanyFillingAudit c SET c.isActive = false WHERE c.companyId = :companyId AND c.isActive = true")
    void deactivateOldRecords(@Param("companyId") Long companyId);
}
