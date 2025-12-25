package com.example.multi_tanent.tenant.reports.controller;

import com.example.multi_tanent.tenant.reports.dto.*;
import com.example.multi_tanent.tenant.reports.service.HRComplianceReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.util.List;

@RestController
@RequestMapping("/api/reports/compliance")
@RequiredArgsConstructor
public class HRComplianceReportController {

    private final HRComplianceReportService hrComplianceReportService;

    // --- JSON Endpoints ---

    @GetMapping("/labor-card")
    public ResponseEntity<List<LaborCardReportDto>> getLaborCardReports() {
        return ResponseEntity.ok(hrComplianceReportService.getLaborCardReports());
    }

    @GetMapping("/company-quota")
    public ResponseEntity<CompanyQuotaReportDto> getCompanyQuotaReport() {
        return ResponseEntity.ok(hrComplianceReportService.getCompanyQuotaReport());
    }

    @GetMapping("/nationality")
    public ResponseEntity<List<NationalityReportDto>> getNationalityReports() {
        return ResponseEntity.ok(hrComplianceReportService.getNationalityReports());
    }

    @GetMapping("/emiratization")
    public ResponseEntity<EmiratizationReportDto> getEmiratizationReport() {
        return ResponseEntity.ok(hrComplianceReportService.getEmiratizationReport());
    }

    @GetMapping("/absconding")
    public ResponseEntity<List<AbscondingReportDto>> getAbscondingReports() {
        return ResponseEntity.ok(hrComplianceReportService.getAbscondingReports());
    }

    // --- Export Endpoints ---

    @GetMapping("/labor-card/export")
    public ResponseEntity<InputStreamResource> exportLaborCardReport() {
        return exportExcel(hrComplianceReportService.generateLaborCardExcel(), "Labor-Card-Report.xlsx");
    }

    @GetMapping("/company-quota/export")
    public ResponseEntity<InputStreamResource> exportCompanyQuotaReport() {
        return exportExcel(hrComplianceReportService.generateCompanyQuotaExcel(), "Company-Quota-Report.xlsx");
    }

    @GetMapping("/nationality/export")
    public ResponseEntity<InputStreamResource> exportNationalityReport() {
        return exportExcel(hrComplianceReportService.generateNationalityExcel(), "Nationality-Report.xlsx");
    }

    @GetMapping("/emiratization/export")
    public ResponseEntity<InputStreamResource> exportEmiratizationReport() {
        return exportExcel(hrComplianceReportService.generateEmiratizationExcel(), "Emiratization-Report.xlsx");
    }

    @GetMapping("/absconding/export")
    public ResponseEntity<InputStreamResource> exportAbscondingReport() {
        return exportExcel(hrComplianceReportService.generateAbscondingExcel(), "Absconding-Report.xlsx");
    }

    private ResponseEntity<InputStreamResource> exportExcel(ByteArrayInputStream in, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=" + filename);

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }
}
