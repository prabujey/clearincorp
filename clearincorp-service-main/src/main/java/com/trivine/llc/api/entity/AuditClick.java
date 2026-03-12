package com.trivine.llc.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(
        name = "audit_click",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_owner_business",   columnNames = {"owner_id","business_id"}),
                @UniqueConstraint(name = "uq_session_business", columnNames = {"session_id","business_id"})
        },
        indexes = {
                @Index(name = "idx_business_time", columnList = "business_id, occurred_at"),
                @Index(name = "idx_session_time",  columnList = "session_id, occurred_at")
        }
)
public class AuditClick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MUST match your FK types; leave columnDefinition off since you've created the table already
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "owner_id")
    private BusinessOwner owner; // nullable for anonymous

    @Column(name = "session_id", length = 64)
    private String sessionId; // nullable when owner present

    @CreationTimestamp
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;
}
