package com.trivine.llc.api.repository;

import com.trivine.llc.api.entity.AuditClick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AuditClickRepository extends JpaRepository<AuditClick, Long> {

    boolean existsByOwner_OwnerIdAndBusiness_BusinessId(Long ownerId, Long businessId);

    boolean existsByBusiness_BusinessIdAndSessionId(Long businessId, String sessionId);

    @Query("""
        select count(distinct ac.owner.ownerId)
        from AuditClick ac
        where ac.business.businessId = :businessId and ac.owner.ownerId is not null
    """)
    long countDistinctOwners(Long businessId);

    @Query("""
        select count(ac)
        from AuditClick ac
        where ac.business.businessId = :businessId and ac.owner.ownerId is null
    """)
    long countAnonymous(Long businessId);
}