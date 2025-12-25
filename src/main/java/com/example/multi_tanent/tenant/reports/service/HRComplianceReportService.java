package com.example.multi_tanent.tenant.reports.service;

import com.example.multi_tanent.spersusers.enitity.CompanyInfo;
import com.example.multi_tanent.spersusers.enitity.Employee;
import com.example.multi_tanent.spersusers.enums.EmployeeStatus;
import com.example.multi_tanent.tenant.employee.entity.EmployeeProfile;
import com.example.multi_tanent.tenant.employee.entity.JobDetails;
import com.example.multi_tanent.tenant.employee.repository.EmployeeProfileRepository;
import com.example.multi_tanent.tenant.employee.repository.EmployeeRepository;
import com.example.multi_tanent.tenant.employee.repository.JobDetailsRepository;
import com.example.multi_tanent.tenant.payroll.service.CompanyInfoService;
import com.example.multi_tanent.tenant.reports.dto.*;
import com.example.multi_tanent.tenant.reports.service.EndOfServiceReportService.ReportRowPopulator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HRComplianceReportService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final JobDetailsRepository jobDetailsRepository;
    private final CompanyInfoService companyInfoService; // Assuming this exists or using Repository

    @Transactional(readOnly = true)
    public List<LaborCardReportDto> getLaborCardReports() {
        return employeeProfileRepository.findAll().stream()
                .filter(p -> p.getLaborCardNumber() != null)
                .map(this::mapToLaborCardReportDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CompanyQuotaReportDto getCompanyQuotaReport() {
        CompanyInfo companyInfo = companyInfoService.getCompanyInfo();
        Integer totalQuota = companyInfo.getVisaQuotaTotal() != null ? companyInfo.getVisaQuotaTotal() : 0;

        long activeEmployees = employeeRepository.findAll().stream()
                .filter(e -> e.getStatus() == EmployeeStatus.ACTIVE)
                .count();

        Integer usedQuota = (int) activeEmployees;
        Integer available = totalQuota - usedQuota;
        Double utilization = totalQuota > 0 ? ((double) usedQuota / totalQuota) * 100 : 0.0;

        return CompanyQuotaReportDto.builder()
                .companyName(companyInfo.getCompanyName())
                .totalVisaQuota(totalQuota)
                .usedVisaQuota(usedQuota)
                .availableVisaQuota(available)
                .utilizationPercentage(Math.round(utilization * 100.0) / 100.0)
                .build();
    }

    @Transactional(readOnly = true)
    public List<NationalityReportDto> getNationalityReports() {
        List<EmployeeProfile> profiles = employeeProfileRepository.findAll();
        long totalEmployees = profiles.size();

        Map<String, List<EmployeeProfile>> byNationality = profiles.stream()
                .filter(p -> p.getNationality() != null)
                .collect(Collectors.groupingBy(EmployeeProfile::getNationality));

        return byNationality.entrySet().stream()
                .map(entry -> {
                    String nat = entry.getKey();
                    long count = entry.getValue().size();
                    double pct = totalEmployees > 0 ? ((double) count / totalEmployees) * 100 : 0.0;

                    List<NationalityReportDto.EmployeeDetail> details = entry.getValue().stream()
                            .map(p -> NationalityReportDto.EmployeeDetail.builder()
                                    .employeeCode(p.getEmployee().getEmployeeCode())
                                    .employeeName(p.getEmployee().getName())
                                    .designation(p.getJobTitle()) // Or from JobDetails
                                    .build())
                            .collect(Collectors.toList());

                    return NationalityReportDto.builder()
                            .nationality(nat)
                            .employeeCount(count)
                            .percentage(Math.round(pct * 100.0) / 100.0)
                            .employees(details)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EmiratizationReportDto getEmiratizationReport() {
        List<EmployeeProfile> profiles = employeeProfileRepository.findAll();
        long total = profiles.size();
        long emiratiCount = profiles.stream()
                .filter(p -> "United Arab Emirates".equalsIgnoreCase(p.getNationality())
                        || "UAE".equalsIgnoreCase(p.getNationality()))
                .count();
        long expats = total - emiratiCount;
        double pct = total > 0 ? ((double) emiratiCount / total) * 100 : 0.0;

        return EmiratizationReportDto.builder()
                .totalEmployees(total)
                .totalNationals(emiratiCount)
                .totalExpats(expats)
                .emiratizationPercentage(Math.round(pct * 100.0) / 100.0)
                .status(pct >= 2.0 ? "COMPLIANT" : "NON_COMPLIANT") // Example 2% target
                .build();
    }

    @Transactional(readOnly = true)
    public List<AbscondingReportDto> getAbscondingReports() {
        return employeeRepository.findAll().stream()
                .filter(e -> e.getStatus() == EmployeeStatus.ABSCONDING)
                .map(e -> AbscondingReportDto.builder()
                        .employeeId(e.getId())
                        .employeeCode(e.getEmployeeCode())
                        .employeeName(e.getName())
                        .department(jobDetailsRepository.findByEmployeeId(e.getId())
                                .map(JobDetails::getDepartment).orElse("N/A"))
                        .reportedDate(e.getUpdatedAt() != null ? e.getUpdatedAt().toLocalDate() : LocalDate.now()) // Approximation
                        .status(e.getStatus().name())
                        .build())
                .collect(Collectors.toList());
    }

    // --- Helpers ---

    private LaborCardReportDto mapToLaborCardReportDto(EmployeeProfile p) {
        LocalDate expiry = p.getLaborCardExpiry();
        long daysToExpiry = expiry != null ? ChronoUnit.DAYS.between(LocalDate.now(), expiry) : 0;
        String status = "ACTIVE";
        if (expiry != null) {
            if (expiry.isBefore(LocalDate.now())) {
                status = "EXPIRED";
            } else if (daysToExpiry <= 30) {
                status = "EXPIRING_SOON";
            }
        }

        return LaborCardReportDto.builder()
                .employeeId(p.getEmployee().getId())
                .employeeCode(p.getEmployee().getEmployeeCode())
                .employeeName(p.getEmployee().getName())
                .laborCardNumber(p.getLaborCardNumber())
                .expiryDate(expiry)
                .daysToExpiry(daysToExpiry)
                .status(status)
                .build();
    }

    // --- Excel Exports ---

    public ByteArrayInputStream generateLaborCardExcel() {
        return generateExcelReport("Labor Card Status",
                new String[] { "Code", "Name", "Labor Card No", "Expiry Date", "Days to Expiry", "Status" },
                getLaborCardReports(),
                (row, dto) -> {
                    row.createCell(0).setCellValue(dto.getEmployeeCode());
                    row.createCell(1).setCellValue(dto.getEmployeeName());
                    row.createCell(2).setCellValue(dto.getLaborCardNumber());
                    row.createCell(3).setCellValue(dto.getExpiryDate() != null ? dto.getExpiryDate().toString() : "");
                    row.createCell(4).setCellValue(dto.getDaysToExpiry());
                    row.createCell(5).setCellValue(dto.getStatus());
                });
    }

    public ByteArrayInputStream generateCompanyQuotaExcel() {
        CompanyQuotaReportDto dto = getCompanyQuotaReport();
        return generateExcelReport("Company Quota",
                new String[] { "Company", "Total Quota", "Used Quota", "Available", "Utilization %" },
                Collections.singletonList(dto),
                (row, item) -> {
                    row.createCell(0).setCellValue(item.getCompanyName());
                    row.createCell(1).setCellValue(item.getTotalVisaQuota());
                    row.createCell(2).setCellValue(item.getUsedVisaQuota());
                    row.createCell(3).setCellValue(item.getAvailableVisaQuota());
                    row.createCell(4).setCellValue(item.getUtilizationPercentage() + "%");
                });
    }

    public ByteArrayInputStream generateNationalityExcel() {
        List<NationalityReportDto> reports = getNationalityReports();
        try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {

            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Nationality Summary");
            // Header
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Nationality");
            headerRow.createCell(1).setCellValue("Count");
            headerRow.createCell(2).setCellValue("%");

            int rowIdx = 1;
            for (NationalityReportDto dto : reports) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(dto.getNationality());
                row.createCell(1).setCellValue(dto.getEmployeeCount());
                row.createCell(2).setCellValue(dto.getPercentage());
            }

            // Details
            org.apache.poi.ss.usermodel.Sheet detailsSheet = workbook.createSheet("Employee Details");
            org.apache.poi.ss.usermodel.Row dHeader = detailsSheet.createRow(0);
            dHeader.createCell(0).setCellValue("Nationality");
            dHeader.createCell(1).setCellValue("Code");
            dHeader.createCell(2).setCellValue("Name");
            dHeader.createCell(3).setCellValue("Designation");

            int dRowIdx = 1;
            for (NationalityReportDto dto : reports) {
                for (NationalityReportDto.EmployeeDetail detail : dto.getEmployees()) {
                    org.apache.poi.ss.usermodel.Row row = detailsSheet.createRow(dRowIdx++);
                    row.createCell(0).setCellValue(dto.getNationality());
                    row.createCell(1).setCellValue(detail.getEmployeeCode());
                    row.createCell(2).setCellValue(detail.getEmployeeName());
                    row.createCell(3).setCellValue(detail.getDesignation());
                }
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel report: " + e.getMessage());
        }
    }

    public ByteArrayInputStream generateEmiratizationExcel() {
        EmiratizationReportDto dto = getEmiratizationReport();
        return generateExcelReport("Emiratization",
                new String[] { "Total Employees", "Nationals", "Expats", "Emiratization %", "Status" },
                Collections.singletonList(dto),
                (row, item) -> {
                    row.createCell(0).setCellValue(item.getTotalEmployees());
                    row.createCell(1).setCellValue(item.getTotalNationals());
                    row.createCell(2).setCellValue(item.getTotalExpats());
                    row.createCell(3).setCellValue(item.getEmiratizationPercentage() + "%");
                    row.createCell(4).setCellValue(item.getStatus());
                });
    }

    public ByteArrayInputStream generateAbscondingExcel() {
        return generateExcelReport("Absconding Report",
                new String[] { "Code", "Name", "Department", "Reported Date", "Status" },
                getAbscondingReports(),
                (row, dto) -> {
                    row.createCell(0).setCellValue(dto.getEmployeeCode());
                    row.createCell(1).setCellValue(dto.getEmployeeName());
                    row.createCell(2).setCellValue(dto.getDepartment());
                    row.createCell(3).setCellValue(dto.getReportedDate().toString());
                    row.createCell(4).setCellValue(dto.getStatus());
                });
    }

    private <T> ByteArrayInputStream generateExcelReport(String sheetName, String[] headers, List<T> data,
            ReportRowPopulator<T> populator) {
        try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

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
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel report: " + e.getMessage());
        }
    }

    // Duplicate interface from EOS service to avoid circular dep or move to common
    // utils
    @FunctionalInterface
    public interface ReportRowPopulator<T> {
        void populate(org.apache.poi.ss.usermodel.Row row, T item);
    }
}
