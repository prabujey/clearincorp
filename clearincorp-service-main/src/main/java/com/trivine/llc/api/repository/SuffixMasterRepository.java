package com.trivine.llc.api.repository;

import com.trivine.llc.api.entity.SuffixMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SuffixMasterRepository extends JpaRepository<SuffixMaster, Long> {
    Optional<SuffixMaster> findBySuffix(String suffix);
    SuffixMaster findBySuffixId(Long suffixId);
    @Query("select s.suffix from SuffixMaster s where s.active = true order by s.suffix")
    List<String> findAllActiveSuffixes();

}