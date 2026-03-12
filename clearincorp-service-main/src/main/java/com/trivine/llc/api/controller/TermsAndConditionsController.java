package com.trivine.llc.api.controller;

import com.trivine.llc.api.service.TermsAndConditionsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/terms")
public class TermsAndConditionsController {

    @Autowired
    private TermsAndConditionsService service;

    @GetMapping("/getTermsByFormType")
    public ResponseEntity<?> getTermsByFormType(@RequestParam String formType) {
        return ResponseEntity.ok(service.getTermsByFormType(formType));
    }
}

