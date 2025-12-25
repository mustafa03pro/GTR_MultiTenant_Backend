package com.example.multi_tanent.tenant.payroll.controller;

import com.example.multi_tanent.tenant.payroll.service.WpsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payroll/wps")
@RequiredArgsConstructor
public class WpsController {

    private final WpsService wpsService;

    @GetMapping("/{payrollRunId}/sif")
    public ResponseEntity<String> downloadSifFile(@PathVariable Long payrollRunId) {
        Map<String, Object> result = wpsService.generateSifFile(payrollRunId);
        String fileName = (String) result.get("fileName");
        String content = (String) result.get("content");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(content);
    }

    @GetMapping("/compliance")
    public ResponseEntity<com.example.multi_tanent.tenant.payroll.dto.WpsComplianceReportDto> getComplianceReport(
            @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(wpsService.generateComplianceReport(year, month));
    }

    @GetMapping("/compliance/export")
    public ResponseEntity<org.springframework.core.io.InputStreamResource> exportComplianceReport(
            @RequestParam int year, @RequestParam int month) {
        java.io.ByteArrayInputStream in = wpsService.generateComplianceReportExcel(year, month);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=WPS-Compliance-" + month + "-" + year + ".xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new org.springframework.core.io.InputStreamResource(in));
    }
}
