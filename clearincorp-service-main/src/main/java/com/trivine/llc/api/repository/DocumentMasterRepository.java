package com.trivine.llc.api.repository;

import com.trivine.llc.api.dto.response.DocumentMasterDto;
import com.trivine.llc.api.entity.DocumentMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentMasterRepository extends JpaRepository<DocumentMaster, Integer> {
    Optional<DocumentMaster> findByTypeName(String typeName);
    @Query("""
        select new com.trivine.llc.api.dto.response.DocumentMasterDto(d.documentTypeId, d.typeName)
        from DocumentMaster d
        order by d.typeName
    """)
    List<DocumentMasterDto> findAllAsDto();
}