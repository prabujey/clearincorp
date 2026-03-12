package com.trivine.llc.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "formation_status_master")
public class FormationStatusMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "formation_status_id", nullable = false)
    private Long formationStatusId;

    @NotBlank(message = "Status name is required")
    @Size(max = 255)
    @Column(name = "formation_status_name", nullable = false)
    private String formationStatusName;

    @Column(name = "formation_status_desc", columnDefinition = "TEXT")
    @Size(max = 255)
    private String formationStatusDesc;

    @Column(name = "created_on", insertable = false, updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
