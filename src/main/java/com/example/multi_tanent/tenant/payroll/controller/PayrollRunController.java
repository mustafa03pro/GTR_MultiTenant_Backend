package com.example.multi_tanent.tenant.payroll.controller;

import com.example.multi_tanent.tenant.payroll.dto.PayrollRunRequest;
import com.example.multi_tanent.tenant.payroll.dto.PayrollRunResponse;
import com.example.multi_tanent.tenant.payroll.dto.PayslipResponse;
import com.example.multi_tanent.tenant.payroll.service.PayrollRunService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payroll-runs")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','HRMS_ADMIN','HR','MANAGER')")
public class PayrollRunController {

    private final PayrollRunService payrollRunService;
    private final com.example.multi_tanent.tenant.payroll.service.WpsService wpsService;
    private final com.example.multi_tanent.tenant.payroll.service.PayrollService payrollService;

    public PayrollRunController(PayrollRunService payrollRunService,
            com.example.multi_tanent.tenant.payroll.service.WpsService wpsService,
            com.example.multi_tanent.tenant.payroll.service.PayrollService payrollService) {
        this.payrollRunService = payrollRunService;
        this.wpsService = wpsService;
        this.payrollService = payrollService;
    }

    @PostMapping
    public ResponseEntity<PayrollRunResponse> createPayrollRun(@RequestBody PayrollRunRequest request) {
        var createdRun = payrollRunService.createPayrollRun(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(createdRun.getId()).toUri();
        return ResponseEntity.created(location).body(PayrollRunResponse.fromEntity(createdRun));
    }

    @PostMapping("/process-with-inputs")
    public ResponseEntity<PayrollRunResponse> processWithInputs(
            @RequestBody List<com.example.multi_tanent.tenant.payroll.dto.PayrollInputDto> inputs,
            @RequestParam int year,
            @RequestParam int month) {
        var processedRun = payrollService.processPayroll(inputs, year, month);
        return ResponseEntity.ok(PayrollRunResponse.fromEntity(processedRun));
    }

    @PostMapping("/{id}/execute/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HRMS_ADMIN','HR')")
    public ResponseEntity<PayslipResponse> executeForSingleEmployee(
            @PathVariable Long id,
            @PathVariable Long employeeId) {
        var payslip = payrollRunService.executePayrollForSingleEmployee(id, employeeId);
        return ResponseEntity.ok(PayslipResponse.fromEntity(payslip));
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<PayrollRunResponse> executePayrollRun(@PathVariable Long id) {
        var executedRun = payrollRunService.executePayrollRun(id);
        return ResponseEntity.ok(PayrollRunResponse.fromEntity(executedRun));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HRMS_ADMIN','HR')")
    public ResponseEntity<PayrollRunResponse> updatePayrollRunStatus(
            @PathVariable Long id,
            @RequestParam com.example.multi_tanent.tenant.payroll.enums.PayrollStatus status) {
        var updatedRun = payrollRunService.updatePayrollRunStatus(id, status);
        return ResponseEntity.ok(PayrollRunResponse.fromEntity(updatedRun));
    }

    @GetMapping
    public ResponseEntity<List<PayrollRunResponse>> getAllPayrollRuns() {
        List<PayrollRunResponse> runs = payrollRunService.getAllPayrollRuns().stream()
                .map(PayrollRunResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(runs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PayrollRunResponse> getPayrollRunById(@PathVariable Long id) {
        return payrollRunService.getPayrollRunById(id)
                .map(run -> ResponseEntity.ok(PayrollRunResponse.fromEntity(run)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/payslips")
    public ResponseEntity<List<PayslipResponse>> getPayslipsForRun(@PathVariable Long id) {
        List<PayslipResponse> payslips = payrollRunService.getPayslipsForRun(id).stream()
                .map(PayslipResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(payslips);
    }

    @GetMapping("/{id}/wps-sif")
    public ResponseEntity<String> downloadWpsSif(@PathVariable Long id) {
        java.util.Map<String, Object> result = wpsService.generateSifFile(id);
        String fileName = (String) result.get("fileName");
        String content = (String) result.get("content");

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"")
                .contentType(org.springframework.http.MediaType.TEXT_PLAIN)
                .body(content);
    }
}
