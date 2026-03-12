package com.trivine.llc.api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "forum_topic_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForumTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "topic_id")
    private Long topicId;

    @Column(name = "topic_name", nullable = false, unique = true, length = 120)
    private String topicName;

    @Column(length = 255)
    private String description;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}

