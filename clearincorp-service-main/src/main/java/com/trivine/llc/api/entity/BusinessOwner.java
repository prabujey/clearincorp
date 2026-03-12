package com.trivine.llc.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "business_owners")
public class BusinessOwner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "login_user_id")
    private Long loginUserId; // ✅ NEW

    @Column(name = "first_name", length = 100, nullable = false)
    private String firstName;

    @Column(name = "last_name", length = 100, nullable = false)
    private String lastName;

    @Column(name = "personal_email", length = 255, nullable = false)
    private String personalEmail;

    @Column(name = "contact_number", length = 30)
    private String contactNumber;

    @CreationTimestamp
    @Column(name = "created_on", updatable = false)
    private Instant createdOn;
}
