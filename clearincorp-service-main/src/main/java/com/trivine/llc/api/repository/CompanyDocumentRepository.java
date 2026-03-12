package com.trivine.llc.api.repository;


import com.trivine.llc.api.dto.response.DocumentResponseDto;
import com.trivine.llc.api.entity.CompanyDocuments;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyDocumentRepository extends JpaRepository<CompanyDocuments, Long> {

    @Query("SELECT c FROM CompanyDocuments c WHERE c.company.companyId = :companyId")
    Optional<CompanyDocuments> findByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT cd FROM CompanyDocuments cd WHERE cd.company.id = :companyId AND cd.documentMaster.documentTypeId = :documentTypeId")
    Optional<CompanyDocuments> findByCompanyIdAndDocumentTypeId(
            @Param("companyId") Long companyId,
            @Param("documentTypeId") Long documentTypeId);

    @Modifying
    @Query("DELETE FROM CompanyDocuments cd WHERE cd.company.id = :companyId AND cd.documentMaster.documentTypeId = :documentTypeId")
    void deleteByCompanyIdAndDocumentTypeId(
            @Param("companyId") Long companyId,
            @Param("documentTypeId") Long documentTypeId);

    @Query("SELECT cd FROM CompanyDocuments cd JOIN FETCH cd.documentMaster dm JOIN cd.company c")
    List<CompanyDocuments> findAllWithCompanyAndDocumentMaster();



        @Query("SELECT d FROM CompanyDocuments d " +
                "JOIN FETCH d.company c " +
                "JOIN FETCH d.documentMaster dm " +
                "WHERE (:companyName IS NULL OR LOWER(c.companyName) LIKE LOWER(CONCAT('%', :companyName, '%'))) " +
                "AND (:state IS NULL OR LOWER(c.state) LIKE LOWER(CONCAT('%', :state, '%'))) " +
                "AND (:typeName IS NULL OR LOWER(dm.typeName) LIKE LOWER(CONCAT('%', :typeName, '%')))")
        Page<CompanyDocuments> searchWithJoins(@Param("companyName") String companyName,
                                               @Param("state") String state,
                                               @Param("typeName") String typeName,
                                               Pageable pageable);


        @Query("""
        select new com.trivine.llc.api.dto.response.DocumentResponseDto(
            concat(
                c.companyName,
                case
                    when sm.suffix is null or sm.suffix = '' then ''
                    else concat(' ', sm.suffix)
                end
            ),
            c.state,
            dm.typeName,
            c.companyId,
            false,
            cd.uploadedAt
        )
        from CompanyDocuments cd
        join cd.company c
        join cd.documentMaster dm
        left join c.suffixMaster sm
        where c.regForm3 = true
          and c.loginUser.loginUserId = :loginUserId
          and (:companyId is null or c.companyId = :companyId)
        """)
        List<DocumentResponseDto> findUserFileDetailsOptimized(Long loginUserId, Long companyId);



}
