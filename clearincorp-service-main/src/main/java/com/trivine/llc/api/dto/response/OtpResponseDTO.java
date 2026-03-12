package com.trivine.llc.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpResponseDTO {
    private String message;
    private Long loginUserId;
    private String token;
    private String role;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private Long CompanyCount;
    private Boolean newUser;

    private String accessToken;
    private String idToken;
    private String refreshToken;
    private String profileImageUrl;


    public OtpResponseDTO(String message, Long userId, String token, String role) {
        this.message = message;
        this.loginUserId = userId;
        this.token = token;
        this.role = role;
    }

}
