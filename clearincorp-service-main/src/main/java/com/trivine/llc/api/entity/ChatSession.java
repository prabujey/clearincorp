package com.trivine.llc.api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_session")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true, length = 64)
    private String sessionId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_email", length = 255)
    private String userEmail;

    /**
     * Store as JSON string. DB column is JSON type.
     */
    @Column(name = "conversation", nullable = false, columnDefinition = "json")
    private String conversation;

    @Column(name = "rating")
    private Byte rating;

    @Column(name = "review_comment", length = 1000)
    private String reviewComment;

    @Column(name = "is_issue", nullable = false)
    private boolean issue;

    @Column(name = "is_resolved", nullable = false)
    private boolean resolved;

    @Column(name = "created_at", updatable = false,
            columnDefinition = "timestamp default current_timestamp")
    private LocalDateTime createdAt;

    @Column(name = "updated_at",
            columnDefinition = "timestamp default current_timestamp on update current_timestamp")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
