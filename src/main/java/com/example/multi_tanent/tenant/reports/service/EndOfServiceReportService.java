package com.example.multi_tanent.tenant.reports.service;

import com.example.multi_tanent.tenant.payroll.entity.EndOfService;
import com.example.multi_tanent.tenant.payroll.enums.TerminationReason;
import com.example.multi_tanent.tenant.payroll.repository.EndOfServiceRepository;
import com.example.multi_tanent.tenant.reports.dto.EosbReportDto;
import com.example.multi_tanent.tenant.reports.dto.FinalSettlementReportDto;
import com.example.multi_tanent.tenant.reports.dto.TerminationReasonReportDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EndOfServiceReportService {

    private final EndOfServiceRepository endOfServiceRepository;

    @Transactional(readOnly = true)
    public List<EosbReportDto> getEosbReports() {
        return endOfServiceRepository.findAll().stream()
                .map(this::mapToEosbReportDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FinalSettlementReportDto> getFinalSettlementReports() {
        return endOfServiceRepository.findAll().stream()
                .map(this::mapToFinalSettlementReportDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TerminationReasonReportDto> getTerminationReasonReports() {
        Map<TerminationReason, List<EndOfService>> groupedByReason = endOfServiceRepository.findAll().stream()
                .filter(eos -> eos.getTerminationReason() != null)
                .collect(Collectors.groupingBy(EndOfService::getTerminationReason));

        return groupedByReason.entrySet().stream()
                .map(entry -> TerminationReasonReportDto.builder()
                        .reason(entry.getKey())
                        .employeeCount(entry.getValue().size())
                        .employees(entry.getValue().stream()
                                .map(eos -> TerminationReasonReportDto.EmployeeDetail.builder()
                                        .employeeCode(eos.getEmployee().getEmployeeCode())
                                        .employeeName(eos.getEmployee().getName()) // Assuming getName() exists
                                        .lastWorkingDay(eos.getLastWorkingDay())
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }

    private EosbReportDto mapToEosbReportDto(EndOfService eos) {
        return EosbReportDto.builder()
                .employeeId(eos.getEmployee().getId())
                .employeeCode(eos.getEmployee().getEmployeeCode())
                .employeeName(eos.getEmployee().getName()) // Assuming getName() exists
                .joiningDate(eos.getJoiningDate())
                .lastWorkingDay(eos.getLastWorkingDay())
                .totalYearsOfService(eos.getTotalYearsOfService())
                .lastBasicSalary(eos.getLastBasicSalary())
                .gratuityAmount(eos.getGratuityAmount())
                .calculationDetails(eos.getCalculationDetails())
                .build();
    }

    private FinalSettlementReportDto mapToFinalSettlementReportDto(EndOfService eos) {
        // Note: Leave Encashment and Deductions are currently placeholders
        // as they might require integration with Leave module and Payroll deductions.
        // For now, assuming 0 or extending EndOfService entity later to store them
        // explicitly at settlement time.
        BigDecimal leaveEncashment = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;

        return FinalSettlementReportDto.builder()
                .employeeId(eos.getEmployee().getId())
                .employeeCode(eos.getEmployee().getEmployeeCode())
                .employeeName(eos.getEmployee().getName()) // Assuming getName() exists
                .lastWorkingDay(eos.getLastWorkingDay())
                .gratuityAmount(eos.getGratuityAmount())
                .leaveEncashmentAmount(leaveEncashment)
                .totalDeductions(totalDeductions)
                .netPayable(eos.getGratuityAmount().add(leaveEncashment).subtract(totalDeductions))
                .status(eos.isPaid() ? "PAID" : "PENDING")
                .build();
    }

    public java.io.ByteArrayInputStream generateEosbReportExcel() {
        return generateExcelReport("EOSB Calculation Report",
                new String[] { "Employee Code", "Name", "Joining Date", "Last Working Day", "Years of Service",
                        "Last Basic", "Gratuity Amount", "Details" },
                getEosbReports(),
                (row, dto) -> {
                    row.createCell(0).setCellValue(dto.getEmployeeCode());
                    row.createCell(1).setCellValue(dto.getEmployeeName());
                    row.createCell(2).setCellValue(dto.getJoiningDate().toString());
                    row.createCell(3).setCellValue(dto.getLastWorkingDay().toString());
                    row.createCell(4).setCellValue(dto.getTotalYearsOfService().doubleValue());
                    row.createCell(5).setCellValue(dto.getLastBasicSalary().doubleValue());
                    row.createCell(6).setCellValue(dto.getGratuityAmount().doubleValue());
                    row.createCell(7).setCellValue(dto.getCalculationDetails());
                });
    }

    public java.io.ByteArrayInputStream generateFinalSettlementReportExcel() {
        return generateExcelReport("Final Settlement Report",
                new String[] { "Employee Code", "Name", "Last Working Day", "Gratuity", "Leave Encashment",
                        "Deductions", "Net Payable", "Status" },
                getFinalSettlementReports(),
                (row, dto) -> {
                    row.createCell(0).setCellValue(dto.getEmployeeCode());
                    row.createCell(1).setCellValue(dto.getEmployeeName());
                    row.createCell(2).setCellValue(dto.getLastWorkingDay().toString());
                    row.createCell(3).setCellValue(dto.getGratuityAmount().doubleValue());
                    row.createCell(4).setCellValue(dto.getLeaveEncashmentAmount().doubleValue());
                    row.createCell(5).setCellValue(dto.getTotalDeductions().doubleValue());
                    row.createCell(6).setCellValue(dto.getNetPayable().doubleValue());
                    row.createCell(7).setCellValue(dto.getStatus());
                });
    }

    public java.io.ByteArrayInputStream generateTerminationReasonReportExcel() {
        List<TerminationReasonReportDto> reports = getTerminationReasonReports();

        try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {

            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Termination Reasons");

            // Header for Summary
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Reason");
            headerRow.createCell(1).setCellValue("Count");

            int rowIdx = 1;
            for (TerminationReasonReportDto dto : reports) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(dto.getReason() != null ? dto.getReason().name() : "Unknown");
                row.createCell(1).setCellValue(dto.getEmployeeCount());
            }

            // Details Sheet
            org.apache.poi.ss.usermodel.Sheet detailsSheet = workbook.createSheet("Employee Details");
            org.apache.poi.ss.usermodel.Row detailsHeader = detailsSheet.createRow(0);
            detailsHeader.createCell(0).setCellValue("Reason");
            detailsHeader.createCell(1).setCellValue("Employee Code");
            detailsHeader.createCell(2).setCellValue("Name");
            detailsHeader.createCell(3).setCellValue("Last Working Day");

            int detailsRowIdx = 1;
            for (TerminationReasonReportDto dto : reports) {
                for (TerminationReasonReportDto.EmployeeDetail detail : dto.getEmployees()) {
                    org.apache.poi.ss.usermodel.Row row = detailsSheet.createRow(detailsRowIdx++);
                    row.createCell(0).setCellValue(dto.getReason() != null ? dto.getReason().name() : "Unknown");
                    row.createCell(1).setCellValue(detail.getEmployeeCode());
                    row.createCell(2).setCellValue(detail.getEmployeeName());
                    row.createCell(3).setCellValue(detail.getLastWorkingDay().toString());
                }
            }

            workbook.write(out);
            return new java.io.ByteArrayInputStream(out.toByteArray());
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to generate Excel report: " + e.getMessage());
        }
    }

    private <T> java.io.ByteArrayInputStream generateExcelReport(String sheetName, String[] headers, List<T> data,
            ReportRowPopulator<T> populator) {
        try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {

            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet(sheetName);

            // Header Row
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data Rows
            int rowIdx = 1;
            for (T item : data) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
                populator.populate(row, item);
            }

            // Autosize columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new java.io.ByteArrayInputStream(out.toByteArray());
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to generate Excel report: " + e.getMessage());
        }
    }

    @FunctionalInterface
    interface ReportRowPopulator<T> {
        void populate(org.apache.poi.ss.usermodel.Row row, T item);
    }
}
