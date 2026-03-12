package com.trivine.llc.api.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trivine.llc.api.dto.*;
import com.trivine.llc.api.dto.llc.request.BulkAssignmentByCompanyFilterRequest;
import com.trivine.llc.api.dto.request.AssignedTaskMasterSearchDto;
import com.trivine.llc.api.dto.request.BulkAdminAssignmentRequest;
import com.trivine.llc.api.dto.request.BulkSuperFilerAssignmentRequest;
import com.trivine.llc.api.dto.request.TaskStatusUpdateRequest;
import com.trivine.llc.api.dto.response.PagedResponse;
import com.trivine.llc.api.entity.AssignedTaskMaster;
import com.trivine.llc.api.service.AssignmentService;
import com.trivine.llc.api.service.S3Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;


@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class AssignmentController {
    private final ObjectMapper objectMapper;
    private final AssignmentService service;
    private final S3Service s3FileService;

    @PostMapping(value = "/admin/bulk-assign", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AssignedTaskMasterDto> createBulk(@RequestPart("request") String requestJson,
                                                            @RequestPart(name = "files", required = false) List<MultipartFile> files) throws IOException {
        BulkAdminAssignmentRequest req = objectMapper.readValue(requestJson, BulkAdminAssignmentRequest.class);  // <-- parse
        AssignedTaskMasterDto dto = service.createAndAssignForAdmin(req, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping(value = "/SuperFiler/bulk-assign", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AssignedTaskMasterDto> assignByCompanyFilter(@RequestPart("request") String requestJson, @RequestPart(name = "files", required = false) List<MultipartFile> files) throws IOException {

        BulkAssignmentByCompanyFilterRequest req = objectMapper.readValue(requestJson, BulkAssignmentByCompanyFilterRequest.class);
        AssignedTaskMasterDto dto=service.createAndAssignByCompanyFilter(req, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping(value ="/SuperFiler/selected-assign", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AssignedTaskMasterDto> bulkAssign(@RequestPart("request") String requestJson, @RequestPart(name = "files", required = false) List<MultipartFile> files) throws IOException {
        BulkSuperFilerAssignmentRequest req = objectMapper.readValue(requestJson, BulkSuperFilerAssignmentRequest.class);
        AssignedTaskMasterDto dto = service.createAndAssignForSuperFiler(req, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping
    public AssignedTaskResponse searchTaskItems(@RequestParam(required = false) Long assigneeId, @RequestParam(required = false) String masterId, @RequestParam(required = false) Long companyId, @RequestParam(required = false) TaskStatus status, @RequestParam(required = false) LocalDate dueDateFrom, @RequestParam(required = false) LocalDate dueDateTo, @RequestParam(required = false) String search, @RequestParam(required = false) Priority priority, @RequestParam(required = false) Boolean completed, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, @RequestParam(required = false) List<String> sort) {
        return service.searchTaskItems(assigneeId,masterId, companyId, status, dueDateFrom, dueDateTo, search, priority, completed, page, size, sort);
    }

    @GetMapping("/assignedTask")
    public PagedResponse<AssignedTaskMasterDto> search(@RequestParam(required = false) String query, @RequestParam(required = false) Long createdById, @RequestParam(required = false) Priority priority, @RequestParam(required = false) LocalDate dueFrom, @RequestParam(required = false) LocalDate dueTo, @RequestParam(required = false) LocalDateTime createdFrom, @RequestParam(required = false) LocalDateTime createdTo, @RequestParam(required = false) LocalDateTime updatedFrom, @RequestParam(required = false) LocalDateTime updatedTo, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, @RequestParam(required = false) List<String> sort) {
        AssignedTaskMasterSearchDto filter = AssignedTaskMasterSearchDto.builder().query(query).createdById(createdById).priority(priority).dueFrom(dueFrom).dueTo(dueTo).createdFrom(createdFrom).createdTo(createdTo).updatedFrom(updatedFrom).updatedTo(updatedTo).build();
        return service.search(filter, page, size, sort);
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AssignedTaskMasterDto> updateAssignedTask(@RequestPart("request") String requestJson, @RequestPart(name = "files", required = false) List<MultipartFile> files) throws IOException {
        AssignedTaskMasterDto req = objectMapper.readValue(requestJson, AssignedTaskMasterDto.class);
        return ResponseEntity.ok().body(service.update(req,files));
    }

    @DeleteMapping("/master")
    public ResponseEntity<Void> deleteMaster(@RequestParam String id) {
        service.deleteMasterAndChildren(id);
        return ResponseEntity.noContent().build(); // 204
    }

    @PutMapping( value = "/status",consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AssignedTaskMasterDto> taskUpdate(@RequestPart("request") String requestJson, @RequestPart(name = "files", required = false) List<MultipartFile> files) throws IOException {
        TaskStatusUpdateRequest req = objectMapper.readValue(requestJson, TaskStatusUpdateRequest.class);

        service.updateTaskStatus(req,files);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/api/directory-presigned-urls")
    public List<String> getPresignedUrlsForDirectory(@RequestParam UUID masterId) {
        String prefix = "tasks/masters/%s/".formatted(masterId);
        return s3FileService.getPresignedUrlsForDirectory(prefix);
    }
    @GetMapping("/analytics/{masterId}")
    public ResponseEntity<TaskAnalyticsResponse> getAnalytics(
            @PathVariable String masterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) TaskStatus status) { // <-- Parameters changed

        // Create Pageable object manually
        // We'll keep the sort by createdOn as a sensible default
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdOn"));

        // Call the service with the new status parameter
        TaskAnalyticsResponse analyticsResponse =
                service.getFullTaskAnalytics(masterId, status, pageable);

        return ResponseEntity.ok(analyticsResponse);
    }
}