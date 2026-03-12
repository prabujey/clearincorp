package com.trivine.llc.api.controller;


import com.trivine.llc.api.entity.ReasonForApplying;
import com.trivine.llc.api.service.ReasonForApplyingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/reasonForApplying")
public class ReasonForApplyingController {

    private final ReasonForApplyingService service;

    @GetMapping("/getAll")
    public ResponseEntity<List<ReasonForApplying>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }
}
