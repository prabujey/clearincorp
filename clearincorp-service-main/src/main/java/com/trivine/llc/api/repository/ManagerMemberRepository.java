package com.trivine.llc.api.repository;

import com.trivine.llc.api.entity.Company;
import com.trivine.llc.api.entity.ManagerMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ManagerMemberRepository extends JpaRepository<ManagerMember, Long> {
    // Fetch list of members by company
    Optional< List<ManagerMember>> findByCompany(Company company);

    // Delete members by company
    void deleteByCompany(Company company);
    Optional<List<ManagerMember>> findByCompany_CompanyId(Long companyId);


}
