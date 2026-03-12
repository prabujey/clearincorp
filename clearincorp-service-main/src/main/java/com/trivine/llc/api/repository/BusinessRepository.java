package com.trivine.llc.api.repository;

import com.trivine.llc.api.entity.Business;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BusinessRepository extends JpaRepository<Business, Long> {

    List<Business> findByService_ServiceId(Long serviceId);
    @Query("""
        SELECT b FROM Business b
        WHERE LOWER(b.updatedBy) = 'admin'
        AND (:serviceId IS NULL OR b.service.serviceId = :serviceId)
        AND (:zip IS NULL OR b.zipCode = :zip)
        """)
    Page<Business> findAdminBusinesses(@Param("serviceId") Long serviceId,@Param("zip") String zip,Pageable pageable);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
                 update Business b
                    set b.clickOwnerCount = coalesce(b.clickOwnerCount, 0) + 1,
                        b.clickTotalCount = coalesce(b.clickTotalCount, 0) + 1
                  where b.businessId = :id
              """)
    void incOwnerClickCounters(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
                 update Business b
                    set b.clickAnonCount  = coalesce(b.clickAnonCount, 0) + 1,
                        b.clickTotalCount = coalesce(b.clickTotalCount, 0) + 1
                  where b.businessId = :id
              """)
    void incAnonClickCounters(@Param("id") Long id);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Business b
           set b.updatedBy = :updatedBy,
               b.updatedOn = CURRENT_TIMESTAMP
         where b.businessId = :businessId
           and b.updatedOn is null
    """)
    int markVerified(@Param("businessId") Long businessId,
                     @Param("updatedBy") String updatedBy);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Business b
           set b.updatedBy = null,
               b.updatedOn = null
         where b.businessId = :businessId
           and b.updatedOn is not null
    """)
    void unverify(@Param("businessId") Long businessId);

    boolean existsById(Long id);


    Page<Business> findByService_ServiceIdAndZipCode(Long serviceId, String zipCode, Pageable pageable);

    Page<Business> findByService_ServiceId(Long serviceId, Pageable pageable);

    Page<Business> findByZipCode(String zipCode, Pageable pageable);

    @Query("""
   select
     sum(case when lower(b.updatedBy) = 'admin' then 1 else 0 end) as verifiedCount,
     sum(case when b.updatedBy is null then 1 else 0 end)         as unverifiedCount,
     count(b)                                                     as totalCount
   from Business b
   where (:serviceId is null or b.service.serviceId = :serviceId)
     and (:zip      is null or b.zipCode = :zip)
   """)
    com.trivine.llc.api.repository.projection.VerifyCounts countVerifyBuckets(
            @Param("serviceId") Long serviceId,
            @Param("zip")       String zip);


    @Query("""
    SELECT b FROM Business b
    WHERE (:serviceId IS NULL OR b.service.serviceId = :serviceId)
      AND (:zip IS NULL OR :zip = '' OR b.zipCode = :zip)
    """)
    Page<Business> findByAll(@Param("serviceId") Long serviceId,
                                         @Param("zip") String zip,
                                         Pageable pageable);


    List<Business> findByOwner_OwnerId(Long ownerId);


    @Query("""
  select b from Business b
  join fetch b.owner o
  join fetch b.service s
  where o.ownerId = :ownerId
    and b.updatedBy is null
""")
    List<Business> findInProgressByOwnerId(@Param("ownerId") Long ownerId);

    @Query("""
  select b from Business b
  join fetch b.owner o
  join fetch b.service s
  where o.ownerId = :ownerId
    and lower(b.updatedBy) = 'admin'
""")
    List<Business> findRegisteredByOwnerId(@Param("ownerId") Long ownerId);

    @Query("""
  select b from Business b
  join fetch b.owner o
  join fetch b.service s
  where lower(b.createdBy) = 'admin'
""")
    List<Business> findAllAdminCreated();

    @Modifying
    @Query(value = """
  UPDATE business
  SET
    reject = 1,
    updated_by = CASE
      WHEN LOWER(updated_by) = 'admin' THEN NULL
      ELSE updated_by
    END,
    updated_on = NOW(6)
  WHERE business_id = :businessId
""", nativeQuery = true)
    int markRejectedAndMaybeClearAdmin(@Param("businessId") Long businessId);


}

