package com.trivine.llc.api.repository;

import com.trivine.llc.api.entity.UserCompany;
import org.springframework.data.jpa.repository.JpaRepository;



public interface UserCompanyRepository extends JpaRepository<UserCompany,Long> {
}
