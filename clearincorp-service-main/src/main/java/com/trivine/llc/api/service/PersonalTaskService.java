package com.trivine.llc.api.service;



import com.trivine.llc.api.dto.*;
import org.springframework.data.domain.*;

import java.util.UUID;

public interface PersonalTaskService {
    PersonalTaskResponseDto create(PersonalTaskCreateDto dto);
    PersonalTaskResponseDto get(UUID id);
    PersonalTaskResponseDto update(UUID id, PersonalTaskUpdateDto dto);
    void delete(UUID id);
    PersonalTaskResponseDto markCompleted(UUID id, boolean completed);
    PersonalTaskSearchResponse search(PersonalTaskFilterDto filter, Pageable pageable);
}
