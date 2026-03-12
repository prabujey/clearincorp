package com.trivine.llc.api.controller;

import com.trivine.llc.api.dto.BusinessDto;
import com.trivine.llc.api.dto.BusinessOwnerDto;
import com.trivine.llc.api.dto.response.BusinessOwnerResponseDto;
import com.trivine.llc.api.dto.response.BusinessRegistrationDto;
import com.trivine.llc.api.service.BusinessOwnerService;
import com.trivine.llc.api.service.BusinessRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/business-owners")
@RequiredArgsConstructor
public class BusinessOwnerController {

    private final BusinessOwnerService service;
    private final BusinessRegistrationService registrationService;


    @PatchMapping("/reject")
    public ResponseEntity<?> rejectBusiness(@RequestParam Long businessId) {
        service.rejectBusiness(businessId);
        return ResponseEntity.ok(Map.of(
                "businessId", businessId,
                "message", "Reject processed (reject=1, updated_by cleared only if it was admin)"
        ));
    }

    @GetMapping("/businesses/admin-created-lists")
    public ResponseEntity<List<BusinessRegistrationDto>> listAdminCreatedBusinesses() {
        return ResponseEntity.ok(service.listAdminCreatedBusinesses());
    }

    @GetMapping("/businesses/in-progress/{loginUserId}")
    public ResponseEntity<List<BusinessDto>> inProgress(@PathVariable Long loginUserId) {
        return ResponseEntity.ok(service.getInProgressBusinessesByLoginUserId(loginUserId));
    }

    @GetMapping("/businesses/registered/{loginUserId}")
    public ResponseEntity<List<BusinessDto>> registered(@PathVariable Long loginUserId) {
        return ResponseEntity.ok(service.getRegisteredBusinessesByLoginUserId(loginUserId));
    }


    @PostMapping("/create")
    public ResponseEntity<BusinessOwnerDto> create(@RequestBody BusinessOwnerDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<BusinessOwnerDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @GetMapping("/list")
    public ResponseEntity<List<BusinessOwnerDto>> list() {
        return ResponseEntity.ok(service.list());
    }

    @GetMapping("/email")
    public ResponseEntity<BusinessOwnerResponseDto> getOwnerByEmail(@RequestParam String email) {
        return service.getOwnerByPersonalEmailDto(email)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }


    @PutMapping("/update/{id}")
    public ResponseEntity<BusinessOwnerDto> update(@PathVariable Long id, @RequestBody BusinessOwnerDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    public ResponseEntity<BusinessRegistrationDto> register(
            @RequestBody BusinessRegistrationDto body,
            @RequestParam(value = "loginUserId") Long loginUserId) {

        BusinessRegistrationDto out = registrationService.register(body, loginUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(out);
    }
    @GetMapping("/listBusinesses/{ownerId}")
    public ResponseEntity<List<BusinessRegistrationDto>> listBusinesses(@PathVariable Long ownerId) {
        return ResponseEntity.ok(registrationService.listBusinessesByOwnerId(ownerId));
    }


}
