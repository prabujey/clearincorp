package com.trivine.llc.api.mapper;

import com.trivine.llc.api.dto.PostDto;
import com.trivine.llc.api.dto.ReplyDto;
import com.trivine.llc.api.dto.request.PostCreateRequestDto;
import com.trivine.llc.api.entity.ForumPost;
import com.trivine.llc.api.entity.ForumReply;
import com.trivine.llc.api.entity.ForumTopic;
import com.trivine.llc.api.entity.LoginUser;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PostMapper {

    /* ========= ENTITY -> DTO ========= */
    @Mappings({
            @Mapping(target = "loginUserId",
                    expression = "java(p.getLoginUser()!=null ? p.getLoginUser().getLoginUserId() : null)"),
            @Mapping(target = "loginUserName",
                    expression = "java(fullNameOrEmail(p))"),
            @Mapping(target = "topicId",
                    expression = "java(p.getTopic()!=null ? p.getTopic().getTopicId() : null)"),
            @Mapping(target = "topicName",
                    expression = "java(p.getTopic()!=null ? p.getTopic().getTopicName() : null)"),
            @Mapping(target = "pinnedById",
                    expression = "java(p.getPinnedBy()!=null ? p.getPinnedBy().getLoginUserId() : null)"),
            @Mapping(source = "descriptionMd", target = "content"),

            // Map titleId (FK) from relation
            @Mapping(target = "titleId",
                    expression = "java(p.getTitle()!=null ? p.getTitle().getTitleId() : null)"),
            // Map display title string from relation
            @Mapping(target = "title",
                    expression = "java(p.getTitle()!=null ? p.getTitle().getTitle() : null)"),

            // If your PostDto has a list of replies, keep it ignored unless you want to populate it explicitly
            @Mapping(target = "replies", ignore = true)
    })
    PostDto toDto(ForumPost p);

    List<PostDto> toDtoList(List<ForumPost> posts);

    /* ========= Reply to DTO ========= */
    @Mappings({
            @Mapping(target = "replyId",      source = "replyId"),
            @Mapping(target = "postId",       source = "post.postId"),
            @Mapping(target = "loginUserId",  source = "loginUser.loginUserId"),
            @Mapping(target = "loginUserName",
                    expression = "java(replyUserFullNameOrEmail(r))")
    })
    ReplyDto toReplyDto(ForumReply r);

    List<ReplyDto> toReplyDtoList(List<ForumReply> replies);

    /* ========= DTO -> ENTITY =========
       Relations are ignored here; set them in the service after loading from repos.
    */
    @Mappings({
            @Mapping(target = "postId", ignore = true),
            @Mapping(target = "loginUser", ignore = true),
            @Mapping(target = "topic", ignore = true),
            @Mapping(target = "title", ignore = true), // relation set in service
            @Mapping(target = "pinned", constant = "false"),
            @Mapping(target = "pinnedAt", ignore = true),
            @Mapping(target = "pinnedBy", ignore = true),
            @Mapping(target = "likesCount", constant = "0"),
            @Mapping(target = "viewsCount", constant = "0"),
            @Mapping(target = "replyCount", constant = "0"),
            @Mapping(target = "deleted", constant = "false"),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "editedAt", ignore = true),
            @Mapping(target = "lastActivityAt",
                    expression = "java(java.time.Instant.now())")
    })
    ForumPost toEntity(PostCreateRequestDto dto,
                       @Context LoginUser loginUser,
                       @Context ForumTopic topic);

    @AfterMapping
    default void attachRelations(@MappingTarget ForumPost post,
                                 @Context LoginUser loginUser,
                                 @Context ForumTopic topic) {
        post.setLoginUser(loginUser);
        post.setTopic(topic);
    }

    /* ========= helpers ========= */
    default String fullNameOrEmail(ForumPost p) {
        if (p == null || p.getLoginUser() == null) return null;
        var u = p.getLoginUser();
        String f = u.getFirstName() == null ? "" : u.getFirstName();
        String l = u.getLastName() == null ? "" : u.getLastName();
        String full = (f + " " + l).trim();
        return full.isEmpty() ? u.getEmail() : full;
    }

    default String replyUserFullNameOrEmail(ForumReply r) {
        if (r == null || r.getLoginUser() == null) return null;
        var u = r.getLoginUser();
        String f = u.getFirstName() == null ? "" : u.getFirstName();
        String l = u.getLastName() == null ? "" : u.getLastName();
        String full = (f + " " + l).trim();
        return full.isEmpty() ? u.getEmail() : full;
    }
}
