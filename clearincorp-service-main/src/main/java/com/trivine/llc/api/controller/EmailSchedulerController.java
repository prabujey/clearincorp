package com.trivine.llc.api.controller;

import com.trivine.llc.api.service.EmailSchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/email-scheduler")
@RequiredArgsConstructor
public class EmailSchedulerController {

    private final EmailSchedulerService emailSchedulerService;

    // Endpoint to trigger the daily email
    @PostMapping("/send-daily-email")
    public ResponseEntity<Map<String, Object>> sendDailyEmail() {
        emailSchedulerService.sendDailyEmail();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Daily reminder emails have been sent successfully.");
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }
    // Endpoint to trigger the 3-day email
    @PostMapping("/send-3days-email")
    public ResponseEntity<Map<String, Object>> sendEvery3DaysEmail() {
        emailSchedulerService.sendEvery3DaysEmail();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "3-day reminder emails have been sent successfully.");
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    // Endpoint to trigger the weekly email
    @PostMapping("/send-weekly-email")
    public ResponseEntity<Map<String, Object>> sendWeeklyEmail() {
        emailSchedulerService.sendWeeklyEmail();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Weekly reminder emails have been sent successfully.");
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    // Endpoint to trigger the bi-weekly email
    @PostMapping("/send-biweekly-email")
    public ResponseEntity<Map<String, Object>> sendBiWeeklyEmail() {
        emailSchedulerService.sendBiWeeklyEmail();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Bi-weekly reminder emails have been sent successfully.");
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }
}
