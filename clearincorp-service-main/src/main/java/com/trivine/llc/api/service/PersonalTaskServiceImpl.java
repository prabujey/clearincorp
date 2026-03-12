package com.trivine.llc.api.service;


import com.trivine.llc.api.dto.*;
import com.trivine.llc.api.dto.response.PagedResponse;
import com.trivine.llc.api.entity.PersonalTask;
import com.trivine.llc.api.mapper.PersonalTaskMapper;
import com.trivine.llc.api.repository.PersonalTaskRepository;
import com.trivine.llc.api.repository.specifications.PersonalTaskSpecifications;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PersonalTaskServiceImpl implements PersonalTaskService {

    private final PersonalTaskRepository repository;
    private final PersonalTaskMapper mapper;

    @Override
    public PersonalTaskResponseDto create(PersonalTaskCreateDto dto) {
        PersonalTask entity = mapper.toEntity(dto);
        entity.setCompleted(false); // default new tasks as not completed
        PersonalTask saved = repository.save(entity);
        return mapper.toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    @Override
    public PersonalTaskResponseDto get(UUID id) {
        return mapper.toResponseDto(findOrThrow(id));
    }

    @Override
    public PersonalTaskResponseDto update(UUID id, PersonalTaskUpdateDto dto) {
        PersonalTask entity = findOrThrow(id);
        mapper.updateEntityFromDto(dto, entity);
        return mapper.toResponseDto(repository.save(entity));
    }

    @Override
    public void delete(UUID id) {
        PersonalTask entity = findOrThrow(id);
        repository.delete(entity);
    }

    @Override
    public PersonalTaskResponseDto markCompleted(UUID id, boolean completed) {
        PersonalTask entity = findOrThrow(id);
        entity.setCompleted(completed);
        return mapper.toResponseDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    @Override
    public PersonalTaskSearchResponse search(PersonalTaskFilterDto filter, Pageable pageable) {

        // 1. GET PAGINATED RESULTS (1st DB Call)
        // This spec still respects ALL filters given by the user
        var mainSpec = PersonalTaskSpecifications.byFilter(filter);
        Page<PersonalTaskResponseDto> page = repository.findAll(mainSpec, pageable)
                .map(mapper::toResponseDto);

        // 2. GET STATS (2nd DB Call)
        // We pass the "base" filters to our new, efficient query.
        // Note: The logic for handling the search string (trim, etc.) must match the query.
        String search = (filter.getSearch() != null && !filter.getSearch().isBlank())
                ? filter.getSearch().trim()
                : null;

        PersonalTaskStatsDto stats = repository.getTaskStats(
                filter.getLoginUserId(),
                LocalDate.now()
        ) ;

        // 3. BUILD THE FINAL RESPONSE
        return PersonalTaskSearchResponse.builder()
                .pageData(PagedResponse.from(page))
                .stats(stats)
                .build();
    }

    private PersonalTask findOrThrow(UUID id) {
        return repository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("PersonalTask not found: " + id)
        );
    }
}

