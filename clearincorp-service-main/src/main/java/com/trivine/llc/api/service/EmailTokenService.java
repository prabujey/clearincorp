package com.trivine.llc.api.service;

import com.trivine.llc.api.config.AwsConfiguration;
import com.trivine.llc.api.dto.response.CognitoJwtResponse;

import com.trivine.llc.api.constants.RoleEnum;
import com.trivine.llc.api.constants.ServiceConstants;
import com.trivine.llc.api.dto.request.ValidateRequestDto;
import com.trivine.llc.api.entity.*;
import com.trivine.llc.api.repository.CompanyRepository;
import com.trivine.llc.api.repository.EmailTokenRepository;
import com.trivine.llc.api.repository.LoginUserRepository;
import com.trivine.llc.api.service.utility.SendEmailService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.trivine.llc.api.dto.response.OtpResponseDTO;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
import jakarta.servlet.http.HttpServletResponse;


import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;



import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;


@Service
@Slf4j
public class EmailTokenService {

    private static final String FROM_EMAIL = ServiceConstants.FROM_EMAIL;
    private static final String SUBJECT = ServiceConstants.TOKEN_SUBJECT;
    private static final int OTP_EXPIRY_MINUTES = ServiceConstants.OTP_EXPIRY_MINUTES;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final Long ConsumerId = RoleEnum.Consumer.getRoleId();
    private final EmailTokenRepository emailTokenRepository;
    private final SendEmailService sendEmailService;
    private final LoginUserRepository loginUserRepository;
    private final CognitoIdentityProviderClient cognitoClient;
    private final CompanyRepository companyRepository;
    private final S3Service s3Service;
    private final LoginAuditService loginAuditService;
    private final boolean cognitoEnabled;

        @Value("${frontend.origin.cloudFront:}")
        private String fullUrl;
        private String cloudFrontOrigin;
        @PostConstruct
        void init() {
            if (fullUrl != null && !fullUrl.isBlank()) {
                URI uri = URI.create(fullUrl.trim());
                this.cloudFrontOrigin = uri.getHost();   // e.g., d2g1vqx667lhiu.cloudfront.net
                }
            }

    private final String userPoolId;
    private final String clientId;
    private final String clientSecret;
    private final String hmacAlgorithm;
    private final String defaultPassword;

    public EmailTokenService(EmailTokenRepository emailTokenRepository, SendEmailService sendEmailService, LoginUserRepository loginUserRepository, @org.springframework.lang.Nullable CognitoIdentityProviderClient cognitoClient, CompanyRepository companyRepository, AwsConfiguration awsConfiguration, S3Service s3Service, LoginAuditService loginAuditService) {
        this.emailTokenRepository = emailTokenRepository;
        this.sendEmailService = sendEmailService;
        this.loginUserRepository = loginUserRepository;
        this.cognitoClient = cognitoClient;
        this.cognitoEnabled = cognitoClient != null;
        this.companyRepository = companyRepository;
        this.s3Service = s3Service;
        this.loginAuditService = loginAuditService;
        AwsConfiguration.CognitoConfig cognitoConfig = awsConfiguration.getCognito();
        this.userPoolId = cognitoConfig.getUserPoolId();
        this.clientId = cognitoConfig.getClientId();
        this.clientSecret = cognitoConfig.getClientSecret();
        this.hmacAlgorithm = cognitoConfig.getHmacAlgorithm();
        this.defaultPassword = cognitoConfig.getDefaultPassword();

        if (!cognitoEnabled) {
            log.warn("Cognito client is null - authentication features will not work!");
        }
    }

    private void requireCognito() {
        if (!cognitoEnabled) {
            throw new IllegalStateException("Cognito authentication is not configured. Please set AWS_COGNITO_ENABLED=true and provide valid AWS credentials.");
        }
    }


    private void upsertLoginUserIdAttribute(String email, Long loginUserId) {
        requireCognito();
        cognitoClient.adminUpdateUserAttributes(b -> b
                .userPoolId(userPoolId)
                .username(email) // must match Cognito username (you use email)
                .userAttributes(
                        AttributeType.builder()
                                .name("custom:login_user_id")
                                .value(String.valueOf(loginUserId))
                                .build()
                )
        );
    }



