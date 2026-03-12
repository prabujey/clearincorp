package com.trivine.llc.api.service;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.trivine.llc.api.dto.response.InvoiceDto;
import com.trivine.llc.api.dto.response.InvoiceItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final S3Service s3Service;
    private static final DateTimeFormatter ORDER_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy", Locale.US);


    public byte[] generateInvoice(InvoiceDto invoice) throws IOException {
        String fileKey = "Company_Document/" + invoice.getCompanyId() + "/Invoice_" + invoice.getId() + ".pdf";

        // 1. Try to fetch from S3
        try {
            return s3Service.downloadFile(fileKey).asByteArray();
        } catch (Exception e) {
            log.info("File not found in S3 or download failed, generating new one.");
        }

        // 2. Generate PDF
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        generatePdf(invoice, baos); // assumed this is a private method inside service

        // 3. Upload to S3
        s3Service.uploadFile(fileKey, baos);

        // 4. Return the generated PDF
        return baos.toByteArray();
    }

    private void generatePdf(InvoiceDto invoice, ByteArrayOutputStream baos) throws IOException {
        Objects.requireNonNull(invoice, "invoice must not be null");

        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document doc = new Document(pdfDoc, PageSize.A4);
        doc.setMargins(36, 36, 36, 36);

        PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        // ===== GLOBAL TYPOGRAPHY SETTINGS (based on screenshot) =====
        final float GLOBAL_LEADING = 1.5f;          // line height (1.35x)
        final float TITLE_FONT_SIZE = 22f;           // "Invoice"
        final float SECTION_HEADER_FONT_SIZE = 13f;  // "Bill From", "Bill To", TERMS, SUPPORT
        final float BODY_FONT_SIZE = 12f;            // body text, order info, table text
        final float GRAND_TOTAL_FONT_SIZE = 14f;     // "Grand Total"

        // ===== GLOBAL COLORS (from screenshot style) =====
        DeviceRgb labelColor = new DeviceRgb(17, 24, 39);    // very dark gray (labels / headings)
        DeviceRgb valueColor = new DeviceRgb(75, 85, 99);    // mid gray (values / body)
        DeviceRgb rowLineColor = new DeviceRgb(230, 233, 239);    // thin row separator line

        // ===== HEADER: Invoice (left) + Logo (right) =====
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{8, 2})) // wider left col, tighter right
                .useAllAvailableWidth()
                .setMarginBottom(12)
                .setMarginRight(0);

        // Title "Invoice" - LEFT
        Paragraph title = new Paragraph("Invoice")
                .setFont(bold)
                .setFontSize(TITLE_FONT_SIZE)
                .setFontColor(labelColor)
                .setMultipliedLeading(GLOBAL_LEADING)
                .setMargin(0);

        Cell titleCell = new Cell()
                .add(title)
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.LEFT)
                .setVerticalAlignment(VerticalAlignment.TOP) // new
                .setPadding(0)
                .setMargin(0);
        headerTable.addCell(titleCell);

        // Logo - RIGHT
        ImageData logoData = ImageDataFactory.create(new ClassPathResource("static/logo.png").getURL());
        Image logo = new Image(logoData).scaleToFit(105, 60); // slightly larger & sharper

        Cell logoCell = new Cell()
                .add(logo)
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .setVerticalAlignment(VerticalAlignment.TOP) // ensures top alignment
                .setPadding(0) // remove internal padding
                .setMargin(0); // remove spacing from edges
        headerTable.addCell(logoCell);

        doc.add(headerTable);


        // ===== VERY THIN, LIGHT SEPARATOR =====
        SolidLine thinLine = new SolidLine(0.6f);                 // very thin
        thinLine.setColor(rowLineColor);                          // light gray
        LineSeparator separator = new LineSeparator(thinLine);
        separator.setMarginTop(4).setMarginBottom(14);
        doc.add(separator);

        // ===== ORDER INFO BLOCK (LEFT ALIGNED) =====
        Paragraph orderInfo = new Paragraph()
                .setFont(regular)
                .setFontSize(BODY_FONT_SIZE)
                .setFontColor(valueColor)
                .setMultipliedLeading(GLOBAL_LEADING)
                .setMargin(0)
                .setMarginBottom(12);

        // Order Id
        orderInfo.add(new Text("Order Id ").setFont(bold).setFontColor(labelColor));
        orderInfo.add(new Text("#" + safeString(invoice.getInvoiceId()))
                .setFont(regular).setFontColor(valueColor));
        orderInfo.add("\n");
        String status = "";
        if (invoice.getStatus().equals("succeeded")) {
            status = "paid";
        }
        // Order Status
        orderInfo.add(new Text("Order Status ").setFont(bold).setFontColor(labelColor));
        orderInfo.add(new Text(safeString(status))
                .setFont(regular).setFontColor(valueColor));
        orderInfo.add("\n");

        // Order Date
        orderInfo.add(new Text("Order Date ").setFont(bold).setFontColor(labelColor));
        orderInfo.add(new Text(formatOrderDateSafe(invoice))
                .setFont(regular).setFontColor(valueColor));

        doc.add(orderInfo);

        doc.add(separator);

        // -------- 2. BILL FROM / BILL TO (two columns) --------
        Table partiesTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth()
                .setMarginTop(16)
                .setMarginBottom(18);

        // ===== LEFT: BILL FROM =====
        Paragraph billFromHeader = new Paragraph("Bill From")
                .setFont(bold)
                .setFontSize(SECTION_HEADER_FONT_SIZE)
                .setFontColor(labelColor)
                .setMultipliedLeading(GLOBAL_LEADING)
                .setMarginTop(0)
                .setMarginBottom(8);

        Paragraph billFromBody = new Paragraph()
                .add(safeString(invoice.getBillFrom()) + "\n")
                .add(safeString(invoice.getBillFromEmail()) + "\n")
                .add(safeString(invoice.getBillFromAddress()) + "\n")
                .add(safeString(invoice.getBillFromPhone()))
                .setFont(regular)
                .setFontSize(BODY_FONT_SIZE)
                .setFontColor(valueColor)
                .setMultipliedLeading(GLOBAL_LEADING)
                .setMargin(0);

        Cell fromCell = new Cell()
                .add(billFromHeader)
                .add(billFromBody)
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.LEFT);

        partiesTable.addCell(fromCell);

        // ===== RIGHT: BILL TO =====
        Paragraph billToHeader = new Paragraph("Bill To")
                .setFont(bold)
                .setFontSize(SECTION_HEADER_FONT_SIZE)
                .setFontColor(labelColor)
                .setMultipliedLeading(GLOBAL_LEADING)
                .setMarginTop(0)
                .setMarginBottom(8);

        Paragraph billToBody = new Paragraph()
                .add(safeString(invoice.getBillTo()) + "\n")
                .add(safeString(invoice.getBillToEmail()) + "\n")
                .add(safeString(invoice.getBillToAddress()) + "\n")
                .add(safeString(invoice.getBillToPhone()))
                .setFont(regular)
                .setFontSize(BODY_FONT_SIZE)
                .setFontColor(valueColor)
                .setMultipliedLeading(GLOBAL_LEADING)
                .setMargin(0);

        Cell toCell = new Cell()
                .add(billToHeader)
                .add(billToBody)
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT); // alignment unchanged

        partiesTable.addCell(toCell);

        doc.add(partiesTable);

        // --- Separator Line ---
        doc.add(separator);

        // -------- 3. ITEMS TABLE: Item Name | Unit Price | Unit | Total Cost --------
        Table itemTable = new Table(UnitValue.createPercentArray(new float[]{4, 2, 1, 2}))
                .useAllAvailableWidth()
                .setMarginTop(16)
                .setMarginBottom(24);

        // Header row (only bottom line)
        String[] headers = {"Item Name", "Unit Price", "Unit", "Total Cost"};
        for (int i = 0; i < headers.length; i++) {
            String h = headers[i];

            TextAlignment align =
                    (i == 0) ? TextAlignment.LEFT :
                            (i == 2) ? TextAlignment.CENTER :
                                    TextAlignment.RIGHT;

            Paragraph headerPara = new Paragraph(h)
                    .setFont(bold)
                    .setFontSize(BODY_FONT_SIZE)
                    .setFontColor(labelColor)
                    .setMultipliedLeading(GLOBAL_LEADING)
                    .setMargin(0);

            Cell headerCell = new Cell()
                    .add(headerPara)
                    .setBorderLeft(Border.NO_BORDER)
                    .setBorderRight(Border.NO_BORDER)
                    .setBorderTop(Border.NO_BORDER)
                    .setBorderBottom(new SolidBorder(rowLineColor, 0.5f)) // thin bottom line
                    .setTextAlignment(align)
                    .setPaddingTop(6)
                    .setPaddingBottom(8);

            itemTable.addCell(headerCell);
        }

        List<InvoiceItemDto> items = invoice.getOrders();
        if (items == null) {
            items = Collections.emptyList();
        }

        NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);
        BigDecimal grandTotal = BigDecimal.ZERO;

        if (items.isEmpty()) {
            Paragraph emptyPara = new Paragraph("No items found")
                    .setFont(regular)
                    .setFontSize(BODY_FONT_SIZE)
                    .setFontColor(valueColor)
                    .setMultipliedLeading(GLOBAL_LEADING)
                    .setMargin(0);

            Cell emptyCell = new Cell(1, 4)
                    .add(emptyPara)
                    .setBorderLeft(Border.NO_BORDER)
                    .setBorderRight(Border.NO_BORDER)
                    .setBorderTop(Border.NO_BORDER)
                    .setBorderBottom(new SolidBorder(rowLineColor, 0.5f))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPaddingTop(8)
                    .setPaddingBottom(8);
            itemTable.addCell(emptyCell);
        } else {
            for (InvoiceItemDto item : items) {

                String name = safeString(item.getItemName());
                String qty = item.getUnits() != null ? item.getUnits().toString() : "-";
                BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                BigDecimal unitTotal = item.getUnitTotalPrice() != null
                        ? item.getUnitTotalPrice()
                        : unitPrice.multiply(new BigDecimal(qty)); // fallback

                grandTotal = grandTotal.add(unitTotal);

                // Item Name
                itemTable.addCell(createItemCell(
                        name, regular, TextAlignment.LEFT, rowLineColor));

                // Unit Price
                itemTable.addCell(createItemCell(
                        currency.format(unitPrice), regular, TextAlignment.RIGHT, rowLineColor));

                // Unit
                itemTable.addCell(createItemCell(
                        qty, regular, TextAlignment.CENTER, rowLineColor));

                // Total Cost
                itemTable.addCell(createItemCell(
                        currency.format(unitTotal), regular, TextAlignment.RIGHT, rowLineColor));
            }
        }

        doc.add(itemTable);

        // -------- 4. GRAND TOTAL (right aligned, no box) --------
        Paragraph grandTotalPara = new Paragraph("Grand Total: " + currency.format(grandTotal))
                .setFont(bold)
                .setFontSize(GRAND_TOTAL_FONT_SIZE)
                .setFontColor(labelColor)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMultipliedLeading(GLOBAL_LEADING)
                .setMarginTop(8);

        doc.add(grandTotalPara);
        doc.close(); // closes pdfDoc & flushes baos
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }


    private Cell createItemCell(
            String text,
            PdfFont font,
            TextAlignment alignment,
            Color bottomLineColor
    ) {
        final float GLOBAL_LEADING = 1.5f;
        final float BODY_FONT_SIZE = 12f;
        DeviceRgb valueColor = new DeviceRgb(75, 85, 99); // mid-gray from screenshot

        Paragraph para = new Paragraph(text)
                .setFont(font)
                .setFontSize(BODY_FONT_SIZE)
                .setFontColor(valueColor)
                .setMultipliedLeading(GLOBAL_LEADING)
                .setMargin(0);

        return new Cell()
                .add(para)
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER)
                .setBorderTop(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(bottomLineColor, 0.5f))
                .setTextAlignment(alignment)
                .setPaddingTop(10)
                .setPaddingBottom(10);
    }


    private String formatOrderDateSafe(InvoiceDto invoice) {
        try {
            if (invoice.getOrderDate() == null) {
                return "-";
            }
            return invoice.getOrderDate().format(ORDER_DATE_FORMATTER);
        } catch (Exception ex) {
            // Fallback if date type or value is unexpected
            return "-";
        }
    }
}
