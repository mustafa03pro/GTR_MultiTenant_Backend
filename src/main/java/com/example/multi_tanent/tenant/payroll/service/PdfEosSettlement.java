package com.example.multi_tanent.tenant.payroll.service;

import com.example.multi_tanent.spersusers.enums.ContractType;
import com.example.multi_tanent.spersusers.enitity.CompanyInfo;
import com.example.multi_tanent.spersusers.enitity.Employee;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import com.example.multi_tanent.tenant.employee.entity.JobDetails;
import com.example.multi_tanent.tenant.payroll.dto.FinalSettlementPdfData;
import com.example.multi_tanent.tenant.payroll.entity.EmployeeLoan;
import com.example.multi_tanent.tenant.payroll.entity.EndOfService;
import com.example.multi_tanent.tenant.payroll.enums.LoanStatus;
import com.example.multi_tanent.tenant.payroll.repository.EmployeeLoanRepository;
import com.example.multi_tanent.tenant.service.FileStorageService;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.core.io.ClassPathResource;
import com.itextpdf.io.image.ImageDataFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Renders an EOS statement that visually matches the “ASIES School” sample.
 */
@Service
public class PdfEosSettlement {

    private final EmployeeLoanRepository employeeLoanRepository;
    private final FileStorageService fileStorageService;
    private final PdfFont regularFont;
    private final PdfFont boldFont;

    // Colors used to mimic the screenshot
    private static final Color GREY_BAND = new DeviceGray(0.90f);
    private static final Color LIGHT_GREY_BORDER = new DeviceGray(0.80f);
    private static final Color PALE_YELLOW = new DeviceRgb(255, 236, 153);
    private static final Color STRONG_YELLOW = new DeviceRgb(255, 221, 51);
    private static final Color GREEN_TAG = new DeviceRgb(178, 224, 171);
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public PdfEosSettlement(EmployeeLoanRepository employeeLoanRepository,
            FileStorageService fileStorageService) {
        this.employeeLoanRepository = employeeLoanRepository;
        this.fileStorageService = fileStorageService;
        try {
            // You can replace the font with your preferred Latin/Arabic capable font.
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

    /** Public API */
    public byte[] generate(FinalSettlementPdfData data) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(baos);
                PdfDocument pdf = new PdfDocument(writer);
                Document doc = new Document(pdf, PageSize.A4)) {

            doc.setMargins(18, 22, 22, 22);

            addBrandRow(doc, data.getTenant());
            addTopTitle(doc, data.getCompanyInfo()); // grey band with statement title

            addRefBand(doc, data); // ALHS055 … (your ref/number if any)
            addCompanyTitle(doc, data.getCompanyInfo());

            addTwoColumnInfoTable(doc, data); // Date/Contract/Employee/Reason/Joining/Resign/Last day/Basic/Gross

            addDescriptionBox(doc, data); // large grid with A, B, Net rows

            addAcknowledgement(doc, data); // paragraph acknowledgement

            addSignatureGrid(doc, data); // signatures section

            addBottomStrip(doc, data.getCompanyInfo()); // footer contacts (simple)

        } catch (Exception e) {
            throw new RuntimeException("Error generating EOS PDF", e);
        }
        return baos.toByteArray();
    }

    // ------------------------------------------------------------
    // Header & branding
    // ------------------------------------------------------------
    private void addBrandRow(Document doc, Tenant tenant) {
        Table brand = new Table(UnitValue.createPercentArray(new float[] { 1, 1, 1 }))
                .useAllAvailableWidth();
        brand.setBorder(Border.NO_BORDER);
        // Left logo (placeholder if none)
        brand.addCell(logoCell(tenant, HorizontalAlignment.LEFT));
        // Center blank spacer
        brand.addCell(new Cell().setBorder(Border.NO_BORDER));
        // Right logo (same as left; adapt if you have a second)
        brand.addCell(logoCell(tenant, HorizontalAlignment.RIGHT));
        doc.add(brand);
    }