    public OtpResponseDTO generateOtp(String email) {
        log.info("OTP generate request from: {}", email);

        OtpResponseDTO response = new OtpResponseDTO();
        Optional<LoginUser> userOpt = loginUserRepository.findByEmail(email);
        String firstName="there";
        if(userOpt.isPresent()){

            if (userOpt.get().getDeleted()) {
                response.setMessage("User is deleted. Please contact support.");
                return response;
            }
            if(userOpt.get().getFirstName()!=null){
                firstName=userOpt.get().getFirstName();
            }
        }
        String otpCode = generateOtpCode();
        boolean otpSent = sendEmailService.sendTokenEmailInZoho(FROM_EMAIL, email, SUBJECT, otpCode,firstName);
        if (!otpSent) {
            log.warn("Failed to send OTP email to: {}", email);
            throw new IllegalStateException("Email Not Found or Failed to Send");
        }
        EmailToken emailToken = new EmailToken();
        emailToken.setEmail(email);
        emailToken.setTokenValue(otpCode);
        emailToken.setCreatedOn(LocalDateTime.now());
        emailToken.setIsUsed(false);
        emailTokenRepository.save(emailToken);
        userOpt.ifPresent(loginUser -> response.setLoginUserId(loginUser.getLoginUserId()));
        response.setMessage("Verification code sent! Please check your email.");
        return response;
    }


    @Transactional
    public OtpResponseDTO validateOtp(ValidateRequestDto request, HttpServletResponse servletResponse,HttpServletRequest httpServletRequest) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Validating OTP for email: {}", email);


        OtpResponseDTO response = new OtpResponseDTO();
        EmailToken emailToken = emailTokenRepository.findFirstByEmailOrderByCreatedOnDesc(email)
                .orElse(null);

        if (emailToken == null) {
            response.setMessage("Otp Not generated Yet");
            return response;
        }
        if (emailToken.getIsUsed()) {
            response.setMessage("OTP already used. Please generate a new one.");
            return response;
        }
        if (Duration.between(emailToken.getCreatedOn(), LocalDateTime.now()).toMinutes() > OTP_EXPIRY_MINUTES) {
            response.setMessage("OTP expired. Please generate a new one.");
            return response;
        }

        if (!emailToken.getTokenValue().equals(request.getToken())) {
            response.setMessage("Incorrect OTP.");
            return response;
        }

        LoginUser loginUser;
        String roleName;

        Optional<LoginUser> loginUserOpt = loginUserRepository.findByEmail(email);
        if (loginUserOpt.isEmpty()) {
            loginUser = new LoginUser();
            loginUser.setEmail(email);
            loginUser.setDeleted(false);
            Role role = new Role();
            role.setId(ConsumerId);
            roleName = RoleEnum.Consumer.name();
            loginUser.setRoleId(role);
            response.setNewUser(true);
            UserCompany userCompany = new UserCompany();
            userCompany.setId(ServiceConstants.DEFAULT_VENDOR_USER_ID);
            loginUser.setUserCompanyId(userCompany);
            loginUser = loginUserRepository.save(loginUser);

            sendEmailService.sendConsumerWelcomeEmail(ServiceConstants.FROM_EMAIL, email, ServiceConstants.CONSUMER_WELCOME_SUBJECT, "User");
        } else {
            loginUser = loginUserOpt.get();
            if (loginUser.getDeleted()) {
                response.setMessage("User is deleted. Please contact support.");
                return response;
            }

            roleName = loginUser.getRoleId().getName();
        }

        emailToken.setIsUsed(true);
        emailTokenRepository.save(emailToken);

        response.setLoginUserId(loginUser.getLoginUserId());
        response.setMessage("OTP validated successfully.");
        loginAuditService.logUserLogin(request.getEmail(),httpServletRequest);
        response.setRole(roleName);
        response.setCompanyCount(getCompanyCountByUserId(loginUser.getLoginUserId()));
        response.setFirstName(loginUser.getFirstName());
        response.setLastName(loginUser.getLastName());
        response.setPhoneNumber(loginUser.getPhoneNumber());
        response.setEmail(loginUser.getEmail());

