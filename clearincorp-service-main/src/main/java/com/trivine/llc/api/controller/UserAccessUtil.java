package com.trivine.llc.api.controller;


import com.trivine.llc.api.dto.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
public final class UserAccessUtil {

    private UserAccessUtil() {
    }

    /* ---------- USER ID CHECKS ---------- */

    // Just compare two userIds safely
    public static boolean isSameUser(Long loginUserId, Long targetUserId) {
        return loginUserId != null && loginUserId.equals(targetUserId);
    }

    // Common pattern: allow only if same user OR admin
    public static void assertSameUserOrAdmin(Long loginUserId,
                                             Long targetUserId,
                                             String roleHeader) {
        UserRole role = UserRole.from(roleHeader);
        if (isAdmin(role)) {
            return; // admin is allowed
        }

        if (!isSameUser(loginUserId, targetUserId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not allowed to access this resource.");
        }
    }

    /* ---------- ROLE CHECKS (STRING + ENUM) ---------- */

    public static boolean isAdmin(String role) {
        return isAdmin(UserRole.from(role));
    }

    public static boolean isSuperfiler(String role) {
        return isSuperfiler(UserRole.from(role));
    }

    public static boolean isVendor(String role) {
        return isVendor(UserRole.from(role));
    }

    public static boolean isConsumer(String role) {
        return isConsumer(UserRole.from(role));
    }

    public static boolean isAdmin(UserRole role) {
        return role == UserRole.ADMIN;
    }

    public static boolean isSuperfiler(UserRole role) {
        return role == UserRole.SUPERFILER;
    }

    public static boolean isVendor(UserRole role) {
        return role == UserRole.VENDOR;
    }

    public static boolean isConsumer(UserRole role) {
        return role == UserRole.CONSUMER;
    }

    /* ---------- GENERIC HELPERS ---------- */

    public static boolean hasAnyRole(String role, UserRole... allowed) {
        UserRole r = UserRole.from(role);
        if (r == null) return false;
        for (UserRole ar : allowed) {
            if (r == ar) return true;
        }
        return false;
    }

    public static void assertHasAnyRole(String role, UserRole... allowed) {
        if (!hasAnyRole(role, allowed)) {
            throw new AccessDeniedException("You are not allowed to perform this action.");
        }
    }
}

