package com.trivine.llc.api.dto;

import lombok.*;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Email;



import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginUserDto {
    private Long loginUserId;

    @Size(max = 60, message = "First name must not exceed 60 characters")
    private String firstName;

    @Size(max = 60, message = "Last name must not exceed 60 characters")
    private String lastName;

    @Email(regexp = ".+[@].+[\\.].+", message = "Email should be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Pattern(
            regexp = "^\\(\\d{3}\\) \\d{3}-\\d{4}$",
            message = "Phone number must be in the format (123) 456-7890"
    )    @Size(max = 255, message = "Phone number must not exceed 255 characters")
    private String phoneNumber;

    private RoleDto roleId;

    private UserCompanyDto userCompanyId;

    private LocalDateTime createdOn;

    private String createdBy;

    private Boolean isActive;

    private Boolean deleted;
}
