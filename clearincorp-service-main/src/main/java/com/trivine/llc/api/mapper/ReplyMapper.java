package com.trivine.llc.api.mapper;

import com.trivine.llc.api.dto.ReplyDto;
import com.trivine.llc.api.dto.request.ReplyCreateRequestDto;
import com.trivine.llc.api.entity.ForumPost;
import com.trivine.llc.api.entity.ForumReply;
import com.trivine.llc.api.entity.LoginUser;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReplyMapper {

    /* ========= ENTITY -> DTO ========= */
    @Mappings({
            @Mapping(target = "postId",
                    expression = "java(r.getPost() != null ? r.getPost().getPostId() : null)"),

            @Mapping(
                    target = "parentReplyId",
                    expression = "java(r.getParentReply() != null ? r.getParentReply().getReplyId() : null)"
            ),

            @Mapping(target = "loginUserId",
                    expression = "java(r.getLoginUser() != null ? r.getLoginUser().getLoginUserId() : null)"),
            @Mapping(target = "loginUserName",
                    expression = "java(getFullName(r.getLoginUser()))"),

            @Mapping(source = "contentMd", target = "content")
    })
    ReplyDto toDto(ForumReply r);

    List<ReplyDto> toDtoList(List<ForumReply> replies);

    /* ========= DTO -> ENTITY ========= */
    @Mappings({
            @Mapping(target = "replyId", ignore = true),
            @Mapping(target = "post", ignore = true),
            @Mapping(target = "parentReply", ignore = true),
            @Mapping(target = "loginUser", ignore = true),
            @Mapping(target = "depth", ignore = true),
            @Mapping(target = "likesCount", constant = "0"),
            @Mapping(target = "deleted", constant = "false"),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "editedAt", ignore = true),
            @Mapping(target = "contentMd", source = "dto.contentMd")
    })
    ForumReply toEntity(ReplyCreateRequestDto dto,
                        @Context ForumPost post,
                        @Context LoginUser loginUser,
                        @Context ForumReply parent);

    @AfterMapping
    default void attachContext(@MappingTarget ForumReply reply,
                               @Context ForumPost post,
                               @Context LoginUser loginUser,
                               @Context ForumReply parent) {
        reply.setPost(post);
        reply.setParentReply(parent);
        reply.setLoginUser(loginUser);
        short depth = (short) (parent != null ? parent.getDepth() + 1 : 0);
        reply.setDepth(depth);
    }

    /* ========= helpers ========= */
    default String getFullName(LoginUser u) {
        if (u == null) return null;
        String f = u.getFirstName() == null ? "" : u.getFirstName().trim();
        String l = u.getLastName() == null ? "" : u.getLastName().trim();
        String full = (f + " " + l).trim();
        return full.isEmpty() ? u.getEmail() : full;
    }
}
