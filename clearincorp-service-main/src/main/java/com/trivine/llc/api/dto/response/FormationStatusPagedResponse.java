package com.trivine.llc.api.dto.response;

import com.trivine.llc.api.entity.FormationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FormationStatusPagedResponse {
    private Page<FormationStatus> page;
    private Map<String, Long> statusCounts;

}

