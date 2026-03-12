package com.trivine.llc.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "login_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginUser {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "login_user_id", nullable = false)
    private Long loginUserId;

    @Size(max = 255)
    @Column(name = "first_name")
    private String firstName;

    @Size(max = 255)
    @Column(name = "last_name")
    private String lastName;

    @Email(regexp = ".+[@].+[\\.].+", message = "Email should be valid")
    @Size(max = 255)
    @Column(name = "email", unique = true)
    private String email;

    @Size(max = 14) // ensures length is valid
    @Pattern(regexp = "^(|\\(\\d{3}\\) \\d{3}-\\d{4})$")
    @Column(name = "phone_number")
    private String phoneNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role roleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_company_id")
    private UserCompany userCompanyId;

    @Column(name = "created_on", insertable = false, updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "created_by")
    @Size(max = 255)
    private String createdBy;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted = false;

    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "loginUser", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Company> companies;

    public LoginUser(Long loginId) {
        this.loginUserId = loginId;
    }


}
