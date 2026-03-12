package com.trivine.llc.api.controller;


import com.itextpdf.kernel.exceptions.PdfException;
import com.trivine.llc.api.service.PdfService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;


@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("pdf")
public class    PdfController {

    private final PdfService pdfService;

//    @DeleteMapping("/delete")
//    public ResponseEntity<?> deleteRecordAndFile(@RequestParam Long companyId, @RequestParam String documentType) {
//        return pdfService.deleteRecordAndFile(companyId, documentType);
//    }

    @GetMapping("/generate")
    public ResponseEntity<?> generatePdf(@RequestParam Long companyId,
                                         @RequestParam String state,
                                         @RequestParam(required = false) Long filerLoginUserId) throws Exception {
        return ResponseEntity.ok(Map.of("message",pdfService.generateAndUploadPdf(companyId, state, filerLoginUserId)));
    }

//    @GetMapping("/Op-AgreementDoc")
//    public ResponseEntity<?> generateOperatingAgreement(@RequestParam Long companyId)
//            throws IOException, PdfException {
//        String message = pdfService.generateAndUploadOperatingAgreement(companyId);
//        return ResponseEntity.ok(Map.of("message", message));
//    }


    @GetMapping("/getAllFileDetails")
    public ResponseEntity<?> getAllFileDetails() {
        return pdfService.getAllFileDetails();
    }

    @GetMapping("/getAllFileDetailsBySearch")
    public ResponseEntity<?> getAllFileDetails(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String typeName) {
        return ResponseEntity.ok(pdfService.getPaginatedDocumentDetails(page, size, companyName, state, typeName));
    }

    @GetMapping("/getFile")
    public ResponseEntity<?> getFile(@RequestParam Long companyId,
                                     @RequestParam String documentType,
                                     @RequestParam(defaultValue = "view") String action,
                                     @RequestParam(defaultValue = "view") String purpose,
                                     @RequestParam(required = false) Long filerLoginUserId) throws IOException {
        return pdfService.getFile(companyId, documentType, action, purpose, filerLoginUserId);
    }

//    @GetMapping("/getUserFileDetailsOld")
//    public ResponseEntity<?> getUserFileDetails(@RequestParam Long loginUserId,
//                                                @RequestParam(required = false) Long companyId) { // <--- ADDED OPTIONAL PARAMETER
//        return ResponseEntity.ok(pdfService.getUserFileDetails(loginUserId, companyId)); // <--- UPDATED
//    }
    @GetMapping("/getUserFileDetails")
    public ResponseEntity<?> getUserFileDetailsNew(@RequestParam Long loginUserId,
                                                @RequestParam(required = false) Long companyId) { // <--- ADDED OPTIONAL PARAMETER
        return ResponseEntity.ok(pdfService.getUserFileDetailsNew(loginUserId, companyId)); // <--- UPDATED
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
                                        @RequestParam("companyId") Long companyId,
                                        @RequestParam("documentType") String documentType,
                                        @RequestParam(defaultValue = "view") String purpose,
                                        @RequestParam(required = false) Long filerLoginUserId) throws IOException {
        return pdfService.uploadDocument(file, companyId, documentType, purpose, filerLoginUserId);
    }

    @GetMapping("/getEinFile")
    public ResponseEntity<byte[]> downloadEinPdf(@RequestParam Long companyId)
            throws Exception {

        byte[] fileContent = pdfService.getOrGenerateEinPdf(companyId);
        String fileName = "EIN_Application.pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(fileContent);
    }



}
