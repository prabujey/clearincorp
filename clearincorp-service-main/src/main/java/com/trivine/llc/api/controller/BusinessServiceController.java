package com.trivine.llc.api.controller;

import com.trivine.llc.api.dto.BusinessServiceDto;
import com.trivine.llc.api.repository.projection.ServiceIdName;
import com.trivine.llc.api.service.BusinessCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/business-services")
@RequiredArgsConstructor
public class BusinessServiceController {

    private final BusinessCatalogService service;

    @PostMapping("/create")
    public ResponseEntity<BusinessServiceDto> create(@RequestBody BusinessServiceDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<BusinessServiceDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @GetMapping("/list")
    public ResponseEntity<List<ServiceIdName>> list() {
        return ResponseEntity.ok(service.listIdName());
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<BusinessServiceDto> update(@PathVariable Long id, @RequestBody BusinessServiceDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
