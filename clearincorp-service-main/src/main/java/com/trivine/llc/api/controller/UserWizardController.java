package com.trivine.llc.api.controller;


import com.trivine.llc.api.dto.ProfileUpdateDto;

import com.trivine.llc.api.dto.response.UserProgressDto;
import com.trivine.llc.api.service.*;
import com.trivine.llc.api.service.utility.RequestContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;


@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/user-wizard")
public class UserWizardController {
    private final RequestContext requestContext;

    private final  LoginUserService loginUserService;
    private final UserWizardService userWizardService;


//    @GetMapping("/getUserProgressOld")
//    public ResponseEntity<?> getUserProgressNew(@RequestParam Long login_user_id) {
////        UserAccessUtil.assertSameUserOrAdmin(login_user_id,requestContext.getLoginUserId(), requestContext.getRole());
//        return ResponseEntity.ok(userWizardService.getUserProgress(login_user_id));
//    }
    @GetMapping("/getUserProgress")
    public ResponseEntity<?> getUserProgressNew1(@RequestParam Long login_user_id) {
        UserAccessUtil.assertSameUserOrAdmin(login_user_id,requestContext.getLoginUserId(), requestContext.getRole());
        return ResponseEntity.ok(userWizardService.getUserProgressNew(login_user_id));
    }

//    @GetMapping("/getUserProgressByCompanyIdOld")
//    public ResponseEntity<?> getUserProgressByCompanyId(@RequestParam Long companyId) throws ExecutionException, InterruptedException, TimeoutException {
//        return ResponseEntity.ok(userWizardService.getUserProgressDto(companyId,false));
//    }

    @PutMapping(value = "/updateProfile")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody ProfileUpdateDto dto) throws IOException {
        return ResponseEntity.ok(loginUserService.updateProfile(dto));
    }
    @GetMapping("/getUserProgressByCompanyId")
    public ResponseEntity<UserProgressDto> getProgress(
            @RequestParam Long companyId,
            @RequestParam(defaultValue = "false") Boolean isHelper
    ) {
        return ResponseEntity.ok(userWizardService.getUserProgressDtoOptimized(companyId, isHelper));
    }

}