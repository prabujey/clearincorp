package com.trivine.llc.api.repository;

import com.trivine.llc.api.entity.FilingFailure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FilingFailureRepository extends JpaRepository<FilingFailure, Long> {
}

