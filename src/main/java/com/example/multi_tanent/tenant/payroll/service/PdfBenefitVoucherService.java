package com.example.multi_tanent.tenant.payroll.service;

import com.example.multi_tanent.spersusers.enitity.CompanyInfo;
import com.example.multi_tanent.spersusers.enitity.Employee;
import com.example.multi_tanent.tenant.payroll.dto.BenefitVoucherPdfData;
import com.example.multi_tanent.tenant.payroll.entity.EmployeeBenefitProvision;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
public class PdfBenefitVoucherService {

    private final PdfFont regularFont;
    private final PdfFont boldFont;

    public PdfBenefitVoucherService() {
        try {
            ClassPathResource fontRes = new ClassPathResource("fonts/NotoNaskhArabic-Regular.ttf");
            byte[] fontBytes = fontRes.getInputStream().readAllBytes();
            this.regularFont = PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H,
                    PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            this.boldFont = PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H,
                    PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load fonts for PDF generation", e);
        }
    }

    public byte[] generateVoucher(BenefitVoucherPdfData pdfData) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(baos);
                PdfDocument pdf = new PdfDocument(writer);
                Document document = new Document(pdf, PageSize.A4)) {

            document.setMargins(36, 36, 36, 36);

            EmployeeBenefitProvision provision = pdfData.getProvision();
            CompanyInfo companyInfo = pdfData.getCompanyInfo();
            Employee employee = provision.getEmployee(); // This is safe now

            // Header
            addHeader(document, companyInfo);
            document.add(new Paragraph("\n"));

            // Details Table
            addDetailsTable(document, provision, employee);
            document.add(new Paragraph("\n\n"));

            // Signature Section
            addSignatureSection(document);

        } catch (Exception e) {
            throw new RuntimeException("Error generating Benefit Payout Voucher PDF", e);
        }
        return baos.toByteArray();
    }

    private void addHeader(Document document, CompanyInfo companyInfo) {
        Table headerTable = new Table(UnitValue.createPercentArray(new float[] { 1 })).useAllAvailableWidth();
        headerTable.setBorder(Border.NO_BORDER);
        if (companyInfo != null) {
            headerTable.addCell(
                    new Cell().add(new Paragraph(companyInfo.getCompanyName()).setFont(boldFont).setFontSize(18))
                            .setBorder(Border.NO_BORDER));
        }
        headerTable.addCell(new Cell().add(new Paragraph("Benefit Payout Voucher").setFont(boldFont).setFontSize(14))
                .setBorder(Border.NO_BORDER));
        document.add(headerTable);
    }

    private void addDetailsTable(Document document, EmployeeBenefitProvision provision, Employee employee) {
        Table detailsTable = new Table(UnitValue.createPercentArray(new float[] { 1, 2 })).useAllAvailableWidth();
        detailsTable.addCell(createDetailCell("Voucher No:", "PROV-" + provision.getId()));
        detailsTable.addCell(createDetailCell("Date:",
                provision.getPaidOutDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))));
        detailsTable
                .addCell(createDetailCell("Employee Name:", employee.getFirstName() + " " + employee.getLastName()));
        detailsTable.addCell(createDetailCell("Employee Code:", employee.getEmployeeCode()));
        detailsTable.addCell(createDetailCell("Benefit Type:", provision.getBenefitType().getName()));
        detailsTable.addCell(createDetailCell("Benefit Cycle:",
                provision.getCycleStartDate() + " to " + provision.getCycleEndDate()));
        detailsTable.addCell(createDetailCell("Amount Paid:", String.format("AED %,.2f", provision.getPaidAmount())));
        detailsTable.addCell(createDetailCell("Payment Method:", provision.getPaymentMethod().toString()));
        detailsTable.addCell(createDetailCell("Payment Details:", provision.getPaymentDetails()));
        document.add(detailsTable);
    }

    private Cell createDetailCell(String label, String value) {
        Cell cell = new Cell().setBorder(new SolidBorder(new DeviceGray(0.8f), 0.5f)).setPadding(8);
        cell.add(new Paragraph(label).setFont(boldFont).setFontSize(10));
        cell.add(new Paragraph(value).setFont(regularFont).setFontSize(10));
        return cell;
    }

    private void addSignatureSection(Document document) {
        Table signatureTable = new Table(UnitValue.createPercentArray(new float[] { 1, 1 })).useAllAvailableWidth();
        signatureTable.setBorder(Border.NO_BORDER).setMarginTop(50);

        signatureTable.addCell(
                new Cell().add(new Paragraph("\n\n____________________\nReceived By (Employee)").setFont(regularFont))
                        .setBorder(Border.NO_BORDER));
        signatureTable
                .addCell(new Cell()
                        .add(new Paragraph("\n\n____________________\nAuthorized Signatory")
                                .setTextAlignment(TextAlignment.RIGHT).setFont(regularFont))
                        .setBorder(Border.NO_BORDER));

        document.add(signatureTable);
    }
}