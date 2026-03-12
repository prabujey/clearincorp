package com.trivine.llc.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Table(name = "formation_status")
public class FormationStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_status_id", nullable = false)
    private Long companyStatusId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private FormationStatusMaster status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "login_user_id")
    private LoginUser loginUser;

    @Column(name = "status_date", nullable = false)
    private LocalDate   statusDate;

    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
