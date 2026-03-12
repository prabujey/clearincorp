package com.trivine.llc.api.entity;

import com.trivine.llc.api.dto.Priority;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "personal_tasks",
        indexes = {
                @Index(name="idx_personal_tasks_login_user_id", columnList = "login_user_id"),
                @Index(name="idx_personal_tasks_due_date", columnList = "due_date"),
                @Index(name="idx_personal_tasks_priority", columnList = "priority"),
                @Index(name="idx_personal_tasks_completed", columnList = "completed")
        })
public class PersonalTask {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.VARCHAR)                 // stores as VARCHAR(36)
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private UUID id;

    @Column(name = "task_title", nullable = false, length = 300)
    private String taskTitle;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "priority", nullable = false, length = 16)
    private Priority priority;


    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "completed", nullable = false)
    private boolean completed;

    @CreatedDate
    @Column(name = "created_on", nullable = false, updatable = false)
    private Instant createdOn;

    @LastModifiedDate
    @Column(name = "updated_on")
    private Instant updatedOn;

    @NotNull
    @Column(name = "login_user_id", nullable = false)
    private Long loginUserId; // FK to login_user.login_user_id
}
