package com.trivine.llc.api.service;

import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.PdfPageFormCopier;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.source.ByteArrayOutputStream; // alias not allowed in Java, remove
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.trivine.llc.api.crypto.PiiCryptoService;
import com.trivine.llc.api.dto.EinDetailsDto;
import com.trivine.llc.api.dto.llc.request.MemberMemberDto;
import com.trivine.llc.api.dto.response.UserProgressDto;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Robust SS-4 filler that:
 *  - reads template from classpath (never overwrites it)
 *  - repairs template in-memory (lenient parse + page copy)
 *  - fills fields, flattens, and appends "Ein Application Details" page
 *  - returns final PDF bytes as ByteArrayOutputStream
 *
 * iText 7/8 compatible.
 */
@Service
@RequiredArgsConstructor
public class Ss4PdfService {

    // --------------------------- CONSTANTS ---------------------------
    private static final String TEMPLATE_CLASSPATH = "/ss4.pdf"; // put EIN_Application (19).pdf as ss4.pdf
    private static final DateTimeFormatter MDY = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter MDY_FALLBACK = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private  final PiiCryptoService piiCryptoService;

    // --------------------------- PUBLIC API --------------------------

    /**
     * Main entry used by your PdfService. Safe and idempotent.
     */
    public ByteArrayOutputStream fillAndFlatten(UserProgressDto progress) throws Exception {
        // 1) Gather data
        EinDetailsDto ein = progress.getEinDetailsDto();
        int membersCount = ein.getNumberOfMembers();
        if (membersCount <= 0) membersCount = 1;

        MemberMemberDto resp = Optional.ofNullable(progress.getStep13b()).orElseGet(List::of)
                .stream().filter(m -> Boolean.TRUE.equals(m.getIsEinResponsibleParty()))
                .findFirst().orElse(null);

        String respFirst = (resp != null && resp.getFirstName() != null) ? resp.getFirstName()
                : (progress.getStep5() != null ? nvl(progress.getStep5().getFirstName()) : "");
        String respLast = (resp != null && resp.getLastName() != null) ? resp.getLastName()
                : (progress.getStep5() != null ? nvl(progress.getStep5().getLastName()) : "");
        String respPhone = (resp != null && resp.getPhoneNumber() != null) ? resp.getPhoneNumber()
                : (progress.getStep5() != null ? nvl(progress.getStep5().getPhoneNumber()) : "");
        String ssn = piiCryptoService.decrypt(ein.getSsnIdCipherJson());

        String legalName = joinSpace(
               nvl(progress.getStep2().getCompanyName()),
                nvl(progress.getStep2().getLlcSuffix())
        );
        String tradeName = nvl(ein.getTradeName());

        // Mailing (prefer step14 then step6 then EIN table)
        String mailStreet, mailCity, mailState, mailZip;

        mailStreet = nvl(ein.getMailingStreetAddress());
        mailCity = nvl(ein.getMailingCity());
        mailState = nvl(ein.getMailingState());
        mailZip = nvl(ein.getMailingZipCode());


        // Physical address
        String bizStreet = nvl(ein.getBusinessStreetAddress());
        String bizCity = nvl(ein.getBusinessCity());
        String bizState = nvl(ein.getBusinessState());
        String bizZip = nvl(ein.getBusinessZipCode());

        // Dates & counts
        LocalDate started = ein.getFormationDate();
        String closingMonth = ein.getClosingMonth() == null ? "" : String.valueOf(ein.getClosingMonth());

        String agEmp = nzInt(ein.getAgriculturalEmployees());
        String hhEmp = nzInt(ein.getHouseholdEmployees());
        String otherEmp = nzInt(ein.getOtherEmployees());

        String principalActivity = ein.getPrincipalActivity();

//        long reasonId = Optional.ofNullable(ein.getReasonForApplyingId()).orElse(1L);
//        String reasonKey = mapReasonKey(reasonId);

        String taxClass = chooseTaxClassification(membersCount, ein.getLlcType());
        String phone = Optional.ofNullable(ein.getPhoneNumber()).orElse(respPhone);
        String email = Optional.ofNullable(ein.getEmail()).orElse("");

        // 2) Read template bytes robustly
        byte[] tplBytes = readResourceBytes(TEMPLATE_CLASSPATH);
        verifyPdfHeader(tplBytes);

// Try to repair with PDFBox first if the iText cleanCopy fails
        try {
            tplBytes = cleanCopy(tplBytes); // original iText method
        } catch (com.itextpdf.io.exceptions.IOException e) {
            System.err.println("iText cleanCopy failed, trying repair with PDFBox...");
            tplBytes = repairWithPdfbox(tplBytes);
            // After PDFBox repair, try the cleanCopy again
            tplBytes = cleanCopy(tplBytes);
        } // in-memory healed copy

        // 3) Prepare output
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // 4) Open ONE PdfDocument with lenient reader + writer to out
        ReaderProperties props = new ReaderProperties();
        try (PdfDocument pdf = new PdfDocument(
                new PdfReader(new ByteArrayInputStream(tplBytes), props),
                new PdfWriter(out))) {

            stripXfa(pdf);

            PdfAcroForm form = PdfAcroForm.getAcroForm(pdf, true);
            form.setGenerateAppearance(true);
            form.setNeedAppearances(true);

            // 5) Fill text
            setTextBySuffix(form, "f1_2[0]", legalName);                                     // 1
            setTextBySuffix(form, "f1_3[0]", tradeName);                                     // 2
            setTextBySuffix(form, "f1_5[0]", mailStreet);                                    // 4a
            setTextBySuffix(form, "f1_6[0]", joinCityStateZip(mailCity, mailState, mailZip));// 4b
            setTextBySuffix(form, "f1_7[0]", bizStreet);                                     // 5a
            setTextBySuffix(form, "f1_8[0]", joinCityStateZip(bizCity, bizState, bizZip));   // 5b
            setTextBySuffix(form, "f1_9[0]", joinComma(bizState,"USA"));                  // 6 (fallback)
            setTextBySuffix(form, "f1_10[0]", joinSpace(respFirst, respLast));                // 7a
            setTextBySuffix(form, "f1_11[0]", ssn);                                           // 7b
            setTextBySuffix(form, "f1_12[0]", String.valueOf(membersCount));                  // 8b
            setTextBySuffix(form, "f1_31[0]", started.format(MDY));                           // 11
            setTextBySuffix(form, "f1_32[0]", closingMonth);                                  // 12
            setTextBySuffix(form, "f1_33[0]", agEmp);                                         // 13 ag
            setTextBySuffix(form, "f1_34[0]", hhEmp);                                         // 13 hh
            setTextBySuffix(form, "f1_35[0]", otherEmp);                                      // 13 other
            setTextBySuffix(form, "f1_19[0]", "LLC"); // 9a specify field

            // 6) Radios & checks
            selectRadioByIndex(form, "c1_1", 0); // 8a LLC? Yes
            selectRadioByIndex(form, "c1_2", 0); // 8c US organized?
            selectRadioByIndex(form, "c1_3", 15); //9a Type of entity


            int idx10=1;
            selectRadioByIndex(form, "c1_4", idx10); // Reason For Applying

            setCheckBySuffix(form, "c1_5[0]", Boolean.TRUE.equals(ein.getFileAnnualPayrollTaxes())); // 14 in ein Form

            String pa = normalize(principalActivity);
            int idx16 = switch (pa) {
                case "health care & social assistance" -> 0;
                case "wholesale—agent/broker" -> 1;
                case "construction" -> 2;
                case "rental & leasing" -> 3;
                case "transportation & warehousing"-> 4;
                case "accommodation & food services" -> 5;
                case "wholesale—other" -> 6;
                case "retail" -> 7;
                case "real estate" -> 8;
                case "manufacturing" ->9;
                case "finance & insurance" -> 10;
                default  -> 11;
            };
            selectRadioByIndex(form, "c1_6", idx16); // Principal Activity Radio Button
            if(idx16==11){
                setTextBySuffix(form, "f1_37[0]", nvl(ein.getPreviousFederalTaxId())); // Principal Activity others specify
            }

            selectRadioByIndex(form, "c1_7", ein.getPaysFederalExciseTaxes() ? 0 : 1);
            if (Boolean.TRUE.equals(ein.getPaysFederalExciseTaxes())) {
                setTextBySuffix(form, "f1_39[0]", nvl(ein.getPreviousFederalTaxId()));
            }

            addEinDetailsPage(pdf,ein);

            // 7) Flatten (with tagged PDF rescue)
            try {
                form.flattenFields();
            } catch (Exception taggingIssue) {
                pdf.getCatalog().getPdfObject().remove(PdfName.StructTreeRoot);
                pdf.getCatalog().getPdfObject().remove(PdfName.RoleMap);
                pdf.getCatalog().getPdfObject().remove(PdfName.MarkInfo);
                for (int i = 1; i <= pdf.getNumberOfPages(); i++) {
                    pdf.getPage(i).getPdfObject().remove(PdfName.StructParents);
                }
                form.flattenFields();
            }
        }

        return out;
    }

