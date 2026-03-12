package com.trivine.llc.api.repository;

import com.trivine.llc.api.entity.Company;
import com.trivine.llc.api.entity.RegisteredAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegisteredAgentRepository extends JpaRepository<RegisteredAgent, Long> {
    Optional<RegisteredAgent> findByCompany(Company company);
    Optional<RegisteredAgent> findByCompany_CompanyId(Long companyId);


}

