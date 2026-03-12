//package com.trivine.llc.api.service;
//
//import com.trivine.llc.api.entity.EmailToken;
//import com.trivine.llc.api.entity.LoginAudit;
//import com.trivine.llc.api.entity.LoginUser;
//import com.trivine.llc.api.exception.ResourceNotFoundException;
//import com.trivine.llc.api.repository.EmailTokenRepository;
//import com.trivine.llc.api.repository.LoginAuditRepository;
//import com.trivine.llc.api.repository.LoginUserRepository;
//import jakarta.servlet.http.HttpServletRequest;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.json.JSONObject;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.client.RestTemplate;
//
//import java.time.LocalDateTime;
//
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class LoginAuditService {
//
//    private final  LoginUserRepository loginUserRepository;
//    private final LoginAuditRepository loginAuditRepository;
//    private final  EmailTokenRepository emailTokenRepository;
//
//
//    @Transactional
//    public void logUserLogin(String email, HttpServletRequest request) {
//        log.info("Logging login activity for user: {}", email);
//
//        LoginUser loginUser = loginUserRepository.findByEmail(email)
//                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
//
//        String clientIp = getClientIp(request);
//        String userAgent = request.getHeader("User-Agent");
//        String browserDetails = extractBrowserInfo(userAgent);
//        String location = getLocation(clientIp);
//
//        String tokenValue = emailTokenRepository.findFirstByEmailOrderByCreatedOnDesc(email)
//                .filter(token -> token.getEmail().equals(email))
//                .map(EmailToken::getTokenValue)
//                .orElse("DEFAULT_TOKEN");
//
//        LoginAudit loginAudit = LoginAudit.builder()
//                .loginUser(loginUser)
//                .createdOn(LocalDateTime.now())
//                .tokenValue(tokenValue)
//                .ipAddress(clientIp)
//                .browserId(userAgent)
//                .browser(browserDetails)
//                .location(location)
//                .build();
//
//        loginAuditRepository.save(loginAudit);
//        log.info("Login audit saved for user: {}", email);
//    }
//
////'''To log a warning when fetching public IP fails without local try-catch, you can wrap this logic in a higher-level method and let your global exception handler handle it — or throw a custom exception if publicIp is critical.
////
////    But if you want to retain graceful fallback + logging behavior, a small acceptable exception catch just around the AWS call is okay (since this is not a failure case but a degraded fallback).
//    private String getClientIp(HttpServletRequest request) {
//        try {
//            RestTemplate restTemplate = new RestTemplate();
//            String publicIp = restTemplate.getForObject("https://checkip.amazonaws.com", String.class);
//            if (publicIp != null && !publicIp.trim().isEmpty()) {
//                return publicIp.trim();
//            }
//        } catch (Exception e) {
//            log.warn("Failed to fetch public IP from AWS: {}", e.getMessage());
//        }
//        return request.getRemoteAddr(); // fallback to remote address
//    }
//
//    private String extractBrowserInfo(String userAgent) {
//        if (userAgent == null || userAgent.isEmpty()) {
//            return "Unknown Browser";
//        }
//
//        String[] browsers = {"Edg", "Chrome", "Firefox", "Safari", "Opera"};
//        for (String browser : browsers) {
//            if (userAgent.contains(browser)) {
//                return browser;
//            }
//        }
//
//        return "Unknown Browser";
//    }
//
//    private String getLocation(String ip) {
//        if (ip == null || ip.isEmpty()) {
//            return "Unknown Location";
//        }
//
//        try {
//            String apiUrl = "http://ip-api.com/json/" + ip;
//            RestTemplate restTemplate = new RestTemplate();
//            String response = restTemplate.getForObject(apiUrl, String.class);
//            log.debug("IP Location API response: {}", response);
//
//            JSONObject json = new JSONObject(response);
//            if ("fail".equalsIgnoreCase(json.optString("status"))) {
//                log.warn("IP location lookup failed for IP: {} - Response: {}", ip, response);
//                return "Unknown Location";
//            }
//
//            String country = json.optString("country", "Unknown Country");
//            String region = json.optString("regionName", "Unknown Region");
//            String city = json.optString("city", "Unknown City");
//            double latitude = json.optDouble("lat", 0.0);
//            double longitude = json.optDouble("lon", 0.0);
//
//            return String.format("%s, %s, %s (%.4f, %.4f)", city, region, country, latitude, longitude);
//        } catch (Exception e) {
//            log.error("Error retrieving location for IP {}: {}", ip, e.getMessage());
//            return "Unknown Location";
//        }
//    }
//}


package com.trivine.llc.api.service;

import com.trivine.llc.api.entity.EmailToken;
import com.trivine.llc.api.entity.LoginAudit;
import com.trivine.llc.api.entity.LoginUser;
import com.trivine.llc.api.exception.ResourceNotFoundException;
import com.trivine.llc.api.repository.EmailTokenRepository;
import com.trivine.llc.api.repository.LoginAuditRepository;
import com.trivine.llc.api.repository.LoginUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoginAuditService {

    private final LoginUserRepository loginUserRepository;
    private final LoginAuditRepository loginAuditRepository;
    private final EmailTokenRepository emailTokenRepository;

    // --- small helper: RestTemplate with short timeouts so we never block the login ---
    private RestTemplate fastRestTemplate() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(2000); // 2s connect
        f.setReadTimeout(2000);    // 2s read
        return new RestTemplate(f);
    }

    @Transactional
    public void logUserLogin(String email, HttpServletRequest request) {
        log.info("Logging login activity for user: {}", email);

        LoginUser loginUser = loginUserRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        String clientIp = getClientIp(request);                 // ✅ real end-user IP from ALB headers
        String userAgent = request.getHeader("User-Agent");
        String browserDetails = extractBrowserInfo(userAgent);
        String location = getLocation(clientIp);                // ✅ fast (2s timeout) + graceful fallback

        String tokenValue = emailTokenRepository.findFirstByEmailOrderByCreatedOnDesc(email)
                .filter(token -> token.getEmail().equals(email))
                .map(EmailToken::getTokenValue)
                .orElse("DEFAULT_TOKEN");

        LoginAudit loginAudit = LoginAudit.builder()
                .loginUser(loginUser)
                .createdOn(LocalDateTime.now())
                .tokenValue(tokenValue)
                .ipAddress(clientIp)
                .browserId(userAgent)
                .browser(browserDetails)
                .location(location)
                .build();

        loginAuditRepository.save(loginAudit);
        log.info("Login audit saved for user: {} ip={} location={}", email, clientIp, location);
    }

    /**
     * Get the real client IP. ALB sets X-Forwarded-For as "client, proxy1, proxy2".
     * No external call here.
     */
    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String first = xff.split(",")[0].trim();
            if (!first.isBlank()) return first;
        }
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) return xri.trim();

        return request.getRemoteAddr(); // last resort
    }

    private String extractBrowserInfo(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) return "Unknown Browser";
        String[] browsers = {"Edg", "Chrome", "Firefox", "Safari", "Opera"};
        for (String b : browsers) if (userAgent.contains(b)) return b;
        return "Unknown Browser";
    }

    /**
     * Quick, non-blocking(ish) geo lookup. Uses short timeouts and returns "Unknown Location" on any issue.
     * Note: ip-api.com free uses HTTP only; consider a paid HTTPS geo API for production.
     */
    private String getLocation(String ip) {
        if (ip == null || ip.isBlank()) return "Unknown Location";
        try {
            // Secure URL with the IP address
            String url = "https://ipinfo.io/" + ip + "/json";

            // Making the HTTP request
            String response = fastRestTemplate().getForObject(url, String.class);
            log.debug("IP Location API response: {}", response);

            if (response == null || response.isBlank()) return "Unknown Location";

            // Parse the JSON response
            JSONObject json = new JSONObject(response);
            String location = json.optString("city", "Unknown City") + ", " +
                    json.optString("region", "Unknown Region") + ", " +
                    json.optString("country", "Unknown Country");
            String locDetails = json.optString("loc", "0.0,0.0");
            String[] latLon = locDetails.split(",");
            double lat = latLon.length > 0 ? Double.parseDouble(latLon[0]) : 0.0;
            double lon = latLon.length > 1 ? Double.parseDouble(latLon[1]) : 0.0;

            return String.format("%s (%.4f, %.4f)", location, lat, lon);
        } catch (Exception e) {
            log.warn("Geo lookup error for {}: {}", ip, e.getMessage());
            return "Unknown Location";
        }
    }

}