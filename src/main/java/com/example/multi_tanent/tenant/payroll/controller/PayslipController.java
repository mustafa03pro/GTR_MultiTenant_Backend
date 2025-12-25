package com.example.multi_tanent.tenant.payroll.controller;

import com.example.multi_tanent.tenant.payroll.dto.PayslipResponse;
import com.example.multi_tanent.tenant.payroll.service.PdfGenerationService;
import com.example.multi_tanent.tenant.payroll.service.PayslipService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payslips")
@CrossOrigin(origins = "*")
public class PayslipController {

    private final PayslipService payslipService;
    private final PdfGenerationService pdfGenerationService;

    public PayslipController(PayslipService payslipService, PdfGenerationService pdfGenerationService) {
        this.payslipService = payslipService;
        this.pdfGenerationService = pdfGenerationService;
    }

    @GetMapping("/employee/{employeeCode}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PayslipResponse>> getPayslipsForEmployee(@PathVariable String employeeCode) {
        // TODO: Add security check to ensure user can only see their own payslips, or
        // if they are HR/Admin.
        List<PayslipResponse> payslips = payslipService.getPayslipsForEmployee(employeeCode).stream()
                .map(PayslipResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(payslips);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PayslipResponse> getPayslipById(@PathVariable Long id) {
        // TODO: Add security check to ensure user can only see their own payslip.
        return payslipService.getPayslipById(id)
                .map(payslip -> ResponseEntity.ok(PayslipResponse.fromEntity(payslip)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HRMS_ADMIN','HR')")
    public ResponseEntity<PayslipResponse> updatePayslipStatus(
            @PathVariable Long id,
            @RequestParam com.example.multi_tanent.tenant.payroll.enums.PayrollStatus status) {
        var updatedPayslip = payslipService.updatePayslipStatus(id, status);
        return ResponseEntity.ok(PayslipResponse.fromEntity(updatedPayslip));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadPayslipPdf(@PathVariable Long id) {
        // TODO: Add security check to ensure user can only download their own payslip.
        return payslipService.getPayslipDataForPdf(id)
                .map(pdfData -> {
                    byte[] pdfBytes = pdfGenerationService.generatePayslipPdf(pdfData);
                    String filename = String.format("Payslip-%s-%d-%s.pdf",
                            pdfData.getPayslip().getPayDate().getMonth()
                                    .getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH),
                            pdfData.getPayslip().getYear(),
                            pdfData.getPayslip().getEmployee().getEmployeeCode());

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_PDF);
                    // Use "attachment" to force download, or "inline" to suggest preview in
                    // browser.
                    headers.setContentDispositionFormData("attachment", filename);
                    headers.setContentLength(pdfBytes.length);

                    return ResponseEntity.ok().headers(headers).body(pdfBytes);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
