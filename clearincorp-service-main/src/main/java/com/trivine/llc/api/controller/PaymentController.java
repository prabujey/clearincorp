package com.trivine.llc.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.stripe.exception.StripeException;
import com.trivine.llc.api.dto.llc.request.PaymentRequestDto;

import com.trivine.llc.api.service.PaymentService;
import com.trivine.llc.api.service.ProcessingChargesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/payment")
public class PaymentController {

    private final ProcessingChargesService processingChargesService;
    private final PaymentService paymentService;

    @GetMapping("/charges")
    public ResponseEntity<?> getProcessingCharges(@RequestParam("company_id") Long companyId) {
        return ResponseEntity.ok(processingChargesService.getProcessingCharges(companyId));
    }

    @PostMapping("/create-payment-intent")
    public ResponseEntity<?> createPaymentIntent(@RequestBody PaymentRequestDto paymentRequestDto) throws StripeException {
        return ResponseEntity.ok(paymentService.createPaymentIntent(paymentRequestDto));
    }

    @PostMapping("/stripe")
    public ResponseEntity<?> saveStripePayment(@RequestBody JsonNode stripeJson,
                                               @RequestParam Long companyId) throws JsonProcessingException {
        paymentService.saveStripePayment(stripeJson, companyId);
        return ResponseEntity.ok(Collections.singletonMap("message", "Payment saved successfully"));
    }

}
