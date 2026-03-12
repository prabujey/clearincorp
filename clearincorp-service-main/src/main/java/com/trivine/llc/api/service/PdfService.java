package com.trivine.llc.api.service;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.exceptions.PdfException;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;
import com.trivine.llc.api.constants.ServiceConstants;
import com.trivine.llc.api.dto.llc.request.MemberMemberDto;
import com.trivine.llc.api.dto.response.DocumentResponseDto;
import com.trivine.llc.api.dto.response.PagedResponse;
import com.trivine.llc.api.dto.response.UserProgressDto;
import com.trivine.llc.api.entity.*;
import com.trivine.llc.api.exception.ResourceNotFoundException;
import com.trivine.llc.api.repository.*;
import com.trivine.llc.api.service.utility.SendEmailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfService {

    private final S3Service s3Service;
    private final CompanyRepository companyRepository;
    private final UserWizardService userWizardService;
    private final CompanyServiceRepository companyServiceRepository;
    private  PdfFont bodyFont;
    private  PdfFont boldFont;
    private final MemberRepository memberRepository;
    private final CompanyDocumentRepository companyDocumentRepository;
    private final DocumentMasterRepository documentMasterRepository;
    private final FormationStatusRepository formationStatusRepository;
    private final FormationStatusMasterRepository formationStatusMasterRepository;
    private final SendEmailService sendEmailService;
    private final Ss4PdfService ss4PdfService;
    private final AsyncCompanyService asyncCompanyService;


    private static final DateTimeFormatter OUT = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    @Transactional
    public String generateAndUploadPdf(Long companyId, String state, Long filerLoginUserId) throws Exception {
        log.info("Generating article for companyId: {}, state: {}", companyId, state);


        CompletableFuture<Optional<Company>> companyFuture = asyncCompanyService.getCompany(companyId);
        CompletableFuture<Optional<CompanyPrincipal>> principalFuture = asyncCompanyService.getPrincipal(companyId);
        CompletableFuture<Optional<List<ManagerMember>>> managerFuture = asyncCompanyService.getManagerMembers(companyId);
        CompletableFuture<Optional<List<Member>>> memberFuture = asyncCompanyService.getMembers(companyId);
        CompletableFuture<Optional<RegisteredAgent>> agentFuture = asyncCompanyService.getAgentByCompanyId(companyId);
        // 2. Generate article PDF and upload to S3

        CompletableFuture.allOf(companyFuture,agentFuture, principalFuture, managerFuture, memberFuture)
                .get(15, TimeUnit.SECONDS);

        // Mandatory
        Company company = companyFuture.join()
                .orElseThrow(() -> new RuntimeException("Company not found"));
        CompanyPrincipal organizer = principalFuture.join()
                .orElseThrow(() -> new RuntimeException("Organizer not found"));
        RegisteredAgent agent = agentFuture.join()
                .orElseThrow(() -> new RuntimeException("Registered Agent not found"));

        // Management logic
        List<ManagerMember> managers = managerFuture.join().orElse(List.of());
        List<Member> members = memberFuture.join()
                .orElseThrow(() -> new RuntimeException("At least one Member is required"));

        // Validate management style
        if ("Member-Managed".equalsIgnoreCase(company.getManagementStyle()) && members.isEmpty()) {
            throw new RuntimeException("Member-Managed company must have at least one Member");
        }
        if ("Manager-Managed".equalsIgnoreCase(company.getManagementStyle()) && managers.isEmpty()) {
            throw new RuntimeException("Manager-Managed company must have at least one Manager");
        }

        // Now proceed
        ByteArrayOutputStream pdfFile = generatePdf(company, organizer, agent, managers, members);
        String s3Key = "Company_Document/" + company.getCompanyId() + "/Articles of Organization.pdf";
        s3Service.uploadFile(s3Key, pdfFile);

        // 4. Get Document Type
        DocumentMaster documentMaster = documentMasterRepository.findByTypeName("Articles of Organization")
                .orElseThrow(() -> new ResourceNotFoundException("Document type not found"));

        // 5. Upsert Company Document
        CompanyDocuments document = companyDocumentRepository
                .findByCompanyIdAndDocumentTypeId(companyId, documentMaster.getDocumentTypeId())
                .orElse(new CompanyDocuments());

        document.setCompany(company);
        document.setDocumentMaster(documentMaster);
        document.setViewed(false);
        document.setDownloaded(false);
        document.setUploadedAt(LocalDateTime.now());
        companyDocumentRepository.save(document);

        // 6. Update formation status
        updateFormationStatus(filerLoginUserId, company, "reviewed");

        return "Article of organization generated successfully";
    }

    @Transactional
    public ResponseEntity<?> getFile(Long companyId, String documentType, String action,
                                               String purpose, Long filerLoginUserId) throws IOException {
        log.info("Fetching file for companyId: {}, documentType: {}, action: {}", companyId, documentType, action);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        Long documentTypeId = documentMasterRepository.findByTypeName(documentType)
                .map(DocumentMaster::getDocumentTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Document type not found"));

        CompanyDocuments document = companyDocumentRepository
                .findByCompanyIdAndDocumentTypeId(companyId, documentTypeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document not found for companyId: " + companyId + " and documentType: " + documentType));

        // Action: download
        if ("download".equalsIgnoreCase(action)) {
            if ("Articles of Organization".equals(documentType) && "filling".equalsIgnoreCase(purpose)) {
                updateFormationStatus(filerLoginUserId, company, "ready to file");
            }
            if(filerLoginUserId ==null) {
                document.setDownloaded(true);
                companyDocumentRepository.save(document);
            }
            return ResponseEntity.ok(Map.of("message", "File downloaded successfully"));
        }

        if(filerLoginUserId==null) {
            document.setViewed(true);
            companyDocumentRepository.save(document);
        }

        String fileKey = String.format("Company_Document/%d/%s.pdf", companyId, documentType);
        byte[] fileContent = s3Service.downloadFile(fileKey).asByteArray();

        String disposition = action.equalsIgnoreCase("download") ? "attachment" : "inline";
        String fileName = documentType + companyId + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=" + fileName)
                .contentType(MediaType.APPLICATION_PDF)
                .body(fileContent);
    }

    @Transactional
    public ResponseEntity<?> uploadDocument(MultipartFile file,
                                            Long companyId,
                                            String documentType,
                                            String purpose,
                                            Long filerLoginUserId) throws IOException {
        log.info("Uploading file for companyId: {}, documentType: {}", companyId, documentType);

        DocumentMaster documentMaster = documentMasterRepository.findByTypeName(documentType)
                .orElseThrow(() -> new ResourceNotFoundException("Document type not found"));

        CompanyDocuments companyDocument = companyDocumentRepository
                .findByCompanyIdAndDocumentTypeId(companyId, documentMaster.getDocumentTypeId())
                .orElse(new CompanyDocuments());

        Company company = companyRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found for id: " + companyId));

        // Upload to S3
        String fileKey = "Company_Document/" + companyId + "/" + documentType + ".pdf";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(file.getBytes());
        s3Service.uploadFile(fileKey, baos);

        // Save document metadata
        companyDocument.setCompany(company);
        companyDocument.setDocumentMaster(documentMaster);
        companyDocument.setViewed(false);
        companyDocument.setDownloaded(false);
        companyDocument.setUploadedAt(LocalDateTime.now());
        companyDocumentRepository.save(companyDocument);

        // Send email if specific doc + purpose
        if ("Formation Document".equals(documentType) && "notify_success".equals(purpose)) {
            FormationStatus formationStatus = updateFormationStatus(
                    filerLoginUserId,
                    company,
                    "success"
            );
            // Check if the new status is SUCCESS by comparing the name in master table
            if ("success".equalsIgnoreCase(formationStatus.getStatus().getFormationStatusName())
                    && formationStatus.getIsActive()) {

                LocalDateTime dateTime = formationStatus.getCreatedOn();
                String formattedDate = dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a"));

                String userName = Optional.ofNullable(company.getLoginUser())
                        .map(LoginUser::getFirstName)
                        .orElse("User");

                assert company.getLoginUser() != null;
                boolean emailSent = sendEmailService.sendCompanyFormationCompletedEmail(
                        ServiceConstants.FROM_EMAIL,
                        company.getLoginUser().getEmail(),
                        String.format(ServiceConstants.FILED_SUCCESS_SUBJECT,company.getCompanyName()),
                        userName,
                        company.getCompanyName(),
                        company.getState(),
                        formattedDate
                );

                if (!emailSent) {
                    log.error("Failed to send email notification for company formation completion, companyId={}", company.getCompanyId());
                }
            } else {
                log.info("Skipping email: formation status = {}", formationStatus.getStatus().getFormationStatusName());
            }
        }
        if ("Ein Document".equals(documentType)){
            CompanyServices companyServices = companyServiceRepository.findUncompletedPaidEINRegistrationByCompanyId(companyId);
            if (companyServices != null) {
                companyServices.setServiceCompletionDate(LocalDateTime.now());
                companyServiceRepository.save(companyServices);
            }
        }

        return ResponseEntity.ok(Map.of("message", "File uploaded successfully"));
    }

        public ResponseEntity<?> getAllFileDetails() {
        log.info("Fetching all document details");

        List<CompanyDocuments> companyDocumentsList = companyDocumentRepository.findAllWithCompanyAndDocumentMaster();

        List<DocumentResponseDto> responseList = companyDocumentsList.stream().map(doc -> {
            DocumentResponseDto dto = new DocumentResponseDto();
            dto.setType(doc.getDocumentMaster().getTypeName());
            dto.setCompanyName(doc.getCompany().getCompanyName() + " " + doc.getCompany().getSuffixMaster().getSuffix());
            dto.setUploadedOn(doc.getUploadedAt());
            dto.setJurisdiction(doc.getCompany().getState());
            dto.setCompanyId(doc.getCompany().getCompanyId());
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }

    public List<DocumentResponseDto> getUserFileDetails(Long loginUserId, Long companyId) {
        log.info("Fetching user document details for loginUserId: {}, optional companyId: {}", loginUserId, companyId);

        // Step 1: Fetch relevant companies for the user
        List<Company> companies = companyRepository.findByRegForm3TrueAndLoginUser_LoginUserId(loginUserId);

        // Filter companies if companyId is provided
        if (companyId != null) {
            // Keep only the company that matches the provided companyId
            companies = companies.stream()
                    .filter(c -> c.getCompanyId().equals(companyId))
                    .collect(Collectors.toList());
        }

        // If no companies remain after filtering (e.g., provided companyId doesn't belong to the user)
        if (companies.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> userCompanyIds = companies.stream()
                .map(Company::getCompanyId)
                .collect(Collectors.toSet());

        // Step 2: Fetch all documents and filter by the company IDs (now potentially one ID)
        List<DocumentResponseDto> result;
        result = companyDocumentRepository.findAllWithCompanyAndDocumentMaster().stream()
                .filter(doc -> userCompanyIds.contains(doc.getCompany().getCompanyId()))
                .map(doc -> {
                    DocumentResponseDto dto = new DocumentResponseDto();
                    dto.setType(doc.getDocumentMaster().getTypeName());
                    // Handle potential null SuffixMaster before concatenating
                    String suffix = Optional.ofNullable(doc.getCompany().getSuffixMaster())
                            .map(SuffixMaster::getSuffix)
                            .orElse("");

                    dto.setCompanyName(doc.getCompany().getCompanyName() + (suffix.isEmpty() ? "" : " " + suffix));
                    dto.setUploadedOn(doc.getUploadedAt());
                    dto.setJurisdiction(doc.getCompany().getState());
                    dto.setCompanyId(doc.getCompany().getCompanyId());
                    return dto;
                }).collect(Collectors.toList());

        return result;
    }

    public List<DocumentResponseDto> getUserFileDetailsNew(Long loginUserId, Long companyId) {
        return companyDocumentRepository.findUserFileDetailsOptimized(loginUserId, companyId);
    }

    @Transactional
    public ResponseEntity<?> deleteRecordAndFile(Long companyId, String documentType) {
        log.info("Deleting file for companyId: {}, documentType: {}", companyId, documentType);

        Long documentTypeId = documentMasterRepository.findByTypeName(documentType)
                .map(DocumentMaster::getDocumentTypeId)
                .orElseThrow(() -> new RuntimeException("Document type not found"));

        companyDocumentRepository.deleteByCompanyIdAndDocumentTypeId(companyId, documentTypeId);

        String fileKey = "Company_Document/" + companyId + "/" + documentType + ".pdf";
        s3Service.deleteFile(fileKey);

        return ResponseEntity.ok(Map.of("message", "Record and file deleted successfully"));
    }

    public PagedResponse<DocumentResponseDto> getPaginatedDocumentDetails(int page, int size,
                                                                          String companyName,
                                                                          String state,
                                                                          String typeName) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("uploadedAt").descending());

        Page<CompanyDocuments> documentPage = companyDocumentRepository.searchWithJoins(companyName, state, typeName, pageable);

        List<DocumentResponseDto> responseList = documentPage.getContent().stream().map(doc -> {
            DocumentResponseDto dto = new DocumentResponseDto();
            dto.setType(doc.getDocumentMaster().getTypeName());
            dto.setCompanyName(doc.getCompany().getCompanyName() + " " + doc.getCompany().getSuffixMaster().getSuffix());
            dto.setUploadedOn(doc.getUploadedAt());
            dto.setJurisdiction(doc.getCompany().getState());
            dto.setCompanyId(doc.getCompany().getCompanyId());
            return dto;
        }).toList();

        return new PagedResponse<>(
                responseList,
                documentPage.getNumber(),
                documentPage.getSize(),
                documentPage.getTotalElements(),
                documentPage.getTotalPages(),
                documentPage.isLast()
        );
    }

    // ---- GLOBAL FONT SIZES ----
    private static final float FS_HEADING        = 24f; // Top main heading
    private static final float FS_ARTICLE_TITLE  = 14f; // "Article I", "Article II", etc.
    private static final float FS_BODY           = 11f; // Paragraph text, tables, labels, etc.


//    public ByteArrayOutputStream generatePdf(
//            Company company,
//            CompanyPrincipal organizer,
//            RegisteredAgent agent,
//            List<ManagerMember> managers,
//            List<Member> members
//    ) throws Exception {
//
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        PdfWriter writer = new PdfWriter(baos);
//        PdfDocument pdf = new PdfDocument(writer);
//        Document document = new Document(pdf, PageSize.A4, false);
//
//        // Fonts
//        boldFont = PdfFontFactory.createFont(StandardFonts.TIMES_BOLD);
//        bodyFont = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
//
//        // 1-inch margins
//        document.setMargins(72, 72, 72, 72);
//
//        // ---- Title (24) ----
//        document.add(heading("Articles of Organization"));
//
//        // --- Article I (14 + 11)
//        addArticle(document,
//                "Article I: Name of the Limited Liability Company",
//                bodyPara()
//                        .add(new Text("The name of the Limited Liability Company is: ").setFont(bodyFont).setFontSize(FS_BODY))
//                        .add(new Text(company.getCompanyName() + " " +
//                                (company.getSuffixMaster() != null ? company.getSuffixMaster().getSuffix() : ""))
//                                .setFont(boldFont).setFontSize(FS_BODY))
//        );
//
//        // --- Article II
//        addArticle(document,
//                "Article II: Purpose of the Limited Liability Company",
//                bodyPara()
//                        .add("The purpose for which this Limited Liability Company is formed is to engage in any lawful act or activity for which a limited liability company may be organized under the laws of the State of ")
//                        .add(new Text(nullSafe(company.getState())).setFont(boldFont).setFontSize(FS_BODY))
//                        .add(".")
//        );
//
//        // --- Article III
//        addArticle(document,
//                "Article III: Principal Place of Business",
//                bodyPara()
//                        .add(new Text("Address: ").setFont(boldFont).setFontSize(FS_BODY))
//                        .add(new Text(formatAddress(
//                                company.getStreetAddress1(), company.getStreetAddress2(),
//                                company.getCity(), company.getState(),
//                                company.getZipCode(), company.getCountry()
//                        )).setFont(bodyFont).setFontSize(FS_BODY))
//        );
//
//        // --- Article IV
//        addArticle(document,
//                "Article IV: Registered Agent and Office",
//                bodyParaWithMargin()
//                        .add(new Text("Name: ").setFont(boldFont).setFontSize(FS_BODY))
//                        .add(new Text(nullSafe(agent.getFirstName()) + " " + nullSafe(agent.getLastName()))
//                                .setFont(bodyFont).setFontSize(FS_BODY))
//                        .add("\n")
//                        .add(new Text("Address: ").setFont(boldFont).setFontSize(FS_BODY))
//                        .add(new Text(formatAddress(
//                                agent.getStreetAddress1(), agent.getStreetAddress2(),
//                                agent.getCity(), agent.getState(),
//                                agent.getZipCode(), agent.getCountry()
//                        )).setFont(bodyFont).setFontSize(FS_BODY))
//        );
//
//        // --- Article V
//        document.add(articleTitle("Article V: Management Structure"));
//        document.add(bodyPara().add("The Limited Liability Company will be managed by: " + nullSafe(company.getManagementStyle())));
//
//        if (managers != null && !managers.isEmpty()) {
//            document.add(bodyBold("Managers (If applicable):"));
//            Table managerTable = buildTable();
//            for (ManagerMember mm : managers) {
//                managerTable.addCell(new Paragraph(nullSafe(mm.getFirstName()) + " " + nullSafe(mm.getLastName()))
//                        .setFont(bodyFont).setFontSize(FS_BODY).setPadding(5));
//                managerTable.addCell(new Paragraph(formatAddress(
//                        mm.getStreetAddress1(), mm.getStreetAddress2(),
//                        mm.getCity(), mm.getState(), mm.getZipCode(), mm.getCountry()
//                )).setFont(bodyFont).setFontSize(FS_BODY).setPadding(5));
//            }
//            document.add(managerTable);
//        }
//
//        if (members != null && !members.isEmpty()) {
//            document.add(bodyBold("Members:"));
//            Table memberTable = buildTable();
//            for (Member m : members) {
//                memberTable.addCell(new Paragraph(nullSafe(m.getFirstName()) + " " + nullSafe(m.getLastName()))
//                        .setFont(bodyFont).setFontSize(FS_BODY).setPadding(5));
//                memberTable.addCell(new Paragraph(formatAddress(
//                        m.getStreetAddress1(), m.getStreetAddress2(),
//                        m.getCity(), m.getState(), m.getZipCode(), m.getCountry()
//                )).setFont(bodyFont).setFontSize(FS_BODY).setPadding(5));
//            }
//            document.add(memberTable);
//        }
//        addSeparator(document);
//
//        // --- Article VI
//        addArticle(document,
//                "Article VI: Duration of the Limited Liability Company",
//                bodyPara().add("The duration of the Limited Liability Company shall be: Perpetual")
//        );
//
//        // --- Article VII
//        addArticle(document,
//                "Article VII: Organizer",
//                bodyParaWithMargin()
//                        .add(new Text("Name: ").setFont(boldFont).setFontSize(FS_BODY))
//                        .add(new Text(nullSafe(organizer.getFirstName()) + " " + nullSafe(organizer.getLastName()))
//                                .setFont(bodyFont).setFontSize(FS_BODY))
//                        .add("\n")
//                        .add(new Text("Address: ").setFont(boldFont).setFontSize(FS_BODY))
//                        .add(new Text(nullSafe(organizer.getEmail()))
//                                .setFont(bodyFont).setFontSize(FS_BODY))
//        );
//
//        // --- Signature block
//        document.add(articleTitle("Signature of Organizer").setMarginTop(20));
//        document.add(bodyPara().add(
//                "I, the undersigned, being the organizer of this Limited Liability Company, do hereby certify that the information contained in these Articles of Organization is true and correct to the best of my knowledge and belief."
//        ));
//        document.add(bodyPara()
//                .add(new Text("Signature: ").setFont(boldFont).setFontSize(FS_BODY))
//                .add(new Text(nullSafe(organizer.getFirstName()) + " " + nullSafe(organizer.getLastName()))
//                        .setFont(bodyFont).setFontSize(FS_BODY))
//        );
//        document.add(bodyPara()
//                .add(new Text("Date: ").setFont(boldFont).setFontSize(FS_BODY))
//                .add(new Text(LocalDate.now().toString()).setFont(bodyFont).setFontSize(FS_BODY))
//        );
//
//        document.close();
//        return baos;
//    }

    // ---- Helpers (centralized styles) ----

    private Paragraph heading(String text) {
        return new Paragraph(text)
                .setFont(boldFont)
                .setFontSize(FS_HEADING)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
    }

    private Paragraph articleTitle(String text) {
        return new Paragraph(text)
                .setFont(boldFont)
                .setFontSize(FS_ARTICLE_TITLE)
                .setMultipliedLeading(1.5f)
                .setTextAlignment(TextAlignment.LEFT)
                .setMarginBottom(5);
    }

    private Paragraph bodyPara() {
        return new Paragraph()
                .setFont(bodyFont)
                .setFontSize(FS_BODY)
                .setMultipliedLeading(1.5f)
                .setTextAlignment(TextAlignment.LEFT)
                .setFirstLineIndent(20f)
                .setMarginBottom(5);
    }
    private Paragraph bodyParaWithMargin() {
        return new Paragraph()
                .setFont(bodyFont)
                .setFontSize(FS_BODY)
                .setMultipliedLeading(1.5f)
                .setTextAlignment(TextAlignment.LEFT)
                .setMarginLeft(20f)
                .setMarginBottom(5);
    }

    private Paragraph bodyBold(String text) {
        return new Paragraph(text)
                .setFont(boldFont)
                .setFontSize(FS_BODY)
                .setMultipliedLeading(1.5f)
                .setTextAlignment(TextAlignment.LEFT)
                .setMarginBottom(5);
    }

    private void addArticle(Document document, String title, Paragraph body) {
        document.add(articleTitle(title));
        // Ensure body uses body font size
        body.setFont(bodyFont).setFontSize(FS_BODY);
        document.add(body);
        addSeparator(document);
    }

    private void addSeparator(Document document) {
        document.add(new LineSeparator(new SolidLine())
                .setMarginTop(5)
                .setMarginBottom(15));
    }

    private Table buildTable() {
        Table table = new Table(UnitValue.createPercentArray(new float[]{3, 7}))
                .useAllAvailableWidth()
                .setMarginBottom(10);
        table.addHeaderCell(new Cell().add(new Paragraph("Name").setFont(boldFont).setFontSize(FS_BODY)).setPadding(5));
        table.addHeaderCell(new Cell().add(new Paragraph("Address").setFont(boldFont).setFontSize(FS_BODY)).setPadding(5));
        return table;
    }

    private String formatAddress(String addr1, String addr2, String city, String state, String zip, String country) {
        StringBuilder sb = new StringBuilder();
        if (addr1 != null && !addr1.isBlank()) sb.append(addr1).append(' ');
        if (addr2 != null && !addr2.isBlank()) sb.append(addr2).append(' ');
        if (city != null && !city.isBlank())   sb.append(city).append(", ");
        if (state != null && !state.isBlank()) sb.append(state).append(' ');
        if (zip != null && !zip.isBlank())     sb.append(zip).append(", ");
        if (country != null && !country.isBlank()) sb.append(country);
        return sb.toString().trim();
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }



    // ==== Small helpers to keep heading layout identical to yours ====
    private Paragraph articleHeading(String articleLabel, String subheading) {
        return new Paragraph()
                .add(new Text(articleLabel + "\n").setBold().setFontSize(14))
                .add(new Text(subheading + "\n").setBold().setFontSize(14))
                .setTextAlignment(TextAlignment.CENTER)
                .setFixedLeading(30)
                .setMarginLeft(10)
                .setKeepWithNext(true);
    }

    private String roman(int n) {
        String[] r = {"","I","II","III","IV","V","VI","VII","VIII","IX","X","XI","XII","XIII","XIV","XV","XVI","XVII","XVIII","XIX","XX"};
        return (n >= 0 && n < r.length) ? r[n] : String.valueOf(n);
    }

    private Table formatManagersTable(List<ManagerMember> managers) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{35f, 65f}))
                .setWidth(UnitValue.createPercentValue(80f))              // 80% page width
                .setHorizontalAlignment(HorizontalAlignment.CENTER)       // center the whole table block
                .setBorder(new SolidBorder(1f))                           // outer border of table
                .setMarginTop(8f)
                .setMarginBottom(8f);

        // Header
        t.addHeaderCell(headerCell("Name"));
        t.addHeaderCell(headerCell("Address"));

        for (ManagerMember m : managers) {
            String name = (nvl(m.getFirstName()) + " " + nvl(m.getLastName())).trim();
            String streetLine   = joinSpace(nvl(m.getStreetAddress1()), z(m.getStreetAddress2()));
            String cityStateZip = joinCityStateZip(nvl(m.getCity()), nvl(m.getState()), nvl(m.getZipCode()));
            String address      = joinComma(streetLine, cityStateZip);

            t.addCell(bodyCell(name));
            t.addCell(bodyCell(address));
        }
        return t;
    }

    private Table formatMembersTable(List<Member> members) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{35f, 65f}))
                .setWidth(UnitValue.createPercentValue(80f))              // 80% page width
                .setHorizontalAlignment(HorizontalAlignment.CENTER)       // center the whole table block
                .setBorder(new SolidBorder(1f))                           // outer border of table
                .setMarginTop(8f)
                .setMarginBottom(8f);

        // Header
        t.addHeaderCell(headerCell("Name"));
        t.addHeaderCell(headerCell("Address"));

        for (Member m : members) {
            String name = (nvl(m.getFirstName()) + " " + nvl(m.getLastName())).trim();
            String streetLine   = joinSpace(nvl(m.getStreetAddress1()), z(m.getStreetAddress2()));
            String cityStateZip = joinCityStateZip(nvl(m.getCity()), nvl(m.getState()), nvl(m.getZipCode()));
            String address      = joinComma(streetLine, cityStateZip);

            t.addCell(bodyCell(name));
            t.addCell(bodyCell(address));
        }
        return t;
    }

    private Cell headerCell(String text) {
        Paragraph p = new Paragraph(text)
                .setBold()
                .setFontSize(12f)
                .setFixedLeading(14f)
                .setTextAlignment(TextAlignment.CENTER);

        return new Cell()
                .add(p)
                .setBorder(new SolidBorder(0.5f))  // grid lines
                .setPadding(4f);
    }

    private Cell bodyCell(String text) {
        Paragraph p = new Paragraph(blankToDash(text))
                .setFontSize(12f)
                .setFixedLeading(14f)
                .setTextAlignment(TextAlignment.CENTER);

        return new Cell()
                .add(p)
                .setBorder(new SolidBorder(0.5f))  // grid lines
                .setPadding(4f);
    }



    public String getS3Uri(String bucketname, String s3Key) {
        return "s3://" + bucketname + "/" + s3Key;
    }

    private FormationStatus updateFormationStatus(Long filerLoginUserId, Company company, String name) {
        FormationStatus currentStatus = formationStatusRepository.findFirstByCompanyIdAndIsActiveTrue(company.getCompanyId())
                .orElseThrow(() -> new RuntimeException("No active status found for the company"));
        currentStatus.setIsActive(false);
        formationStatusRepository.save(currentStatus);

        FormationStatusMaster statusMaster = formationStatusMasterRepository.findByFormationStatusName(name)
                .orElseThrow(() -> new RuntimeException("Status not found in FormationStatusMaster"));

        FormationStatus newStatus = new FormationStatus();
        newStatus.setCompany(company);
        LoginUser loginUser = new LoginUser();
        loginUser.setLoginUserId(filerLoginUserId);
        newStatus.setLoginUser(loginUser);
        newStatus.setStatus(statusMaster);
        newStatus.setStatusDate(LocalDate.now());
        newStatus.setCreatedOn(LocalDateTime.now());
        newStatus.setIsActive(true);
        return formationStatusRepository.save(newStatus);
    }

    @Transactional
    public byte[] getOrGenerateEinPdf(Long companyId)
            throws Exception {

        String s3Key = "Company_Document/" + companyId + "/EIN_Application.pdf";
        CompanyServices companyServices =
                companyServiceRepository.findUncompletedPaidEINRegistrationByCompanyId(companyId);
        if (companyServices != null) {
            companyServices.setServiceCreatedDate(LocalDateTime.now());
            companyServiceRepository.save(companyServices);
        }

        if (s3Service.doesObjectExist(s3Key)) {
            return s3Service.downloadFile(s3Key).asByteArray();
        }

        // Build from the official SS-4 template
        UserProgressDto progress = userWizardService.getUserProgressDto(companyId,true);
        ByteArrayOutputStream pdf = ss4PdfService.fillAndFlatten(progress);

        s3Service.uploadFile(s3Key, pdf);
        return pdf.toByteArray();
    }

    public ByteArrayOutputStream build(UserProgressDto progress) {
        int membersCount = progress.getEinDetailsDto().getNumberOfMembers();
        if (membersCount == 0) membersCount = 1;

        String entityType = membersCount == 1 ? "LLC (Disregarded Entity)" : "LLC (Partnership)";
        String titleOfPerson = membersCount == 1 ? "Owner" : "Managing Member";

        MemberMemberDto resp = progress.getStep13b() != null
                ? progress.getStep13b().stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsEinResponsibleParty()))
                .findFirst().orElse(null)
                : null;

        String respFirst = resp != null ? resp.getFirstName()
                : (progress.getStep5() != null ? progress.getStep5().getFirstName() : "");
        String respLast = resp != null ? resp.getLastName()
                : (progress.getStep5() != null ? progress.getStep5().getLastName() : "");
        String respPhone = resp != null ? resp.getPhoneNumber()
                : (progress.getStep5() != null ? progress.getStep5().getPhoneNumber() : "");
        String ssn = blankToDash(progress.getEinDetailsDto().getSsnId());

        String legalName = joinSpace(
                progress.getStep2() != null ? nvl(progress.getStep2().getCompanyName()) : "",
                progress.getStep2() != null ? nvl(progress.getStep2().getLlcSuffix()) : "LLC"
        );
        String tradeName = progress.getStep4() != null ? nvl(progress.getStep4().getTradeName()) : "—";
        String principal = progress.getStep4() != null ? nvl(progress.getStep4().getPrincipalActivity()) : "—";

        LocalDate started = progress.getStep3() != null ? progress.getStep3() : LocalDate.now();

        // Mailing address: step14 preferred, else step6
        String mailStreet = null, mailStreet2 = null, mailCity = null, mailState = null, mailZip = null;
        if (progress.getStep14() != null) {
            mailStreet  = nvl(progress.getStep14().getStreetAddress1());
            mailStreet2 = z(progress.getStep14().getStreetAddress2());
            mailCity    = nvl(progress.getStep14().getCity());
            mailState   = nvl(progress.getStep1());
            mailZip     = nvl(progress.getStep14().getZipCode());
        } else if (progress.getStep6() != null) {
            mailStreet  = nvl(progress.getStep6().getStreetAddress1());
            mailStreet2 = z(progress.getStep6().getStreetAddress2());
            mailCity    = nvl(progress.getStep6().getCity());
            mailState   = nvl(progress.getStep6().getState());
            mailZip     = nvl(progress.getStep6().getZipCode());
        }

        boolean sameAddress = eq(mailStreet, mailStreet2) && eq(mailCity, mailState) && eq(mailZip, "");

        // --- PDF ---
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.A4);
        doc.setMargins(36, 36, 36, 36);

        doc.add(new Paragraph("EIN Application Form")
                .setBold().setFontSize(16).setTextAlignment(TextAlignment.CENTER).setMarginBottom(35));

        Table table = new Table(UnitValue.createPercentArray(new float[]{55, 45})).useAllAvailableWidth();
        table.setBorder(Border.NO_BORDER);

        int n = 1;
        addRow(table, n++, "Legal name of entity (or individual):", legalName);
        addRow(table, n++, "Trade name of business:", blankToDash(tradeName));
        addRow(table, n++, "Mailing address:", joinComma(mailStreet, mailStreet2));
        addRow(table, n++, "City, state, and ZIP code:", joinCityStateZip(mailCity, mailState, mailZip));

        addRow(table, n++, "Street address (if different):", sameAddress ? "—" : joinComma(mailStreet, mailStreet2));
        addRow(table, n++, "City, state, and ZIP code:", sameAddress ? "—" : joinCityStateZip(mailCity, mailState, mailZip));

        addRow(table, n++, "County and state:", "USA , " + nvl(progress.getStep1()));
        addRow(table, n++, "Name of responsible party:", joinSpace(respFirst, respLast));
        addRow(table, n++, "SSN ID:", ssn.isEmpty() ? "Not Present" : maskSsn(ssn));
        addRow(table, n++, "Is this application for a limited liability company (LLC)?", "Yes");
        addRow(table, n++, "If 8a is “Yes,” enter number of LLC members:", String.valueOf(membersCount));
        addRow(table, n++, "Type of entity:", entityType);
        addRow(table, n++, "Reason for applying:", "Started a new business");

        addRow(table, n++, "Date business started:", OUT.format(started));
        addRow(table, n++, "Closing month of accounting year:", "December");
        addRow(table, n++, "Highest number of employees expected in 12 months:", "1");
        addRow(table, n++, "Principal activity:", blankToDash(principal));
        addRow(table, n++, "First date wages or annuities were paid:", "—");
        addRow(table, n++, "Principal line of merchandise/products/services:", blankToDash(principal));
        addRow(table, n++, "Has the applicant ever applied for EIN before?", "No");
        addRow(table, n++, "Name and title of person signing the form:", blankToDash(joinSpace(respFirst, respLast) + ", " + titleOfPerson));
        addRow(table, n++, "Applicant’s daytime phone number:", blankToDash(formatPhone(respPhone)));

        // Additional information
        addRow(table, n++, "LLC Name:", nvl(progress.getEinDetailsDto().getLlcName()));
        addRow(table, n++, "Number of Members:", String.valueOf(progress.getEinDetailsDto().getNumberOfMembers()));
        addRow(table, n++, "Business Street Address:", nvl(progress.getEinDetailsDto().getBusinessStreetAddress()));
        addRow(table, n++, "Business City:", nvl(progress.getEinDetailsDto().getBusinessCity()));
        addRow(table, n++, "Business State:", nvl(progress.getEinDetailsDto().getBusinessState()));
        addRow(table, n++, "Business Zip Code:", nvl(progress.getEinDetailsDto().getBusinessZipCode()));

        addRow(table, n++, "Mailing Street Address:", nvl(progress.getEinDetailsDto().getMailingStreetAddress()));
        addRow(table, n++, "Mailing City:", nvl(progress.getEinDetailsDto().getMailingCity()));
        addRow(table, n++, "Mailing State:", nvl(progress.getEinDetailsDto().getMailingState()));
        addRow(table, n++, "Mailing Zip Code:", nvl(progress.getEinDetailsDto().getMailingZipCode()));
        addRow(table, n++, "Email:", nvl(progress.getEinDetailsDto().getEmail()));
        addRow(table, n++, "Phone Number:", formatPhone(nvl(progress.getEinDetailsDto().getPhoneNumber())));

        // Additional business rules
        addRow(table, n++, "Closing Month:", nvl(progress.getEinDetailsDto().getClosingMonth()));
        addRow(table, n++, "Principal Activity:", nvl(progress.getEinDetailsDto().getPrincipalActivity()));
        addRow(table, n++, "Principal Sub Activity:", nvl(progress.getEinDetailsDto().getPrincipalSubActivity()));
        addRow(table, n++, "Formation Date:", OUT.format(progress.getEinDetailsDto().getFormationDate()));
        addRow(table, n++, "Use Physical Address for Mailing:", progress.getEinDetailsDto().getUsePhysicalAddressForMailing() ? "Yes" : "No");
        addRow(table, n++, "Husband/Wife Members:", progress.getEinDetailsDto().getHusbandWifeMembers() ? "Yes" : "No");
        addRow(table, n++, "Sells Alcohol/Tobacco/Firearms:", progress.getEinDetailsDto().getSellsAlcoholTobaccoFirearms() ? "Yes" : "No");
        addRow(table, n++, "File Annual Payroll Taxes:", progress.getEinDetailsDto().getFileAnnualPayrollTaxes() ? "Yes" : "No");
        addRow(table, n++, "Involves Gambling:", progress.getEinDetailsDto().getInvolvesGambling() ? "Yes" : "No");
        addRow(table, n++, "Owns Heavy Vehicle:", progress.getEinDetailsDto().getOwnsHeavyVehicle() ? "Yes" : "No");
        addRow(table, n++, "Pays Federal Excise Taxes:", progress.getEinDetailsDto().getPaysFederalExciseTaxes() ? "Yes" : "No");
        addRow(table, n++, "Hire Employee in 12 Months:", progress.getEinDetailsDto().getHireEmployeeIn12Months() ? "Yes" : "No");

        // Employee Counts
        addRow(table, n++, "Household Employees:", String.valueOf(progress.getEinDetailsDto().getHouseholdEmployees()));
        addRow(table, n++, "Agricultural Employees:", String.valueOf(progress.getEinDetailsDto().getAgriculturalEmployees()));
        addRow(table, n++, "Other Employees:", String.valueOf(progress.getEinDetailsDto().getOtherEmployees()));

        // LLC Type and Federal Tax ID
        addRow(table, n++, "LLC Type:", nvl(progress.getEinDetailsDto().getLlcType()));
        addRow(table, n++, "Previous Federal Tax ID:", nvl(progress.getEinDetailsDto().getPreviousFederalTaxId()));

        doc.add(table);
        doc.close();
        return out;
    }

    private static int safeSize(List<?> l) { return l == null ? 0 : l.size(); }
    private static String nvl(String s) { return s == null ? "" : s.trim(); }
    private static String z(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }
    private static boolean eq(String a, String b) { return nvl(a).equals(nvl(b)); }
    private static String blankToDash(String s) { return (s == null || s.isBlank()) ? "—" : s; }
    private static String joinSpace(String... s) { return Arrays.stream(s).filter(x -> x != null && !x.isBlank()).collect(Collectors.joining(" ")); }
    private static String joinComma(String... s) { return Arrays.stream(s).filter(x -> x != null && !x.isBlank()).collect(Collectors.joining(", ")); }
    private static String joinCityStateZip(String city, String st, String zip) {
        String left = joinComma(city, st);
        return joinComma(left, zip);
    }
    private static void addRow(Table t, int num, String label, String value) {
        Cell l = new Cell().add(new Paragraph(num + ". " + label).setBold())
                .setBorder(Border.NO_BORDER).setPaddingBottom(6);
        Cell r = new Cell().add(new Paragraph(blankToDash(value)))
                .setBorder(Border.NO_BORDER).setPaddingBottom(6);
        t.addCell(l);
        t.addCell(r);
    }
    private static String maskSsn(String ssn) {
        if (ssn == null || ssn.isBlank()) return "—";
        String d = ssn.replaceAll("[^0-9]", "");
        if (d.length() == 9) return d.substring(0, 3) + "-" + d.substring(3, 5) + "-" + d.substring(5);
        return ssn;
    }
    private static String formatPhone(String raw) {
        if (raw == null) return null;
        String d = raw.replaceAll("[^0-9]", "");
        if (d.length() == 10) return "(" + d.substring(0, 3) + ") " + d.substring(3, 6) + "-" + d.substring(6);
        return raw;
    }

