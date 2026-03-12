package com.trivine.llc.api.repository;

import com.trivine.llc.api.entity.ReasonForApplying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReasonForApplyingRepository extends JpaRepository<ReasonForApplying, Long> {
    @Query("SELECT r FROM ReasonForApplying r WHERE r.active = true")
    List<ReasonForApplying> findAllActive();
}
