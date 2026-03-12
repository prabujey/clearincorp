package com.trivine.llc.api.repository;

import com.trivine.llc.api.entity.LoginAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginAuditRepository extends JpaRepository<LoginAudit, Long> {
}