///////////////////// Operating Agreement //////////////////////////////

    @Transactional
    public String generateAndUploadOperatingAgreement(Long companyId) throws IOException, PdfException {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found for id: " + companyId));

        List<Member> members = memberRepository.findByCompany(company).orElse(Collections.emptyList());

        // Build PDF
        ByteArrayOutputStream pdf = buildOperatingAgreementPdf(company, members);

        // Upload to S3
        String s3Key = "Company_Document/" + companyId + "/Operating Agreement.pdf";
        s3Service.uploadFile(s3Key, pdf);

        // Upsert company_document record for type "OperatingAgreement"
        DocumentMaster dm = documentMasterRepository.findByTypeName("Operating Agreement")
                .orElseGet(() -> documentMasterRepository.findById(1)
                        .orElseThrow(() -> new ResourceNotFoundException("Document type not found")));

        CompanyDocuments doc = companyDocumentRepository
                .findByCompanyIdAndDocumentTypeId(companyId, dm.getDocumentTypeId())
                .orElse(new CompanyDocuments());

        doc.setCompany(company);
        doc.setDocumentMaster(dm);
        doc.setViewed(false);
        doc.setDownloaded(false);
        doc.setUploadedAt(LocalDateTime.now());
        companyDocumentRepository.save(doc);

        return "Operating Agreement generated & uploaded: s3://" + s3Service.getBucketName() + "/" + s3Key;
    }

