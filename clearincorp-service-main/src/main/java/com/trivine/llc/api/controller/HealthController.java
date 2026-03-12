package com.trivine.llc.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.TimeZone;

@RestController
@RequestMapping("/health")
public class HealthController {

        @GetMapping
        public ResponseEntity<String> healthCheck() {
            System.out.println(TimeZone.getDefault().getID());
            return ResponseEntity.ok("OK");//test
        }
}
