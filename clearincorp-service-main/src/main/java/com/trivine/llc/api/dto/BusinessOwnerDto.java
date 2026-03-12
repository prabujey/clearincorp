package com.trivine.llc.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BusinessOwnerDto
{
    private Long ownerId;
    private Long loginUserId;   // ✅ NEW
    private String firstName;
    private String lastName;
    private String personalEmail;
    private String contactNumber;
    private Instant createdOn;
}