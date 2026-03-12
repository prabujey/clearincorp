package com.trivine.llc.api.entity;

import com.trivine.llc.api.crypto.StringListJsonConverter;
import com.trivine.llc.api.dto.Priority;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "assigned_task_master")
@Getter @Setter @NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignedTaskMaster {

    @Id
    @GeneratedValue
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.VARCHAR)               // store as text
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private String id;

    @Column(name = "task_title", nullable = false, length = 255)
    private String taskTitle;

    @Column(name = "description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private LoginUser createdBy;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "priority")
    private Priority priority;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "created_on", insertable = false, updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "updated_on", insertable = false, updatable = false)
    private LocalDateTime updatedOn;

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "attachment_keys", columnDefinition = "json")
    private List<String> attachmentKeys;

    @Column(name = "assign_details", columnDefinition = "json")
    private String assignDetails;
}
