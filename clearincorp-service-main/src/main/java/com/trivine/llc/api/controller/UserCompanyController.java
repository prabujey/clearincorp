package com.trivine.llc.api.controller;

import com.trivine.llc.api.dto.UserCompanyDto;
import com.trivine.llc.api.service.UserCompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@Validated
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/usercompany")
public class UserCompanyController {

    private final UserCompanyService userCompanyService;

    @DeleteMapping("/deleteById/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteById(@PathVariable Long id) {
        userCompanyService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/findAll")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> findAll() {
        return ResponseEntity.ok(userCompanyService.findAll());
    }

    @GetMapping("/findById/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        return ResponseEntity.ok(userCompanyService.findById(id));
    }

    @PostMapping("/save")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> save(@Valid @RequestBody UserCompanyDto userCompanyDto) {
        UserCompanyDto saved = userCompanyService.save(userCompanyDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/update")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@Valid @RequestBody UserCompanyDto userCompanyDto) {
        UserCompanyDto updated = userCompanyService.save(userCompanyDto);
        return ResponseEntity.ok(updated);
    }
}
