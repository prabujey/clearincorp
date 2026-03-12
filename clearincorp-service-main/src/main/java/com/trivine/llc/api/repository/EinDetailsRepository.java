package com.trivine.llc.api.repository;

import com.trivine.llc.api.entity.EinDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EinDetailsRepository extends JpaRepository<EinDetails, Long> {
    Optional<EinDetails> findByCompany_CompanyId(Long companyId);

}

