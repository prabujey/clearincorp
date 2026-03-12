package com.trivine.llc.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BusinessDto {
    private Long businessId;
    private Long ownerId;
    private Long serviceId;
    private String serviceName;
    private String businessName;
    private String serviceDescription;
    private String yearsInBusiness;
    private String zipCode;
    private String businessAddress;
    private String websiteUrl;
    private String businessLicense;
    private String businessEmail;
    private String businessHours;
    private String city;
    private String state;

    private String createdBy;
    private String updatedBy;
    private Instant createdOn;
    private Instant updatedOn;
}
