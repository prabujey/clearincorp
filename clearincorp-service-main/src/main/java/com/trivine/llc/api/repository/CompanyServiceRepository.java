package com.trivine.llc.api.repository;

import com.trivine.llc.api.entity.Company;
import com.trivine.llc.api.entity.CompanyServices;
import com.trivine.llc.api.entity.ServiceMaster;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Transactional
public interface CompanyServiceRepository extends JpaRepository<CompanyServices, Long> {
        @Query("SELECT cs FROM CompanyServices cs WHERE cs.company.companyId = :companyId AND cs.trueFalse = TRUE ")
        List<CompanyServices> findActiveCompanyServices(@Param("companyId") Long companyId);

        @Query("SELECT cs FROM CompanyServices cs WHERE cs.company.companyId = :companyId AND cs.trueFalse = TRUE AND cs.payment IS NULL")
        List<CompanyServices> findActiveUnPaidCompanyServices(@Param("companyId") Long companyId);

        Optional<CompanyServices> findByCompanyAndServiceMaster(Company company, ServiceMaster serviceMaster);

        @Query("SELECT cs FROM CompanyServices cs WHERE cs.company.companyId = :companyId")
        Optional<List<CompanyServices>> findCompanyServices(@Param("companyId") Long companyId);

        List<CompanyServices> findByPayment_PaymentId(Long paymentId);

        @Query("SELECT cs FROM CompanyServices cs JOIN FETCH cs.serviceMaster WHERE cs.company.companyId = :companyId")
        Optional<List<CompanyServices>> findWithServiceMaster(@Param("companyId") Long companyId);

        @Query("""
                    SELECT cs
                    FROM CompanyServices cs
                    WHERE cs.company.companyId = :companyId
                      AND cs.serviceMaster.serviceName = 'EIN Registration'
                      AND cs.payment IS NOT NULL
                      AND cs.serviceCompletionDate IS NULL
                """)
        CompanyServices findUncompletedPaidEINRegistrationByCompanyId(@Param("companyId") Long companyId);

        @Query(
                value = """
                        SELECT cs
                        FROM CompanyServices cs
                        JOIN cs.company c
                        WHERE c.regForm4 = true
                          AND cs.serviceMaster.serviceName = 'EIN Registration'
                          AND cs.payment IS NOT NULL
                          AND cs.serviceCompletionDate IS NULL
                          AND (:state IS NULL OR TRIM(:state) = '' OR LOWER(c.state) = LOWER(:state))
                          AND (:companyName IS NULL OR TRIM(:companyName) = '' OR LOWER(c.companyName) LIKE LOWER(CONCAT('%', :companyName, '%')))
                          AND (:isExpedited IS NULL OR c.IsExpeditedServiceSelected = :isExpedited)
                        ORDER BY
                          CASE WHEN cs.serviceCreatedDate IS NULL THEN 0 ELSE 1 END,
                          CASE WHEN c.IsExpeditedServiceSelected = true THEN 0 ELSE 1 END,
                          c.lastUpdated ASC
                        """,
                countQuery = """
                        SELECT COUNT(cs)
                        FROM CompanyServices cs
                        JOIN cs.company c
                        WHERE c.regForm4 = true
                          AND cs.serviceMaster.serviceName = 'EIN Registration'
                          AND cs.payment IS NOT NULL
                          AND cs.serviceCompletionDate IS NULL
                          AND (:state IS NULL OR TRIM(:state) = '' OR LOWER(c.state) = LOWER(:state))
                          AND (:companyName IS NULL OR TRIM(:companyName) = '' OR LOWER(c.companyName) LIKE LOWER(CONCAT('%', :companyName, '%')))
                          AND (:isExpedited IS NULL OR c.IsExpeditedServiceSelected = :isExpedited)
                        """
        )
        Page<CompanyServices> findAllUncompletedPaidEinWhereCompanyRegForm4TrueFiltered(
                @Param("state") String state,
                @Param("companyName") String companyName,
                @Param("isExpedited") Boolean isExpedited,
                Pageable pageable
        );
}

