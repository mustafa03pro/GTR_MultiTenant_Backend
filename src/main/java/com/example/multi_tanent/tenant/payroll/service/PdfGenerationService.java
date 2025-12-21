package com.example.multi_tanent.tenant.payroll.service;

import com.example.multi_tanent.tenant.payroll.entity.EmployeeBankAccount;
import com.example.multi_tanent.tenant.payroll.dto.PayslipPdfData;
import com.example.multi_tanent.spersusers.enitity.CompanyInfo;
import com.example.multi_tanent.tenant.payroll.dto.FinalSettlementPdfData;
import com.example.multi_tanent.tenant.payroll.entity.PayslipTemplate;
import com.example.multi_tanent.tenant.payroll.repository.PayslipTemplateRepository;
import com.example.multi_tanent.tenant.service.FileStorageService;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.example.multi_tanent.tenant.payroll.entity.Payslip;
import com.example.multi_tanent.tenant.payroll.entity.PayslipComponent;
import com.example.multi_tanent.tenant.payroll.enums.SalaryComponentType;
import com.itextpdf.html2pdf.ConverterProperties;
import com.example.multi_tanent.tenant.payroll.repository.EmployeeBankAccountRepository;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.html2pdf.resolver.font.DefaultFontProvider;
import com.itextpdf.layout.font.FontProvider;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Image;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PdfGenerationService {
  private final PdfEosSettlement pdfEosSettlement;
  private final FileStorageService fileStorageService;
  private final PayslipTemplateRepository payslipTemplateRepository;
  private final EmployeeBankAccountRepository employeeBankAccountRepository;
  private PdfFont regularFont;
  private PdfFont boldFont;

  public PdfGenerationService(PayslipTemplateRepository payslipTemplateRepository,
      FileStorageService fileStorageService,
      EmployeeBankAccountRepository employeeBankAccountRepository,
      PdfEosSettlement pdfEosSettlement) { // Still needed for Final Settlement
    // In a real application, you would load a font that supports Arabic, like Arial
    // or Noto Sans Arabic.
    // For this example, we will use a standard font and assume it can render the
    // text.
    try {
      // Load a font that supports Arabic. Place the .ttf file in
      // src/main/resources/fonts
      ClassPathResource fontRes = new ClassPathResource("fonts/NotoNaskhArabic-Regular.ttf");
      byte[] fontBytes = fontRes.getInputStream().readAllBytes();
      this.regularFont = PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H,
          PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
      this.boldFont = PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H,
          PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED); // Using same font for bold for simplicity
    } catch (IOException e) {
      // Fallback to default font if the custom font is not found
      System.err
          .println("Custom Arabic font not found, falling back to default. Arabic text will not render correctly.");
      // throw new RuntimeException("Failed to load fonts for PDF generation", e);
    }
    this.payslipTemplateRepository = payslipTemplateRepository;
    this.fileStorageService = fileStorageService;
    this.employeeBankAccountRepository = employeeBankAccountRepository;
    this.pdfEosSettlement = pdfEosSettlement;
  }

  public byte[] generatePayslipPdf(PayslipPdfData pdfData) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    CompanyInfo companyInfo = pdfData.getCompanyInfo();
    if (companyInfo == null || companyInfo.getTenant() == null) {
      throw new IllegalStateException("Company Info or Tenant not found for this payslip.");
    }

    Optional<PayslipTemplate> templateOpt = payslipTemplateRepository
        .findByTenantIdAndIsDefaultTrue(companyInfo.getTenant().getId());

    String htmlContent;
    if (templateOpt.isPresent()) {
      // 2. Populate the custom HTML template with data
      htmlContent = populateHtmlTemplate(templateOpt.get().getTemplateContent(), pdfData);
    } else {
      // 3. Fallback to a default, hardcoded HTML template if none is found
      htmlContent = populateHtmlTemplate(getFallbackTemplate(), pdfData);
    }

    try (PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4)) {

      // 4. Configure iText for HTML to PDF conversion
      ConverterProperties properties = new ConverterProperties();
      FontProvider fontProvider = new DefaultFontProvider(false, false, false);
      if (regularFont != null) {
        fontProvider.addFont(regularFont.getFontProgram());
      }
      if (boldFont != null) {
        fontProvider.addFont(boldFont.getFontProgram());
      }
      properties.setFontProvider(fontProvider);

      // 5. Convert the final HTML to PDF
      HtmlConverter.convertToPdf(htmlContent, pdf, properties);

    } catch (Exception e) {
      throw new RuntimeException("Error generating PDF", e);
    }
    return baos.toByteArray();
  }

  private String populateHtmlTemplate(String template, PayslipPdfData data) {
    Payslip payslip = data.getPayslip();
    if (payslip == null || payslip.getPayDate() == null) {
      throw new IllegalArgumentException("Cannot generate PDF. The payslip data or its pay date is missing.");
    }

    String monthName = payslip.getPayDate().getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH);

    // --- Handle Company Logo ---
    String logoImgTag = ""; // Default to empty string if no logo
    CompanyInfo companyInfo = data.getCompanyInfo(); // Keep this for local use
    if (companyInfo != null && companyInfo.getLogoUrl() != null && !companyInfo.getLogoUrl().isEmpty()) {
      try {
        Resource logoResource = fileStorageService.loadFileAsResource(companyInfo.getLogoUrl());
        byte[] logoBytes = logoResource.getInputStream().readAllBytes();
        String base64Logo = Base64.getEncoder().encodeToString(logoBytes);
        String mimeType = logoResource.getURL().openConnection().getContentType();
        logoImgTag = String.format("<img src='data:%s;base64,%s' style='max-height: 60px; max-width: 180px;' />",
            mimeType, base64Logo);
      } catch (IOException e) {
        System.err.println("Could not load or encode company logo: " + e.getMessage());
      }
    }
    template = template.replace("{{company.logo}}", logoImgTag);

    // Basic Info
    String companyAddress = companyInfo != null ? (companyInfo.getAddress() != null ? companyInfo.getAddress() : "")
        : "N/A";
    String companyContact = companyInfo != null ? (companyInfo.getPhone() != null ? companyInfo.getPhone() : "")
        : "N/A";
    template = template.replace("{{company.address}}", companyAddress);
    template = template.replace("{{company.contact}}", companyContact);
    template = template.replace("{{company.name}}", companyInfo != null ? companyInfo.getCompanyName() : "N/A");
    template = template.replace("{{payslip.monthYear}}", monthName + " " + payslip.getYear());
    template = template.replace("{{employee.name}}", data.getEmployeeFullName());
    template = template.replace("{{employee.code}}",
        payslip.getEmployee() != null ? payslip.getEmployee().getEmployeeCode() : "N/A");
    template = template.replace("{{payslip.payDate}}",
        payslip.getPayDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

    // --- Add more Job Details ---
    String designation = "N/A";
    String joiningDate = "N/A";
    String department = "N/A";
    String contractType = "N/A";
    if (data.getJobDetails() != null) {
      designation = data.getJobDetails().getDesignation() != null ? data.getJobDetails().getDesignation() : "N/A";
      joiningDate = data.getJobDetails().getDateOfJoining() != null
          ? data.getJobDetails().getDateOfJoining().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
          : "N/A";
      department = data.getJobDetails().getDepartment() != null ? data.getJobDetails().getDepartment() : "N/A";
      contractType = data.getJobDetails().getContractType() != null
          ? String.valueOf(data.getJobDetails().getContractType())
          : "N/A";
    }
    template = template.replace("{{employee.designation}}", designation);
    template = template.replace("{{employee.joiningDate}}", joiningDate);
    template = template.replace("{{employee.department}}", department);
    template = template.replace("{{employee.contractType}}", contractType);

    // Attendance
    template = template.replace("{{payslip.totalDays}}",
        payslip.getTotalDaysInMonth() != null ? String.valueOf(payslip.getTotalDaysInMonth()) : "0");
    template = template.replace("{{payslip.payableDays}}",
        payslip.getPayableDays() != null ? payslip.getPayableDays().toPlainString() : "0.0");
    template = template.replace("{{payslip.lopDays}}",
        payslip.getLossOfPayDays() != null ? payslip.getLossOfPayDays().toPlainString() : "0.0");

    // Earnings & Deductions
    template = template.replace("{{payslip.earnings}}", buildComponentTable(payslip, SalaryComponentType.EARNING));
    template = template.replace("{{payslip.deductions}}", buildComponentTable(payslip, SalaryComponentType.DEDUCTION));
    template = template.replace("{{payslip.grossEarnings}}", formatCurrency(payslip.getGrossEarnings()));
    template = template.replace("{{payslip.totalDeductions}}", formatCurrency(payslip.getTotalDeductions()));

    // Net Pay
    template = template.replace("{{payslip.netSalary}}", formatCurrency(payslip.getNetSalary()));
    // A proper amount-to-words conversion is complex. This is a placeholder.
    template = template.replace("{{payslip.netSalaryInWords}}", "Amount in words placeholder");

    // --- Bank Details ---
    Optional<EmployeeBankAccount> bankAccountOpt = Optional.empty();
    if (payslip.getEmployee() != null) {
      bankAccountOpt = employeeBankAccountRepository.findByEmployeeId(payslip.getEmployee().getId());
    } else {
      System.err.println("Warning: Employee object is null for payslip ID: " + payslip.getId());
    }
    template = template.replace("{{employee.bankName}}",
        bankAccountOpt.map(EmployeeBankAccount::getBankName).orElse("N/A"));
    template = template.replace("{{employee.bankAccount}}",
        bankAccountOpt.map(EmployeeBankAccount::getAccountNumber).orElse("N/A"));
    template = template.replace("{{employee.ifscCode}}",
        bankAccountOpt.map(EmployeeBankAccount::getIfscCode).orElse("N/A"));

    // --- Footer Details ---
    template = template.replace("{{payslip.id}}", String.valueOf(payslip.getId()));
    template = template.replace("{{payslip.generatedOn}}",
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));

    return template;
  }

  private String buildComponentTable(Payslip payslip, SalaryComponentType type) {
    StringBuilder sb = new StringBuilder();
    List<PayslipComponent> components = payslip.getComponents().stream()
        .filter(c -> c.getSalaryComponent().getType() == type)
        .collect(Collectors.toList());

    for (PayslipComponent pc : components) {
      sb.append("<tr>");
      sb.append("<td>").append(pc.getSalaryComponent().getName()).append("</td>");
      sb.append("<td class='amount'>").append(formatCurrency(pc.getAmount())).append("</td>");
      sb.append("</tr>");
    }
    return sb.toString();
  }

  private String getFallbackTemplate() {
    // This is a basic, table-based HTML template. Your frontend designer can create
    // much better ones.
    return """
                 <!doctype html>
                 <html lang="en">
                 <head>
                   <meta charset="utf-8" />
                   <title>Payslip - {{payslip.monthYear}}</title>
                   <style>
                     @page { size: A4; margin: 18mm; }
                     body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Naskh Arabic", sans-serif; color: #222; font-size: 13px; }
                     .payslip-card { max-width: 900px; margin: 0 auto; background: #fff; border: 1px solid #e6e9ef; padding: 20px 28px; border-radius: 8px; }
                     .layout-table { width: 100%; border-collapse: collapse; }
                     .layout-table td { border: none; padding: 0; vertical-align: top; }
                     .company-logo { max-height: 64px; max-width: 220px; object-fit: contain; }
                     .company-meta h2 { margin: 0; font-size: 18px; }
                     .company-meta p { margin: 0; font-size: 12px; color: #556; }
                     .title { text-align: right; font-weight: 700; color: #0b3b73; font-size: 18px; }
                     .section-box { background: #fafbfd; border: 1px solid #eef3fb; padding: 12px; border-radius: 6px; height: 100%; }
                     .section-box h4 { margin: 0 0 8px 0; font-size: 13px; color: #234; }
                     .kv-table { width: 100%; }
                     .kv-table td { padding: 2px 0; font-size: 13px; }
                     .kv-table td.key { color: #1b2b4a; font-weight: bold; }
                     .kv-table td.val { text-align: right; }
                     .data-table { width: 100%; border-collapse: collapse; margin-bottom: 12px; }
                     .data-table th, .data-table td { padding: 10px 8px; border-bottom: 1px solid #eef2f7; font-size: 13px; }
                     .data-table th { background: #f3f7fc; text-align: left; color: #173354; }
                     .amount { text-align: right; white-space: nowrap; }
                     .summary-box { padding: 12px 8px; border-radius: 6px; border: 1px solid #e6eef9; background: #fff; margin-top: 8px; }
                     .summary-box .label { font-size: 14px; color: #334; }
                     .summary-box .value { font-weight: 700; font-size: 16px; }
                     .footer-note { font-size: 12px; color: #556; }
                     .sign-box { text-align: center; }
                     .sign-line { height: 48px; border-bottom: 1px solid #dbe7fb; margin-bottom: 8px; }
                     .sign-title { font-size: 12px; color: #445; }
                     /* RTL support */
                    body.rtl{direction:rtl}
                     body.rtl .kv-table td.val, body.rtl .title { text-align: left; }
                     body.rtl .data-table th, body.rtl .data-table td { text-align: right; }
                   </style>
                 </head>
                 <body>
                   <div class="payslip-card">
                     <table class="layout-table" style="margin-bottom: 18px;">
                       <tr>
                         <td style="width: 70%;">
                           <table class="layout-table">
                             <tr>
                               <td style="width: 65px; padding-right: 16px;">{{company.logo}}</td>
                               <td>
                                 <div class="company-meta">
                                   <h2>{{company.name}}</h2>
                                   <p>{{company.address}} &nbsp;|&nbsp; {{company.contact}}</p>
                                 </div>
                               </td>
                             </tr>
                           </table>
                         </td>
                         <td class="title">PAYSLIP<br><small style="font-weight:600;color:#516;">{{payslip.monthYear}}</small></td>
                       </tr>
                     </table>
                     <table class="layout-table" style="margin-bottom: 18px; border-spacing: 20px 0; margin-left: -20px;">
                       <tr>
                         <td style="width: 50%; padding: 0 20px 0 20px;">
                           <div class="section-box">
                             <h4>Employee Details</h4>
                             <table class="kv-table">
                               <tr><td class="key">Name</td><td class="val">{{employee.name}}</td></tr>
                               <tr><td class="key">Employee Code</td><td class="val">{{employee.code}}</td></tr>
                               <tr><td class="key">Designation</td><td class="val">{{employee.designation}}</td></tr>
                               <tr><td class="key">Department</td><td class="val">{{employee.department}}</td></tr>
                             </table>
                           </div>
                         </td>
                         <td style="width: 50%; padding: 0 20px 0 20px;">
                           <div class="section-box">
                             <h4>Pay & Job Info</h4>
                             <table class="kv-table">
                               <tr><td class="key">Joining Date</td><td class="val">{{employee.joiningDate}}</td></tr>
                               <tr><td class="key">Contract Type</td><td class="val">{{employee.contractType}}</td></tr>
                               <tr><td class="key">Pay Date</td><td class="val">{{payslip.payDate}}</td></tr>
                               <tr><td class="key">Working Days</td><td class="val">{{payslip.totalDays}} (Payable: {{payslip.payableDays}} | LOP: {{payslip.lopDays}})</td></tr>
                             </table>
                           </div>
                         </td>
                       </tr>
                     </table>
                     <table class="layout-table" style="border-spacing: 16px 0; margin-left: -16px;">
                       <tr>
                         <td style="width: 50%; padding: 0 16px 0 16px;">
                           <table class="data-table" aria-label="Earnings">
                             <thead><tr><th>Earnings</th><th class="amount">Amount</th></tr></thead>
                             <tbody>{{payslip.earnings}}<tr style="font-weight:700"><td>Gross Earnings</td><td class="amount">{{payslip.grossEarnings}}</td></tr></tbody>
                           </table>
                         </td>
                         <td style="width: 50%; padding: 0 16px 0 16px;">
                           <table class="data-table" aria-label="Deductions">
                             <thead><tr><th>Deductions</th><th class="amount">Amount</th></tr></thead>
                             <tbody>{{payslip.deductions}}<tr style="font-weight:700"><td>Total Deductions</td><td class="amount">{{payslip.totalDeductions}}</td></tr></tbody>
                           </table>
                         </td>
                       </tr>
                     </table>
                     <div class="summary-box">
                       <table class="layout-table">
                         <tr>
                           <td><div class="label">Net Salary (in figures)</div><div class="value">{{payslip.netSalary}}</div></td>
                           <td style="text-align:right"><div class="label">Net Salary (in words)</div><div style="font-weight:600">{{payslip.netSalaryInWords}}</div></td>
                         </tr>
                       </table>
                     </div>
                     <table class="layout-table" style="margin-top:14px; font-size:12px;">
                       <tr>
                         <td><strong>Bank / Payment Details</strong><br>{{employee.bankName}} | A/C: {{employee.bankAccount}} | IFSC: {{employee.ifscCode}}</td>
                         <td style="text-align:right;"><strong>Payslip ID:</strong> {{payslip.id}}<br><strong>Generated on:</strong> {{payslip.generatedOn}}</td>
                       </tr>
                     </table>
                     <table class="layout-table" style="margin-top:22px;">
                       <tr>
                         <td class="footer-note">Note: This is a computer generated payslip and does not require a physical signature.</td>
                         <td style="width: 50%; padding-left: 20px;">
                           <table class="layout-table" style="border-spacing: 18px 0; margin-left: -18px;">
                             <tr>
                               <td class="sign-box" style="padding: 0 18px 0 18px;"><div class="sign-line"></div><div class="sign-title">Authorized Signatory</div></td>
                               <td class="sign-box" style="padding: 0 18px 0 18px;"><div class="sign-line"></div><div class="sign-title">Employee Signature</div></td>
                             </tr>
                           </table>
                         </td>
                       </tr>
                     </table>
                   </div>
                 </body>
                 </html>
        """;
  }

  public byte[] generateFinalSettlementPdf(FinalSettlementPdfData pdfData) {
    // Delegate the generation to the dedicated service
    return pdfEosSettlement.generate(pdfData);
  }

  private String formatCurrency(BigDecimal amount) {
    return amount != null ? String.format("%,.2f", amount) : "0.00";
  }
}