package com.trivine.llc.api.controller;

import com.trivine.llc.api.dto.request.OtpRequestDTO;
import com.trivine.llc.api.dto.request.ValidateRequestDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.trivine.llc.api.service.EmailTokenService;

@RestController
@RequestMapping("/token")
@Slf4j
@RequiredArgsConstructor
@Validated
public class EmailController {

    private final EmailTokenService emailTokenService;

    @PostMapping("/generate")
    public ResponseEntity<?> generateOtp(@Valid @RequestBody OtpRequestDTO request) {
        return ResponseEntity.ok(emailTokenService.generateOtp(request.getEmail().trim().toLowerCase()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(name = "refreshToken", required = false) String refreshToken, HttpServletResponse response) {
        return ResponseEntity.ok(emailTokenService.logout(refreshToken, response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            @CookieValue(value = "idToken", required = false) String idToken,
            @RequestParam @NotBlank @Email String email,
            HttpServletResponse response
    ) {
        // Sanitize and validate email before processing
        String sanitizedEmail = email.trim().toLowerCase();
        return ResponseEntity.ok(emailTokenService.refreshAccessToken(idToken, refreshToken, sanitizedEmail, response));
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateOtp(@Valid @RequestBody ValidateRequestDto request, HttpServletResponse response, HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(emailTokenService.validateOtp(request, response, httpServletRequest));
    }

}

