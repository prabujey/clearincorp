package com.trivine.llc.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reg_agent_master")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisteredAgentMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reg_agent_id", nullable = false)
    private Long regAgentId;

    @Size(max = 255)
    @Column(name = "reg_agent_first_name")
    private String RegAgentFirstName;

    @Size(max = 255)
    @Column(name = "reg_agent_second_name")
    private String regAgentSecondName;

    @Size(max = 255)
    @Column(name = "street_address_1")
    private String streetAddress1;

    @Size(max = 255)
    @Column(name = "street_address_2")
    private String streetAddress2;

    @Size(max = 255)
    @Column(name = "city", nullable = false)
    private String city;

    @Size(max = 255)
    @Column(name = "state", nullable = false)
    private String state;

    @Size(max = 255)
    @Column(name = "zip_code", nullable = false)
    private String zipCode;

    @Size(max = 255)
    @Column(name = "country", nullable = false)
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
    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "created_on", insertable = false, updatable = false)
    private LocalDateTime createdOn;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;
}
