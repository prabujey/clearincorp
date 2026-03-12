package com.trivine.llc.api.controller;

import com.trivine.llc.api.dto.LoginUserDto;
import com.trivine.llc.api.service.LoginUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/consumer")
@Slf4j
@RequiredArgsConstructor
public class ConsumerPageController {

    private final  LoginUserService loginUserService;

    @DeleteMapping("/deleteConsumer")
    public ResponseEntity<?> deleteConsumer(@RequestParam Long id) {
        loginUserService.deleteConsumer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/getAllConsumers")
    public ResponseEntity<?> getAllConsumersByVendor(@RequestParam Long loginUserId) {
        return ResponseEntity.ok(loginUserService.findAllConsumers(loginUserId));
    }

    @PostMapping("/saveConsumer")
    public ResponseEntity<?> saveConsumer(@RequestBody LoginUserDto dto,
                                          @RequestParam Long loginUserId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loginUserService.saveConsumer(dto, loginUserId));
    }

    @PutMapping("/updateConsumer")
    public ResponseEntity<?> updateConsumer(@RequestBody LoginUserDto dto) {
        loginUserService.updateConsumer(dto);
        return ResponseEntity.ok(Map.of("message", "Consumer updated successfully"));
    }



}




