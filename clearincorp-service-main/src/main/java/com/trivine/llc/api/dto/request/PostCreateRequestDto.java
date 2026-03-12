package com.trivine.llc.api.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostCreateRequestDto {

    @NotNull
    private Long topicId;

    @NotNull
    private Long titleId;

    private String descriptionMd;

}