        try {

            try {
                cognitoClient.adminGetUser(AdminGetUserRequest.builder()
                        .userPoolId(userPoolId)
                        .username(email)
                        .build());
                log.info("User already exists in Cognito: {}", email);
            } catch (UserNotFoundException e) {
                log.info("Creating user in Cognito: {}", email);
                createUser(email, roleName);
                setPermanentPassword(email, defaultPassword);
            }
            // NEW: ensure custom:login_user_id is set on the Cognito user BEFORE minting tokens
            upsertLoginUserIdAttribute(email, loginUser.getLoginUserId());

            AuthenticationResultType authResult = authenticateUser(email, defaultPassword);

            CognitoJwtResponse jwtTokens = new CognitoJwtResponse(
                    authResult.accessToken(),
                    authResult.idToken(),
                    authResult.refreshToken()
            );

            response.setAccessToken(jwtTokens.getAccessToken());
            response.setIdToken(jwtTokens.getIdToken());
            String fileKey = "User_Document/" + loginUser.getLoginUserId() + "/" + "profileImg";
            response.setProfileImageUrl(
                    s3Service.generatePresignedGetUrlIfExists(fileKey).orElse(null)
            );

            // Clear the refresh token cookie (localhost)
            ResponseCookie idCookie = ResponseCookie.from("idToken", jwtTokens.getIdToken())
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("None")
                    .domain(cloudFrontOrigin)
                    .path("/")
                    .maxAge(Duration.ofDays(1))
                    .build();
            ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", jwtTokens.getRefreshToken())
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("None")
                    .domain(cloudFrontOrigin)// no cross-site on localhost; works over http
                    .path("/")
                    .maxAge(Duration.ofDays(1))         // expire immediately
                    .build();

            servletResponse.addHeader(HttpHeaders.SET_COOKIE, idCookie.toString());
            servletResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        } catch (Exception e) {
            log.error("Cognito error for user {}: {}", email, e.getMessage(), e);
            throw new IllegalStateException("Authentication with Cognito failed.");
        }

        return response;
    }





    public CognitoJwtResponse refreshAccessToken(String idToken,String refreshToken, String email, HttpServletResponse response) {
        requireCognito();
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalStateException("Refresh token cookie is missing");
        }

        String normalizedEmail = email.trim().toLowerCase();

        AdminGetUserResponse userResponse = cognitoClient.adminGetUser(AdminGetUserRequest.builder()
                .userPoolId(userPoolId)
                .username(normalizedEmail)
                .build());

        String actualUsername = userResponse.username();

        Map<String, String> authParams = new HashMap<>();
        authParams.put("REFRESH_TOKEN", refreshToken.trim());
        authParams.put("USERNAME", actualUsername);

        if (clientSecret != null && !clientSecret.isBlank()) {
            authParams.put("SECRET_HASH", calculateSecretHash(clientId, clientSecret, actualUsername));
        }

        AdminInitiateAuthRequest authRequest = AdminInitiateAuthRequest.builder()
                .userPoolId(userPoolId)
                .clientId(clientId)
                .authFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
                .authParameters(authParams)
                .build();

        AdminInitiateAuthResponse authResponse = cognitoClient.adminInitiateAuth(authRequest);

        AuthenticationResultType result = authResponse.authenticationResult();

        if (result == null) {
            throw new IllegalStateException("Authentication result is null - refresh failed");
        }

        // Clear the refresh token cookie (localhost)
        ResponseCookie idCookie = ResponseCookie.from("idToken", result.idToken() != null ? result.idToken() : idToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .domain(cloudFrontOrigin)
                .path("/")
                .maxAge(Duration.ofDays(1))
                .build();
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken",result.refreshToken() != null ? result.refreshToken() : refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .domain(cloudFrontOrigin)
                .path("/")
                .maxAge(Duration.ofDays(1))        // expire immediately
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, idCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        return new CognitoJwtResponse(
                result.accessToken(),
                result.idToken(),
                result.refreshToken() != null ? result.refreshToken() : refreshToken
        );
    }



