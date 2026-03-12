package com.trivine.llc.api.controller;


import com.trivine.llc.api.dto.PrincipalActivityDto;
import com.trivine.llc.api.service.PrincipalActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/principal-activities")
@RequiredArgsConstructor
public class PrincipalActivityController {

    private final PrincipalActivityService service;

    @GetMapping
    public ResponseEntity<List<PrincipalActivityDto>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }
    @GetMapping("/values")
    public ResponseEntity<List<String>> getValuesOnly() {
        return ResponseEntity.ok(service.getAllValuesOnly());
    }
    @GetMapping("/sub-category")
    public ResponseEntity<List<String>> getSubCategory(@RequestParam String value) {
        return ResponseEntity.ok(service.getSubsByValue(value));
    }
    @GetMapping("/by-value")
    public ResponseEntity<PrincipalActivityDto> getByValue(@RequestParam String value) {
        return service.getByValue(value)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
