package com.trivine.llc.api.controller;

import com.trivine.llc.api.crypto.EncryptResult;
import com.trivine.llc.api.crypto.PiiCryptoService;
import com.trivine.llc.api.dto.BusinessDetailsDto;
import com.trivine.llc.api.dto.CompanyLiteDto;
import com.trivine.llc.api.dto.EinDetailsDto;
import com.trivine.llc.api.dto.llc.request.*;
import com.trivine.llc.api.dto.response.*;

import com.trivine.llc.api.repository.projection.CompanySlim;
import com.trivine.llc.api.service.CompanyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/company")
@Validated
public class CompanyController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final CompanyService companyService;
    private final PiiCryptoService piiCryptoService;

    @GetMapping("/states")
    public ResponseEntity<List<String>> getStateKeys() {
        List<String> states = companyService.getStateKeys();
        return ResponseEntity.ok(states);
    }

    @GetMapping("/suffix")
    public ResponseEntity<List<String>> getSuffixes() {
        return ResponseEntity.ok(companyService.getActiveSuffixes());
    }

    @PostMapping("/state")
    public ResponseEntity<CompanyResponseDto> saveCompanyState(@Valid @RequestBody CompanyStateDto companyStateDto) {
        return ResponseEntity.ok(companyService.saveCompanyState(companyStateDto));
    }

    @PostMapping("/name")
    public ResponseEntity<CompanyResponseDto> saveCompanyName(@Valid @RequestBody CompanyRequestDto dto,
                                                               @RequestParam("company_id") Long companyId) {
        return ResponseEntity.ok(companyService.saveCompanyName(dto, companyId));
    }

    @PostMapping("/formationdate")
    public ResponseEntity<CompanyResponseDto> saveFormationDate(
            @RequestParam("formation_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate formationDate,
            @RequestParam("company_id") Long companyId) {
        return ResponseEntity.ok(companyService.saveFormationDate(companyId, formationDate));
    }

    @PostMapping("/description")
    public ResponseEntity<CompanyResponseDto> saveBusinessDescription(@Valid @RequestBody BusinessDetailsDto businessDetailsDto) {
        return ResponseEntity.ok(companyService.saveBusinessDescription(businessDetailsDto));
    }

    @PostMapping("/contactdetails")
    public ResponseEntity<CompanyResponseDto> saveContactDetails(@Valid @RequestBody CompanyPrimaryContactDto dto,
                                                                  @RequestParam("company_id") Long companyId) {
        return ResponseEntity.ok(companyService.saveContactDetails(dto, companyId));
    }

    @PostMapping("/regForm1")
    public ResponseEntity<CompanyResponseDto> updateRegForm1(@RequestParam("regForm1") Boolean regForm1,
                                                              @RequestParam("company_id") Long companyId) {
        return ResponseEntity.ok(companyService.updateRegForm1(companyId, regForm1));
    }

    @PostMapping("/regForm2")
    public ResponseEntity<CompanyResponseDto> updateRegForm2(@RequestParam("regForm2") Boolean regForm2,
                                                              @RequestParam("company_id") Long companyId) {
        return ResponseEntity.ok(companyService.updateRegForm2(companyId, regForm2));
    }

    @PostMapping("/managementStyle")
    public ResponseEntity<CompanyResponseDto> saveManagementStyle(@RequestParam("managementStyle") String managementStyle,
                                                                   @RequestParam("company_id") Long companyId) {
        return ResponseEntity.ok(companyService.saveManagementStyle(managementStyle, companyId));
    }

    @PostMapping("/MailingAttributes")
    public ResponseEntity<CompanyResponseDto> saveMailingAttributes(@Valid @RequestBody CompanyMailingAttributesDto dto,
                                                                     @RequestParam("company_id") Long companyId) {
        return ResponseEntity.ok(companyService.saveMailingAttributes(dto, companyId));
    }

    @PostMapping("/regForm3")
    public ResponseEntity<CompanyResponseDto> updateRegForm3(@RequestParam("regForm3") Boolean regForm3,
                                                              @RequestParam("company_id") Long companyId,
                                                              @RequestParam(required = false) Boolean einChosen) {
        return ResponseEntity.ok(companyService.updateRegForm3(companyId, regForm3, einChosen));
    }

    @PostMapping("/saveEinDetails")
    public ResponseEntity<EinDetailsDto> saveEinDetails(@Valid @RequestBody EinDetailsDto einDetailsDto) throws Exception {
        return ResponseEntity.ok(companyService.saveEin(einDetailsDto));
    }

    @PostMapping("/regForm4")
    public ResponseEntity<CompanyResponseDto> updateRegForm4(@RequestParam("regForm4") Boolean regForm4,
                                                              @RequestParam("company_id") Long companyId) {
        return ResponseEntity.ok(companyService.updateRegForm4(companyId, regForm4));
    }

    @GetMapping("/getDocumentTypeList")
    public ResponseEntity<List<DocumentMasterDto>> getDocumentTypeList() {
        return ResponseEntity.ok(companyService.getDocumentTypeList());
    }

    @GetMapping("/getCompanyStatusPaged")
    public ResponseEntity<Map<String, Object>> getCompanyStatusPaged(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(MAX_PAGE_SIZE) int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) Boolean isExpedited) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        return ResponseEntity.ok(companyService.getCompanyStatusPaged(page, safeSize, status, state, companyName, isExpedited));
    }

    @PutMapping("/update")
    public ResponseEntity<String> updateCompanyDetails(@Valid @RequestBody CompanyDetailsDto companyDetailsDto,
                                                        @RequestParam Long companyId,
                                                        @RequestParam String notes,
                                                        @RequestParam Long loginUserId) {
        return ResponseEntity.ok(companyService.updateCompany(companyId, companyDetailsDto, notes, loginUserId));
    }

    @GetMapping("/getEinStatusPaged")
    public ResponseEntity<Map<String, Object>> getEinStatusPaged(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(MAX_PAGE_SIZE) int size,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String companyName,
            @RequestParam(name = "isExpedited", required = false) Boolean isExpedited) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        return ResponseEntity.ok(companyService.getEinStatusPaged(page, safeSize, state, companyName, isExpedited));
    }

    @PostMapping("/decryptText")
    public ResponseEntity<Map<String, String>> getPlainText(@Valid @RequestBody EncryptResult encryptResult) throws Exception {
        String message = piiCryptoService.decrypt(encryptResult);
        return ResponseEntity.ok(Collections.singletonMap("message", message));
    }

    @GetMapping("/filter")
    public PagedResponse<CompanySlim> searchCompany(
            @RequestParam(required = false) List<String> principalActivity,
            @RequestParam(required = false) List<String> states,
            @RequestParam(required = false) LocalDate effectiveFrom,
            @RequestParam(required = false) LocalDate effectiveTo,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long loginUserId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(MAX_PAGE_SIZE) int size,
            @RequestParam(required = false) String[] sort) {

        int safeSize = Math.min(size, MAX_PAGE_SIZE);

        // Allow only these fields for sorting to keep it fast & safe
        final var ALLOWED = Set.of("createdOn", "companyName");

        // Tolerant parsing (supports "field,dir" or "field"&"dir")
        var orders = new ArrayList<Sort.Order>();
        if (sort != null) {
            for (int i = 0; i < sort.length; i++) {
                String token = sort[i];
                if (token == null || token.isBlank()) continue;

                String field;
                String dirStr = null;
                if (token.contains(",")) {
                    var parts = token.split(",", 2);
                    field = parts[0].trim();
                    dirStr = parts.length > 1 ? parts[1].trim() : null;
                } else {
                    field = token.trim();
                    if (i + 1 < sort.length) {
                        String next = sort[i + 1] == null ? null : sort[i + 1].trim();
                        if ("asc".equalsIgnoreCase(next) || "desc".equalsIgnoreCase(next)) {
                            dirStr = next;
                            i++;
                        }
                    }
                }
                if (!ALLOWED.contains(field)) continue;
                var dir = "desc".equalsIgnoreCase(dirStr) ? Sort.Direction.DESC : Sort.Direction.ASC;
                orders.add(new Sort.Order(dir, field));
            }
        }
        if (orders.isEmpty()) {
            orders.add(new Sort.Order(Sort.Direction.ASC, "companyId"));
        }

        Pageable pageable = PageRequest.of(page, safeSize, Sort.by(orders));
        Page<CompanySlim> result = companyService.search(
                principalActivity, states, effectiveFrom, effectiveTo, search, loginUserId, pageable
        );
        return PagedResponse.from(result);
    }

    @GetMapping("/CompanyById")
    public ResponseEntity<CompanyLiteDto> getById(@RequestParam("companyId") Long companyId) {
        return ResponseEntity.ok(companyService.getLiteById(companyId));
    }
}
