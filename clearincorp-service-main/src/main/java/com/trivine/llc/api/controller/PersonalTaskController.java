package com.trivine.llc.api.controller;

import com.trivine.llc.api.dto.*;
import com.trivine.llc.api.dto.response.PagedResponse;
import com.trivine.llc.api.service.PersonalTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.trivine.llc.api.controller.SortUtil.buildSort;

@RestController
@RequestMapping("/personal-tasks")
@RequiredArgsConstructor
public class PersonalTaskController {

    private final PersonalTaskService service;

    @PostMapping
    public PersonalTaskResponseDto create(@Valid @RequestBody PersonalTaskCreateDto dto) {
        return service.create(dto);
    }

    @GetMapping("/{id}")
    public PersonalTaskResponseDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public PersonalTaskResponseDto update(@PathVariable UUID id,
                                          @Valid @RequestBody PersonalTaskUpdateDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @PatchMapping("/{id}/completed")
    public PersonalTaskResponseDto markCompleted(@PathVariable UUID id,
                                                 @RequestParam boolean completed) {
        return service.markCompleted(id, completed);
    }

    @GetMapping
    public PersonalTaskSearchResponse search(
            @RequestParam(required = false) Long loginUserId,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Boolean completed,
            @RequestParam(required = false) LocalDate dueDateFrom,
            @RequestParam(required = false) LocalDate dueDateTo,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, name = "sort") List<String> sortParams
    ) {
        var ALLOWED = Set.of("id","taskTitle","priority","dueDate","completed","createdOn","updatedOn","loginUserId");
        Sort defaultSort = Sort.by(Sort.Order.asc("dueDate"));

        Pageable pageable = PageRequest.of(page, size, buildSort(sortParams, ALLOWED, defaultSort));

        var filter = PersonalTaskFilterDto.builder()
                .loginUserId(loginUserId)
                .priority(priority)
                .completed(completed)
                .dueDateFrom(dueDateFrom)
                .dueDateTo(dueDateTo)
                .search(search)
                .build();

        return service.search(filter, pageable);
    }

}
