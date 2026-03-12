

package com.trivine.llc.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "forum_reply")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForumReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reply_id")
    private Long replyId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_reply_id")
    private ForumReply parentReply;

    @OneToMany(mappedBy = "parentReply", cascade = CascadeType.ALL)
    private Set<ForumReply> children = new HashSet<>();

    @Column(name = "parent_reply_id", insertable = false, updatable = false)
    private Long parentReplyIdFk;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private ForumPost post;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "login_user_id", nullable = false)
    private LoginUser loginUser;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "title_id", nullable = false)
    private ForumTitle title;

    @Column(name = "hidden_by_login_user_ids")
    private String hiddenByLoginUserIds;


    @Lob
    @Column(name = "content_md", nullable = false)
    private String contentMd;

    @Column(nullable = false)
    private short depth;

    @Column(name = "likes_count", nullable = false)
    private int likesCount;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "edited_at")
    private Instant editedAt;

    @Column(name = "report_count", nullable = false)
    private int reportCount = 0;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
