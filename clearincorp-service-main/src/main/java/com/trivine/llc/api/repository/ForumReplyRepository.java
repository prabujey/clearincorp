

package com.trivine.llc.api.repository;

import com.trivine.llc.api.entity.ForumPost;
import com.trivine.llc.api.entity.ForumReply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ForumReplyRepository extends JpaRepository<ForumReply, Long> {
    Page<ForumReply> findByPostAndParentReplyIsNullOrderByCreatedAtAsc(ForumPost post, Pageable pageable);

    Page<ForumReply> findByParentReplyOrderByCreatedAtAsc(ForumReply parent, Pageable pageable);

    long countByPost(ForumPost post);

    List<ForumReply> findByTitle_TitleIdOrderByCreatedAtAsc(Long titleId);

    List<ForumReply> findByParentReply_ReplyIdOrderByCreatedAtAsc(Long parentId);

    boolean existsByTitle_TitleId(Long titleId);

    List<ForumReply> findByPost_PostIdInOrderByCreatedAtAsc(List<Long> postIds);

}