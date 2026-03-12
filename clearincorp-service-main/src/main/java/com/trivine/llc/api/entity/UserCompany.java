package com.trivine.llc.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_company")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserCompany {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false)@Size(max = 255)
    private String name;

    @Column(name = "ein", nullable = false)@Size(max = 255)
    private String ein;

    @Pattern(
            regexp = "^\\(\\d{3}\\) \\d{3}-\\d{4}$",
            message = "Phone number must be in the format (123) 456-7890"
    )
    @Column(name = "phone", length = 20)
    private String phoneNumber;

    @Column(name = "email")
    @Email(regexp = ".+[@].+[\\.].+", message = "Email should be valid")
    private String email;

    @Column(name = "address")
    @Size(max = 255)
    private String address;

    @Column(name = "city")
    @Size(max = 255)
    private String city;

    @Column(name = "state", length = 2)
    @Size(max = 2)
    private String state;

    @Column(name = "country", length = 20)
    @Size(max = 20)
    private String country;

    @Column(name = "zip", length = 10)
    @Size(max = 10)
    private String zipCode;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "add_by", length = 100)
    @Size(max = 100)
    private String addBy;

    @Column(name = "add_date", nullable = false, updatable = false)
    private LocalDateTime addDate;

    @Column(name = "last_mod_by", length = 100)
    @Size(max = 100)
    private String lastModBy;

    @Column(name = "last_mod_date")
    private LocalDateTime lastModDate;

    public UserCompany(Long id) {
        this.id = id;
    }

    @PrePersist
    protected void onCreate() {
        if (this.addDate == null) this.addDate = LocalDateTime.now();
        this.isDeleted = false;
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastModDate = LocalDateTime.now();
    }
}
