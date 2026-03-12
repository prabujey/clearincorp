package com.trivine.llc.api.controller;

import com.trivine.llc.api.dto.LoginUserDto;
import com.trivine.llc.api.service.LoginUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/superfiler")
@Slf4j
@RequiredArgsConstructor
public class SuperFilerPageController {

    private final  LoginUserService loginUserService;

    @DeleteMapping("/deleteSuperFiler")
    public ResponseEntity<Void> deleteSuperFiler(@RequestParam Long id) {
        loginUserService.deleteSuperFiler(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/getAllSuperFiler")
    public ResponseEntity<?> getAllSuperFiler() {
        return ResponseEntity.ok(loginUserService.findAllSuperFiler());
    }

    @PostMapping("/saveSuperFiler")
    public ResponseEntity<LoginUserDto> saveSuperFiler(@RequestBody LoginUserDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loginUserService.saveSuperFiler(dto));
    }

    @PutMapping("/updateSuperFiler")
    public ResponseEntity<LoginUserDto> updateSuperFiler(@RequestBody LoginUserDto dto) {
        return ResponseEntity.ok(loginUserService.updateSuperFiler(dto));
    }





}
