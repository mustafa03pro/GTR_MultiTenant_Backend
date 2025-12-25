package com.example.multi_tanent.tenant.reports.controller;

import com.example.multi_tanent.tenant.reports.dto.EosbReportDto;
import com.example.multi_tanent.tenant.reports.dto.FinalSettlementReportDto;
import com.example.multi_tanent.tenant.reports.dto.TerminationReasonReportDto;
import com.example.multi_tanent.tenant.reports.service.EndOfServiceReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports/eos")
@RequiredArgsConstructor
public class EndOfServiceReportController {

    private final EndOfServiceReportService endOfServiceReportService;

    @GetMapping("/eosb")
    public ResponseEntity<List<EosbReportDto>> getEosbReports() {
        return ResponseEntity.ok(endOfServiceReportService.getEosbReports());
    }

    @GetMapping("/final-settlement")
    public ResponseEntity<List<FinalSettlementReportDto>> getFinalSettlementReports() {
        return ResponseEntity.ok(endOfServiceReportService.getFinalSettlementReports());
    }

    @GetMapping("/termination-reason")
    public ResponseEntity<List<TerminationReasonReportDto>> getTerminationReasonReports() {
        return ResponseEntity.ok(endOfServiceReportService.getTerminationReasonReports());
    }

    @GetMapping("/eosb/export")
    public ResponseEntity<org.springframework.core.io.InputStreamResource> exportEosbReport() {
        return exportExcel(endOfServiceReportService.generateEosbReportExcel(), "EOSB-Report.xlsx");
    }

    @GetMapping("/final-settlement/export")
    public ResponseEntity<org.springframework.core.io.InputStreamResource> exportFinalSettlementReport() {
        return exportExcel(endOfServiceReportService.generateFinalSettlementReportExcel(),
                "Final-Settlement-Report.xlsx");
    }

    @GetMapping("/termination-reason/export")
    public ResponseEntity<org.springframework.core.io.InputStreamResource> exportTerminationReasonReport() {
        return exportExcel(endOfServiceReportService.generateTerminationReasonReportExcel(),
                "Termination-Reason-Report.xlsx");
    }

    private ResponseEntity<org.springframework.core.io.InputStreamResource> exportExcel(java.io.ByteArrayInputStream in,
            String filename) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=" + filename);

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(org.springframework.http.MediaType
                        .parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new org.springframework.core.io.InputStreamResource(in));
    }
}
