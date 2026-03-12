package com.trivine.llc.api.repository;

import com.trivine.llc.api.entity.BusinessOwner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BusinessOwnerRepository extends JpaRepository<BusinessOwner, Long> {
    Optional<BusinessOwner> findByPersonalEmail(String personalEmail);

    Optional<BusinessOwner> findByLoginUserId(Long loginUserId); // ✅ NEW

    // ✅ For your current duplicate situation (same email repeated many times)
    Optional<BusinessOwner> findFirstByPersonalEmailIgnoreCaseOrderByOwnerIdAsc(String personalEmail);

}