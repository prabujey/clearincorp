package com.trivine.llc.api.controller;


import com.trivine.llc.api.dto.response.InvoiceDto;

import com.trivine.llc.api.service.InvoiceService;
import com.trivine.llc.api.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/invoice")
public class InvoiceController {

private final InvoiceService invoiceService;
private final PaymentService paymentService;

    @PostMapping(value = "/generate", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generateInvoice(@RequestBody InvoiceDto invoice) throws IOException {
        byte[] invoiceData = invoiceService.generateInvoice(invoice);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=invoice.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(invoiceData);
    }


//    @GetMapping("/getAllInvoicebysearchOld")
//    public ResponseEntity<?> getAllInvoicesbysearch(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size,
//            @RequestParam(required = false) String search) {
//        return ResponseEntity.ok(paymentService.getPaginatedInvoicesBySearch(page, size, search));
//    }
    @GetMapping("/getAllInvoicebysearch")
    public ResponseEntity<?> getAllInvoicesbysearchNew(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(paymentService.getPaginatedInvoicesBySearchNew(page, size, search));
    }

//    @GetMapping("/getInvoiceByIdOld")
//    public ResponseEntity<?> getInvoicesById(@RequestParam Long paymentId) {
//        return ResponseEntity.ok(paymentService.getInvoiceById(paymentId));
//    }

    @GetMapping("/getInvoiceById")
    public ResponseEntity<?> getInvoicesByIdNew(@RequestParam Long paymentId) {
        return ResponseEntity.ok(paymentService.getInvoiceByIdNew(paymentId));
    }

//    @GetMapping("/getUserInvoiceOld")
//    public ResponseEntity<?> getUserInvoices(@RequestParam Long loginUserId) {
//        return ResponseEntity.ok(paymentService.getUserInvoice(loginUserId));
//    }
    @GetMapping("/getUserInvoice")
    public ResponseEntity<?> getUserInvoicesNew(@RequestParam Long loginUserId) {
        return ResponseEntity.ok(paymentService.getUserInvoiceOptimized(loginUserId));
    }
}