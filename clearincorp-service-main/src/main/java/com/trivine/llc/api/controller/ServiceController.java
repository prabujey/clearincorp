package com.trivine.llc.api.controller;

import com.trivine.llc.api.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/service")
public class ServiceController {

    @Autowired
    private final CompanyService companyService;

    @PostMapping("/ein")
    public ResponseEntity<?> addService1(@RequestParam("einRequired") Boolean einRequired,
                                         @RequestParam("company_id") Long companyId) {
        return ResponseEntity.ok(companyService.addEinService(companyId, einRequired));
    }

    @PostMapping("/expedict")
    public ResponseEntity<?> addService3(@RequestParam("expedictRequired") Boolean required,
                                         @RequestParam("company_id") Long companyId) {
        return ResponseEntity.ok(companyService.addExpeditService(companyId, required));
    }

    @PostMapping("/operatingAggrement")
    public ResponseEntity<?> addService2(@RequestParam("operatingaggrementRequired") Boolean required,
                                         @RequestParam("company_id") Long companyId) {
        return ResponseEntity.ok(companyService.addOperatingAggrementService(companyId, required));
    }
}
