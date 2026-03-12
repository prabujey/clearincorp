package com.trivine.llc.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_master")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    @NotBlank(message = "Service name is required")
    @Size(max = 100)
    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;

    @Size(max = 255)
    @Column(name = "service_desc")
    private String serviceDesc;

    @NotNull(message = "Service price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Service price must be greater than 0")
    @Digits(integer = 8, fraction = 2)
    @Column(name = "service_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal servicePrice;

    @Column(name = "created_on", insertable = false, updatable = false)
    private LocalDateTime createdOn;

    @Builder.Default
    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;
}