    // --------------------------- PAGE 2 BUILDER ---------------------------

    private void addEinDetailsPage(PdfDocument pdf, EinDetailsDto ein) throws Exception {
        // Define page size and get the newly added page
        PageSize size = PageSize.LETTER;
        pdf.addNewPage(size);
        int pageNumber = pdf.getNumberOfPages();
        PdfPage newPage = pdf.getPage(pageNumber);

        // Define fonts and colors
        PdfFont fontRegular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        Color lineColor = new DeviceRgb(0x33, 0x33, 0x33);

        // --- Use low-level PdfCanvas for elements with absolute positioning ---

        // Draw top-right "Page 3" text
        new Canvas(new PdfCanvas(newPage), newPage.getPageSize())
                .showTextAligned(
                        new Paragraph("Page ").setFont(fontRegular).setFontSize(9f)
                                .add(new Paragraph(String.valueOf(pageNumber)).setFont(fontBold).setFontSize(11f)),
                        newPage.getPageSize().getWidth() - 36,
                        newPage.getPageSize().getHeight() - 24,
                        TextAlignment.RIGHT
                ).close();

        // Draw top and bottom horizontal lines
        PdfCanvas pdfCanvas = new PdfCanvas(newPage);
        pdfCanvas.setLineWidth(0.75f).setStrokeColor(lineColor)
                .moveTo(36, newPage.getPageSize().getHeight() - 40)
                .lineTo(newPage.getPageSize().getWidth() - 36, newPage.getPageSize().getHeight() - 40)
                .stroke();
        pdfCanvas.setLineWidth(0.75f).setStrokeColor(lineColor)
                .moveTo(36, 36)
                .lineTo(size.getWidth() - 36, 36)
                .stroke();


        // --- Use a high-level Canvas for the main layout content (title and table) ---

        // Create a rectangle that defines the writable area INSIDE the margins
        Rectangle contentArea = newPage.getPageSize().clone()
                .applyMargins(50, 36, 40, 36, false); // top, right, bottom, left

        // Create a Canvas that will render elements only within that contentArea
        try (Canvas canvas = new Canvas(pdfCanvas, contentArea)) {
            // Add Title
            canvas.add(new Paragraph("Ein Application Details")
                    .setFont(fontBold).setFontSize(20f)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(18));

            // Create and populate the Table
            float[] cols = {45f, 55f};
            Table table = new Table(UnitValue.createPercentArray(cols)).useAllAvailableWidth();

            Cell h1 = new Cell().add(new Paragraph("Data Fields").setFont(fontBold).setFontSize(12f))
                    .setTextAlignment(TextAlignment.CENTER);
            Cell h2 = new Cell().add(new Paragraph("Value").setFont(fontBold).setFontSize(12f))
                    .setTextAlignment(TextAlignment.CENTER);
            Border hdrBorder = new SolidBorder(1f);
            h1.setBorder(hdrBorder);
            h2.setBorder(hdrBorder);
            table.addCell(h1);
            table.addCell(h2);

            addRow(table, "Husband Wife Members", yn(ein.getHusbandWifeMembers()), fontRegular);
            if(ein.getHusbandWifeMembers()){
                addRow(table, "LLC Type",ein.getLlcType() , fontRegular);
            }
            addRow(table, "Sells Alcohol Tobacco Firearms", yn(ein.getSellsAlcoholTobaccoFirearms()), fontRegular);
            addRow(table, "File Annual Payroll Taxes", yn(ein.getFileAnnualPayrollTaxes()), fontRegular);
            addRow(table, "Involves Gambling", yn(ein.getInvolvesGambling()), fontRegular);
            addRow(table, "Owns Heavy Vehicle", yn(ein.getOwnsHeavyVehicle()), fontRegular);
            addRow(table, "Hire Employee in 12 Months", yn(ein.getHireEmployeeIn12Months()), fontRegular);
            addRow(table, "First Wage Date", fmtDate(ein.getFirstWageDate()), fontRegular);
            addRow(table, "Principal Activity Sub Category", ein.getPrincipalSubActivity(), fontRegular);
            addRow(table, "Use Physical Address for Mailing", yn(ein.getUsePhysicalAddressForMailing()), fontRegular);


            // Add the table to the canvas
            canvas.add(table);
        } // The try-with-resources block will automatically close the canvas
    }

