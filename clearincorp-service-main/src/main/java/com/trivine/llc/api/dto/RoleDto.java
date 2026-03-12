package com.trivine.llc.api.dto;

import lombok.*;
import jakarta.validation.constraints.Size;


import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleDto {
    private Long id;

    @Size(max = 255, message = "Role name must not exceed 255 characters")
    private String name;

    private Boolean isDeleted;

    @Size(max = 255, message = "Added by must not exceed 255 characters")
    private String addBy;

    private LocalDateTime addDate;

    @Size(max = 255, message = "Last modified by must not exceed 255 characters")
    private String lastModBy;

    private LocalDateTime lastModDate;
}