// ─────────────────────────────────────────────────────────────────────────────
    // NEW: Operating Agreement PDF builder (Multi-Member Managed)

    private Paragraph keep(Paragraph p) {
        return p.setKeepTogether(true).setKeepWithNext(true);
    }

    private void addK(Document doc, Paragraph p) {
        doc.add(keep(p).setKeepTogether(true).setKeepWithNext(true));
    }

    private void addArticleHeader(Document doc, String article, String title, PdfFont bold) {
        addK(doc, new Paragraph(article)
                .setFont(bold).setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10f).setMarginBottom(2f)
                .setKeepWithNext(true)
        );

        addK(doc, new Paragraph(title)
                .setFont(bold).setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(6f)
                .setKeepWithNext(true)
        );
    }

    private void addBullet(Document doc, String labelBold, String text, PdfFont regular) {
        addK(doc, new Paragraph()
                .add(new Text(labelBold + " ").setBold())
                .add(new Text(text))
                .setFont(regular).setFontSize(12)
                .setTextAlignment(TextAlignment.JUSTIFIED)
                .setMarginBottom(4f)
                .setKeepTogether(true)
                .setKeepWithNext(true));
    }
    private ByteArrayOutputStream buildOperatingAgreementPdf(Company company, List<Member> members) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.LETTER);

        // Tighter top margin
        doc.setMargins(36, 55, 72, 55);

        // Fonts & color
        PdfFont regular = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
        PdfFont bold = PdfFontFactory.createFont(StandardFonts.TIMES_BOLD);
        Color LIGHT_BLACK = new DeviceRgb(55, 55, 55);

        // Company name (safe)
        String companyFullName = (nvl(company.getCompanyName()) + " " +
                Optional.ofNullable(company.getSuffixMaster()).map(SuffixMaster::getSuffix).orElse("")).trim();

        // Title
        addK(doc, new Paragraph("OPERATING AGREEMENT")
                .setFont(bold).setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(0f)
                .setKeepTogether(true));

        addK(doc, new Paragraph("FOR")
                .setFont(bold).setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(0f)
                .setKeepTogether(true));

        addK(doc, new Paragraph(companyFullName)
                .setFont(bold).setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(0f)
                .setKeepTogether(true));

        // Rule (kept as-is)
        addK(doc, new Paragraph(company.getCompanyName())
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(0f)
                .setKeepTogether(true));

        // Determine member-managed or multiple-member-managed LLC
        int memberCount = (members == null) ? 0 : members.size();
        String mmHeading = (memberCount > 1)
                ? "A MULTIPLE MEMBER-MANAGED LIMITED LIABILITY COMPANY"
                : "A SINGLE MEMBER-MANAGED LIMITED LIABILITY COMPANY";

        addK(doc, new Paragraph(mmHeading)
                .setFont(regular).setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(0f)
                .setKeepTogether(true));

        // ARTICLE I — Company Formation
        addArticleHeader(doc, "ARTICLE I", "Company Formation", bold);
        addBullet(doc, "FORMATION.",
                "The members have formed a Limited Liability Company (the \"Company\") according to the laws of the state in which the Company was formed. This operating agreement is entered into and effective as of the date it is adopted by the members.",
                regular);
        addBullet(doc, "REGISTERED AGENT.",
                "The name and location of the Company’s registered agent will be stated in the company’s formation documents.",
                regular);
        addBullet(doc, "TERM.",
                "The Company will continue perpetually unless: (a) Members whose capital interest as defined in Article 2.2 exceeds 50 percent vote for dissolution; (b) Any event which causes the Company’s business to become unlawful; (c) The death, resignation, expulsion, bankruptcy, retirement of a member or any other event that terminates the continued membership of a member of the Company; or (d) Any other event causing dissolution of the Company under applicable state laws.",
                regular);
        addBullet(doc, "CONTINUANCE OF COMPANY.",
                "If an event described in Section 1.3(c) occurs and there are at least two remaining members, those members may continue the business of the Company by unanimous vote within ninety (90) days after the event.",
                regular);
        addBullet(doc, "BUSINESS PURPOSE.",
                "The Company will conduct any lawful business deemed appropriate in carrying out the Company’s objectives.",
                regular);
        addBullet(doc, "PRINCIPAL PLACE OF BUSINESS.",
                "The Company’s principal place of business will be stated in the formation documents, or as selected by the members.",
                regular);
        addBullet(doc, "THE MEMBERS.",
                "The name and residential address of each member are listed in the Certification of Member section of this agreement.",
                regular);
        addBullet(doc, "ADMISSION OF ADDITIONAL MEMBERS.",
                "Additional members may only be admitted through issuance of a Certificate of New Membership or as otherwise provided in this agreement.",
                regular);

        // ARTICLE II — Capital Contributions
        addArticleHeader(doc, "ARTICLE II", "Capital Contributions", bold);
        addBullet(doc, "INITIAL CONTRIBUTIONS.",
                "The members will initially contribute capital to the Company, as described in Exhibit 1. The agreed total value of such property and cash is ____________________________.",
                regular);
        addBullet(doc, "ADDITIONAL CONTRIBUTIONS.",
                "Except as provided in ARTICLE 6.2, no member will be obligated to make any additional contribution to the Company's capital.",
                regular);

        // ARTICLE III — Profits, Losses and Distributions
        addArticleHeader(doc, "ARTICLE III", "Profits, Losses and Distributions", bold);
        addBullet(doc, "PROFITS/LOSSES.",
                "For financial accounting and tax purposes, the Company's net profits or net losses will be determined on an annual basis and allocated to the members in proportion to each member's capital interest.",
                regular);
        addBullet(doc, "DISTRIBUTIONS.",
                "The members will determine and distribute available funds annually or as they see fit, subject to Treasury Regulations including qualified income offset.",
                regular);

        // ARTICLE IV — Management (condensed)
        addArticleHeader(doc, "ARTICLE IV", "Management", bold);
        addBullet(doc, "MANAGEMENT OF THE BUSINESS.",
                "The members are responsible for the management of the Company.",
                regular);
        addBullet(doc, "POWERS OF MEMBERS.",
                "Members may act on behalf of the Company to manage assets, borrow money, grant security interests, compromise claims, and employ persons or firms for operations.",
                regular);
        addBullet(doc, "CHIEF EXECUTIVE MEMBER.",
                "The members may elect a Chief Executive Member to carry out decisions; the other members will not bind the Company if so elected.",
                regular);
        addBullet(doc, "RECORDS & INFORMATION.",
                "Company records will be maintained and made available to members upon request; members may inspect and copy such records at their expense.",
                regular);
        addBullet(doc, "EXCULPATION & INDEMNIFICATION.",
                "Acts or omissions performed in good faith to promote the Company’s interests will not subject the Chief Executive Member to liability; the Company will indemnify eligible persons as allowed by law.",
                regular);

        // ARTICLE V — Compensation
        addArticleHeader(doc, "ARTICLE V", "Compensation", bold);
        addBullet(doc, "MANAGEMENT FEE.",
                "Any member rendering services to the Company is entitled to compensation proportionate with the value of those services.",
                regular);
        addBullet(doc, "REIMBURSEMENT.",
                "The Company will reimburse members for all direct out-of-pocket expenses incurred in managing the Company.",
                regular);

        // ARTICLE VI — Bookkeeping
        addArticleHeader(doc, "ARTICLE VI", "Bookkeeping", bold);
        addBullet(doc, "BOOKS.",
                "The Company will maintain complete and accurate accounting records at its principal place of business.",
                regular);
        addBullet(doc, "MEMBERS’ ACCOUNTS.",
                "Separate capital and distribution accounts will be maintained for each member per Treasury Regulations.",
                regular);
        addBullet(doc, "REPORTS.",
                "After each calendar year, a statement of each member’s distributive share of income and expense will be prepared and delivered for tax reporting purposes.",
                regular);

        // ARTICLE VII — Transfers (condensed)
        addArticleHeader(doc, "ARTICLE VII", "Transfers", bold);
        addBullet(doc, "ASSIGNMENT.",
                "A member proposing to sell or assign any part of their interest must first offer it to the other members. Without unanimous approval, the transferee has only economic rights and no management rights.",
                regular);

        // ARTICLE VIII — Dissolution (condensed)
        addArticleHeader(doc, "ARTICLE VIII", "Dissolution", bold);
        addBullet(doc, "DISSOLUTION.",
                "The members may dissolve the Company at any time. Upon dissolution the Company must pay its debts before distributing any remaining assets or capital to the members.",
                regular);

        // CERTIFICATION OF MEMBER
        addK(doc, new Paragraph("\nCERTIFICATION OF MEMBER")
                .setFont(bold).setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(8f).setMarginBottom(0f)  // Removed marginBottom to reduce space
                .setKeepTogether(true));

        LocalDate effectiveDate = company.getCompanyEffectiveDate(); // may be null
        Integer day = (effectiveDate != null) ? effectiveDate.getDayOfMonth() : null;
        String mon = (effectiveDate != null) ? effectiveDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) : null;
        Integer year = (effectiveDate != null) ? effectiveDate.getYear() : null;

        addK(doc, new Paragraph()
                .add(new Text("The undersigned hereby agree, acknowledge, and certify that the foregoing operating agreement is adopted ")
                        .setFont(regular).setFontSize(12))
                .add(new Text("and approved by each member as of this ").setFont(regular).setFontSize(12))
                .add(new Text(day != null ? String.valueOf(day) : "_____").setFont(regular).setFontSize(12).setFontColor(LIGHT_BLACK).setUnderline())
                .add(new Text(" day of ").setFont(regular).setFontSize(12))
                .add(new Text(mon != null ? mon : "______________").setFont(regular).setFontSize(12).setFontColor(LIGHT_BLACK).setUnderline())
                .add(new Text(", ").setFont(regular).setFontSize(12))
                .add(new Text(year != null ? String.valueOf(year) : "20____").setFont(regular).setFontSize(12).setFontColor(LIGHT_BLACK).setUnderline())
                .add(new Text(".").setFont(regular).setFontSize(12))
                .setTextAlignment(TextAlignment.JUSTIFIED));

        addK(doc, new Paragraph("Members:")
                .setFont(bold).setFontSize(12)
                .setMarginBottom(0f)  // Removed marginBottom to reduce space
                .setKeepTogether(true));

        Table membersCert = formatMembersCertTable(members, regular, bold)
                .setKeepTogether(true);
        doc.add(membersCert);

        // Exhibit 1
        addK(doc, new Paragraph("\nEXHIBIT 1")
                .setFont(bold).setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(12f).setMarginBottom(0f)  // Removed marginBottom to reduce space
                .setKeepTogether(true));

        addK(doc, new Paragraph("CAPITAL CONTRIBUTIONS")
                .setFont(bold).setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(8f)
                .setKeepTogether(true));

        addK(doc, new Paragraph(
                "Pursuant to ARTICLE 2, the members' initial contribution to the Company capital is stated to be " + company.getCompanyEffectiveDate() +
                        "The description and each individual portion of this initial contribution is as follows:")
                .setFont(regular).setFontSize(12)
                .setTextAlignment(TextAlignment.JUSTIFIED)
                .setMarginBottom(6f)
                .setKeepTogether(true));

        // Adjust table for members
        Table ex1 = new Table(UnitValue.createPercentArray(new float[]{50, 50})) // Two columns: one for description, one for amount
                .useAllAvailableWidth()
                .setKeepTogether(true);

        int totalMembers = (members == null) ? 0 : members.size();  // Determine the number of members

        for (int i = 0; i < totalMembers; i++) {
            Member member = members.get(i);
            String name = member.getFirstName() + " " + member.getLastName();
            String amount = "$______________"; // Placeholder for member's capital contributions

            ex1.addCell(new Cell().add(keep(new Paragraph(name))).setBorder(new SolidBorder(0.5f)));
            ex1.addCell(new Cell().add(keep(new Paragraph(amount))).setBorder(new SolidBorder(0.5f)));
        }

        doc.add(ex1);  // Add the table to the document

        doc.close();
        return out;
    }


    private Table formatMembersCertTable(List<Member> members, PdfFont regular, PdfFont bold) {
        // Name 28% | Ownership 14% | Address 44% | Signature 14%
        Table t = new Table(UnitValue.createPercentArray(new float[]{28, 14, 44, 14}))
                .setWidth(UnitValue.createPercentValue(95))
                .setHorizontalAlignment(HorizontalAlignment.CENTER)
                .setKeepTogether(true); // keep the whole table on one page if possible

        // Adding headers
        t.addHeaderCell(headerCellTight("Name", bold));
        t.addHeaderCell(headerCellTight("Ownership", bold));  // Ensure "Ownership" is one word
        t.addHeaderCell(headerCellTight("Address", bold));
        t.addHeaderCell(headerCellTight("Signature", bold));

        int total = (members == null) ? 0 : members.size();
        if (members != null && !members.isEmpty()) {
            for (Member m : members) {
                String name = (nvl(m.getFirstName()) + " " + nvl(m.getLastName())).trim();
                String street = joinSpace(nvl(m.getStreetAddress1()), z(m.getStreetAddress2()));
                String addr = joinComma(street, joinCityStateZip(nvl(m.getCity()), nvl(m.getState()), nvl(m.getZipCode())));
                String pct = normalizePercent(extractOwnership(m), total);

                t.addCell(bodyCellTight(name, regular));
                t.addCell(bodyCellTight(pct, regular).setTextAlignment(TextAlignment.CENTER));
                t.addCell(bodyCellTight(addr, regular));  // Wider column for addresses to avoid wrapping
                t.addCell(bodyCellTight("X____________________________", regular));
            }
        } else {
            for (int i = 0; i < 4; i++) {
                t.addCell(bodyCellTight("____________________________", regular));
                t.addCell(bodyCellTight("______%", regular).setTextAlignment(TextAlignment.CENTER));
                t.addCell(bodyCellTight("____________________________", regular));
                t.addCell(bodyCellTight("X____________________________", regular));
            }
        }
        return t;
    }

    private Cell headerCellTight(String text, PdfFont bold) {
        // Adjusting the header cell for proper alignment
        Paragraph p = new Paragraph(text)
                .setFont(bold).setFontSize(12)
                .setMultipliedLeading(1.0f)  // tight line height
                .setTextAlignment(TextAlignment.CENTER);  // Align text to center
        return new Cell()
                .add(p)
                .setBorder(new SolidBorder(0.8f))
                .setPaddingTop(5).setPaddingBottom(5)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setKeepTogether(true);
    }

    private Cell bodyCellTight(String text, PdfFont regular) {
        // Adjusting body cell for proper alignment and ensuring text is not broken
        Paragraph p = new Paragraph(blankToDash(text))
                .setFont(regular).setFontSize(12)
                .setMultipliedLeading(1.0f)  // tighter lines to reduce row height
                .setTextAlignment(TextAlignment.CENTER);  // Align text to center
        return new Cell()
                .add(p)
                .setBorder(new SolidBorder(0.5f))
                .setPaddingTop(4).setPaddingBottom(4)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setKeepTogether(true);
    }


    // Ownership extraction (tries common getters; adjust if you have a fixed one)
    private String extractOwnership(Member m) {
        for (String method : new String[]{"getOwnership", "getOwnershipPercentage", "getPercent"}) {
            try {
                Method meth = m.getClass().getMethod(method);
                Object v = meth.invoke(m);
                if (v != null) return v.toString();
            } catch (Exception ignored) {}
        }
        return "";
    }

    private String normalizePercent(String raw, int count) {
        String s = raw == null ? "" : raw.trim();
        if (!s.isBlank()) return s.endsWith("%") ? s : s + "%";
        if (count > 0) {
            double v = Math.round((100.0 / count) * 100.0) / 100.0;
            return (v == Math.floor(v)) ? ((int) v) + "%" : v + "%";
        }
        return "—";
    }



    public ByteArrayOutputStream generatePdf(
            Company company,
            CompanyPrincipal organizer,
            RegisteredAgent agent,
            List<ManagerMember> managers,
            List<Member> members
    ) throws Exception {

        final float FS_BODY = 11f;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4, false);

        // Fonts
        boldFont = PdfFontFactory.createFont(StandardFonts.TIMES_BOLD);
        bodyFont = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);

        // 1-inch margins
        document.setMargins(72, 72, 72, 72);

        // =========================
        // TITLE BLOCK
        // =========================
        String fullCompanyName =
                nullSafe(company.getCompanyName()) + " " +
                        (company.getSuffixMaster() != null ? nullSafe(company.getSuffixMaster().getSuffix()) : "LLC");

        Paragraph title = new Paragraph("ARTICLES OF ORGANIZATION")
                .setFont(boldFont)
                .setFontSize(18)
                .setTextAlignment(TextAlignment.CENTER);

        Paragraph ofLine = new Paragraph("OF")
                .setFont(boldFont)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER);

        Paragraph companyNameP = new Paragraph(fullCompanyName)
                .setFont(boldFont)
                .setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER);

        document.add(title);
        document.add(ofLine);
        document.add(companyNameP);
        document.add(new Paragraph("\n"));

        // Utility for article headings
        java.util.function.Function<String, Paragraph> articleHeading = headingText ->
                new Paragraph(headingText)
                        .setFont(boldFont)
                        .setFontSize(12)
                        .setMarginTop(10)
                        .setMarginBottom(4);

        // =========================
        // ARTICLE 1 – ENTITY NAME
        // =========================
        document.add(articleHeading.apply("ARTICLE 1 – ENTITY NAME"));

        document.add(
                new Paragraph()
                        .setFont(bodyFont)
                        .setFontSize(FS_BODY)
                        .add("The name of the limited liability company (LLC) is ")
                        .add(new Text(fullCompanyName).setFont(boldFont).setFontSize(FS_BODY))
                        .add(", which shall comply with the requirements of the applicable state's Limited Liability Company Act and include an appropriate designation such as \"LLC\" or \"Limited Liability Company.\"")
        );

        // =========================
        // ARTICLE 2 – PRINCIPAL OFFICE
        // =========================
        document.add(articleHeading.apply("ARTICLE 2 – PRINCIPAL OFFICE"));

        document.add(
                new Paragraph()
                        .setFont(bodyFont)
                        .setFontSize(FS_BODY)
                        .add("The principal office of the LLC is located at:\n")
                        .add(new Text("Address: ").setFont(boldFont))
                        .add(new Text(nullSafe(company.getStreetAddress1()) +
                                (company.getStreetAddress2() != null && !company.getStreetAddress2().isBlank()
                                        ? ", " + company.getStreetAddress2() : ""))
                                .setFont(bodyFont))
                        .add("\n")
                        .add(new Text("City, State, ZIP: ").setFont(boldFont))
                        .add(new Text(
                                nullSafe(company.getCity()) + ", " +
                                        nullSafe(company.getState()) + " " +
                                        nullSafe(company.getZipCode())
                        ).setFont(bodyFont))
        );

        // =========================
        // ARTICLE 3 – REGISTERED AGENT AND REGISTERED OFFICE
        // =========================
        document.add(articleHeading.apply("ARTICLE 3 – REGISTERED AGENT AND REGISTERED OFFICE"));

        document.add(
                new Paragraph()
                        .setFont(bodyFont)
                        .setFontSize(FS_BODY)
                        .add("The registered agent and registered office for service of process are as follows:\n\n")
                        .add(new Text("Registered Agent Name: ").setFont(boldFont))
                        .add(new Text(
                                nullSafe(agent.getFirstName()) + " " + nullSafe(agent.getLastName())
                        ).setFont(bodyFont))
                        .add("\n")
                        .add(new Text("Registered Office Address: ").setFont(boldFont))
                        .add(new Text(
                                formatAddress(
                                        agent.getStreetAddress1(), agent.getStreetAddress2(),
                                        agent.getCity(), agent.getState(),
                                        agent.getZipCode(), agent.getCountry()
                                )
                        ).setFont(bodyFont))
                        .add("\n\n")
                        .add("The registered agent is either  an individual resident of the state or an entity authorized to conduct business in the state.")
        );

        // =========================
        // ARTICLE 4 – BUSINESS PURPOSE
        // =========================
        document.add(articleHeading.apply("ARTICLE 4 – BUSINESS PURPOSE"));

        document.add(
                new Paragraph()
                        .setFont(bodyFont)
                        .setFontSize(FS_BODY)
                        .add("The purpose of the LLC is to engage in any lawful business activity for which a limited liability company may be organized under the laws of ")
                        .add(new Text(nullSafe(company.getState())))
                        .add(".")
        );

        // =========================
        // ARTICLE 5 – MANAGEMENT STRUCTURE
        // =========================
        document.add(articleHeading.apply("ARTICLE 5 – MANAGEMENT STRUCTURE"));

        String managementStyle = nullSafe(company.getManagementStyle());
        boolean isManagerManaged = managementStyle.equalsIgnoreCase("manager");

        document.add(
                new Paragraph()
                        .setFont(bodyFont)
                        .setFontSize(FS_BODY)
                        .add("The LLC will be ")
                        .add(new Text(isManagerManaged ? "manager-managed." : "member-managed.").setFont(boldFont))
        );

        if (isManagerManaged && managers != null && !managers.isEmpty()) {
            document.add(
                    new Paragraph()
                            .setFont(bodyFont)
                            .setFontSize(FS_BODY)
                            .setMarginTop(6)
                            .add(new Text("The names and addresses of initial managers are:\n"))
            );

            com.itextpdf.layout.element.List managerList =
                    new com.itextpdf.layout.element.List(ListNumberingType.DECIMAL)
                            .setFont(bodyFont)
                            .setFontSize(FS_BODY)
                            .setMarginLeft(22)
                            .setSymbolIndent(6);

            for (ManagerMember mm : managers) {
                String managerName = nullSafe(mm.getFirstName()) + " " + nullSafe(mm.getLastName());
                String managerAddr = formatAddress(
                        mm.getStreetAddress1(), mm.getStreetAddress2(),
                        mm.getCity(), mm.getState(),
                        mm.getZipCode(), mm.getCountry()
                );

                managerList.add((ListItem) new ListItem()
                        .add(new Paragraph()
                                .add(new Text(managerName).setFont(boldFont))
                                .add(" – " + nullSafe(managerAddr))
                                .setFont(bodyFont)
                                .setFontSize(FS_BODY)
                        )
                        .setMarginBottom(3)
                );
            }
            document.add(managerList);
        }

            if (members != null && !members.isEmpty()) {
                document.add(
                        new Paragraph()
                                .setFont(bodyFont)
                                .setFontSize(FS_BODY)
                                .setMarginTop(6)
                                .add(new Text("The names and addresses of initial members are:\n"))
                );


                com.itextpdf.layout.element.List memberList =
                        new com.itextpdf.layout.element.List(ListNumberingType.DECIMAL)
                                .setFont(bodyFont)
                                .setFontSize(FS_BODY)
                                .setMarginLeft(22)
                                .setSymbolIndent(6);

                for (Member m : members) {
                    String memberName = nullSafe(m.getFirstName()) + " " + nullSafe(m.getLastName());
                    String memberAddr = formatAddress(
                            m.getStreetAddress1(), m.getStreetAddress2(),
                            m.getCity(), m.getState(),
                            m.getZipCode(), m.getCountry()
                    );

                    memberList.add(
                            (ListItem) new ListItem()
                                    .add(new Paragraph()
                                            .add(new Text(memberName).setFont(boldFont))
                                            .add(" – " + nullSafe(memberAddr))
                                            .setFont(bodyFont)
                                            .setFontSize(FS_BODY)
                                    )
                                    .setMarginBottom(3)
                    );
                }
                document.add(memberList);

            }

            // =========================
            // ARTICLE 6 – DURATION
            // =========================
            document.add(articleHeading.apply("ARTICLE 6 – DURATION"));

            document.add(
                    new Paragraph()
                            .setFont(bodyFont)
                            .setFontSize(FS_BODY)
                            .add("The duration of the LLC shall be perpetual, unless otherwise stated in an amendment or dissolved under the applicable Limited Liability Company Act.")
            );

            // =========================
            // ARTICLE 7 – ORGANIZER INFORMATION
            // =========================
            document.add(articleHeading.apply("ARTICLE 7 – ORGANIZER INFORMATION"));

            document.add(
                    new Paragraph()
                            .setFont(bodyFont)
                            .setFontSize(FS_BODY)
                            .add("The name and address of the organizer of the LLC is:\n\n")
                            .add(new Text("Organizer Name: ").setFont(boldFont))
                            .add(new Text(nullSafe(organizer.getFirstName()) + " " + nullSafe(organizer.getLastName()))
                                    .setFont(bodyFont))
                            .add("\n")
                            .add(new Text("Organizer Address: ").setFont(boldFont))
                            .add(new Text(nullSafe(organizer.getEmail())).setFont(bodyFont)) // replace with real address if you have it
            );

            // =========================
            // ARTICLE 8 – EFFECTIVE DATE OF FILING
            // =========================
            document.add(articleHeading.apply("ARTICLE 8 – EFFECTIVE DATE OF FILING"));

            document.add(
                    new Paragraph()
                            .setFont(bodyFont)
                            .setFontSize(FS_BODY)
                            .add("This document shall become effective upon filing or on the date specified by law. For internal record purposes, the intended effective date is ")
                            .add(new Text(LocalDate.now().toString()))
                            .add(".")
            );

            // =========================
            // ARTICLE 9 – LIABILITY OF MEMBERS AND MANAGERS
            // =========================
            document.add(articleHeading.apply("ARTICLE 9 – LIABILITY OF MEMBERS AND MANAGERS"));

            document.add(
                    new Paragraph()
                            .setFont(bodyFont)
                            .setFontSize(FS_BODY)
                            .add("No member or manager of the LLC shall be personally liable for the debts, obligations, or liabilities of the company, except as required by law or by a separate written agreement.")
            );

            // =========================
            // ARTICLE 10 – INDEMNIFICATION
            // =========================
            document.add(articleHeading.apply("ARTICLE 10 – INDEMNIFICATION"));

            document.add(
                    new Paragraph()
                            .setFont(bodyFont)
                            .setFontSize(FS_BODY)
                            .add("The LLC may indemnify its members, managers, officers, employees, and agents to the fullest extent permitted by applicable state law.")
            );

            // =========================
            // ARTICLE 11 – TAX TREATMENT
            // =========================
            document.add(articleHeading.apply("ARTICLE 11 – TAX TREATMENT"));

            document.add(
                    new Paragraph()
                            .setFont(bodyFont)
                            .setFontSize(FS_BODY)
                            .add("For federal income tax purposes, the LLC initially elects to be treated in a manner consistent with applicable IRS regulations, as determined by the members. Unless otherwise specified in the operating agreement or subsequent elections, the LLC will be treated in a tax-efficient manner as permitted by law.")
            );

            // =========================
            // ARTICLE 12 – AMENDMENTS
            // =========================
            document.add(articleHeading.apply("ARTICLE 12 – AMENDMENTS"));

            document.add(
                    new Paragraph()
                            .setFont(bodyFont)
                            .setFontSize(FS_BODY)
                            .add("These Articles of Organization may be amended as permitted under the governing state laws and as approved by the required majority of members or managers, as set forth in the operating agreement or applicable statute.")
            );

            // =========================
            // ARTICLE 13 – DISSOLUTION
            // =========================
            document.add(articleHeading.apply("ARTICLE 13 – DISSOLUTION"));

            document.add(
                    new Paragraph()
                            .setFont(bodyFont)
                            .setFontSize(FS_BODY)
                            .add("The LLC may be dissolved upon the occurrence of any event specified in an operating agreement, by written consent of the required members or managers, or as otherwise provided by applicable state law.")
            );

            // =========================
            // ARTICLE 14 – SIGNATURE OF ORGANIZER
            // =========================
            document.add(articleHeading.apply("ARTICLE 14 – SIGNATURE OF ORGANIZER"));

            document.add(
                    new Paragraph()
                            .setFont(bodyFont)
                            .setFontSize(FS_BODY)
                            .add("I, the undersigned organizer, execute these Articles of Organization and affirm that the information provided is correct.\n\n")
            );

            // Signature block
            document.add(
                    new Paragraph()
                            .setFont(bodyFont)
                            .setFontSize(FS_BODY)
                            .add("Organizer Signature:" +
                                    nullSafe(organizer.getFirstName()) + " " + nullSafe(organizer.getLastName()) + "\n")
                            .add("Date: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))));

            document.close();
            return baos;
        }

}