package com.trivine.llc.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "forum_title")
@Getter @Setter
public class ForumTitle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "title_id")
    private Long titleId;

    @NotNull
    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @NotBlank
    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "report_count", nullable = false)
    private int reportCount = 0;


    @Column(name = "hidden_by_login_user_ids")
    private String hiddenByLoginUserIds;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "login_user_id", nullable = false)
    private LoginUser createdBy;

    @NotBlank
    @Column(name = "description_md", nullable = false, columnDefinition = "TEXT")
    private String descriptionMd;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();


}