    private Cell logoCell(Tenant tenant, HorizontalAlignment align) {
        Cell c = new Cell().setBorder(Border.NO_BORDER);
        try {
            if (tenant != null && tenant.getLogoImgUrl() != null && !tenant.getLogoImgUrl().isEmpty()) {
                Resource res = fileStorageService.loadFileAsResource(tenant.getLogoImgUrl());
                Image img = new Image(ImageDataFactory.create(res.getURL())).setAutoScale(true);
                img.setMaxHeight(34);
                c.add(img);
            }
        } catch (Exception ignored) {
        }
        c.setHorizontalAlignment(align);
        return c;
    }

    private void addTopTitle(Document doc, CompanyInfo company) {
        String school = company != null && company.getCompanyName() != null ? company.getCompanyName()
                : "School / Company";
        Table t = new Table(UnitValue.createPercentArray(new float[] { 1 })).useAllAvailableWidth();
        Cell title = new Cell()
                .add(new Paragraph("Statement of End of Service (EOS) Dues")
                        .setFont(boldFont).setFontSize(12).setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(GREY_BAND)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f))
                .setPadding(6);
        t.addCell(title);
        Cell note = new Cell()
                .add(new Paragraph(
                        "(The below EOS is payable subject to completion of clearance formalities and signing of the EOS certification)")
                        .setFont(regularFont).setFontSize(8).setTextAlignment(TextAlignment.CENTER))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f))
                .setPadding(4);
        t.addCell(note);
        doc.add(t);
    }

    private void addRefBand(Document doc, FinalSettlementPdfData data) {
        Table band = new Table(UnitValue.createPercentArray(new float[] { 1 }))
                .useAllAvailableWidth();
        band.addCell(new Cell()
                .add(new Paragraph("Ref: " + safe(data.getEndOfService().getId(), "")))
                .setFont(regularFont).setFontSize(9)
                .setTextAlignment(TextAlignment.CENTER)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f))
                .setPadding(4));
        doc.add(band);
    }

    private void addCompanyTitle(Document doc, CompanyInfo company) {
        String school = company != null && company.getCompanyName() != null ? company.getCompanyName()
                : "Your School Name";
        Table t = new Table(UnitValue.createPercentArray(new float[] { 1 })).useAllAvailableWidth();
        t.addCell(new Cell()
                .add(new Paragraph(school).setFont(boldFont).setFontSize(11).setTextAlignment(TextAlignment.CENTER))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f))
                .setPadding(5));
        doc.add(t);
    }

    // ------------------------------------------------------------
    // Two-column info area (matches screenshot structure)
    // ------------------------------------------------------------
    private void addTwoColumnInfoTable(Document doc, FinalSettlementPdfData data) {
        EndOfService eos = data.getEndOfService();
        Employee emp = eos.getEmployee();
        JobDetails jd = data.getJobDetails();

        Table t = new Table(UnitValue.createPercentArray(new float[] { 1.2f, .9f, .9f, 1.2f, .9f, .9f }))
                .useAllAvailableWidth();
        t.setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f));

        // Row 1
        t.addCell(headCell("Date of EOS Generation"));
        t.addCell(valueCell(
                date(eos.getCalculatedAt() != null ? eos.getCalculatedAt().toLocalDate() : null, LocalDate.now())));
        t.addCell(headCell("Nature of Contract"));
        t.addCell(valueCell(safe(jd.getContractType()).toString()));
        t.addCell(new Cell(1, 2).setBorder(Border.NO_BORDER)); // filler

        // Row 2
        t.addCell(headCell("Name of Employee"));
        t.addCell(valueCell(emp != null ? (safe(emp.getFirstName()) + " " + safe(emp.getLastName())) : ""));
        t.addCell(headCell("Reason for exit"));
        // Cell reason = valueCell(safe(eos.getTerminationReason(),
        // "Resignation").toString());
        // reason.setBackgroundColor(GREEN_TAG);
        // t.addCell(reason);
        t.addCell(headCell("Basic Salary (p.m.)"));
        t.addCell(amountCell(eos.getLastBasicSalary()));

        // Row 3
        t.addCell(headCell("Joining Date"));
        t.addCell(valueCell(date(eos.getJoiningDate(), null)));
        t.addCell(headCell("Resignation Date"));
        // Assuming last working day is the effective resignation date for this report
        t.addCell(valueCell(date(eos.getLastWorkingDay(), null)));
        t.addCell(headCell("Gross Salary (p.m.)"));
        // Gross salary is not stored on EOS. Placeholder for now.
        // To implement this, you'd need to add lastGrossSalary to the EndOfService
        // entity.
        t.addCell(amountCell(null));

        // Row 4
        t.addCell(headCell("Last Working Day"));
        t.addCell(valueCell(date(eos.getLastWorkingDay(), null)));
        // trailing cells empty to match picture grid
        t.addCell(blankCell(4));

        doc.add(t);
    }

    // ------------------------------------------------------------
    // The big description / amounts table with (A) (B) and NET highlighted
    // ------------------------------------------------------------
    private void addDescriptionBox(Document doc, FinalSettlementPdfData data) {
        EndOfService eos = data.getEndOfService();

        // Earnings pieces
        BigDecimal gratuity = nvl(eos.getGratuityAmount());
        BigDecimal leaveSalary = BigDecimal.ZERO; // Placeholder for leave salary

        BigDecimal totalA = gratuity.add(leaveSalary);

        // Recoveries/deductions (B)
        BigDecimal noticeRecovery = BigDecimal.ZERO; // Placeholder for notice period recovery
        BigDecimal loanOutstanding = outstandingLoan(eos);
        BigDecimal totalB = noticeRecovery.add(loanOutstanding);

        BigDecimal net = totalA.subtract(totalB);

        // Top header row ("Description", "Amount")
        Table outer = new Table(UnitValue.createPercentArray(new float[] { 1 })).useAllAvailableWidth();
        outer.setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f));
        outer.addCell(new Cell().add(new Paragraph("Description").setFont(boldFont).setFontSize(10))
                .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 0.8f))
                .setPadding(6));

        // Inner grid
        Table grid = new Table(UnitValue.createPercentArray(new float[] { 3, 1 }))
                .useAllAvailableWidth();

        // Section: End of Service Entitlements / Calculation
        grid.addCell(subHead("End of Service Entitlements / Calculation"));
        grid.addCell(blankCell());

        grid.addCell(rowText("Gratuity"));
        grid.addCell(amountCell(gratuity));

        grid.addCell(rowText("Leave salary"));
        grid.addCell(amountCell(leaveSalary));

        // TOTAL GROSS END OF SERVICE DUES (A) highlighted
        grid.addCell(totalLabel("(A)  TOTAL GROSS END OF SERVICE DUES", STRONG_YELLOW));
        grid.addCell(totalAmount(totalA, STRONG_YELLOW));

        // Section: Recoveries
        grid.addCell(subHead("1. On account of not serving notice period 30 days"));
        grid.addCell(amountCell(noticeRecovery));

        if (loanOutstanding.compareTo(BigDecimal.ZERO) > 0) {
            grid.addCell(rowText("Loan outstanding"));
            grid.addCell(amountCell(loanOutstanding));
        }

        grid.addCell(totalLabel("(B)  TOTAL RECOVERIES TO BE ADJUSTED AGAINST GROSS END OF SERVICE DUES", PALE_YELLOW));
        grid.addCell(totalAmount(totalB, PALE_YELLOW));

        // NET AMOUNT (A-B) highlighted
        grid.addCell(totalLabel("NET AMOUNT PAYABLE ON ACCOUNT OF END OF SERVICE (A-B)", STRONG_YELLOW));
        grid.addCell(totalAmount(net, STRONG_YELLOW));

        outer.addCell(new Cell().add(grid).setBorder(Border.NO_BORDER));
        doc.add(outer);
    }

    // ------------------------------------------------------------
    // Acknowledgement paragraph
    // ------------------------------------------------------------
    private void addAcknowledgement(Document doc, FinalSettlementPdfData data) {
        EndOfService eos = data.getEndOfService();
        CompanyInfo company = data.getCompanyInfo();
        Employee emp = eos.getEmployee();

        String companyName = company != null && company.getCompanyName() != null ? company.getCompanyName()
                : "the School";
        String employeeName = emp != null ? (safe(emp.getFirstName()) + " " + safe(emp.getLastName())) : "I";
        String start = date(eos.getJoiningDate(), null);
        String end = date(eos.getLastWorkingDay(), null);

        Table t = new Table(UnitValue.createPercentArray(new float[] { 1 }))
                .useAllAvailableWidth();
        t.addCell(new Cell()
                .add(new Paragraph("Certificate of Acknowledgement of End of Service Dues")
                        .setFont(boldFont).setFontSize(10))
                .setBackgroundColor(GREY_BAND)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f))
                .setPadding(6));

        String body = String.format(
                "%s, the undersigned, unconditionally certify that the amount mentioned in the above End of Service statement "
                        + "given to me and signed by me, constitutes the full and final settlement amount due to me towards all my legal and "
                        + "financial entitlements for the period I have been in service with %s in the Emirates of Dubai starting from %s "
                        + "till the date of %s. This entitlement includes but is not limited to, the monthly salaries, the end of service "
                        + "benefits, unpaid leave salary, other allowances and all other benefits accrued as per the Terms of my Employment "
                        + "contract. I further certify that I have thoroughly checked the calculations of the above amount and am satisfied "
                        + "and do not have any disagreement on the same.",
                employeeName, companyName, start, end);

        t.addCell(new Cell()
                .add(new Paragraph(body).setFont(regularFont).setFontSize(9).setFixedLeading(12))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f))
                .setPadding(8));
        doc.add(t);
    }

    // ------------------------------------------------------------
    // Signatures grid
    // ------------------------------------------------------------
    private void addSignatureGrid(Document doc, FinalSettlementPdfData data) {
        EndOfService eos = data.getEndOfService();
        Employee emp = eos.getEmployee();

        Table t = new Table(UnitValue.createPercentArray(new float[] { 1, 1 }))
                .useAllAvailableWidth();
        t.setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f));

        // Row 1
        t.addCell(sigCell("Employee Name: ",
                emp != null ? (safe(emp.getFirstName()) + " " + safe(emp.getLastName())) : ""));
        t.addCell(sigCell("Employee Signature: ", ""));

        // Row 2
        t.addCell(sigCell("Prepared By School HR:", ""));
        t.addCell(sigCell("Checked By School Accountant:", ""));

        // Row 3
        t.addCell(sigCell("Reviewed By: HO HR:", ""));
        t.addCell(sigCell("Date:", date(LocalDate.now(), LocalDate.now())));

        // Row 4
        t.addCell(sigCell("Approved By: Head of Human Resource:", ""));
        t.addCell(sigCell("Chief Financial Officer:", ""));

        doc.add(t);
    }

    private Cell sigCell(String left, String right) {
        Cell c = new Cell().setPadding(8)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f));
        Paragraph p = new Paragraph()
                .add(text(left, 9, true))
                .add(text(" " + nullToEmpty(right), 9, false));
        c.add(p);
        return c;
    }

    // ------------------------------------------------------------
    // Footer strip (simple contact line)
    // ------------------------------------------------------------
    private void addBottomStrip(Document doc, CompanyInfo company) {
        String footer = "";
        if (company != null) {
            footer = nullToEmpty(company.getPhone()) + "   |   "
                    + nullToEmpty(company.getWebsite()) + "   |   "
                    + nullToEmpty(company.getAddress());
        }
        Paragraph p = new Paragraph(footer)
                .setFont(regularFont).setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.DARK_GRAY);
        doc.add(p);
    }

    // ------------------------------------------------------------
    // Helpers (cells, text, amounts, formatting)
    // ------------------------------------------------------------
    private Cell headCell(String label) {
        return new Cell()
                .add(new Paragraph(label).setFont(regularFont).setFontSize(9))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f))
                .setPadding(6);
    }

    private Cell valueCell(String value) {
        return new Cell()
                .add(new Paragraph(nullToEmpty(value)).setFont(regularFont).setFontSize(9))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f))
                .setPadding(6);
    }

    private Cell amountCell(BigDecimal amount) {
        String v = amount == null ? "" : String.format("%,.2f", amount);
        return new Cell()
                .add(new Paragraph(v).setFont(regularFont).setFontSize(9).setTextAlignment(TextAlignment.RIGHT))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f))
                .setPadding(6);
    }

    private Cell amountCell(BigDecimal amount, TextAlignment align) {
        String v = amount == null ? "" : String.format("%,.2f", amount);
        return new Cell()
                .add(new Paragraph(v).setFont(regularFont).setFontSize(9).setTextAlignment(align))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f))
                .setPadding(6);
    }

    private Cell blankCell() {
        return new Cell().setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f)).setPadding(6);
    }

    private Cell blankCell(int colspan) {
        return new Cell(1, colspan).setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f)).setPadding(6);
    }

    private Cell rowText(String s) {
        return new Cell().add(new Paragraph(s).setFont(regularFont).setFontSize(9))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f))
                .setPadding(6);
    }

    private Cell subHead(String s) {
        return new Cell().add(new Paragraph(s).setFont(boldFont).setFontSize(9))
                .setBackgroundColor(new DeviceGray(0.95f))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f))
                .setPadding(6);
    }

    private Cell totalLabel(String s, Color bg) {
        return new Cell().add(new Paragraph(s).setFont(boldFont).setFontSize(10))
                .setBackgroundColor(bg)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f))
                .setPadding(6);
    }

    private Cell totalAmount(BigDecimal v, Color bg) {
        return new Cell().add(new Paragraph(v == null ? "" : String.format("%,.2f", v))
                .setFont(boldFont).setFontSize(10).setTextAlignment(TextAlignment.RIGHT))
                .setBackgroundColor(bg)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f))
                .setPadding(6);
    }

    private Paragraph text(String s, int size, boolean bold) {
        return new Paragraph(nullToEmpty(s))
                .setFont(bold ? boldFont : regularFont)
                .setFontSize(size);
    }

    private String date(LocalDate d, LocalDate fallback) {
        LocalDate v = d != null ? d : fallback;
        return v == null ? "" : v.format(DMY);
    }

    private String safe(Object s) {
        return s == null ? "" : String.valueOf(s);
    }

    private String safe(Object s, String def) {
        return s == null ? def : String.valueOf(s);
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private BigDecimal nvl(BigDecimal b) {
        return b == null ? BigDecimal.ZERO : b;
    }

    private BigDecimal outstandingLoan(EndOfService eos) {
        if (eos == null || eos.getEmployee() == null || eos.getEmployee().getId() == null)
            return BigDecimal.ZERO;
        Optional<EmployeeLoan> loanOpt = employeeLoanRepository.findByEmployeeIdAndStatus(eos.getEmployee().getId(),
                LoanStatus.APPROVED);
        if (loanOpt.isEmpty())
            return BigDecimal.ZERO;
        EmployeeLoan loan = loanOpt.get();
        if (loan.getEmiAmount() == null || loan.getRemainingInstallments() == null)
            return BigDecimal.ZERO;
        return loan.getEmiAmount().multiply(BigDecimal.valueOf(loan.getRemainingInstallments()));
    }
}
