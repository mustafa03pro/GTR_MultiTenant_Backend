package com.example.multi_tanent.tenant.reports.controller;

import com.example.multi_tanent.tenant.reports.service.PayrollReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;

@RestController
@RequestMapping("/api/reports/payroll")
@RequiredArgsConstructor
public class PayrollReportController {

    private final PayrollReportService payrollReportService;

    @GetMapping("/register/export")
    public ResponseEntity<InputStreamResource> exportPayrollRegister(
            @RequestParam int year,
            @RequestParam int month) {

        ByteArrayInputStream in = payrollReportService.generatePayrollRegisterExcel(year, month);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=PayrollRegister_" + month + "_" + year + ".xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(in));
    }
}
