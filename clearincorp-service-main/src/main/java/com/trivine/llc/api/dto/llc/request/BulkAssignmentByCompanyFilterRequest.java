package com.trivine.llc.api.dto.llc.request;

import com.trivine.llc.api.dto.Priority;
import com.trivine.llc.api.dto.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BulkAssignmentByCompanyFilterRequest {
    // Master (task header)
    @NotNull private Long createdById;     // LoginUser who creates the master
    @NotBlank private String taskTitle;
    private String description;
    private Priority priority;
    private LocalDate dueDate;

    private TaskStatus initialStatus=TaskStatus.PENDING;

    // Company filters (all optional)
    private List<String> states;                 // IN filter
    private List<String> principalActivity;      // IN filter
    private LocalDate effectiveFrom;             // company_effective_date >=
    private LocalDate effectiveTo;               // company_effective_date <=
}
