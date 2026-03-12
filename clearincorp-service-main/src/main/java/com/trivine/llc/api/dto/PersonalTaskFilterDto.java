package com.trivine.llc.api.dto;


import lombok.*;

import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class
PersonalTaskFilterDto {
    private Long loginUserId;         // filter tasks for a user
    private Priority priority;        // LOW/MEDIUM/HIGH
    private Boolean completed;        // true/false
    private LocalDate dueDateFrom;    // due date >=
    private LocalDate dueDateTo;      // due date <=
    private String search;            // search in title
}