public String logout(String refreshToken, HttpServletResponse response) {
    requireCognito();
    if (refreshToken == null || refreshToken.isBlank()) {
        throw new IllegalStateException("Refresh token missing");
    }

    try {
        cognitoClient.revokeToken(r -> r
                .clientId(clientId)
                .clientSecret(clientSecret) // only if your app client has a secret
                .token(refreshToken)
        );
        log.info("User successfully logged out from this browser (refresh token revoked).");
    } catch (Exception e) {
        log.error("Error revoking token: {}", e.getMessage(), e);
        throw new IllegalStateException("Logout failed due to Cognito error.");
    }

    // Clear the refresh token cookie (localhost)
    ResponseCookie delId = ResponseCookie.from("idToken", null)
            .httpOnly(true)
            .secure(true)      // prod => true
            .sameSite("None")  // prod => "None"
            .domain(cloudFrontOrigin)
            .path("/")
            .maxAge(0)
            .build();

    ResponseCookie delRefresh = ResponseCookie.from("refreshToken", null)
            .httpOnly(true)
            .secure(true)      // prod => true
            .sameSite("None")    // prod => "None"
            .domain(cloudFrontOrigin)
            .path("/")
            .maxAge(0)
            .build();

    response.addHeader(HttpHeaders.SET_COOKIE, delId.toString());
    response.addHeader(HttpHeaders.SET_COOKIE, delRefresh.toString());

    return "Logged out successfully";
}








    public String generateOtpCode() {
        int otpLength = ServiceConstants.OTP_LENGTH;

        StringBuilder otpBuilder = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            int digit = SECURE_RANDOM.nextInt(10);
            otpBuilder.append(digit);
        }

        return otpBuilder.toString();
    }




    public void createUser(String email,String groupName) {
        requireCognito();
        try {
            AdminGetUserRequest getUserRequest = AdminGetUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(email)
                    .build();
            cognitoClient.adminGetUser(getUserRequest);
            // User exists
        } catch (UserNotFoundException e) {
            // User does not exist, create user
            AdminCreateUserRequest createUserRequest = AdminCreateUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(email)
                    .userAttributes(
                            AttributeType.builder().name("email").value(email).build(),
                            AttributeType.builder().name("email_verified").value("true").build()
                    )
                    .messageAction(MessageActionType.SUPPRESS)
                    .build();
            cognitoClient.adminCreateUser(createUserRequest);
        }
        try {
            AdminAddUserToGroupRequest addToGroupRequest = AdminAddUserToGroupRequest.builder()
                    .userPoolId(userPoolId)
                    .username(email)
                    .groupName(groupName)
                    .build();

            cognitoClient.adminAddUserToGroup(addToGroupRequest);

            log.info("User {} added to group {}", email, groupName);

        } catch (CognitoIdentityProviderException ex) {
            log.error("Failed to add user {} to group {}: {}", email, groupName, ex.awsErrorDetails().errorMessage());
            throw ex;
        }
    }
    public void setPermanentPassword(String email, String password) {
        requireCognito();
        AdminSetUserPasswordRequest setPasswordRequest = AdminSetUserPasswordRequest.builder()
                .userPoolId(userPoolId)
                .username(email)
                .password(password)
                .permanent(true)
                .build();
        cognitoClient.adminSetUserPassword(setPasswordRequest);
    }

    public AuthenticationResultType authenticateUser(String email, String password) {
        requireCognito();
        Map<String, String> authParams = new HashMap<>();
        authParams.put("USERNAME", email);
        authParams.put("PASSWORD", password);

        // Use email as the username
        authParams.put("SECRET_HASH", calculateSecretHash(clientId, clientSecret, email));

        AdminInitiateAuthRequest authRequest = AdminInitiateAuthRequest.builder()
                .userPoolId(userPoolId)
                .clientId(clientId)  // Use injected value here
                .authFlow(AuthFlowType.ADMIN_NO_SRP_AUTH)
                .authParameters(authParams)
                .build();

        AdminInitiateAuthResponse authResponse = cognitoClient.adminInitiateAuth(authRequest);
        if (authResponse.authenticationResult() == null) {
            throw new RuntimeException("Authentication failed for user: " + email);
        }
        return authResponse.authenticationResult();
    }

    public void deleteUser(String email) {
        requireCognito();
        try {
            AdminDeleteUserRequest deleteUserRequest = AdminDeleteUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(email)
                    .build();

            cognitoClient.adminDeleteUser(deleteUserRequest);

            log.info("User {} successfully deleted.", email);

        } catch (UserNotFoundException e) {
            log.warn("User {} not found in the user pool.", email);
        } catch (CognitoIdentityProviderException ex) {
            log.error("Failed to delete user {}: {}", email, ex.awsErrorDetails().errorMessage());
            throw ex;  // Re-throw if needed
        }
    }




    public  String calculateSecretHash(String clientId, String clientSecret, String username) {
        try {
            String message = username + clientId;
            SecretKeySpec signingKey = new SecretKeySpec(clientSecret.getBytes(StandardCharsets.UTF_8), hmacAlgorithm);
            Mac mac = Mac.getInstance(hmacAlgorithm);
            mac.init(signingKey);
            byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Error while calculating secret hash", e);
        }
    }

    public Long getCompanyCountByUserId(Long loginUserId) {
        return companyRepository.countByLoginUserId(loginUserId);
    }
}



