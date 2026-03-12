
package com.trivine.llc.api.repository;

import com.trivine.llc.api.entity.ForumPost;
import com.trivine.llc.api.entity.ForumTopic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ForumPostRepository extends JpaRepository<ForumPost, Long> {

    @EntityGraph(attributePaths = {"loginUser", "topic"})
    Page<ForumPost> findByTopicOrderByPinnedDescLastActivityAtDesc(
            ForumTopic topic, Pageable pageable);


    @EntityGraph(attributePaths = {"loginUser", "topic"})
    Page<ForumPost> findByLoginUser_LoginUserIdOrderByLastActivityAtDesc(
            Long loginUserId, Pageable pageable);

    @EntityGraph(attributePaths = {"loginUser", "topic"})
    @Query("""
      SELECT p FROM ForumPost p
      ORDER BY
        CASE WHEN p.pinnedBy.loginUserId = :loginUserId THEN 0 ELSE 1 END,
        p.pinned DESC,
        p.lastActivityAt DESC
    """)
    Page<ForumPost> findAllOrderByPinnedByMeFirst(
            @Param("loginUserId") Long loginUserId, Pageable pageable);

    boolean existsByTitle_TitleId(Long titleId);

    @EntityGraph(attributePaths = {"loginUser", "topic", "title"})
    Page<ForumPost> findByTitle_TitleIdAndDeletedFalseOrderByCreatedAtDesc(Long titleId, Pageable pageable);
}
