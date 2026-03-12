package com.trivine.llc.api.controller;

import com.trivine.llc.api.dto.VendorPageDto;

import com.trivine.llc.api.service.VendorPageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/vendor")
@Slf4j
@RequiredArgsConstructor
public class VendorPageController {

    private final VendorPageService vendorPageService;

    @DeleteMapping("/deleteVendor")
    public ResponseEntity<?> deleteVendor(@RequestParam Long loginuserId) {
        vendorPageService.deleteById(loginuserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/findAllVendor")
    public ResponseEntity<?> findAllVendor() {
        return ResponseEntity.ok(vendorPageService.findByLoginUser());
    }

    @PostMapping("/saveVendor")
    public ResponseEntity<?> saveVendor(@Valid @RequestBody VendorPageDto vendorPageDto) {
        return ResponseEntity.status(201).body(vendorPageService.save(vendorPageDto));
    }
    @PutMapping("/updateVendor")
    public ResponseEntity<?> updateVendor(@Valid @RequestBody VendorPageDto vendorPageDto) {
        return ResponseEntity.ok(vendorPageService.update(vendorPageDto));
    }
}
