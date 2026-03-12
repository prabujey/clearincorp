package com.trivine.llc.api.controller;

import com.trivine.llc.api.dto.*;
import com.trivine.llc.api.dto.request.AuditClickRequestDto;
import com.trivine.llc.api.dto.response.AuditClickResponseDto;
import com.trivine.llc.api.dto.response.BusinessRegistrationDto;
import com.trivine.llc.api.dto.response.PagedResponse;
import com.trivine.llc.api.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@RestController
@RequestMapping("/business")
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessDirectoryService service;
    private final BusinessWeightedSearchService weightedSearch;
    private final BusinessImageService businessImageService;
    private final BusinessRegistrationService businessRegistrationService;
    private final AuditClickService auditClickService;



    @PostMapping("/create")
    public ResponseEntity<BusinessDto> create(@RequestBody BusinessDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @GetMapping("/id")
    public ResponseEntity<BusinessRegistrationDto> get(@RequestParam Long id) {
        return ResponseEntity.ok(service.get(id));
    }


    @GetMapping("/admin")
    public ResponseEntity<BusinessAdminPageDto> list(
            @RequestParam(required = false) Long serviceId,
            @RequestParam(required = false) String zipCode,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        int pageIndex = Math.max(page, 0);
        int pageSize  = Math.min(Math.max(size, 1), 100);

        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by("createdOn").descending());

        return ResponseEntity.ok(
                businessRegistrationService.getAdminBusinessesWithCounts(serviceId, zipCode, pageable)
        );
    }


    @PutMapping("/{id}")
    public ResponseEntity<BusinessDto> update(@RequestParam Long id, @RequestBody BusinessDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestParam Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("audit-clicks")
    public ResponseEntity<AuditClickResponseDto> track(@RequestBody AuditClickRequestDto in) {
        AuditClickResponseDto out = auditClickService.track(in);
        return ResponseEntity.status(out.isDeduped() ? HttpStatus.OK : HttpStatus.CREATED).body(out);
    }

    @GetMapping("/status")
    public ResponseEntity<AuditClickStatsDto> stats(@RequestParam Long businessId) {
        return ResponseEntity.ok(auditClickService.status(businessId));
    }

    @PatchMapping("/verify")
    public ResponseEntity<Void> toggleVerify(@RequestParam Long businessId,
                                             @RequestParam Long loginUserId) {
        businessRegistrationService.adminToggleVerify(businessId, loginUserId);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/by-admin")
    public ResponseEntity<PagedResponse<BusinessRegistrationDto>> getByAdmin(
            @RequestParam(required = false) Long serviceId, @RequestParam(required = false) String zipCode,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        int pageSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdOn").descending());
        return ResponseEntity.ok(PagedResponse.from(businessRegistrationService.getAdminBusinesses(serviceId, zipCode, pageable)));
    }

    @GetMapping("/nlp")
    public ResponseEntity<Page<WeightedBusinessHitDto>> nlpWeighted(
            @RequestParam("q") String q, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        int pageIndex = Math.max(0, page);
        int pageSize  = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(pageIndex, pageSize);
        return ResponseEntity.ok(weightedSearch.searchWeighted(q, pageable));
    }



    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<String>> uploadImages(
            @RequestParam Long businessId,
            @RequestPart("images") MultipartFile[] images
    ) throws IOException {
        if (images == null || images.length == 0) {
            return ResponseEntity.badRequest().build();
        }
        List<MultipartFile> files = Arrays.stream(images).filter(Objects::nonNull).filter(f -> !f.isEmpty())
                .toList();
        if (files.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<String> keys = businessImageService.uploadManyAtomic(businessId, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(keys);
    }

    @GetMapping("/images")
    public ResponseEntity<List<BusinessImageDto>> listImages(@RequestParam Long businessId) {
        return ResponseEntity.ok(businessImageService.listImages(businessId));
    }

    @GetMapping("/image")
    public ResponseEntity<byte[]> downloadImage(@RequestParam Long businessId,
                                                @RequestParam int imageId) {
        return businessImageService.downloadBusinessImage(businessId, imageId);
    }

    @DeleteMapping("/image")
    public ResponseEntity<Void> deleteImage(@RequestParam Long businessId,
                                            @RequestParam int imageId
    ) {
        businessImageService.deleteBusinessImage(businessId, imageId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/images/all")
    public ResponseEntity<Map<String, Integer>> deleteAllImages(@RequestParam Long businessId) {
        int count = businessImageService.deleteAllBusinessImages(businessId);
        return ResponseEntity.ok(Map.of("deletedCount", count));
    }

}