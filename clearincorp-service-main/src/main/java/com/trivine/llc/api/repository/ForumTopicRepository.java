package com.trivine.llc.api.repository;

import com.trivine.llc.api.dto.response.ForumTopicDto;
import com.trivine.llc.api.entity.ForumTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ForumTopicRepository extends JpaRepository<ForumTopic, Long> {

    Optional<ForumTopic> findByTopicNameIgnoreCase(String topicName);

    boolean existsByTopicNameIgnoreCase(String topicName);

    @Query("SELECT new com.trivine.llc.api.dto.response.ForumTopicDto(t.topicId, t.topicName) FROM ForumTopic t ORDER BY t.topicName ASC")
    List<ForumTopicDto> findAllLite();
}
