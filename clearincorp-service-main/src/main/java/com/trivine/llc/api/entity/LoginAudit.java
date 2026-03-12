package com.trivine.llc.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_audit")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "login_audit_id", nullable = false)
    private Long loginAuditId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "login_user_id")
    private LoginUser loginUser;

    @Column(name = "created_on", insertable = false, updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "token_value")
    @Size(max = 255)
    private String tokenValue;

    @Column(name = "ip_address")
    @Size(max = 255)
    private String ipAddress;

    @Column(name = "browser_id")
    @Size(max = 255)
    private String browserId;

    @Column(name = "browser")
    @Size(max = 255)
    private String browser;

    @Column(name = "location")
    @Size(max = 255)
    private String location;
}
