package com.trivine.llc.api.service;

import com.trivine.llc.api.dto.ReplyDto;
import com.trivine.llc.api.dto.request.ReplyCreateRequestDto;
import com.trivine.llc.api.entity.ForumPost;
import com.trivine.llc.api.entity.ForumReply;
import com.trivine.llc.api.entity.LoginUser;
import com.trivine.llc.api.mapper.ReplyMapper;
import com.trivine.llc.api.repository.ForumPostRepository;
import com.trivine.llc.api.repository.ForumReplyRepository;
import com.trivine.llc.api.repository.LoginUserRepository;
import com.trivine.llc.api.ws.ReplyAddedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ForumReplyService {

    private final ForumReplyRepository replyRepo;
    private final ForumPostRepository postRepo;
    private final LoginUserRepository userRepo;
    private final ForumPostService postService;
    private final ReplyMapper replyMapper;
    private final ApplicationEventPublisher events;
    private static final int REPORT_THRESHOLD = 5;
    private static final Pattern MENTION_PATTERN =
            Pattern.compile("@([A-Za-z][A-Za-z0-9_]*)(?:\\s+([A-Za-z][A-Za-z0-9_]*))?");
    private final ConcurrentMap<String, Boolean> reportState = new ConcurrentHashMap<>();

    private String processMentions(String content) {
        if (content == null || content.isEmpty()) return content;

        Matcher matcher = MENTION_PATTERN.matcher(content);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String first = matcher.group(1);
            String last  = matcher.group(2);
            String rawDisplay = (last == null) ? first : (first + " " + last);
            String key = rawDisplay.replaceAll("\\s+", "").toLowerCase();

            Optional<LoginUser> userOpt = userRepo.findAll().stream()
                    .filter(u -> ((u.getFirstName() == null ? "" : u.getFirstName())
                            + (u.getLastName() == null ? "" : u.getLastName()))
                            .replaceAll("\\s+", "")
                            .toLowerCase()
                            .equals(key))
                    .findFirst();

            String replacement;
            if (userOpt.isPresent()) {
                LoginUser user = userOpt.get();
                String html = "<span class='mention' data-user-id='%d'>@%s</span>"
                        .formatted(user.getLoginUserId(), rawDisplay);
                replacement = Matcher.quoteReplacement(html);
            } else {
                replacement = Matcher.quoteReplacement("@" + rawDisplay);
            }
            matcher.appendReplacement(sb, replacement);
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    @Transactional
    public ForumReply add(Long loginUserId, ReplyCreateRequestDto req) {
        if (loginUserId == null) {
            throw new IllegalArgumentException("login_user_id cannot be null");
        }

        LoginUser loginUser = userRepo.findById(loginUserId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid login_user_id: " + loginUserId));

        ForumPost post = postRepo.findById(req.getPostId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid post_id: " + req.getPostId()));

        // ✅ Load parent reply (if any)
        ForumReply parent = (req.getParentReplyId() != null)
                ? replyRepo.findById(req.getParentReplyId()).orElse(null)
                : null;

        // Safety check: parent must belong to same post
        if (parent != null && !parent.getPost().getPostId().equals(post.getPostId())) {
            throw new IllegalArgumentException("Parent reply belongs to another post");
        }

        // Map DTO → entity
        ForumReply reply = replyMapper.toEntity(req, post, loginUser, parent);

        reply.setTitle(post.getTitle()); // inherit title from post

        String raw = req.getContentMd();
        reply.setContentMd(
                (raw != null && !raw.isBlank())
                        ? processMentions(raw.trim())
                        : null
        );

        reply.setLoginUser(loginUser);
        reply.setPost(post);
        reply.setParentReply(parent);



        ForumReply saved = replyRepo.save(reply);
        postService.incReplyCount(post);

        events.publishEvent(new ReplyAddedEvent(
                post.getPostId(),
                post.getTitle().getTitleId(),
                replyMapper.toDto(saved)
        ));

        return saved;
    }


    @Transactional(readOnly = true)
    public Page<ForumReply> listTopLevel(Long postId, int page, int size) {
        ForumPost post = postRepo.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid post_id: " + postId));
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        return replyRepo.findByPostAndParentReplyIsNullOrderByCreatedAtAsc(post, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ForumReply> listChildren(Long parentId, int page, int size) {
        ForumReply parentReply = replyRepo.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid parent_reply_id: " + parentId));
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        return replyRepo.findByParentReplyOrderByCreatedAtAsc(parentReply, pageable);
    }

    @Transactional(readOnly = true)
    public List<ReplyDto> listChildrenFlat(Long parentId) {
        // handy when you don’t want paging
        var list = replyRepo.findByParentReply_ReplyIdOrderByCreatedAtAsc(parentId);
        return replyMapper.toDtoList(list);
    }

    @Transactional(readOnly = true)
    public boolean existsReplyByTitleId(Long titleId) {
        return replyRepo.existsByTitle_TitleId(titleId);
    }

    /** Group all replies for a title by parentReplyId (null = top-level). */
    @Transactional(readOnly = true)
    public Map<Long, List<ReplyDto>> loadRepliesGroupedByParentForTitle(Long titleId) {
        List<ForumReply> all = replyRepo.findByTitle_TitleIdOrderByCreatedAtAsc(titleId);
        List<ReplyDto> dto = replyMapper.toDtoList(all);
        return dto.stream()
                .collect(Collectors.groupingBy(ReplyDto::getParentReplyId,
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

}