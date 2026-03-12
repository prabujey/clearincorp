package com.trivine.llc.api.controller;

import com.trivine.llc.api.dto.FilingFailureDTO;
import com.trivine.llc.api.service.FilingFailureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/filing-failures")
public class FilingFailureController {

    private final FilingFailureService filingFailureService;

    @PostMapping("/createFiling") // Used
    public ResponseEntity<?> createFilingFailure(@RequestBody FilingFailureDTO filingFailureDTO,
                                                 @RequestParam Long companyId,
                                                 @RequestParam Long LoginId) {
        return ResponseEntity.status(201).body(filingFailureService.createFilingFailure(filingFailureDTO, companyId, LoginId));
    }

    @DeleteMapping("/deleteFiling/{id}")
    public ResponseEntity<?> deleteFilingFailure(@PathVariable long id) {
        filingFailureService.deleteFilingFailure(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/getAllFiling")
    public ResponseEntity<?> getAllFilingFailures() {
        return ResponseEntity.ok(filingFailureService.getAllFilingFailures());
    }

    @GetMapping("/getFiling/{id}")
    public ResponseEntity<?> getFilingFailureById(@PathVariable long id) {
        return ResponseEntity.ok(filingFailureService.getFilingFailureById(id));
    }

    @PutMapping("/updateFiling/{id}")
    public ResponseEntity<?> updateFilingFailure(@PathVariable long id, @RequestBody FilingFailureDTO filingFailureDTO) {
        return ResponseEntity.ok(filingFailureService.updateFilingFailure(id, filingFailureDTO));
    }

}


