package com.trivine.llc.api.entity;

import com.trivine.llc.api.crypto.StringListJsonConverter;
import com.trivine.llc.api.dto.TaskStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "assigned_task")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssignedTask {

    @Id
    @GeneratedValue
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id", nullable = false, length = 36)
    private String id; // UUID string

    @Column(name = "company_id")
    private Long companyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "login_user_id", nullable = false)
    private LoginUser assignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_task_master_id", nullable = false)
    private AssignedTaskMaster master;

    @Column(name = "notes", length = 150)
    private String notes;


    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    private TaskStatus status; // 0=pending, 1=done, 2=ignored

    @Column(name = "created_on", insertable = false, updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "updated_on", insertable = false, updatable = false)
    private LocalDateTime updatedOn;

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "attachment_keys", columnDefinition = "json")
    private List<String> attachmentKeys;

}

