package com.trivine.llc.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "business",
        indexes = {
                @Index(name = "idx_business_owner", columnList = "owner_id"),
                @Index(name = "idx_business_service", columnList = "service_id"),
                @Index(name = "idx_business_zip", columnList = "zip_code")
        })
public class Business {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "business_id")
    private Long businessId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private BusinessOwner owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private BusinessService service;

    @Column(name = "business_name", length = 255, nullable = false)
    private String businessName;

    @Column(name = "service_description", length = 1000)
    private String serviceDescription;

    @Column(name = "years_in_business")
    private String yearsInBusiness;

    @Column(name = "zip_code", length = 20)
    private String zipCode;

    @Column(name = "business_address", length = 500)
    private String businessAddress;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(name = "business_license", length = 120)
    private String businessLicense;

    @Column(name = "business_email", length = 255)
    private String businessEmail;

    @Column(name = "business_hours", length = 255)
    private String businessHours;

    @Column(name = "city", length = 120)
    private String city;

    @Column(name = "state", length = 120)
    private String state;

    @Column(name = "click_owner_count", nullable = false)
    private Long clickOwnerCount = 0L;

    @Column(name = "click_anon_count", nullable = false)
    private Long clickAnonCount = 0L;

    @Column(name = "click_total_count", nullable = false)
    private Long clickTotalCount = 0L;

    @Column(name = "created_by", length = 120)
    private String createdBy;

    @Column(name = "updated_by", length = 120)
    private String updatedBy;

    @Builder.Default
    @Column(name = "reject", nullable = false)
    private Integer reject = 0; // 0 = not rejected, 1 = rejected


    @CreationTimestamp
    @Column(name = "created_on", updatable = false)
    private Instant createdOn;

    @UpdateTimestamp
    @Column(name = "updated_on")
    private Instant updatedOn;

    @PrePersist
    private void prePersistDefaults() {
        if (clickOwnerCount == null) clickOwnerCount = 0L;
        if (clickAnonCount == null) clickAnonCount = 0L;
        if (clickTotalCount == null) clickTotalCount = 0L;

        if (createdBy != null && createdBy.equalsIgnoreCase("admin")) {
            this.updatedBy = "admin";
            this.updatedOn = Instant.now();
        } else {
            this.updatedBy = null;
            this.updatedOn = null;
        }
    }

    @PreUpdate
    private void preUpdateDefaults() {
        if (clickOwnerCount == null) clickOwnerCount = 0L;
        if (clickAnonCount == null) clickAnonCount = 0L;
        if (clickTotalCount == null) clickTotalCount = 0L;
    }
}
