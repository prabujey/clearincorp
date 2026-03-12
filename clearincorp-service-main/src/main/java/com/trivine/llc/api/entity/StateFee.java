package com.trivine.llc.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "state_fee")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StateFee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "state_id", nullable = false)
    private Long stateId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "state", nullable = false, unique = true)
    private String state;

    @NotNull
    @Digits(integer = 36, fraction = 2)
    @Column(name = "state_fee", nullable = false, precision = 38, scale = 2)
    private BigDecimal stateFee;

    @Size(max = 255)
    @Column(name = "state_key")
    private String stateKey;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;   // maps to is_active
}
