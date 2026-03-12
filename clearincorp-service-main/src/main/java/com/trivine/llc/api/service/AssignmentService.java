package com.trivine.llc.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trivine.llc.api.constants.RoleEnum;
import com.trivine.llc.api.controller.SortUtil;
import com.trivine.llc.api.dto.*;
import com.trivine.llc.api.dto.llc.request.BulkAssignmentByCompanyFilterRequest;
import com.trivine.llc.api.dto.request.*;
import com.trivine.llc.api.dto.response.PagedResponse;
import com.trivine.llc.api.entity.AssignedTaskMaster;
import com.trivine.llc.api.entity.LoginUser;
import com.trivine.llc.api.mapper.AssignedTaskMasterMapper;
import com.trivine.llc.api.repository.AssignedTaskMasterRepository;
import com.trivine.llc.api.repository.AssignedTaskRepository;
import com.trivine.llc.api.repository.specifications.AssignedTaskMasterSpecs;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class AssignmentService {

    private final AssignedTaskMasterRepository masterRepo;
    private final AssignedTaskRepository taskRepo;
    @PersistenceContext
    private EntityManager em;
    // At the top of your AssignmentService class
    @Autowired
    private EntityManager entityManager;
    private final AssignedTaskMasterMapper masterMapper;
    private final Long vendorRoleId = RoleEnum.Vendor.getRoleId();
    private final Long superFilerRoleId = RoleEnum.SuperFiler.getRoleId();
    private final S3Service s3;
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Map<String, Set<String>> CT_TO_EXT = Map.of(
            "application/pdf", Set.of("pdf"),
            "image/png",       Set.of("png"),
            "image/jpeg",      Set.of("jpg", "jpeg"),
            "image/webp",      Set.of("webp")
    );

    // validate types up-front
    private static final List<String> ALLOWED = List.of(
            MediaType.APPLICATION_PDF_VALUE,
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/webp" // optional
    );

    @Transactional // Make sure your service method is transactional
    public AssignedTaskMasterDto createAndAssignForAdmin(BulkAdminAssignmentRequest req, List<MultipartFile> files) throws IOException {

        // 1) Build entity *without* ID or attachments
        LoginUser creator = new LoginUser();
        creator.setLoginUserId(req.getCreatedById());
        Map<String, Object> detailsMap = new HashMap<>();
        detailsMap.put("assignType", "Role Based assign");
        detailsMap.put("roleId", req.getAssigneeRole());
        ObjectMapper objectMapper = new ObjectMapper();
        AssignedTaskMaster master = AssignedTaskMaster.builder()
                .taskTitle(req.getTaskTitle())
                .description(req.getDescription())
                .createdBy(creator)
                .priority(req.getPriority())
                .dueDate(req.getDueDate())
                .assignDetails(objectMapper.writeValueAsString(detailsMap))
                .build();

        // 2) Save it FIRST to generate the ID
        // This will be a clean INSERT
        master = masterRepo.save(master);

        // 3) NOW you have the ID. Upload files.
        String prefix = "tasks/masters/%s/".formatted(master.getId());
        List<String> keys = uploadAndCollectKeys(prefix, files);

        // 4) Set the keys on the *managed* entity
        master.setAttachmentKeys(keys);

        // 5) Save AGAIN.
        // Because 'master' is already managed and "dirty",
        // this will correctly trigger an UPDATE.
        master = masterRepo.save(master);

        // 6) Bulk assign
        final TaskStatus initStatus = req.getInitialStatus();


        int inserted = taskRepo.bulkInsertForRole(master.getId(), initStatus, req.getAssigneeRole());

        // 7) Return DTO
        return masterMapper.toDto(master);
    }


    @Transactional
    public AssignedTaskMasterDto createAndAssignForSuperFiler(BulkSuperFilerAssignmentRequest req,List<MultipartFile> files) throws IOException {



        // 1) Build entity *without* ID or attachments
        LoginUser creator = new LoginUser();
        creator.setLoginUserId(req.getCreatedById());
        Map<String, Object> detailsMap = new HashMap<>();
        detailsMap.put("assignType", "Selected assign");
        ObjectMapper objectMapper = new ObjectMapper();
        AssignedTaskMaster master = AssignedTaskMaster.builder()
                .taskTitle(req.getTaskTitle())
                .description(req.getDescription())
                .createdBy(creator)
                .priority(req.getPriority())
                .dueDate(req.getDueDate())
                .assignDetails(objectMapper.writeValueAsString(detailsMap))
                .build();

        // 2) Save it FIRST to generate the ID
        // This will be a clean INSERT
        master = masterRepo.save(master);

        // 3) NOW you have the ID. Upload files.
        String prefix = "tasks/masters/%s/".formatted(master.getId());
        List<String> keys = uploadAndCollectKeys(prefix, files);

        // 4) Set the keys on the *managed* entity
        master.setAttachmentKeys(keys);

        // 5) Save AGAIN.
        // Because 'master' is already managed and "dirty",
        // this will correctly trigger an UPDATE.
        master = masterRepo.save(master);

        // 2️⃣ Prepare bulk SQL for all targets
        final int statusOrdinal = (req.getInitialStatus() == null
                ? TaskStatus.PENDING.ordinal()
                : req.getInitialStatus().ordinal());

        if (req.getTargets() == null || req.getTargets().isEmpty()) {
            throw new IllegalArgumentException("Targets list is empty");
        }

        // Build one single INSERT SQL statement
        StringBuilder sql = new StringBuilder("INSERT INTO assigned_task (company_id, login_user_id, assigned_task_master_id, status) VALUES ");
        Map<String, Object> params = new HashMap<>();
        int i = 0;

        // Set masterId and status once, as they are the same for all rows
        params.put("masterId", master.getId());
        params.put("status", statusOrdinal);

        for (AssigneeTarget t : req.getTargets()) {
            // Omit UUID() from the VALUES list
            sql.append("(:company").append(i)
                    .append(", :user").append(i)
                    .append(", :masterId, :status),"); // Use the common :masterId and :status

            params.put("company" + i, t.getCompanyId());
            params.put("user" + i, t.getLoginUserId());
            i++;
        }

        // remove last comma
        sql.setLength(sql.length() - 1);

        var query = em.createNativeQuery(sql.toString())
                .setParameter("masterId", master.getId())
                .setParameter("status", statusOrdinal);

        // Add parameters dynamically
        for (var entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }

        // 3️⃣ Execute one single insert for all users
        query.executeUpdate();

        return masterMapper.toDto(master);
    }


    @Transactional
    public AssignedTaskMasterDto createAndAssignByCompanyFilter(BulkAssignmentByCompanyFilterRequest req,List<MultipartFile> files) throws IOException {
        // 1) Build entity *without* ID or attachments
        LoginUser creator = new LoginUser();
        creator.setLoginUserId(req.getCreatedById());
        Map<String, Object> filteredAssignParams = new HashMap<>();
        filteredAssignParams.put("assignType", "Filtered assign");
        filteredAssignParams.put("stateFilter", Collections.singletonList(req.getStates()));
        filteredAssignParams.put("principalActivityFilter", Collections.singletonList(req.getPrincipalActivity()));
        filteredAssignParams.put("effectiveFrom", req.getEffectiveFrom());
        filteredAssignParams.put("effectiveTo", req.getEffectiveTo());
        ObjectMapper objectMapper = new ObjectMapper();
        AssignedTaskMaster master = AssignedTaskMaster.builder()
                .taskTitle(req.getTaskTitle())
                .description(req.getDescription())
                .createdBy(creator)
                .priority(req.getPriority())
                .dueDate(req.getDueDate())
                .assignDetails(objectMapper.writeValueAsString(filteredAssignParams))
                .build();

        // 2) Save it FIRST to generate the ID
        // This will be a clean INSERT
        master = masterRepo.save(master);

        // 3) NOW you have the ID. Upload files.
        String prefix = "tasks/masters/%s/".formatted(master.getId());
        List<String> keys = uploadAndCollectKeys(prefix, files);

        // 4) Set the keys on the *managed* entity
        master.setAttachmentKeys(keys);
        // Prepare dynamic parameters in a Map


        // 5) Save AGAIN.
        // Because 'master' is already managed and "dirty",
        // this will correctly trigger an UPDATE.
        master = masterRepo.save(master);

        // 2) INSERT … SELECT (derive assignee + company from Company table by filters)
        TaskStatus status = req.getInitialStatus() != null ? req.getInitialStatus() : TaskStatus.PENDING;

        // Build dynamic SQL (native) to avoid passing null IN lists
        // Build dynamic SQL (native) to avoid passing null IN lists
        StringBuilder sql = new StringBuilder("""
        INSERT INTO assigned_task
        (company_id, login_user_id, assigned_task_master_id, status)
        SELECT c.company_id, c.login_user_id, :masterId, :status
        FROM company c
        JOIN login_user u ON u.login_user_id = c.login_user_id
        WHERE 1=1
        AND EXISTS (
           SELECT 1
             FROM formation_status fs
             JOIN formation_status_master fsm
               ON fsm.formation_status_id = fs.status_id
            WHERE fs.company_id = c.company_id
              AND fs.is_active = 1
              AND UPPER(fsm.formation_status_name) = 'SUCCESS'
        )
        """);

        Map<String, Object> params = new HashMap<>();
        params.put("masterId", master.getId());
        params.put("status", status.ordinal());

        if (req.getStates() != null && !req.getStates().isEmpty()) {
            sql.append(" AND c.state IN (:states) ");
            params.put("states", req.getStates());
        }
        if (req.getPrincipalActivity() != null && !req.getPrincipalActivity().isEmpty()) {
            sql.append(" AND c.principal_activity IN (:acts) ");
            params.put("acts", req.getPrincipalActivity());
        }
        if (req.getEffectiveFrom() != null) {
            sql.append(" AND c.company_effective_date >= :effFrom ");
            params.put("effFrom", req.getEffectiveFrom());
        }
        if (req.getEffectiveTo() != null) {
            sql.append(" AND c.company_effective_date <= :effTo ");
            params.put("effTo", req.getEffectiveTo());
        }

        Query q = em.createNativeQuery(sql.toString());

        params.forEach(q::setParameter);
        int inserted = q.executeUpdate();

        // (Optional) You can log or return count via a wrapper; here we just return the master DTO
        return masterMapper.toDto(master);
    }


    public AssignedTaskResponse searchTaskItems(
            Long assigneeId,
            String masterId,
            Long companyId,
            TaskStatus status,
            LocalDate dueDateFrom,
            LocalDate dueDateTo,
            String search,
            Priority priority,
            Boolean completed,
            int page,
            int size,
            List<String> sort
    ) {
        // 1) Map completed → status if status not given
        TaskStatus effectiveStatus = (status != null)
                ? status
                : (completed == null ? null : (completed ? TaskStatus.DONE : TaskStatus.PENDING));

        // 2) Normalize search
        String q = (search == null || search.isBlank()) ? null : search.trim();

        // 3) Allowed sort keys
        final var ALLOWED = java.util.Set.of(
                "id","status","taskTitle","priority","dueDate","createdOn","updatedOn","companyId"
        );

        // 4) Parse sort & map to aliases
        Sort defaultSort = Sort.by(Sort.Order.desc("createdOn"));
        Sort parsed      = SortUtil.buildSort(sort, ALLOWED, defaultSort);

        java.util.Map<String,String> expr = java.util.Map.of(
                "taskTitle", "m.taskTitle",
                "priority",  "m.priority",
                "dueDate",   "m.dueDate",
                "createdOn", "t.createdOn",
                "updatedOn", "t.updatedOn",
                "status",    "t.status",
                "id",        "t.id",
                "companyId", "t.companyId"
        );
        Sort effective = SortUtil.mapToAliases(parsed, expr);

        // 5) Pageable
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                effective
        );

        // 6) Page data
        Page<TaskItemDto> result = taskRepo.searchWithMaster(
                assigneeId, masterId, companyId, effectiveStatus,
                dueDateFrom, dueDateTo, q, priority, pageable
        );

        // 7) Stats (same filters EXCEPT status/completed; use 'today' for overdue)
        AssignedTaskStatsDto stats = taskRepo.statsWithMaster(
                assigneeId, masterId, companyId,
                dueDateFrom, dueDateTo, q, priority,
                LocalDate.now()
        );

        return AssignedTaskResponse.builder()
                .pageData(PagedResponse.from(result))
                .stats(stats)
                .build();
    }


    public PagedResponse<AssignedTaskMasterDto> search(
            AssignedTaskMasterSearchDto filter,
            int page,
            int size,
            List<String> sortParams
    ) {
        final var ALLOWED_SORT = java.util.Set.of(
                "id", "taskTitle", "priority", "dueDate", "createdOn", "updatedOn"
        );
        Sort defaultSort = Sort.by(Sort.Order.desc("createdOn"));
        Sort parsed = SortUtil.buildSort(sortParams, ALLOWED_SORT, defaultSort);
        Pageable pageable = PageRequest.of(page, size, parsed);

        Page<AssignedTaskMasterDto> p =masterMapper.toDtoPage(masterRepo.findAll(
                AssignedTaskMasterSpecs.withFilter(filter),
                pageable
        ));

        return PagedResponse.from(p);
    }



    public void updateTaskStatus(TaskStatusUpdateRequest dto, List<MultipartFile> files) throws IOException {
        String prefix = "tasks/masters/%s/".formatted(dto.getTaskId());
        List<String> keys = uploadAndCollectKeys(prefix, files);
        int n = taskRepo.updateStatusAndNotes(dto.getTaskId(),dto.getStatus(),dto.getNotes(),keys);
        if (n == 0) throw new EntityNotFoundException("Task not found: " + dto.getTaskId());
    }

    @Transactional
    public AssignedTaskMasterDto update(AssignedTaskMasterDto dto,List<MultipartFile> files) throws IOException {
        if (dto == null || dto.getId() == null || dto.getId().isBlank()) {
            throw new IllegalArgumentException("id is required for update");
        }
        String prefix = "tasks/masters/%s/".formatted(dto.getId());
        List<String> keys = uploadAndCollectKeys(prefix, files);

        // Load existing
        AssignedTaskMaster entity = masterRepo.findById(dto.getId())
                .orElseThrow(() -> new IllegalArgumentException("AssignedTaskMaster not found: " + dto.getId()));

        List<String> mergedKeys = Stream.concat(entity.getAttachmentKeys().stream(), keys.stream())
                .toList();
        entity.setAttachmentKeys(mergedKeys);
        // Apply changes (PATCH-style: nulls are ignored per mapper config)
        masterMapper.updateEntityFromDto(dto, entity);

        // Persist
        AssignedTaskMaster saved = masterRepo.save(entity);

        // Return DTO
        return masterMapper.toDto(saved);
    }


    @Transactional
    public void deleteMasterAndChildren(String masterId) {
        var exists = masterRepo.existsById(masterId);
        if (!exists) {
            throw new IllegalArgumentException("AssignedTaskMaster not found: " + masterId);
        }

        // 1) delete children in one shot
        taskRepo.deleteByMasterId(masterId);

        // 2) delete master
        masterRepo.deleteById(masterId);
    }


    // helpers
    private static String sanitizeName(String name) {
        if (name == null) return "file";
        String base = name.replace("\\", "/");
        base = base.substring(base.lastIndexOf('/') + 1);
        return base.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String safeContentType(MultipartFile f) {
        String ct = f.getContentType();
        if (ct == null || ct.isBlank()) {
            // best-effort detection by extension
            String n = f.getOriginalFilename();
            if (n != null) {
                String low = n.toLowerCase();
                if (low.endsWith(".pdf")) return MediaType.APPLICATION_PDF_VALUE;
                if (low.endsWith(".png")) return MediaType.IMAGE_PNG_VALUE;
                if (low.endsWith(".jpg") || low.endsWith(".jpeg")) return MediaType.IMAGE_JPEG_VALUE;
                if (low.endsWith(".webp")) return "image/webp";
            }
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        return ct;
    }

    private List<String> uploadAndCollectKeys(String prefix, List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) return java.util.Collections.emptyList();
        List<String> keys = new java.util.ArrayList<>();
        int idx = 0;
        for (MultipartFile f : files) {
            if (f.isEmpty()) continue;
            String ct = safeContentType(f);           // your validator
            validateContentTypeAndSize(f, ct);        // e.g., allow jpeg/png/pdf up to 10 MB
            String key = prefix + String.format("%02d-%s", idx++, sanitizeName(f.getOriginalFilename()));
            s3.uploadFileWithContentType(key, f.getBytes(), ct);
            keys.add(key);
        }
        return keys;
    }

    /** Validate CT + size + (optional) extension consistency. Throws IllegalArgumentException on failure. */
    public static void validateContentTypeAndSize(MultipartFile file, String contentType) {
        if (file == null) {
            throw new IllegalArgumentException("File is required");
        }
        if (file.isEmpty() || file.getSize() <= 0) {
            throw new IllegalArgumentException("File is empty: " + nullToEmpty(file.getOriginalFilename()));
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    "File too large (max " + (MAX_FILE_SIZE_BYTES / (1024 * 1024)) + " MB): " + nullToEmpty(file.getOriginalFilename())
            );
        }

        String ct = nullToEmpty(contentType).trim().toLowerCase(Locale.ROOT);
        if (ct.isBlank()) {
            ct = safeContentType(file).toLowerCase(Locale.ROOT);
        }

        if (!ALLOWED.contains(ct)) {
            throw new IllegalArgumentException("Unsupported file type: " + ct + " for " + nullToEmpty(file.getOriginalFilename()));
        }

        // Optional: check that extension matches the content-type allow list
        String ext = getExtension(nullToEmpty(file.getOriginalFilename())).toLowerCase(Locale.ROOT);
        Set<String> allowedExts = CT_TO_EXT.get(ct);
        if (allowedExts != null && !allowedExts.isEmpty()) {
            if (ext.isBlank() || !allowedExts.contains(ext)) {
                throw new IllegalArgumentException(
                        "File extension does not match content type " + ct +
                                " (allowed: " + String.join(",", allowedExts) + "): " + nullToEmpty(file.getOriginalFilename())
                );
            }
        }
    }

    @Transactional
    // Update the return type to the new wrapper DTO
    public TaskAnalyticsResponse getFullTaskAnalytics(String masterId,TaskStatus status, Pageable pageable) {

        // 1. Get the paginated list of tasks (as before)
        // 1. Get the paginated list, now passing the status filter
        Page<AssignedTaskAnalyticsDto> taskPage =
                taskRepo.findTaskAnalyticsByMasterId(masterId, status, pageable);

        // 2. Get the status counts
        List<Object[]> countsResult =
                taskRepo.getStatusCountsByMasterId(masterId);

        // 3. Process the counts into the DTO
        long pending = 0L;
        long done = 0L;
        long ignored = 0L;

        for (Object[] row : countsResult) {
            TaskStatus status1 = (TaskStatus) row[0];
            Long count = (Long) row[1];

            switch (status1) {
                case PENDING:
                    pending = count;
                    break;
                case DONE:
                    done = count;
                    break;
                case IGNORED:
                    ignored = count;
                    break;
            }
        }

        TaskStatusCounts statusCounts = new TaskStatusCounts(pending, done, ignored);

        // 4. Combine both results into the final response DTO
        return new TaskAnalyticsResponse(PagedResponse.from(taskPage), statusCounts);
    }

    // --- helpers ---
    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot >= 0 && dot < filename.length() - 1) ? filename.substring(dot + 1) : "";
    }

}

