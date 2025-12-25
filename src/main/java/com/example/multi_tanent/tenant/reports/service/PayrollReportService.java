package com.example.multi_tanent.tenant.reports.service;

import com.example.multi_tanent.tenant.employee.entity.JobDetails;
import com.example.multi_tanent.tenant.employee.repository.JobDetailsRepository;
import com.example.multi_tanent.tenant.payroll.entity.Payslip;
import com.example.multi_tanent.tenant.payroll.entity.PayslipComponent;
import com.example.multi_tanent.tenant.payroll.entity.SalaryComponent;
import com.example.multi_tanent.tenant.payroll.enums.SalaryComponentType;
import com.example.multi_tanent.tenant.payroll.repository.PayslipRepository;
import com.example.multi_tanent.tenant.payroll.repository.SalaryComponentRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayrollReportService {

    private final PayslipRepository payslipRepository;
    private final SalaryComponentRepository salaryComponentRepository;
    private final JobDetailsRepository jobDetailsRepository;

    @Transactional(readOnly = true)
    public ByteArrayInputStream generatePayrollRegisterExcel(int year, int month) {
        List<Payslip> payslips = payslipRepository.findAllByYearAndMonthWithDetails(year, month);
        List<SalaryComponent> allComponents = salaryComponentRepository.findAll();
        List<JobDetails> jobDetailsList = jobDetailsRepository.findAll();

        Map<Long, JobDetails> jobDetailsMap = jobDetailsList.stream()
                .filter(jd -> jd.getEmployee() != null)
                .collect(Collectors.toMap(jd -> jd.getEmployee().getId(), jd -> jd,
                        (existing, replacement) -> existing));

        // Separate components
        List<SalaryComponent> earnings = allComponents.stream()
                .filter(c -> c.getType() == SalaryComponentType.EARNING)
                .sorted(Comparator.comparingInt(c -> c.getDisplayOrder() != null ? c.getDisplayOrder() : 999))
                .collect(Collectors.toList());

        List<SalaryComponent> deductions = allComponents.stream()
                .filter(c -> c.getType() == SalaryComponentType.DEDUCTION)
                .sorted(Comparator.comparingInt(c -> c.getDisplayOrder() != null ? c.getDisplayOrder() : 999))
                .collect(Collectors.toList());

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Payroll Register " + month + "-" + year);

            // 1. Build Header
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = createHeaderStyle(workbook);

            int colIdx = 0;
            // Static Identity Columns
            createCell(headerRow, colIdx++, "Employee Code", headerStyle);
            createCell(headerRow, colIdx++, "Employee Name", headerStyle);
            createCell(headerRow, colIdx++, "Department", headerStyle);
            createCell(headerRow, colIdx++, "Designation", headerStyle);
            createCell(headerRow, colIdx++, "Pay Days", headerStyle);
            createCell(headerRow, colIdx++, "LOP Days", headerStyle);

            // Dynamic Earnings
            for (SalaryComponent sc : earnings) {
                createCell(headerRow, colIdx++, sc.getName(), headerStyle);
            }
            // Gross Total
            createCell(headerRow, colIdx++, "GROSS EARNINGS", headerStyle);

            // Dynamic Deductions
            for (SalaryComponent sc : deductions) {
                createCell(headerRow, colIdx++, sc.getName(), headerStyle);
            }
            // Total Deductions & Net
            createCell(headerRow, colIdx++, "TOTAL DEDUCTIONS", headerStyle);
            createCell(headerRow, colIdx++, "NET PAY", headerStyle);

            // 2. Build Data Rows
            int rowIdx = 1;
            for (Payslip p : payslips) {
                Row row = sheet.createRow(rowIdx++);
                JobDetails jd = jobDetailsMap.get(p.getEmployee().getId());

                int cellIdx = 0;
                // Identity
                row.createCell(cellIdx++).setCellValue(p.getEmployee().getEmployeeCode());
                row.createCell(cellIdx++)
                        .setCellValue(p.getEmployee().getFirstName() + " " + p.getEmployee().getLastName());
                row.createCell(cellIdx++).setCellValue(jd != null ? jd.getDepartment() : "");
                row.createCell(cellIdx++).setCellValue(jd != null ? jd.getDesignation() : "");
                row.createCell(cellIdx++)
                        .setCellValue(p.getPayableDays() != null ? p.getPayableDays().doubleValue() : 0.0);
                row.createCell(cellIdx++)
                        .setCellValue(p.getLossOfPayDays() != null ? p.getLossOfPayDays().doubleValue() : 0.0);

                // Map Components for this Payslip
                Map<Long, BigDecimal> compMap = p.getComponents().stream()
                        .collect(Collectors.toMap(
                                pc -> pc.getSalaryComponent().getId(),
                                PayslipComponent::getAmount,
                                (v1, v2) -> v1 // Duplicates shouldn't happen, simplify
                        ));

                // Earnings
                for (SalaryComponent sc : earnings) {
                    BigDecimal val = compMap.getOrDefault(sc.getId(), BigDecimal.ZERO);
                    row.createCell(cellIdx++).setCellValue(val.doubleValue());
                }
                // Gross
                row.createCell(cellIdx++).setCellValue(p.getGrossEarnings().doubleValue());

                // Deductions
                for (SalaryComponent sc : deductions) {
                    BigDecimal val = compMap.getOrDefault(sc.getId(), BigDecimal.ZERO);
                    row.createCell(cellIdx++).setCellValue(val.doubleValue());
                }
                // Totals
                row.createCell(cellIdx++).setCellValue(p.getTotalDeductions().doubleValue());
                row.createCell(cellIdx++).setCellValue(p.getNetSalary().doubleValue());
            }

            // Autosize
            for (int i = 0; i < colIdx; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Payroll Register Excel: " + e.getMessage());
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private void createCell(Row row, int colIdx, String value, CellStyle style) {
        Cell cell = row.createCell(colIdx);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }
}
