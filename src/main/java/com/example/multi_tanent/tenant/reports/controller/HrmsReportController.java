package com.example.multi_tanent.tenant.reports.controller;

import com.example.multi_tanent.tenant.reports.service.HrmsReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class HrmsReportController {

    private final HrmsReportService hrmsReportService;

    @GetMapping("/employee-master")
    public ResponseEntity<InputStreamResource> exportEmployeeMasterReport(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String status) {
        ByteArrayInputStream in = hrmsReportService.generateEmployeeMasterReport(status);

        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String filename = "Employee_Master_Report_" + date + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=" + filename);

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }

    @GetMapping("/headcount")
    public ResponseEntity<InputStreamResource> exportHeadcountReport() {
        ByteArrayInputStream in = hrmsReportService.generateHeadcountReport();
        String filename = "Headcount_Report_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + ".xlsx";
        return createExcelResponse(in, filename);
    }

    @GetMapping("/demographics")
    public ResponseEntity<InputStreamResource> exportDemographicReport() {
        ByteArrayInputStream in = hrmsReportService.generateDemographicReport();
        String filename = "Demographic_Report_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + ".xlsx";
        return createExcelResponse(in, filename);
    }

    @GetMapping("/employment-status")
    public ResponseEntity<InputStreamResource> exportEmploymentStatusReport() {
        ByteArrayInputStream in = hrmsReportService.generateEmploymentStatusReport();
        String filename = "Employment_Status_Report_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + ".xlsx";
        return createExcelResponse(in, filename);
    }

    private ResponseEntity<InputStreamResource> createExcelResponse(ByteArrayInputStream in, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=" + filename);
        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }
}
