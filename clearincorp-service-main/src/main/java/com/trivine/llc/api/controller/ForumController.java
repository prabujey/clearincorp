package com.trivine.llc.api.controller;

import com.github.dockerjava.api.exception.UnauthorizedException;
import com.trivine.llc.api.dto.PostDto;
import com.trivine.llc.api.dto.ReplyDto;
import com.trivine.llc.api.dto.TitleCheckResponse;
import com.trivine.llc.api.dto.TitleDto;
import com.trivine.llc.api.dto.request.PostCreateRequestDto;
import com.trivine.llc.api.dto.request.ReplyCreateRequestDto;
import com.trivine.llc.api.dto.response.ForumTopicDto;
import com.trivine.llc.api.entity.ForumPost;
import com.trivine.llc.api.entity.ForumReply;
import com.trivine.llc.api.entity.ForumTitle;
import com.trivine.llc.api.mapper.ForumTitleMapper;
import com.trivine.llc.api.mapper.PostMapper;
import com.trivine.llc.api.mapper.ReplyMapper;
import com.trivine.llc.api.repository.ForumTitleRepository;
import com.trivine.llc.api.service.ForumPostService;
import com.trivine.llc.api.service.ForumReplyService;
import com.trivine.llc.api.service.ForumSearchService;
import com.trivine.llc.api.service.ForumTitleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/forum")
@RequiredArgsConstructor
@Validated
public class ForumController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ForumPostService postService;
    private final ForumReplyService replyService;
    private final ForumSearchService searchService;
    private final ForumTitleService titleService;
    private final ForumTitleMapper titleMapper;
    private final PostMapper postMapper;
    private final ReplyMapper replyMapper;
    private final ForumTitleRepository titleRepo;

    @PostMapping("/posts")
    public PostDto createPost(
            @RequestParam("login_user_id") Long loginUserId,
            @Valid @RequestBody PostCreateRequestDto req) {
        ForumPost p = postService.create(loginUserId, req);
        return postMapper.toDto(p);
    }

    @GetMapping("/posts/by-topic")
    public Page<PostDto> listByTopic(
            @RequestParam("topic_id") Long topicId,
            @RequestParam("login_user_id") Long loginUserId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int size) {

        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        Page<ForumPost> postsPage = postService.listByTopic(topicId, loginUserId, page, safeSize);
        List<PostDto> dtoList = postMapper.toDtoList(postsPage.getContent());
        for (int i = 0; i < postsPage.getContent().size(); i++) {
            ForumPost post = postsPage.getContent().get(i);
            dtoList.get(i).setReplies(replyMapper.toDtoList(post.getReplies()));
        }
        return new PageImpl<>(dtoList, postsPage.getPageable(), postsPage.getTotalElements());
    }

    @GetMapping("/posts/by-user")
    public Page<PostDto> listByAuthor(
            @RequestParam("login_user_id") Long loginUserId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int size) {

        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        Page<ForumPost> postsPage = postService.listByLoginUser(loginUserId, page, safeSize);
        List<PostDto> dtoList = postMapper.toDtoList(postsPage.getContent());
        for (int i = 0; i < postsPage.getContent().size(); i++) {
            ForumPost post = postsPage.getContent().get(i);
            dtoList.get(i).setReplies(replyMapper.toDtoList(post.getReplies()));
        }
        return new PageImpl<>(dtoList, postsPage.getPageable(), postsPage.getTotalElements());
    }

    @GetMapping("/all-posts")
    public Page<PostDto> listAllPosts(
            @RequestParam("login_user_id") Long loginUserId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int size) {

        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        Page<ForumPost> postsPage = postService.listByPinned(loginUserId, page, safeSize);
        List<PostDto> dtoList = postMapper.toDtoList(postsPage.getContent());
        for (int i = 0; i < postsPage.getContent().size(); i++) {
            ForumPost post = postsPage.getContent().get(i);
            dtoList.get(i).setReplies(replyMapper.toDtoList(post.getReplies()));
        }
        return new PageImpl<>(dtoList, postsPage.getPageable(), postsPage.getTotalElements());
    }

    @GetMapping("/post")
    public PostDto getPost(
            @RequestParam("post_id") Long postId,
            @RequestParam("login_user_id") Long loginUserId) {

        ForumPost post = postService.get(postId);
        PostDto dto = postMapper.toDto(post);
        Page<ForumReply> tops = replyService.listTopLevel(postId, 0, 50);
        dto.setReplies(replyMapper.toDtoList(tops.getContent()));
        return dto;
    }

    @PutMapping("/edit")
    public ResponseEntity<?> edit(
            @RequestParam("login_user_id") @NotNull Long loginUserId,
            @RequestParam(value = "title_id", required = false) Long titleId,
            @RequestParam(value = "post_id",  required = false) Long postId,
            @RequestParam(value = "reply_id", required = false) Long replyId,
            @RequestParam(value = "new_content_md",     required = false) String newContentMd,
            @RequestParam(value = "new_title",          required = false) String newTitle,
            @RequestParam(value = "new_description_md", required = false) String newDescriptionMd
    ) {
        Object updated = postService.editItem(
                loginUserId, titleId, postId, replyId, newContentMd, newTitle, newDescriptionMd
        );

        if (updated instanceof ForumPost p) {
            return ResponseEntity.ok(postMapper.toDto(p));
        } else if (updated instanceof ForumReply r) {
            return ResponseEntity.ok(replyMapper.toDto(r));
        } else if (updated instanceof ForumTitle t) {
            return ResponseEntity.ok(titleMapper.toDto(t));
        }
        return ResponseEntity.ok(Map.of("message", "Updated"));
    }

    @ExceptionHandler({ IllegalArgumentException.class, SecurityException.class, IllegalStateException.class })
    public ResponseEntity<?> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "message", "Bad Request",
                "details", ex.getMessage()
        ));
    }

    @PostMapping("/post/pin-toggle")
    public PostDto togglePinned(
            @RequestParam("post_id") Long postId,
            @RequestParam("login_user_id") Long loginUserId) {
        return postMapper.toDto(postService.togglePinned(loginUserId, postId));
    }

    @PostMapping("/post/view")
    public int viewPost(
            @RequestParam("post_id") Long postId,
            @RequestParam("login_user_id") Long loginUserId) {
        return postService.view(postId);
    }

    @PostMapping("/likes")
    public int toggleLike(
            @RequestParam("login_user_id") Long loginUserId,
            @RequestParam(value = "post_id", required = false) Long postId,
            @RequestParam(value = "reply_id", required = false) Long replyId) {
        return postService.toggleLike(loginUserId, postId, replyId);
    }

    @DeleteMapping("/delete")
    public String deleteItem(
            @RequestParam("login_user_id") Long loginUserId,
            @RequestParam(value = "post_id", required = false) Long postId,
            @RequestParam(value = "reply_id", required = false) Long replyId) {
        postService.deleteItem(loginUserId, postId, replyId);
        return (postId != null) ? "Post deleted successfully" : "Reply deleted successfully";
    }


    @PostMapping("/replies")
    public ReplyDto createReply(
            @RequestParam("login_user_id") Long loginUserId, // Ensure this is received properly
            @Valid @RequestBody ReplyCreateRequestDto req) {

        if (loginUserId == null) {
            throw new UnauthorizedException("Missing login_user_id");  // Throw an error if null
        }

        return replyMapper.toDto(replyService.add(loginUserId, req));
    }

    @GetMapping("/post/replies")
    public Page<ReplyDto> listTopReplies(
            @RequestParam("post_id") Long postId,
            @RequestParam("login_user_id") Long loginUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return replyService.listTopLevel(postId, page, size).map(replyMapper::toDto);
    }

    @GetMapping("/reply/children")
    public Page<ReplyDto> listChildReplies(
            @RequestParam("reply_id") Long replyId,
            @RequestParam("login_user_id") Long loginUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return replyService.listChildren(replyId, page, size).map(replyMapper::toDto);
    }


    @GetMapping("/search")
    public List<PostDto> search(
            @RequestParam("query") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return searchService.search(query, page, size);
    }

    @GetMapping("/topics")
    public List<ForumTopicDto> getAllTopicIdsAndNames() {
        return postService.getAllTopicIdsAndNames();
    }



    @PostMapping("/titles")
    public TitleDto createTitle(
            @RequestParam("login_user_id") @NotNull Long loginUserId,
            @RequestParam("topic_id")      @NotNull Long topicId,
            @RequestParam("title")         @NotBlank String title,
            @RequestParam("descriptionMd") @NotBlank String descriptionMd
    ) {
        ForumTitle saved = titleService.createTitle(loginUserId, topicId, title, descriptionMd);
        return titleMapper.toDto(saved);
    }

    @GetMapping("/title-posts")
    public TitleDto getTitleWithPosts(
            @RequestParam("title_id") Long titleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return titleService.getTitleWithPosts(titleId, page, size);
    }

    @GetMapping("/titles")
    public Page<TitleDto> listTitles(
            @RequestParam(value = "topic_id", required = false) Long topicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        boolean hasCreatedAt = true;
        return titleService.listTitles(topicId, page, size, hasCreatedAt)
                .map(titleMapper::toDto);
    }

    @DeleteMapping("/titles/delete")
    public String deleteTitle(
            @RequestParam("login_user_id") @NotNull Long loginUserId,
            @RequestParam("title_id")      @NotNull Long titleId
    ) {
        titleService.deleteTitle(loginUserId, titleId);
        return "Title deleted";
    }

    @GetMapping("/titles/check")
    public ResponseEntity<TitleCheckResponse> checkTitleAvailableGlobally(
            @RequestParam("title") String title,
            @RequestParam(value = "exclude_title_id", required = false) Long excludeId
    ) {
        return ResponseEntity.ok(titleService.checkAvailabilityGlobalWithIds(title, excludeId));
    }


    @PostMapping("/report")
    public ResponseEntity<?> reportItem(
            @RequestParam("login_user_id") Long loginUserId,
            @RequestParam(value = "title_id", required = false) Long titleId,
            @RequestParam(value = "post_id",  required = false) Long postId,
            @RequestParam(value = "reply_id", required = false) Long replyId
    ) {
        int nonNull = 0;
        if (titleId != null) nonNull++;
        if (postId != null)  nonNull++;
        if (replyId != null) nonNull++;
        if (nonNull != 1) {
            throw new IllegalArgumentException("Provide exactly one of title_id, post_id or reply_id.");
        }

        int reportCount = postService.reportItem(loginUserId, titleId, postId, replyId);

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("report_count", reportCount);

        if (titleId != null) {
            body.put("type", "title");
            body.put("title_id", titleId);
        } else if (postId != null) {
            body.put("type", "post");
            body.put("post_id", postId);
        } else {
            body.put("type", "reply");
            body.put("reply_id", replyId);
        }

        return ResponseEntity.ok(body);
    }

    @PostMapping("/admin/soft-delete")
    public ResponseEntity<?> adminSoftDeleteItem(
            @RequestParam("login_user_id") Long loginUserId,
            @RequestParam(value = "title_id", required = false) Long titleId,
            @RequestParam(value = "post_id",  required = false) Long postId,
            @RequestParam(value = "reply_id", required = false) Long replyId
    ) {
        int nonNull = 0;
        if (titleId != null) nonNull++;
        if (postId != null)  nonNull++;
        if (replyId != null) nonNull++;
        if (nonNull != 1) {
            throw new IllegalArgumentException("Provide exactly one of title_id, post_id or reply_id.");
        }

        postService.adminSoftDeleteItem(loginUserId, titleId, postId, replyId);

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("soft_deleted", true);

        if (titleId != null) {
            body.put("type", "title");
            body.put("title_id", titleId);
        } else if (postId != null) {
            body.put("type", "post");
            body.put("post_id", postId);
        } else {
            body.put("type", "reply");
            body.put("reply_id", replyId);
        }

        return ResponseEntity.ok(body);
    }


}