package com.trivine.llc.api.repository;


import com.trivine.llc.api.dto.PersonalTaskStatsDto;
import com.trivine.llc.api.dto.Priority;
import com.trivine.llc.api.entity.PersonalTask;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface PersonalTaskRepository extends
        JpaRepository<PersonalTask, UUID>,
        JpaSpecificationExecutor<PersonalTask> {

    @Query("SELECT new com.trivine.llc.api.dto.PersonalTaskStatsDto(" +
            "   COALESCE(SUM(CASE WHEN t.completed = false THEN 1 ELSE 0 END), 0), " +
            "   COALESCE(SUM(CASE WHEN t.completed = true THEN 1 ELSE 0 END), 0), " +
            "   COALESCE(SUM(CASE WHEN t.completed = false AND t.dueDate < :today THEN 1 ELSE 0 END), 0)" +
            ") " +
            "FROM PersonalTask t " +
            "WHERE (:loginUserId IS NULL OR t.loginUserId = :loginUserId) "
    )
    PersonalTaskStatsDto getTaskStats(
            @Param("loginUserId") Long loginUserId,
            @Param("today") LocalDate today
    );
}

