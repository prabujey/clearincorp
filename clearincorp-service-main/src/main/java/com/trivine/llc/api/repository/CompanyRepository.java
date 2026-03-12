package com.trivine.llc.api.repository;

import com.trivine.llc.api.entity.Company;
import com.trivine.llc.api.entity.LoginUser;
import com.trivine.llc.api.repository.projection.CompanyProgressProjection;
import com.trivine.llc.api.repository.projection.CompanySlim;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long>, JpaSpecificationExecutor<Company> {

    /**
     * @deprecated Use {@link #findById(Long)} instead. This native query is redundant
     * as it performs the same operation as the inherited JpaRepository method.
     */
    @Deprecated
    default Optional<Company> findByCompanyId(Long companyId) {
        return findById(companyId);
    }

    Optional<List<Company>> findByLoginUser(LoginUser loginUser);

    List<Company> findByState(String state);

    List<Company> findByRegForm3True();

    List<Company> findByRegForm3TrueAndLoginUser_LoginUserId(Long loginUserId);

    @Query("""
                SELECT c
                FROM Company c
                LEFT JOIN FETCH c.registeredAgent ra
                LEFT JOIN FETCH c.companyPrincipal cp
                LEFT JOIN FETCH c.managers m
                LEFT JOIN FETCH c.members mm
                WHERE c.id = :companyId
            """)
    Company findCompanyWithRelations(@Param("companyId") Long companyId);

    @Query("""
            SELECT c FROM Company c
            LEFT JOIN FETCH c.suffixMaster s WHERE (:companyName IS NULL OR LOWER(c.companyName) LIKE LOWER(CONCAT('%', :companyName, '%')))
            AND (:state IS NULL OR LOWER(c.state) LIKE LOWER(CONCAT('%', :state, '%')))
            """)
    Page<Company> searchCompanies(@Param("companyName") String companyName,
                                  @Param("state") String state,
                                  Pageable pageable);


    @Query("SELECT COUNT(c) FROM Company c WHERE c.loginUser.loginUserId = :loginUserId")
    long countByLoginUserId(@Param("loginUserId") Long loginUserId);

    @Query("SELECT c FROM Company c " +
            "WHERE c.createdOn >= :startOfDay AND c.createdOn < :endOfDay " +
            "AND (c.regForm1 is null OR c.regForm2 is null OR c.regForm3 is null)")
    List<Company> findCompaniesCreatedExactlyOnDateWithPendingSteps(
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay);


    @Query("""
       select
         c.companyId   as companyId,
         c.companyName as companyName,
         c.loginUser.loginUserId as loginUserId,
         c.state       as state
       from Company c
       where (:principalActivityList is null or c.principalActivity in :principalActivityList)
         and (:states is null or c.state in :states)
         and (:effectiveFrom is null or c.companyEffectiveDate >= :effectiveFrom)
         and (:effectiveTo   is null or c.companyEffectiveDate <= :effectiveTo)
         and (:loginUserId   is null or c.loginUser.loginUserId = :loginUserId)
         and (:search is null or
              lower(concat(
                  coalesce(c.companyName, ''), ' ',
                  coalesce(c.tradeName, ''), ' ',
                  coalesce(c.city, ''), ' ',
                  coalesce(c.principalActivity, '')
              )) like lower(concat('%', :search, '%')))
         and exists (
              select 1
              from FormationStatus fs
                join fs.status sm
              where fs.company = c
                and fs.isActive = true
                and upper(sm.formationStatusName) = 'SUCCESS'
         )
       """)
    Page<CompanySlim> search(
            @Param("principalActivityList") List<String> principalActivityList,
            @Param("states") List<String> states,
            @Param("effectiveFrom") LocalDate effectiveFrom,
            @Param("effectiveTo") LocalDate effectiveTo,
            @Param("search") String search,
            @Param("loginUserId") Long loginUserId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = { "loginUser" })
    @Query("select c from Company c where c.companyId = :id")
    Optional<Company> findLiteById(@Param("id") Long id);



    @Query("""
        SELECT DISTINCT
            c  as company,
            cp as companyPrincipal,
            ra as registeredAgent,
            fs as formationStatus,
            e  as einDetails
        FROM Company c

        LEFT JOIN FETCH c.managers m
        LEFT JOIN FETCH c.members mb
        LEFT JOIN FETCH c.companyServices cs
        LEFT JOIN FETCH cs.serviceMaster sm
        LEFT JOIN FETCH c.loginUser lu
        LEFT JOIN FETCH c.suffixMaster s

        LEFT JOIN CompanyPrincipal cp ON cp.company = c AND cp.isActive = true
        LEFT JOIN RegisteredAgent  ra ON ra.company = c AND ra.isActive = true

        LEFT JOIN FormationStatus fs ON fs.company = c AND fs.isActive = true
        LEFT JOIN FETCH fs.status st

        LEFT JOIN EinDetails e ON e.company = c
        LEFT JOIN FETCH e.reasonForApplying rfa

        WHERE c.companyId = :companyId
    """)
    List<CompanyProgressProjection> findCompanyProgressRows(@Param("companyId") Long companyId);

//    @Modifying(clearAutomatically = true, flushAutomatically = true)
//    @Transactional
//    @Query("""
//            update Company c
//            set c.isNewFlag = true
//            where c.loginUser.loginUserId = :loginUserId
//            and lower(c.companyName) = lower(:companyName)
//            """)
//    int markCompanyNewFlag(@Param("loginUserId") Long loginUserId,
//                           @Param("companyName") String companyName);
//


//    @Query("""
//            select c
//            from Company c
//            where c.loginUser.loginUserId = :loginUserId
//            and c.isNewFlag = false
//            """)
//    List<Company> findByLoginUserIdAndIsNewFlagFalse(@Param("loginUserId") Long loginUserId);

}


