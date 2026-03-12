package com.trivine.llc.api.dto.request;

import com.trivine.llc.api.dto.Priority;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssignedTaskMasterSearchDto {

    // text search across taskTitle + description
    private String query;

    // createdBy (loginUserId)
    private Long createdById;

    // exact priority or leave null
    private Priority priority;

    // due date range
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueTo;

    // createdOn range (optional)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdTo;


    private LocalDateTime updatedFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime updatedTo;
}
