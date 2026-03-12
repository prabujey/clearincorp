package com.trivine.llc.api.service;

import com.trivine.llc.api.dto.PostDto;
import com.trivine.llc.api.dto.ReplyDto;
import com.trivine.llc.api.dto.TitleCheckResponse;
import com.trivine.llc.api.dto.TitleDto;
import com.trivine.llc.api.entity.ForumPost;
import com.trivine.llc.api.entity.ForumReply;
import com.trivine.llc.api.entity.ForumTitle;
import com.trivine.llc.api.entity.LoginUser;
import com.trivine.llc.api.mapper.ForumTitleMapper;
import com.trivine.llc.api.mapper.PostMapper;
import com.trivine.llc.api.mapper.ReplyMapper;
import com.trivine.llc.api.repository.ForumPostRepository;
import com.trivine.llc.api.repository.ForumReplyRepository;
import com.trivine.llc.api.repository.ForumTitleRepository;
import com.trivine.llc.api.repository.LoginUserRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ForumTitleService {

    private final ForumTitleRepository titleRepo;
    private final LoginUserRepository userRepo;
    private final ForumPostRepository postRepo;
    private final PostMapper postMapper;
    private final ReplyMapper replyMapper;
    private final ForumTitleMapper titleMapper;
    private final ForumReplyRepository replyRepo;

    /* ===================== helpers ===================== */

    private LoginUser requireUser(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid login_user_id: " + id));
    }

    private void ensureAdmin(LoginUser u) {
        if (!u.getLoginUserId().equals(1L)) {
            throw new SecurityException("Only admin can perform this action.");
        }
    }

    /** Trim and collapse internal whitespace to a single space. */
    private String normalizeTitle(String s) {
        if (s == null) return null;
        return s.trim().replaceAll("\\s+", " ");
    }

    /* ===================== commands ===================== */

    @Transactional
    public ForumTitle createTitle(@NotNull Long loginUserId,
                                  @NotNull Long topicId,
                                  @NotBlank String title,
                                  @NotBlank String descriptionMd) {
        LoginUser creator = requireUser(loginUserId);

        String norm = normalizeTitle(title);
        // GLOBAL duplicate check (across all topics)
        if (titleRepo.existsByTitleIgnoreCase(norm)) {
            throw new IllegalStateException("A title with the same name already exists.");
        }

        ForumTitle t = new ForumTitle();
        t.setTopicId(topicId);
        t.setCreatedBy(creator);
        t.setTitle(norm);
        t.setDescriptionMd(descriptionMd.trim());
        return titleRepo.save(t);
    }

    @Transactional
    public ForumTitle updateTitle(@NotNull Long loginUserId,
                                  @NotNull Long titleId,
                                  String title,
                                  String descriptionMd) {
        ensureAdmin(requireUser(loginUserId));

        ForumTitle t = getTitle(titleId);

        if (title != null && !title.isBlank()) {
            String norm = normalizeTitle(title);
            // GLOBAL duplicate check excluding current row
            boolean dup = titleRepo.existsByTitleIgnoreCaseAndTitleIdNot(norm, t.getTitleId());
            if (dup) {
                throw new IllegalStateException("A title with the same name already exists.");
            }
            t.setTitle(norm);
        }
        if (descriptionMd != null && !descriptionMd.isBlank()) {
            t.setDescriptionMd(descriptionMd.trim());
        }
        return titleRepo.save(t);
    }

    @Transactional
    public void deleteTitle(@NotNull Long loginUserId, @NotNull Long titleId) {
        ensureAdmin(requireUser(loginUserId));
        ForumTitle t = getTitle(titleId);

        boolean usedByPosts   = postRepo.existsByTitle_TitleId(titleId);
        boolean usedByReplies = replyRepo.existsByTitle_TitleId(titleId);
        if (usedByPosts || usedByReplies) {
            throw new IllegalStateException("Title is in use by posts/replies and cannot be deleted.");
        }
        titleRepo.delete(t);
    }

    /* ===================== queries ===================== */

    @Transactional(readOnly = true)
    public ForumTitle getTitle(@NotNull Long titleId) {
        return titleRepo.findById(titleId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid title_id: " + titleId));
    }

    @Transactional(readOnly = true)
    public Page<ForumTitle> listTitles(Long topicId, int page, int size, boolean hasCreatedAt) {
        page = Math.max(page, 0);
        size = (size <= 0 || size > 100) ? 20 : size;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "titleId"));
        if (topicId == null) return titleRepo.findAll(pageable);
        return hasCreatedAt
                ? titleRepo.findByTopicIdOrderByCreatedAtDesc(topicId, pageable)
                : titleRepo.findByTopicIdOrderByTitleIdDesc(topicId, pageable);
    }

    /**
     * Returns TitleDto with posts, each post containing a FULLY NESTED reply tree:
     * replies[] = top-level; each reply has children[] (multi-level).
     * Single batch for all replies (no N+1).
     */
    @Transactional(readOnly = true)
    public TitleDto getTitleWithPosts(Long titleId, Integer page, Integer size) {
        if (titleId == null) throw new IllegalArgumentException("title_id is required");

        ForumTitle t = getTitle(titleId);

        int p = (page == null || page < 0) ? 0 : page;
        int s = (size == null || size <= 0 || size > 100) ? 20 : size;
        Pageable pageable = PageRequest.of(p, s);

        // 1) Fetch posts for the title
        Page<ForumPost> postsPage =
                postRepo.findByTitle_TitleIdAndDeletedFalseOrderByCreatedAtDesc(titleId, pageable);
        List<ForumPost> posts = postsPage.getContent();

        // 2) Map posts first (no replies yet)
        List<PostDto> postDtos = postMapper.toDtoList(posts);

        if (posts.isEmpty()) {
            return titleMapper.toDtoWithPosts(t, postDtos);
        }

        // 3) Fetch ALL replies for these posts in ONE query (ordered by createdAt ASC)
        List<Long> postIds = posts.stream().map(ForumPost::getPostId).toList();
        List<ForumReply> allReplies = replyRepo.findByPost_PostIdInOrderByCreatedAtAsc(postIds);

        if (allReplies.isEmpty()) {
            // still return posts with empty replies
            return titleMapper.toDtoWithPosts(t, postDtos);
        }

        // 4) Map to DTOs
        List<ReplyDto> allDtos = replyMapper.toDtoList(allReplies);

        // 5) Build nested trees:
        //    - byId for fast parent lookup
        //    - rootsByPost to hold top-level per post
        Map<Long, ReplyDto> byId = new LinkedHashMap<>(allDtos.size());
        for (ReplyDto r : allDtos) {
            byId.put(r.getReplyId(), r);
            if (r.getChildren() == null) r.setChildren(new ArrayList<>());
        }

        Map<Long, List<ReplyDto>> rootsByPost = new LinkedHashMap<>();
        for (ReplyDto r : allDtos) {
            Long parentId = r.getParentReplyId();
            if (parentId == null) {
                rootsByPost
                        .computeIfAbsent(r.getPostId(), k -> new ArrayList<>())
                        .add(r);
            } else {
                ReplyDto parent = byId.get(parentId);
                if (parent != null) {
                    parent.getChildren().add(r);
                } else {
                    // orphan-safety: treat as root if parent missing
                    rootsByPost
                            .computeIfAbsent(r.getPostId(), k -> new ArrayList<>())
                            .add(r);
                }
            }
        }

        // 6) Attach nested roots to each PostDto
        for (int i = 0; i < posts.size(); i++) {
            Long pid = posts.get(i).getPostId();
            List<ReplyDto> roots = rootsByPost.getOrDefault(pid, List.of());
            postDtos.get(i).setReplies(roots);
        }

        return titleMapper.toDtoWithPosts(t, postDtos);
    }

    @Transactional(readOnly = true)
    public TitleCheckResponse checkAvailabilityGlobalWithIds(String rawTitle, Long excludeTitleId) {
        if (rawTitle == null || rawTitle.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        String norm = normalizeTitle(rawTitle);

        // Look up the conflicting row (if any), excluding the given id when editing
        Optional<ForumTitle> hit = (excludeTitleId == null)
                ? titleRepo.findByTitleIgnoreCase(norm)
                : titleRepo.findByTitleIgnoreCaseAndTitleIdNot(norm, excludeTitleId);

        if (hit.isPresent()) {
            ForumTitle t = hit.get();
            return new TitleCheckResponse(
                    t.getTitleId(),
                    t.getTopicId(),
                    norm,
                    true,          // exists
                    false          // available
            );
        }
        return new TitleCheckResponse(
                null, null, norm,
                false,           // exists
                true             // available
        );
    }

    @Transactional(readOnly = true)
    public Map<Long, List<ReplyDto>> getTitleThreadGrouped(Long titleId) {
        var all = replyRepo.findByTitle_TitleIdOrderByCreatedAtAsc(titleId);
        var dto = replyMapper.toDtoList(all);
        return dto.stream().collect(Collectors.groupingBy(
                ReplyDto::getParentReplyId, LinkedHashMap::new, Collectors.toList()));
    }
}
