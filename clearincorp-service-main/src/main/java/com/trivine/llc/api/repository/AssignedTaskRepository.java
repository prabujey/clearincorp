package com.trivine.llc.api.repository;

import com.trivine.llc.api.dto.*;
import com.trivine.llc.api.entity.AssignedTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AssignedTaskRepository extends JpaRepository<AssignedTask, String>, JpaSpecificationExecutor<AssignedTask> {

    @Query("""
select new com.trivine.llc.api.dto.TaskItemDto(
  t.id,
  t.assignee.loginUserId,
  t.status,
  t.companyId,
  t.notes,
  m.id,
  m.taskTitle,
  m.description,
  m.priority,
  m.dueDate,
  t.createdOn
)
from AssignedTask t
join t.master m
where (:assigneeId is null or t.assignee.loginUserId = :assigneeId)
  and (:companyId is null or t.companyId = :companyId)
  and (:masterId is null or m.id = :masterId)
  and (:status     is null or t.status = :status)
  and (:priority   is null or m.priority = :priority)
  and (:dueFrom    is null or m.dueDate >= :dueFrom)
  and (:dueTo      is null or m.dueDate <= :dueTo)
  and (:search     is null or (
        lower(m.taskTitle)    like lower(concat('%', :search, '%'))
     or lower(m.description)  like lower(concat('%', :search, '%'))
  ))
""")
    Page<TaskItemDto> searchWithMaster(
            @Param("assigneeId") Long assigneeId,
            @Param("masterId") String masterId,
            @Param("companyId") Long companyId,
            @Param("status") TaskStatus status,
            @Param("dueFrom") LocalDate dueDateFrom,
            @Param("dueTo") LocalDate dueDateTo,
            @Param("search") String search,
            @Param("priority") Priority priority,
            Pageable pageable);

    @Query("""
select new com.trivine.llc.api.dto.AssignedTaskStatsDto(
    COALESCE( SUM(CASE WHEN t.status = com.trivine.llc.api.dto.TaskStatus.PENDING THEN 1 ELSE 0 END), 0 ),
    COALESCE( SUM(CASE WHEN t.status = com.trivine.llc.api.dto.TaskStatus.DONE    THEN 1 ELSE 0 END), 0 ),
    COALESCE( SUM(CASE WHEN (m.dueDate is not null and m.dueDate < :today
                              and t.status <> com.trivine.llc.api.dto.TaskStatus.DONE)
                       THEN 1 ELSE 0 END), 0 ),
    COALESCE( SUM(CASE WHEN t.status = com.trivine.llc.api.dto.TaskStatus.IGNORED THEN 1 ELSE 0 END), 0 )
)
from AssignedTask t
join t.master m
where (:assigneeId is null or t.assignee.loginUserId = :assigneeId)
  and (:companyId is null or t.companyId = :companyId)
  and (:masterId  is null or m.id = :masterId)
""")
    AssignedTaskStatsDto statsWithMaster(
            @Param("assigneeId") Long assigneeId,
            @Param("masterId") String masterId,
            @Param("companyId") Long companyId,
            @Param("dueFrom") LocalDate dueDateFrom,
            @Param("dueTo") LocalDate dueDateTo,
            @Param("search") String search,
            @Param("priority") Priority priority,
            @Param("today") LocalDate today
    );




    @Modifying
    @Query("""
       update AssignedTask t
          set t.status = :status,
              t.notes  = :notes,
              t.attachmentKeys= :keys
        where t.id     = :taskId
       """)
    int updateStatusAndNotes(@Param("taskId") String taskId,
                             @Param("status") TaskStatus status,
                             @Param("notes")  String notes,
                             @Param("keys")   List<String> keys   );

    @Modifying
    @Query(value = """
    INSERT INTO assigned_task (id, login_user_id, assigned_task_master_id, status)
    SELECT UUID(), u.login_user_id, :masterId, :status
    FROM login_user u
    WHERE u.role_id IN (:roleId) AND u.is_active = 1 AND u.is_deleted = 0
    """, nativeQuery = true)
    int bulkInsertForRole(@Param("masterId") String masterId,
                          @Param("status") TaskStatus status,
                          @Param("roleId") List<Long> roleId);


    @Modifying
    @Query("delete from AssignedTask t where t.master.id = :masterId")
    int deleteByMasterId(@org.springframework.data.repository.query.Param("masterId") String masterId);


    // Your existing paginated query (no changes needed)
    @Query(value = """
            SELECT new com.trivine.llc.api.dto.AssignedTaskAnalyticsDto(
                at.id,
                at.notes,
                at.status,
                at.createdOn,
                at.updatedOn,
                at.attachmentKeys,
                assignee.firstName,
                assignee.lastName,
                assignee.email,
                c.companyName,
                c.state
            )
            FROM AssignedTask at
            JOIN at.assignee assignee
            LEFT JOIN Company c ON at.companyId = c.companyId
            WHERE at.master.id = :masterId
            AND (:status IS NULL OR at.status = :status) 
            """, // <-- This line handles the optional filter
            countQuery = """
            SELECT COUNT(at)
            FROM AssignedTask at
            WHERE at.master.id = :masterId
            AND (:status IS NULL OR at.status = :status)
            """) // <-- This line handles the optional filter
    Page<AssignedTaskAnalyticsDto> findTaskAnalyticsByMasterId(
            @Param("masterId") String masterId,
            @Param("status") TaskStatus status, // <-- Add status parameter
            Pageable pageable);

    /**
     * NEW QUERY:
     * Fetches the total counts for each status for a given masterId.
     * Returns a List of Object arrays, where each array is [TaskStatus, Long count]
     */
    @Query("""
            SELECT at.status, COUNT(at)
            FROM AssignedTask at
            WHERE at.master.id = :masterId
            GROUP BY at.status
            """)
    List<Object[]> getStatusCountsByMasterId(@Param("masterId") String masterId);
}
