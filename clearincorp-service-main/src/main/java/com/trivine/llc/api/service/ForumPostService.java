
package com.trivine.llc.api.service;

import com.trivine.llc.api.dto.request.PostCreateRequestDto;
import com.trivine.llc.api.dto.response.ForumTopicDto;
import com.trivine.llc.api.entity.ForumPost;
import com.trivine.llc.api.entity.ForumReply;
import com.trivine.llc.api.entity.ForumTopic;
import com.trivine.llc.api.entity.ForumTitle;
import com.trivine.llc.api.entity.LoginUser;
import com.trivine.llc.api.mapper.PostMapper;
import com.trivine.llc.api.repository.ForumPostRepository;
import com.trivine.llc.api.repository.ForumReplyRepository;
import com.trivine.llc.api.repository.ForumTopicRepository;
import com.trivine.llc.api.repository.ForumTitleRepository;
import com.trivine.llc.api.repository.LoginUserRepository;
import com.trivine.llc.api.ws.GenericWsEvent;
import com.trivine.llc.api.ws.PostCreatedEvent;
import com.trivine.llc.api.ws.WsTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ForumPostService {

    private final ForumPostRepository postRepo;
    private final LoginUserRepository userRepo;
    private final ForumReplyRepository replyRepo;
    private final ForumTopicRepository topicRepo;
    private final ForumTitleRepository titleRepo;
    private final PostMapper postMapper;
    private final ApplicationEventPublisher events;
    private static final int REPORT_THRESHOLD = 5;

    private static final Pattern MENTION_PATTERN = Pattern.compile("@([a-zA-Z0-9_]+)");
    private final ConcurrentMap<String, Boolean> likeState = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Boolean> reportState = new ConcurrentHashMap<>();

    /**
     * Lock map to provide per-key synchronization without String.intern() issues.
     * Using Lock objects instead of synchronized(String.intern()) to avoid
     * potential memory leaks and JVM string pool contention.
     */
    private final ConcurrentMap<String, Lock> lockMap = new ConcurrentHashMap<>();

    private Lock getLock(String key) {
        return lockMap.computeIfAbsent(key, k -> new ReentrantLock());
    }

    private Map<String, Object> evt(String type, Long refId, Object payload) {
        return Map.of(
                "type", type,
                "refId", refId,
                "payload", payload,
                "at", Instant.now().toString()
        );
    }

    private void emit(String destination, Object payload) {
        events.publishEvent(new GenericWsEvent(destination, payload));
    }

    // -------------------- Create Post (TITLE -> POST) ------------------------

    @Transactional
    public ForumPost create(Long loginUserId, PostCreateRequestDto req) {
        if (loginUserId == null) {
            throw new IllegalArgumentException("login_user_id is required");
        }

        // Load actor, topic, title
        LoginUser loginUser = userRepo.findById(loginUserId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid login_user_id: " + loginUserId));

        ForumTopic topic = topicRepo.findById(req.getTopicId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid topic_id: " + req.getTopicId()));

        ForumTitle ft = titleRepo.findById(req.getTitleId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid title_id: " + req.getTitleId()));

        ForumPost post = postMapper.toEntity(req, loginUser, topic);
        post.setLoginUser(loginUser);
        post.setTopic(topic);
        post.setTitle(ft);

        if (req.getDescriptionMd() != null && !req.getDescriptionMd().isBlank()) {
            post.setDescriptionMd(processMentions(req.getDescriptionMd()));
        }

        ForumPost saved = postRepo.save(post);

        events.publishEvent(new PostCreatedEvent(
                saved.getPostId(),
                saved.getTitle().getTitleId(),
                postMapper.toDto(saved)
        ));

        return saved;
    }

    // -------------------- Listing / Reads -----------------------------------

    @Transactional(readOnly = true)
    public Page<ForumPost> listByTopic(Long topicId, Long loginUserId, int page, int size) {
        ForumTopic topic = topicRepo.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid topic_id: " + topicId));

        // original query (keeps pinned + lastActivity ordering)
        Page<ForumPost> rawPage = postRepo.findByTopicOrderByPinnedDescLastActivityAtDesc(
                topic,
                PageRequest.of(page, size)
        );

        // filter out posts that this user has reported/hidden
        List<ForumPost> visiblePosts = rawPage.getContent().stream()
                .filter(p -> !isHiddenForUser(p.getHiddenByLoginUserIds(), loginUserId))
                .toList();

        // keep your existing reply-hydration logic
        hydrateReplies(visiblePosts);

        // return only visible posts for this user
        return new PageImpl<>(
                visiblePosts,
                rawPage.getPageable(),
                visiblePosts.size()
        );
    }


    @Transactional(readOnly = true)
    public Page<ForumPost> listByPinned(Long loginUserId, int page, int size) {
        Page<ForumPost> postsPage = postRepo.findAllOrderByPinnedByMeFirst(
                loginUserId, PageRequest.of(page, size));

        hydrateReplies(postsPage.getContent());
        return postsPage;
    }

    @Transactional(readOnly = true)
    public Page<ForumPost> listByLoginUser(Long loginUserId, int page, int size) {
        Page<ForumPost> postsPage = postRepo.findByLoginUser_LoginUserIdOrderByLastActivityAtDesc(
                loginUserId, PageRequest.of(page, size));

        hydrateReplies(postsPage.getContent());
        return postsPage;
    }

    private void hydrateReplies(List<ForumPost> posts) {
        if (posts == null || posts.isEmpty()) return;

        for (ForumPost p : posts) {
            Page<ForumReply> topLevelReplies =
                    replyRepo.findByPostAndParentReplyIsNullOrderByCreatedAtAsc(p, Pageable.unpaged());
            p.getReplies().clear();
            p.getReplies().addAll(topLevelReplies.getContent());
        }
    }

    public Page<ForumPost> listAll(int page, int size) {
        var sort = Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("lastActivityAt"));
        Page<ForumPost> postsPage = postRepo.findAll(PageRequest.of(page, size, sort));
        hydrateReplies(postsPage.getContent());
        return postsPage;
    }

    public ForumPost get(Long id) {
        return postRepo.findById(id).orElseThrow();
    }

    // -------------------- Views / Likes -------------------------------------

    @Transactional
    public int view(Long id) {
        ForumPost p = get(id);
        p.setViewsCount(p.getViewsCount() + 1);
        postRepo.save(p);
        return p.getViewsCount();
    }

    @Transactional
    public int toggleLike(Long loginUserId, Long postId, Long replyId) {
        if (loginUserId == null) throw new IllegalArgumentException("login_user_id is required");
        userRepo.findById(loginUserId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid login_user_id: " + loginUserId));

        boolean hasPost = postId != null;
        boolean hasReply = replyId != null;
        if (hasPost == hasReply) {
            throw new IllegalArgumentException("Provide exactly one of post_id or reply_id.");
        }

        final String key = hasPost ? ("P:" + postId + ":" + loginUserId) : ("R:" + replyId + ":" + loginUserId);

        Lock lock = getLock(key);
        lock.lock();
        try {
            final boolean wasLiked = likeState.getOrDefault(key, false);
            final int delta = wasLiked ? -1 : +1;

            if (hasPost) {
                ForumPost p = postRepo.findById(postId)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid post_id: " + postId));
                int updated = Math.max(0, p.getLikesCount() + delta);
                p.setLikesCount(updated);
                postRepo.save(p);
                likeState.put(key, !wasLiked);

                emit(WsTopics.post(p.getPostId()),
                        evt("postLikeToggled", p.getPostId(),
                                Map.of("postId", p.getPostId(), "likesCount", p.getLikesCount())));
                return updated;
            } else {
                ForumReply r = replyRepo.findById(replyId)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid reply_id: " + replyId));
                int updated = Math.max(0, r.getLikesCount() + delta);
                r.setLikesCount(updated);
                replyRepo.save(r);
                likeState.put(key, !wasLiked);

                Long threadId = (r.getPost() != null) ? r.getPost().getPostId() : null;
                if (threadId != null) {
                    emit(WsTopics.post(threadId),
                            evt("replyLikeToggled", threadId,
                                    Map.of("replyId", r.getReplyId(), "likesCount", r.getLikesCount())));
                }
                return updated;
            }
        } finally {
            lock.unlock();
        }
    }

    public void clearLikeStateForPost(Long postId) {
        likeState.keySet().removeIf(k -> k.startsWith("P:" + postId + ":"));
    }

    public void clearLikeStateForReply(Long replyId) {
        likeState.keySet().removeIf(k -> k.startsWith("R:" + replyId + ":"));
    }

    @Transactional
    public void incReplyCount(ForumPost p) {
        p.setReplyCount(p.getReplyCount() + 1);
        p.setLastActivityAt(Instant.now());
        postRepo.save(p);
    }

    // -------------------- Delete / Pin / Edit --------------------------------

    @Transactional
    public void deleteItem(Long loginUserId, Long postId, Long replyId) {
        if (loginUserId == null) throw new IllegalArgumentException("login_user_id is required");
        LoginUser actor = userRepo.findById(loginUserId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid login_user_id: " + loginUserId));
        boolean isAdminUser = isAdmin(actor);

        boolean hasPost = postId != null;
        boolean hasReply = replyId != null;
        if (hasPost == hasReply) {
            throw new IllegalArgumentException("Provide exactly one of post_id or reply_id.");
        }

        if (hasPost) {
            // Handle post deletion
            ForumPost post = postRepo.findById(postId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid post_id: " + postId));
            if (!isAdminUser && !Objects.equals(post.getLoginUser().getLoginUserId(), loginUserId)) {
                throw new SecurityException("You are not authorized to delete this post");
            }
            postRepo.delete(post);
            emit(WsTopics.post(postId), evt("postDeleted", postId, Map.of("postId", postId)));
        } else {
            // Handle reply deletion
            ForumReply reply = replyRepo.findById(replyId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid reply_id: " + replyId));
            if (!isAdminUser && !Objects.equals(reply.getLoginUser().getLoginUserId(), loginUserId)) {
                throw new SecurityException("You are not authorized to delete this reply");
            }

            // Recursively delete all child replies
            deleteReplyAndChildren(reply);

            ForumPost parentPost = reply.getPost();
            replyRepo.delete(reply);
            if (parentPost != null) {
                parentPost.setReplyCount(Math.max(0, parentPost.getReplyCount() - 1));
                postRepo.save(parentPost);
                emit(WsTopics.post(parentPost.getPostId()),
                        evt("replyDeleted", parentPost.getPostId(), Map.of("replyId", replyId)));
            }
        }
    }


    private void deleteReplyAndChildren(ForumReply reply) {
        if (reply.getChildren() != null) {
            for (ForumReply child : reply.getChildren()) {
                deleteReplyAndChildren(child);
            }
        }
        replyRepo.delete(reply);
    }

    @Transactional
    public ForumPost togglePinned(Long loginUserId, Long postId) {
        ForumPost post = get(postId);

        LoginUser actor = userRepo.findById(loginUserId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid login_user_id: " + loginUserId));

        boolean isPinned = post.isPinned();
        if (!isPinned) {
            post.setPinned(true);
            post.setPinnedAt(Instant.now());
            post.setPinnedBy(actor);
        } else {
            Long pinnedById = post.getPinnedBy() != null ? post.getPinnedBy().getLoginUserId() : null;
            if (!Objects.equals(pinnedById, loginUserId)) {
                throw new SecurityException("Only the Owner who pinned the Post.");
            }
            post.setPinned(false);
            post.setPinnedAt(null);
            post.setPinnedBy(null);
        }
        ForumPost res = postRepo.saveAndFlush(post);

        emit(WsTopics.post(res.getPostId()),
                evt("postPinnedChanged", res.getPostId(), postMapper.toDto(res)));
        return res;
    }

    private LoginUser requireUser(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid login_user_id: " + id));
    }

    /**
     * Checks if the user has admin role using role-based access control (RBAC).
     * Replaces hardcoded ID=1L check with proper role checking.
     */
    private boolean isAdmin(LoginUser user) {
        return user != null
                && user.getRoleId() != null
                && user.getRoleId().getName() != null
                && "ADMIN".equalsIgnoreCase(user.getRoleId().getName());
    }

    private void ensureAdmin(LoginUser u) {
        if (!isAdmin(u)) {
            throw new SecurityException("Only admin can perform this action.");
        }
    }

    @Transactional
    public Object editItem(Long loginUserId, Long titleId, Long postId,
                           Long replyId, String newContentMd, String newTitle, String newDescriptionMd) {

        if (loginUserId == null) throw new IllegalArgumentException("login_user_id is required");
        if (titleId == null) throw new IllegalArgumentException("title_id is required");

        final boolean editingPost  = postId  != null;
        final boolean editingReply = replyId != null;
        final boolean editingTitle = !editingPost && !editingReply;

        LoginUser actor = requireUser(loginUserId);
        boolean isAdmin = actor.getRoleId() != null
                && actor.getRoleId().getName() != null
                && "ADMIN".equalsIgnoreCase(actor.getRoleId().getName());

        if (editingTitle) {
            ForumTitle t = titleRepo.findById(titleId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid title_id: " + titleId));

            boolean isCreator = (t.getCreatedBy() != null)
                    && loginUserId.equals(t.getCreatedBy().getLoginUserId());

            if (!isAdmin) {
                if (!isCreator) {
                    throw new SecurityException("Only admin or the creator can edit this title.");
                }
                Instant created = t.getCreatedAt();
                Instant now = Instant.now();
                if (created != null && Duration.between(created, now).toMinutes() > 10) {
                    throw new IllegalStateException("Title can only be edited by the creator within 10 minutes of creation.");
                }
            }

            boolean changed = false;

            if (newTitle != null && !newTitle.isBlank()) {
                String trimmed = newTitle.trim();
                if (!trimmed.equals(t.getTitle())) {
                    t.setTitle(trimmed);
                    changed = true;
                }
            }
            if (newDescriptionMd != null && !newDescriptionMd.isBlank()) {
                String trimmed = newDescriptionMd.trim();
                if (!trimmed.equals(t.getDescriptionMd())) {
                    t.setDescriptionMd(trimmed);
                    changed = true;
                }
            }
            if (!changed) {
                throw new IllegalArgumentException(
                        "Nothing to update. Provide a non-blank newTitle and/or newDescriptionMd different from current values.");
            }

            ForumTitle saved = titleRepo.save(t);

            emit(WsTopics.title(saved.getTitleId()),
                    evt("titleUpdated", saved.getTitleId(), Map.of(
                            "titleId", saved.getTitleId(),
                            "title", saved.getTitle(),
                            "descriptionMd", saved.getDescriptionMd()
                    )));
            return saved;
        }

        // From here on, editing either a post OR a reply (not both)
        if (editingPost && editingReply) {
            throw new IllegalArgumentException("Provide exactly one of post_id or reply_id.");
        }
        if (newContentMd == null || newContentMd.isBlank()) {
            throw new IllegalArgumentException("newContentMd cannot be blank for post/reply edits.");
        }

        if (editingPost) {
            ForumPost post = postRepo.findById(postId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid post_id: " + postId));

            if (post.getTitle() == null || !post.getTitle().getTitleId().equals(titleId)) {
                throw new IllegalArgumentException("post_id " + postId + " does not belong to title_id " + titleId);
            }
            if (!post.getLoginUser().getLoginUserId().equals(loginUserId)) {
                throw new SecurityException("You can only edit your own post.");
            }

            Instant created = post.getCreatedAt();
            Instant now = Instant.now();
            if (created != null && Duration.between(created, now).toMinutes() > 10) {
                throw new IllegalStateException("You can only edit within 10 minutes of creation.");
            }

            post.setDescriptionMd(processMentions(newContentMd.trim()));
            post.setEditedAt(now);

            ForumPost saved = postRepo.save(post);

            emit(WsTopics.post(saved.getPostId()),
                    evt("postUpdated", saved.getPostId(), postMapper.toDto(saved)));
            return saved;
        }

        ForumReply reply = replyRepo.findById(replyId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid reply_id: " + replyId));

        ForumPost thread = reply.getPost();
        if (thread == null || thread.getTitle() == null || !thread.getTitle().getTitleId().equals(titleId)) {
            throw new IllegalArgumentException("reply_id " + replyId + " does not belong to title_id " + titleId);
        }
        if (!reply.getLoginUser().getLoginUserId().equals(loginUserId)) {
            throw new SecurityException("You can only edit your own reply.");
        }

        Instant created = reply.getCreatedAt();
        Instant now = Instant.now();
        if (created != null && Duration.between(created, now).toMinutes() > 10) {
            throw new IllegalStateException("You can only edit within 10 minutes of creation.");
        }

        reply.setContentMd(processMentions(newContentMd.trim()));
        reply.setEditedAt(now);

        ForumReply saved = replyRepo.save(reply);

        Long threadId = saved.getPost().getPostId();
        emit(WsTopics.post(threadId),
                evt("replyUpdated", threadId, Map.of(
                        "replyId", saved.getReplyId(),
                        "content", saved.getContentMd(),
                        "editedAt", saved.getEditedAt()
                )));
        return saved;
    }

    // -------- Helpers for safe Title deletion (Controller uses these) --------
    public boolean existsByTitleId(Long titleId) {
        return postRepo.existsByTitle_TitleId(titleId);
    }

    public boolean existsReplyByTitleId(Long titleId) {
        return replyRepo.existsByTitle_TitleId(titleId);
    }

    // -------------------- Mentions -------------------------------------------

    public String processMentions(String content) {
        Matcher matcher = MENTION_PATTERN.matcher(content);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String username = matcher.group(1);

            // Use indexed query instead of loading all users (N+1 fix)
            Optional<LoginUser> userOpt = userRepo.findByCombinedName(username.replaceAll("\\s+", ""));

            if (userOpt.isPresent()) {
                LoginUser user = userOpt.get();
                // Escape username for XSS prevention in HTML output
                String safeUsername = escapeHtml(username);
                String replacement = String.format(
                        "<span class='mention' data-user-id='%d'>@%s</span>",
                        user.getLoginUserId(), safeUsername);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement("@" + username));
            }
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Escapes HTML special characters to prevent XSS attacks.
     */
    private String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    public List<ForumTopicDto> getAllTopicIdsAndNames() {
        return topicRepo.findAllLite();
    }

    @Transactional
    public int reportItem(Long loginUserId, Long titleId, Long postId, Long replyId) {
        if (loginUserId == null) {
            throw new IllegalArgumentException("login_user_id is required");
        }

        int nonNull = 0;
        if (titleId != null) nonNull++;
        if (postId != null) nonNull++;
        if (replyId != null) nonNull++;
        if (nonNull != 1) {
            throw new IllegalArgumentException("Provide exactly one of title_id, post_id or reply_id.");
        }

        userRepo.findById(loginUserId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid login_user_id: " + loginUserId));

        // ───────── TITLE ─────────
        if (titleId != null) {
            ForumTitle title = titleRepo.findById(titleId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid title_id: " + titleId));

            if (title.isDeleted()) {
                throw new IllegalStateException("Title already removed.");
            }

            final String key = "T:" + titleId + ":" + loginUserId;
            Lock lock = getLock(key);
            lock.lock();
            try {
                if (Boolean.TRUE.equals(reportState.get(key)) ||
                        isHiddenForUser(title.getHiddenByLoginUserIds(), loginUserId)) {
                    return title.getReportCount();
                }

                int updated = title.getReportCount() + 1;
                title.setReportCount(updated);

                if (updated >= REPORT_THRESHOLD && !title.isDeleted()) {
                    title.setDeleted(true);  // global soft delete if too many reports
                }

                title.setHiddenByLoginUserIds(
                        addUserToHidden(title.getHiddenByLoginUserIds(), loginUserId)
                );

                ForumTitle saved = titleRepo.save(title);
                reportState.put(key, true);

                return saved.getReportCount();
            } finally {
                lock.unlock();
            }
        }

        // ───────── POST ─────────
        if (postId != null) {
            ForumPost post = postRepo.findById(postId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid post_id: " + postId));

            if (post.isDeleted()) {
                throw new IllegalStateException("Post already removed.");
            }

            final String key = "P:" + postId + ":" + loginUserId;
            Lock lock = getLock(key);
            lock.lock();
            try {
                if (Boolean.TRUE.equals(reportState.get(key)) ||
                        isHiddenForUser(post.getHiddenByLoginUserIds(), loginUserId)) {
                    return post.getReportCount();
                }

                int updated = post.getReportCount() + 1;
                post.setReportCount(updated);

                if (updated >= REPORT_THRESHOLD && !post.isDeleted()) {
                    post.setDeleted(true);
                }

                post.setHiddenByLoginUserIds(
                        addUserToHidden(post.getHiddenByLoginUserIds(), loginUserId)
                );

                ForumPost saved = postRepo.save(post);
                reportState.put(key, true);

                emit(WsTopics.post(saved.getPostId()),
                        evt("postReported", saved.getPostId(), Map.of(
                                "postId", saved.getPostId(),
                                "reportCount", saved.getReportCount(),
                                "isDeleted", saved.isDeleted()
                        )));

                return saved.getReportCount();
            } finally {
                lock.unlock();
            }
        }

        // ───────── REPLY ─────────
        ForumReply reply = replyRepo.findById(replyId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid reply_id: " + replyId));

        if (reply.isDeleted()) {
            throw new IllegalStateException("Reply already removed.");
        }

        final String key = "R:" + replyId + ":" + loginUserId;
        Lock lock = getLock(key);
        lock.lock();
        try {
            if (Boolean.TRUE.equals(reportState.get(key)) ||
                    isHiddenForUser(reply.getHiddenByLoginUserIds(), loginUserId)) {
                return reply.getReportCount();
            }

            int updated = reply.getReportCount() + 1;
            reply.setReportCount(updated);

            if (updated >= REPORT_THRESHOLD && !reply.isDeleted()) {
                reply.setDeleted(true);
            }

            reply.setHiddenByLoginUserIds(
                    addUserToHidden(reply.getHiddenByLoginUserIds(), loginUserId)
            );

            ForumReply saved = replyRepo.save(reply);
            reportState.put(key, true);

            return saved.getReportCount();
        } finally {
            lock.unlock();
        }
    }



    @Transactional
    public void adminSoftDeleteItem(Long loginUserId, Long titleId, Long postId, Long replyId) {
        if (loginUserId == null) {
            throw new IllegalArgumentException("login_user_id is required");
        }

        int nonNull = 0;
        if (titleId != null) nonNull++;
        if (postId != null) nonNull++;
        if (replyId != null) nonNull++;
        if (nonNull != 1) {
            throw new IllegalArgumentException("Provide exactly one of title_id, post_id or reply_id.");
        }

        LoginUser admin = requireUser(loginUserId);
        ensureAdmin(admin);

        if (titleId != null) {
            ForumTitle title = titleRepo.findById(titleId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid title_id: " + titleId));

            if (!title.isDeleted()) {
                title.setDeleted(true);
                titleRepo.save(title);

                emit(WsTopics.title(titleId),
                        evt("titleSoftDeletedByAdmin", titleId, Map.of(
                                "titleId", titleId,
                                "isDeleted", true
                        )));
            }
            return;
        }

        if (postId != null) {
            Long finalPostId = postId;
            ForumPost post = postRepo.findById(postId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid post_id: " + finalPostId));

            if (!post.isDeleted()) {
                post.setDeleted(true);
                postRepo.save(post);

                emit(WsTopics.post(postId),
                        evt("postSoftDeletedByAdmin", postId, Map.of(
                                "postId", postId,
                                "isDeleted", true
                        )));
            }
            return;
        }

        ForumReply reply = replyRepo.findById(replyId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid reply_id: " + replyId));

        if (!reply.isDeleted()) {
            reply.setDeleted(true);
            replyRepo.save(reply);

            postId = reply.getPost().getPostId();
            emit(WsTopics.post(postId),
                    evt("replySoftDeletedByAdmin", replyId, Map.of(
                            "postId", postId,
                            "replyId", replyId,
                            "isDeleted", true
                    )));
        }
    }



    private boolean isHiddenForUser(String hiddenIds, Long loginUserId) {
        if (hiddenIds == null || hiddenIds.isBlank()) return false;
        String token = loginUserId.toString();
        for (String part : hiddenIds.split(",")) {
            if (token.equals(part.trim())) return true;
        }
        return false;
    }

    private String addUserToHidden(String hiddenIds, Long loginUserId) {
        String token = loginUserId.toString();
        if (hiddenIds == null || hiddenIds.isBlank()) {
            return token;
        }
        // already there?
        if (isHiddenForUser(hiddenIds, loginUserId)) {
            return hiddenIds;
        }
        return hiddenIds + "," + token;
    }


}
