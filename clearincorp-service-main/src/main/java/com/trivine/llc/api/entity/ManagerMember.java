package com.trivine.llc.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "manager")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManagerMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "manager_id", nullable = false)
    private Long managerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Size(max = 255)
    @Column(name = "first_name")
    private String firstName;

    @Size(max = 255)
    @Column(name = "last_name")
    private String lastName;

    @Size(max = 255)
    @Column(name = "street_address_1")
    private String streetAddress1;

    @Size(max = 255)
    @Column(name = "street_address_2")
    private String streetAddress2;

    @Size(max = 255)
    @Column(name = "city")
    private String city;

    @Size(max = 255)
    @Column(name = "state")
    private String state;

    @Size(max = 255)
    @Column(name = "zip_code")
    private String zipCode;

    @Size(max = 255)
    @Column(name = "country")
    private String country;

    @Email(regexp = ".+[@].+[\\.].+", message = "Email should be valid")
    @Size(max = 255)
    @Column(name = "email")
    private String email;

    @Pattern(
            regexp = "^\\(\\d{3}\\) \\d{3}-\\d{4}$",
            message = "Phone number must be in the format (123) 456-7890"
    )
    @Size(max = 255)
    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "created_on", insertable = false, updatable = false)
    private LocalDateTime createdOn;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "use_address")
    private Boolean useAddress;
}
