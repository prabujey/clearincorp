package com.trivine.llc.api.dto.response;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForumTopicDto {
    private Long topicId;
    private String topicName;
}
