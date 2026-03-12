package com.trivine.llc.api.dto.request;


import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssigneeTarget {
    @NotNull(message = "companyId is required")
    private Long companyId;

    @NotNull(message = "loginUserId is required")
    private Long loginUserId;
}
