package com.trivine.llc.api.repository;

import com.trivine.llc.api.entity.RegisteredAgentMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegisteredAgentMasterRepository extends JpaRepository<RegisteredAgentMaster, Long> {
    Optional<RegisteredAgentMaster> findFirstByStateAndIsActiveTrue(String state);
}