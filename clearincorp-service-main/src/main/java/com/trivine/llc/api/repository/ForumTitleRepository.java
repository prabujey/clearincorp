package com.trivine.llc.api.repository;

import com.trivine.llc.api.entity.ForumTitle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ForumTitleRepository extends JpaRepository<ForumTitle, Long> {
    Page<ForumTitle> findByTopicId(Long topicId, Pageable pageable);
    Page<ForumTitle> findByTopicIdOrderByTitleIdDesc(Long topicId, Pageable pageable);
    Page<ForumTitle> findByTopicIdOrderByCreatedAtDesc(Long topicId, Pageable pageable);
    boolean existsByTitleIgnoreCase(String title);
    boolean existsByTitleIgnoreCaseAndTitleIdNot(String title, Long titleId);

    Optional<ForumTitle> findByTitleIgnoreCase(String title);
    Optional<ForumTitle> findByTitleIgnoreCaseAndTitleIdNot(String title, Long titleId);

}
