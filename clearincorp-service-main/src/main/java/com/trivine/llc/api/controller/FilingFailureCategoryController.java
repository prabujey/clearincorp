package com.trivine.llc.api.controller;


import com.trivine.llc.api.dto.FilingFailureCategoryDTO;
import com.trivine.llc.api.service.FilingFailureCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/filing-failure-category")
public class FilingFailureCategoryController {

    private final FilingFailureCategoryService service;

    @PostMapping("/createFiling")
    public ResponseEntity<?> createFilingFailureCategory(@RequestBody FilingFailureCategoryDTO dto) {
        return ResponseEntity.status(201).body(service.saveFilingFailureCategory(dto));
    }

    @DeleteMapping("/deleteFiling/{id}")
    public ResponseEntity<?> deleteFilingFailureCategory(@PathVariable int id) {
        service.deleteFilingFailureCategory(id);
        return ResponseEntity.noContent().build();
    }

//    @GetMapping("/getAllFiling") // Used
//    public ResponseEntity<?> getAllFilingFailureCategories() {
//        return ResponseEntity.ok(service.getAllFilingFailureCategories());
//    }
@GetMapping("/getAllFiling")
public ResponseEntity<List<FilingFailureCategoryDTO>> getAllFilingFailureCategories() {
    return ResponseEntity.ok(service.getAllFilingFailureCategories());
}


    @GetMapping("/getFiling/{id}")
    public ResponseEntity<?> getFilingFailureCategoryById(@PathVariable int id) {
        return ResponseEntity.ok(service.getFilingFailureCategoryById(id));
    }
}



