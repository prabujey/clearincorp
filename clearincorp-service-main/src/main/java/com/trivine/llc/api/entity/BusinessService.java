package com.trivine.llc.api.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "business_service")
public class BusinessService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_id")
    private Long serviceId;

    @Column(name = "service_name", length = 150, nullable = false, unique = true)
    private String serviceName;

    @Column(name = "description", length = 500)
    private String description;
}
