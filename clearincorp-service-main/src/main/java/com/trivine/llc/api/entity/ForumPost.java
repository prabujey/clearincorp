package com.trivine.llc.api.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "forum_post")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ForumPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long postId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "login_user_id", referencedColumnName = "login_user_id")
    private LoginUser loginUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", referencedColumnName = "topic_id")
    private ForumTopic topic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "title_id", nullable = false)
    private ForumTitle title;

    @Lob
    @Column(name = "description_md", nullable = false)
    private String descriptionMd;

    @Column(name = "is_pinned", nullable = false)
    private boolean pinned;

    @Column(name = "pinned_at")
    private Instant pinnedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pinned_by_login_user_id", referencedColumnName = "login_user_id")
    private LoginUser pinnedBy;

    @Column(name = "hidden_by_login_user_ids")
    private String hiddenByLoginUserIds;


    @Column(name = "likes_count", nullable = false)
    private int likesCount;

    @Column(name = "views_count", nullable = false)
    private int viewsCount;

    @Column(name = "reply_count", nullable = false)
    private int replyCount;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "edited_at")
    private Instant editedAt;

    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt;

    @Column(name = "report_count", nullable = false)
    private int reportCount = 0;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        if (lastActivityAt == null) lastActivityAt = now;
    }

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    @BatchSize(size = 50)
    private List<ForumReply> replies = new ArrayList<>();


}