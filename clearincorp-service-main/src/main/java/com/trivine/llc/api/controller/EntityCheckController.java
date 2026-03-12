package com.trivine.llc.api.controller;

import com.trivine.llc.api.dto.response.ErrorResponseDto;
import com.trivine.llc.api.dto.response.CompanyResponseDto;
import com.trivine.llc.api.service.WebScrapingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/entity")
public class EntityCheckController {

    private final WebScrapingService webScrapingService;

//    @GetMapping("/check")
//    public ResponseEntity<?> checkCompany(
//            @RequestParam("state") String state,
//            @RequestParam("companyName") String companyName) {
//
//        log.info("Checking company availability: Name='{}', State='{}'", companyName, state);
//
//        if (companyName == null || companyName.trim().isEmpty()) {
//            log.warn("Missing required parameter: companyName");
//            return ResponseEntity.badRequest()
//                    .body(new ErrorResponseDto("Missing required parameter", "'companyName' must not be empty."));
//        }
//
//        try {
//
//            String result = webScrapingService.checkCompanyOnline(companyName, state);
//            if ("Not Found".equalsIgnoreCase(result)) {
//                log.info("Company not found online, checking offline...");
//                result = webScrapingService.checkCompanyOffline(companyName, state);
//            }
//
//            return ResponseEntity.ok(new CompanyResponseDto(result, null));
//
    ////              return ResponseEntity.ok(new CompanyResponseDto("Not Found",null));
//
//        } catch (Exception e) {
//            log.error("Error while checking company: {}", e.getMessage(), e);
//            return ResponseEntity.internalServerError()
//                    .body(new ErrorResponseDto("Failed to check company", e.getMessage()));
//        }
//    }

    @GetMapping("/check")
    public ResponseEntity<?> checkCompany(@RequestParam String state,
                                          @RequestParam String companyName) {

        if (companyName == null || companyName.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponseDto("Missing required parameter", "'companyName' must not be empty."));
        }

        long t0 = System.nanoTime();
        String result = webScrapingService.checkCompany(companyName, state); // <-- use capped+cached+fallback
        long ms = (System.nanoTime() - t0) / 1_000_000;
        log.info("entity/check state={} name='{}' result={} latencyMs={}", state, companyName, result, ms);

        return ResponseEntity.ok(new CompanyResponseDto(result, null));
    }



}
