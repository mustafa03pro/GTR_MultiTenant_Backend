package com.example.multi_tanent.tenant.reports.controller;

import com.example.multi_tanent.tenant.reports.dto.LeaveAccrualReportDto;
import com.example.multi_tanent.tenant.reports.service.LeaveReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports/leave")
@RequiredArgsConstructor
public class LeaveReportController {

    private final LeaveReportService leaveReportService;

    @GetMapping("/accrual")
    public ResponseEntity<List<LeaveAccrualReportDto>> getAccrualReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {

        List<LeaveAccrualReportDto> report = leaveReportService.generateAccrualReport(asOfDate);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/accrual/export")
    public ResponseEntity<InputStreamResource> exportAccrualReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {

        ByteArrayInputStream in = leaveReportService.generateAccrualReportExcel(asOfDate);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=LeaveAccrualReport.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(in));
    }
}
