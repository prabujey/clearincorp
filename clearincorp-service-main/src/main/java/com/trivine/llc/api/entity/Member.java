package com.trivine.llc.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "member")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @NotBlank
    @Size(max = 50)
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @NotBlank
    @Size(max = 50)
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Size(max = 255)
    @Column(name = "street_address_1")
    private String streetAddress1;

    @Size(max = 255)
    @Column(name = "street_address_2")
    private String streetAddress2;

    @NotBlank
    @Size(max = 100)
    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @NotBlank
    @Size(max = 50)
    @Column(name = "state", nullable = false, length = 50)
    private String state;

    @NotBlank
    @Size(max = 20)
    @Column(name = "zip_code", nullable = false, length = 20)
    private String zipCode;

    @NotBlank
    @Size(max = 50)
    @Column(name = "country", nullable = false, length = 50)
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

    @NotBlank
    @Size(max = 100)
    @Column(name = "ownership", nullable = false, length = 100)
    private String ownership;

    @Column(name = "created_on", insertable = false, updatable = false)
    private LocalDateTime createdOn;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_ein_responsible_party")
    private Boolean isEinResponsibleParty;

    @Column(name = "use_address")
    private Boolean useAddress;
}
