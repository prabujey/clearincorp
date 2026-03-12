package com.trivine.llc.api.repository;

import com.trivine.llc.api.dto.response.UserProgressResponseDto;
import com.trivine.llc.api.entity.FormationStatus;
import com.trivine.llc.api.repository.projection.StatusCountProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FormationStatusRepository extends JpaRepository<FormationStatus, Long> {
    @Query("SELECT fs FROM FormationStatus fs WHERE fs.company.companyId = :companyId AND fs.isActive = true ORDER BY fs.createdOn ASC")
    Optional<FormationStatus> findFirstByCompanyIdAndIsActiveTrue(Long companyId);
    List<FormationStatus>
    findByIsActiveTrueAndStatusFormationStatusNameNotIn(List<String> excludedNames);
//    Page<FormationStatus> findByIsActiveTrueAndStatusFormationStatusNameNotIn(List<String> excludedStatusNames, Pageable pageable);
//
//    Page<FormationStatus> findByIsActiveTrueAndStatusFormationStatusNameContainingIgnoreCaseAndStatusFormationStatusNameNotIn(
//            String status,
//            List<String> excludedStatuses,
//            Pageable pageable
//    );
@Query(
        value = """
    SELECT fs
    FROM FormationStatus fs
    WHERE fs.isActive = true
      AND (:status IS NULL OR TRIM(:status) = '' OR LOWER(fs.status.formationStatusName) LIKE LOWER(CONCAT('%', :status, '%')))
      AND (:state IS NULL OR LOWER(fs.company.state) = LOWER(:state))
      AND (:companyName IS NULL OR LOWER(fs.company.companyName) LIKE LOWER(CONCAT('%', :companyName, '%')))
      AND (:isExpedited IS NULL OR fs.company.IsExpeditedServiceSelected = :isExpedited)
      AND (:excluded IS NULL OR fs.status.formationStatusName NOT IN :excluded)
    ORDER BY
      CASE WHEN fs.company.IsExpeditedServiceSelected = true THEN 0 ELSE 1 END,
      fs.company.lastUpdated ASC
    """,
        countQuery = """
    SELECT COUNT(fs)
    FROM FormationStatus fs
    WHERE fs.isActive = true
      AND (:status IS NULL OR TRIM(:status) = '' OR LOWER(fs.status.formationStatusName) LIKE LOWER(CONCAT('%', :status, '%')))
      AND (:state IS NULL OR LOWER(fs.company.state) = LOWER(:state))
      AND (:companyName IS NULL OR LOWER(fs.company.companyName) LIKE LOWER(CONCAT('%', :companyName, '%')))
      AND (:isExpedited IS NULL OR fs.company.IsExpeditedServiceSelected = :isExpedited)
      AND (:excluded IS NULL OR fs.status.formationStatusName NOT IN :excluded)
    """
)
Page<FormationStatus> searchFormationStatuses(
        @Param("status") String status,
        @Param("state") String state,
        @Param("companyName") String companyName,
        @Param("isExpedited") Boolean isExpedited,
        @Param("excluded") List<String> excluded,
        Pageable pageable
);
    @Query("SELECT s.status.formationStatusName AS statusFormationStatusName, COUNT(s) AS count " +
            "FROM FormationStatus s " +
            "WHERE s.isActive = true AND s.status.formationStatusName NOT IN (:excluded) " +
            "GROUP BY s.status.formationStatusName")
    List<StatusCountProjection> countByStatusExcluding(@Param("excluded") List<String> excluded);

    @Query("""
    SELECT fs
    FROM FormationStatus fs
    WHERE fs.isActive = true
      AND fs.company.loginUser.loginUserId = :loginUserId
      AND fs.createdOn = (
          SELECT MAX(fs2.createdOn)
          FROM FormationStatus fs2
          WHERE fs2.company = fs.company
            AND fs2.isActive = true
      )
""")
    List<FormationStatus> findLatestActiveStatusByLoginUserId(@Param("loginUserId") Long loginUserId);


    @Query("""
    select new com.trivine.llc.api.dto.response.UserProgressResponseDto(
        c.companyId,
        c.companyName,
        coalesce(sm.suffix, ''),
        c.state,
        s.formationStatusName,
        case
            when s.formationStatusName not in ('initial','paid') then 4
            when c.regForm2 = true then 3
            when c.regForm1 = true then 2
            when c.companyDesc is not null then 1
            else 0
        end
    )
    from FormationStatus fs
    join fs.company c
    join fs.status s
    left join c.suffixMaster sm
    where fs.isActive = true
      and c.loginUser.loginUserId = :loginUserId
      and fs.createdOn = (
          select max(fs2.createdOn)
          from FormationStatus fs2
          where fs2.company = c
            and fs2.isActive = true
      )
""")
    List<UserProgressResponseDto> findLatestProgressByLoginUserId(@Param("loginUserId") Long loginUserId);

}