    private void addRow(Table table, String label, String value, PdfFont font) {
        Cell c1 = new Cell().add(new Paragraph(label).setFont(font).setFontSize(11f))
                .setTextAlignment(TextAlignment.LEFT)
                .setBorder(new SolidBorder(1f))
                .setMinHeight(24f);
        Cell c2 = new Cell().add(new Paragraph(value == null ? "—" : value).setFont(font).setFontSize(11f))
                .setTextAlignment(TextAlignment.LEFT)
                .setBorder(new SolidBorder(1f))
                .setMinHeight(24f);
        table.addCell(c1);
        table.addCell(c2);
    }

    // --------------------------- PDF OPEN/REPAIR ---------------------------

    /**
     * Load resource bytes from classpath; throw if missing.
     */
    private byte[] readResourceBytes(String path) throws Exception {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing template on classpath: " + path);
            }
            byte[] b = in.readAllBytes();
            if (b.length < 8) throw new IllegalStateException("Template too small: " + b.length);
            return b;
        }
    }

    /**
     * Verify PDF header to catch wrong file on classpath early.
     */
    private void verifyPdfHeader(byte[] bytes) {
        String head = new String(bytes, 0, Math.min(8, bytes.length));
        if (!head.startsWith("%PDF-")) {
            throw new IllegalStateException("Classpath file is not a PDF (header=" + head + ")");
        }
    }

    /**
     * Create a pristine in-memory copy by page copying.
     * Fixes linearized/incremental/XFA weirdness that causes xref errors.
     */
    private byte[] cleanCopy(byte[] srcBytes) throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream(srcBytes);
        ByteArrayOutputStream out = new ByteArrayOutputStream(srcBytes.length + 2048);

        // ReaderProperties is used, but no need to call setStrictParsing.
        // Lenient parsing is the default behavior.
        ReaderProperties props = new ReaderProperties();

        try (PdfDocument src = new PdfDocument(new PdfReader(in, props));
             PdfDocument dst = new PdfDocument(new PdfWriter(out))) {
            src.copyPagesTo(1, src.getNumberOfPages(), dst, new PdfPageFormCopier());
        }
        return out.toByteArray();
    }


    private byte[] repairWithPdfbox(byte[] srcBytes) throws Exception {
        try (PDDocument doc = Loader.loadPDF(srcBytes)) { // Correct usage
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }



    /**
     * Remove XFA packet so AcroForm layer is authoritative.
     */
    private void stripXfa(PdfDocument pdf) {
        var catalog = pdf.getCatalog().getPdfObject();
        var acro = catalog.getAsDictionary(PdfName.AcroForm);
        if (acro != null && acro.containsKey(PdfName.XFA)) {
            acro.remove(PdfName.XFA);
        }
    }

    // --------------------------- FORM HELPERS ---------------------------

    private PdfFormField findBySuffix(PdfAcroForm form, String suffix) {
        for (Entry<String, PdfFormField> e : form.getAllFormFields().entrySet()) {
            if (e.getKey().endsWith(suffix)) return e.getValue();
        }
        return null;
    }

    private void setTextBySuffix(PdfAcroForm form, String suffix, String value) {
        PdfFormField f = findBySuffix(form, suffix);
        if (f != null) f.setValue(value == null ? "" : value);
    }

    private static String getOnState(PdfFormField f) {
        String[] states = f.getAppearanceStates();
        if (states != null) {
            for (String s : states) {
                if (!"Off".equalsIgnoreCase(s)) return s;
            }
        }
        return "Yes";
    }

    private void setCheckBySuffix(PdfAcroForm form, String suffix, boolean on) {
        PdfFormField f = findBySuffix(form, suffix);
        if (f != null) {
            String onVal = getOnState(f);
            f.setValue(on ? onVal : "Off");
        }
    }

    private void selectRadioByIndex(PdfAcroForm form, String groupBase, int indexToSelect) {
        List<Map.Entry<String, PdfFormField>> widgets = findGroupWidgets(form, groupBase);
        for (int i = 0; i < widgets.size(); i++) {
            PdfFormField f = widgets.get(i).getValue();
            String onVal = getOnState(f);
            f.setValue(i == indexToSelect ? onVal : "Off");
        }
    }

    private List<Entry<String, PdfFormField>> findGroupWidgets(PdfAcroForm form, String groupBase) {
        Pattern p = Pattern.compile(Pattern.quote(groupBase) + "\\[(\\d+)]$");
        List<Entry<String, PdfFormField>> list = new ArrayList<>();
        for (Entry<String, PdfFormField> e : form.getAllFormFields().entrySet()) {
            Matcher m = p.matcher(e.getKey());
            if (m.find()) list.add(Map.entry(e.getKey(), e.getValue()));
        }
        list.sort(Comparator.comparingInt(e -> {
            Matcher m = Pattern.compile("\\[(\\d+)]$").matcher(e.getKey());
            return m.find() ? Integer.parseInt(m.group(1)) : 0;
        }));
        return list;
    }

    // --------------------------- STRING HELPERS ---------------------------

    private static String nvl(String s) {
        return s == null ? "" : s.trim();
    }

    private static String z(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String blankToDash(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    private static String joinSpace(String... s) {
        return Arrays.stream(s).filter(x -> x != null && !x.isBlank()).collect(Collectors.joining(" "));
    }

    private static String joinComma(String... s) {
        return Arrays.stream(s).filter(x -> x != null && !x.isBlank()).collect(Collectors.joining(", "));
    }

    private static String joinCityStateZip(String city, String st, String zip) {
        String left = joinComma(city, st);
        return joinComma(left, zip);
    }

    private static String maskSsn(String ssn) {
        if (ssn == null || ssn.isBlank()) return "—";
        String d = ssn.replaceAll("[^0-9]", "");
        if (d.length() == 9) return d.substring(0, 3) + "-" + d.substring(3, 5) + "-" + d.substring(5);
        return ssn;
    }

    private static String nzInt(Integer i) {
        return i == null ? "0" : String.valueOf(i);
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }

    private static String chooseTaxClassification(int members, String llcTypeRaw) {
        String llcType = normalize(llcTypeRaw);
        if ("corporation".equals(llcType) || "s-corp".equals(llcType)) return "corporation";
        if (members <= 1) return "sole";
        if (members >= 2) return "partnership";
        return "other";
    }

//    private static String mapReasonKey(long id) {
//        return switch ((int) id) {
//            case 1 -> "started";
//            case 2 -> "banking";
//            case 3 -> "hired";
//            case 4 -> "changed";
//            case 5 -> "purchased";
//            case 6 -> "withholding";
//            default -> "other";
//        };
//    }

    private String yn(Boolean b) {
        return b == null ? "—" : (b ? "Yes" : "No");
    }

    private String fmtDate(LocalDate d) {
        try {
            return d == null ? "—" : d.format(MDY);
        } catch (Exception e) {
            return d == null ? "—" : d.format(MDY_FALLBACK);
        }
    }




}
