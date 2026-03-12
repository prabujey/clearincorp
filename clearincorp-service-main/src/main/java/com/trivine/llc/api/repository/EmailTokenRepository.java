package com.trivine.llc.api.repository;

import com.trivine.llc.api.entity.EmailToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailTokenRepository extends JpaRepository<EmailToken, Long> {
    Optional <EmailToken> findFirstByEmailOrderByCreatedOnDesc(String email);
}