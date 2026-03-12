package com.trivine.llc.api.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "reason_for_applying_master")
public class ReasonForApplying {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String label;

    @Column(name = "is_active", nullable = false)
    private Boolean active;
}

