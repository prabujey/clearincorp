package com.trivine.llc.api.controller;


import com.trivine.llc.api.dto.CompanyFilingDto;
import com.trivine.llc.api.service.CompanyFilingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/company-filings")
public class CompanyFilingController {

    private final CompanyFilingService service;

    @PostMapping("/create") // Used
    public ResponseEntity<?> create(@RequestBody CompanyFilingDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/getOne/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/listAll")
    public ResponseEntity<?> listAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody CompanyFilingDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

}
