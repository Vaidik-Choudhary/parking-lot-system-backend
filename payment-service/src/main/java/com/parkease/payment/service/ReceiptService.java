package com.parkease.payment.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import com.parkease.payment.entity.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class ReceiptService {

    @Value("${app.receipt.storage-path}")
    private String storagePath;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private static final BaseColor DARK_BLUE  = new BaseColor(26, 60, 111);
    private static final BaseColor LIGHT_GRAY = new BaseColor(245, 245, 245);
    private static final BaseColor GREEN       = new BaseColor(34, 139, 34);


    public String generateReceipt(Payment payment) {
        try {
            Path dir = Paths.get(storagePath);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
        } catch (IOException e) {
            log.error("Failed to create receipts directory: {}", e.getMessage());
        }

        String fileName = storagePath + "receipt_" + payment.getPaymentId() + ".pdf";

        try {
            Document document = new Document(PageSize.A5);
            PdfWriter.getInstance(document, new FileOutputStream(fileName));
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, DARK_BLUE);
            Font subFont   = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.GRAY);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.DARK_GRAY);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK);
            Font amountFont= FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, GREEN);
            Font statusFont= FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, GREEN);

            Paragraph title = new Paragraph("ParkEase", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph tagline = new Paragraph("Smart Parking Management Platform", subFont);
            tagline.setAlignment(Element.ALIGN_CENTER);
            tagline.setSpacingAfter(15);
            document.add(tagline);

            addDivider(document);

            Font receiptTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, DARK_BLUE);
            Paragraph receiptHead = new Paragraph("PAYMENT RECEIPT", receiptTitle);
            receiptHead.setAlignment(Element.ALIGN_CENTER);
            receiptHead.setSpacingBefore(10);
            receiptHead.setSpacingAfter(15);
            document.add(receiptHead);

            Paragraph amount = new Paragraph("₹ " + String.format("%.2f", payment.getAmount()), amountFont);
            amount.setAlignment(Element.ALIGN_CENTER);
            amount.setSpacingAfter(5);
            document.add(amount);

            Paragraph statusPara = new Paragraph("PAYMENT SUCCESSFUL", statusFont);
            statusPara.setAlignment(Element.ALIGN_CENTER);
            statusPara.setSpacingAfter(15);
            document.add(statusPara);

            addDivider(document);

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(15);
            table.setSpacingAfter(15);
            table.setWidths(new float[]{40f, 60f});

            addTableRow(table, "Receipt No.",   "RCP-" + payment.getPaymentId(),   labelFont, valueFont);
            addTableRow(table, "Booking ID",    "#" + payment.getBookingId(),       labelFont, valueFont);
            addTableRow(table, "Driver",        payment.getDriverEmail(),            labelFont, valueFont);
            addTableRow(table, "Amount Paid",   "₹ " + String.format("%.2f", payment.getAmount()), labelFont, valueFont);
            addTableRow(table, "Currency",      payment.getCurrency(),               labelFont, valueFont);

            if (payment.getMode() != null) {
                addTableRow(table, "Payment Mode", payment.getMode().name(),          labelFont, valueFont);
            }
            if (payment.getRazorpayPaymentId() != null) {
                addTableRow(table, "Transaction ID", payment.getRazorpayPaymentId(),  labelFont, valueFont);
            }
            if (payment.getRazorpayOrderId() != null) {
                addTableRow(table, "Order ID",   payment.getRazorpayOrderId(),        labelFont, valueFont);
            }
            if (payment.getPaidAt() != null) {
                addTableRow(table, "Paid At",    payment.getPaidAt().format(FORMATTER), labelFont, valueFont);
            }
            if (payment.getDescription() != null && !payment.getDescription().isBlank()) {
                addTableRow(table, "Description", payment.getDescription(),            labelFont, valueFont);
            }

            document.add(table);
            addDivider(document);

            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.GRAY);
            Paragraph footer = new Paragraph(
                "This is a computer-generated receipt and does not require a signature.\n" +
                "For support, contact support@parkease.com\n" +
                "Generated: " + java.time.LocalDateTime.now().format(FORMATTER),
                footerFont
            );
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(10);
            document.add(footer);

            document.close();
            log.info("Receipt generated: {}", fileName);
            return fileName;

        } catch (Exception e) {
            log.error("Failed to generate receipt for payment {}: {}", payment.getPaymentId(), e.getMessage());
            return null;
        }
    }
    private void addDivider(Document doc) throws DocumentException {
        LineSeparator line = new LineSeparator();
        line.setLineColor(DARK_BLUE);
        doc.add(new Chunk(line));
    }

    private void addTableRow(PdfPTable table, String label, String value,
                              Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(6);
        labelCell.setBackgroundColor(LIGHT_GRAY);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "-", valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(6);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }
}
