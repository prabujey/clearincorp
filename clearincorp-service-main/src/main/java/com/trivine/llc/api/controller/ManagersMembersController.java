package com.trivine.llc.api.controller;

import com.trivine.llc.api.dto.llc.request.ManagerMemberDto;
import com.trivine.llc.api.dto.llc.request.MemberMemberDto;


import com.trivine.llc.api.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/manager-member") //consumer
@RequiredArgsConstructor
public class ManagersMembersController {

    @Autowired
    private final ManagerMemberService managerMemberService;
    private final MemberService memberService;

    @DeleteMapping("/deleteManagers")
    public ResponseEntity<?> deleteManagers(@RequestParam("company_id") Long companyId) {
        return ResponseEntity.ok(managerMemberService.deleteManagers(companyId));
    }

    @PostMapping("/saveManagers")
    public ResponseEntity<?> saveAllManagers(
            @Valid @RequestBody List<ManagerMemberDto> managerMemberDtos,
            @RequestParam("company_id") Long companyId) {
        return ResponseEntity.ok(managerMemberService.saveAllManagers(managerMemberDtos, companyId));
    }

    @PostMapping("/saveMembers")
    public ResponseEntity<?> saveAllMembers(
            @Valid @RequestBody List<MemberMemberDto> memberMemberDtos,
            @RequestParam("company_id") Long companyId) {
        return ResponseEntity.ok(memberService.saveAllMembers(memberMemberDtos, companyId));
    }
}
