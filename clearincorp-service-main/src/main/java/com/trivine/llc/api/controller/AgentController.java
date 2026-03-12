package com.trivine.llc.api.controller;

import com.trivine.llc.api.dto.llc.request.RegisteredAgentDto;
import com.trivine.llc.api.service.RegisteredAgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/agent") // Used
public class AgentController {


    private final RegisteredAgentService registeredAgentService;

    @GetMapping("/getAgents/{state}")
    public ResponseEntity<?> getOneActiveAgentByState(@PathVariable String state) {
        return ResponseEntity.ok(registeredAgentService.getOneActiveAgentByState(state));
    }

    @PostMapping("/addNewAgent")
    public ResponseEntity<?> save(@Valid @RequestBody RegisteredAgentDto dto,
                                  @RequestParam("company_id") Long companyId,
                                  @RequestParam("isOurs") boolean isOurs) {
        return ResponseEntity.status(201).body(registeredAgentService.saveOrUpdate(dto, companyId, isOurs));
    }
}
