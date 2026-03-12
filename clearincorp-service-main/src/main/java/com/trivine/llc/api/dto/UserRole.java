package com.trivine.llc.api.dto;


public enum UserRole {
    ADMIN,
    SUPERFILER,
    VENDOR,
    CONSUMER;

    public static UserRole from(String role) {
        if (role == null) {
            return null;
        }
        try {
            return UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null; // unknown / invalid role string
        }
    }
}
