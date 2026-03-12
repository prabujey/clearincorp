package com.trivine.llc.api.mapper;

import com.trivine.llc.api.dto.PostDto;
import com.trivine.llc.api.dto.TitleDto;
import com.trivine.llc.api.entity.ForumTitle;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ForumTitleMapper {

    public TitleDto toDto(ForumTitle t) {
        return toDtoWithPosts(t, null);
    }

    public TitleDto toDtoWithPosts(ForumTitle t, List<PostDto> posts) {
        Long createdById = null;
        String createdByName = null;
        if (t.getCreatedBy() != null) {
            var u = t.getCreatedBy();
            createdById = u.getLoginUserId();
            String f = u.getFirstName() == null ? "" : u.getFirstName();
            String l = u.getLastName() == null ? "" : u.getLastName();
            String full = (f + " " + l).trim();
            createdByName = full.isBlank() ? u.getEmail() : full;
        }

        return TitleDto.builder()
                .titleId(t.getTitleId())
                .topicId(t.getTopicId())
                .title(t.getTitle())
                .descriptionMd(t.getDescriptionMd())
                .createdAt(t.getCreatedAt())
                .createdById(createdById)
                .createdByName(createdByName)
                .posts(posts) // can be null or list
                .build();
    }
}
