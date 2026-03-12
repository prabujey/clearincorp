package com.trivine.llc.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CognitoJwtResponse {
    private String accessToken;
    private String idToken;
    private String refreshToken;
}
