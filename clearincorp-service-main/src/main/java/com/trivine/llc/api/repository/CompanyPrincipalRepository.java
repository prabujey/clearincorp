package com.trivine.llc.api.repository;

import com.trivine.llc.api.entity.CompanyPrincipal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyPrincipalRepository extends JpaRepository<CompanyPrincipal, Long> {

    @Query( value="SELECT cp FROM CompanyPrincipal cp WHERE cp.company.companyId = :companyId")
    Optional<CompanyPrincipal> findByCompanyId(Long companyId);
}
