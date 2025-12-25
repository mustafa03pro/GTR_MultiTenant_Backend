package com.example.multi_tanent.tenant.payroll.service;

import com.example.multi_tanent.config.TenantContext;
import com.example.multi_tanent.spersusers.enitity.CompanyInfo;
import com.example.multi_tanent.tenant.employee.entity.EmployeeProfile;
import com.example.multi_tanent.tenant.employee.repository.EmployeeProfileRepository;
import com.example.multi_tanent.tenant.payroll.entity.EmployeeBankAccount;
import com.example.multi_tanent.tenant.payroll.entity.PayrollRun;
import com.example.multi_tanent.tenant.payroll.entity.Payslip;
import com.example.multi_tanent.tenant.payroll.repository.CompanyInfoRepository;
import com.example.multi_tanent.tenant.payroll.repository.EmployeeBankAccountRepository;
import com.example.multi_tanent.tenant.payroll.repository.PayrollRunRepository;
import com.example.multi_tanent.tenant.payroll.repository.PayslipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WpsService {

    private final PayrollRunRepository payrollRunRepository;
    private final CompanyInfoRepository companyInfoRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final EmployeeBankAccountRepository employeeBankAccountRepository;
    private final PayslipRepository payslipRepository;
    private final com.example.multi_tanent.tenant.employee.repository.EmployeeRepository employeeRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmm");
    private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMyyyy");

    @Transactional(readOnly = true)
    public Map<String, Object> generateSifFile(Long payrollRunId) {
        String tenantId = TenantContext.getTenantId();
        PayrollRun payrollRun = payrollRunRepository.findById(payrollRunId)
                .orElseThrow(() -> new IllegalArgumentException("Payroll run not found"));

        CompanyInfo companyInfo = companyInfoRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Company info not found"));

        if (companyInfo.getMohreEstablishmentId() == null || companyInfo.getMohreEstablishmentId().isEmpty()) {
            throw new IllegalStateException("Company MOHRE Establishment ID is missing");
        }
        if (companyInfo.getEmployerBankRoutingCode() == null || companyInfo.getEmployerBankRoutingCode().isEmpty()) {
            throw new IllegalStateException("Company Employer Bank Routing Code is missing");
        }

        List<Payslip> payslips = payslipRepository.findByPayrollRunId(payrollRunId);
        if (payslips.isEmpty()) {
            throw new IllegalStateException("No payslips found for this payroll run.");
        }

        // Fetch Employee Profiles for additional details (Labor Card info)
        List<EmployeeProfile> employeeProfiles = employeeProfileRepository.findAll();
        Map<Long, EmployeeProfile> profileMap = employeeProfiles.stream()
                .collect(Collectors.toMap(p -> p.getEmployee().getId(), p -> p));

        // Fetch Employee Bank Accounts
        List<EmployeeBankAccount> bankAccounts = employeeBankAccountRepository.findAll();
        Map<Long, EmployeeBankAccount> bankAccountMap = bankAccounts.stream()
                .filter(EmployeeBankAccount::isPrimary)
                .collect(Collectors.toMap(b -> b.getEmployee().getId(), b -> b, (b1, b2) -> b1));

        StringBuilder sifContent = new StringBuilder();
        int recordCount = 0;
        BigDecimal totalSalary = BigDecimal.ZERO;

        LocalDate startDate = payrollRun.getPayPeriodStart();
        LocalDate endDate = payrollRun.getPayPeriodEnd();
        String salaryMonth = String.format("%02d%d", payrollRun.getMonth(), payrollRun.getYear());

        // --- Generate EDR (Employee Detail Records) ---
        for (Payslip payslip : payslips) {
            Long employeeId = payslip.getEmployee().getId();
            EmployeeProfile profile = profileMap.get(employeeId);
            EmployeeBankAccount bankAccount = bankAccountMap.get(employeeId);

            if (profile == null || bankAccount == null || !profile.isWpsRegistered()) {
                continue;
            }

            String employeeId14Digit = profile.getLaborCardNumber();
            if (employeeId14Digit == null || employeeId14Digit.length() != 14) {
                continue; // Skip invalid labor card
            }

            String agentId = bankAccount.getRoutingCode();
            if (agentId == null || agentId.length() != 9) {
                if (profile.getRoutingCode() != null && profile.getRoutingCode().length() == 9) {
                    agentId = profile.getRoutingCode();
                } else {
                    continue; // Skip invalid routing code
                }
            }

            String accountNo = bankAccount.getIban() != null && !bankAccount.getIban().isEmpty()
                    ? bankAccount.getIban()
                    : bankAccount.getAccountNumber();

            int daysInPeriod = 30;
            if (payslip.getTotalDaysInMonth() != null) {
                daysInPeriod = payslip.getTotalDaysInMonth();
            }

            BigDecimal fixedSalary = BigDecimal.ZERO;
            BigDecimal variableSalary = BigDecimal.ZERO;
            List<com.example.multi_tanent.tenant.payroll.entity.PayslipComponent> variableComponents = new ArrayList<>();

            if (payslip.getComponents() != null) {
                for (com.example.multi_tanent.tenant.payroll.entity.PayslipComponent component : payslip
                        .getComponents()) {
                    if (Boolean.TRUE.equals(component.getSalaryComponent().getIsWpsIncluded())) {
                        if (Boolean.TRUE.equals(component.getSalaryComponent().getIsVariable())) {
                            variableSalary = variableSalary.add(component.getAmount());
                            variableComponents.add(component);
                        } else {
                            fixedSalary = fixedSalary.add(component.getAmount());
                        }
                    }
                }
            }

            // If no components marked, fallback to Net Salary as Fixed?
            // Better to rely on components if available. If 0, use Net Salary as Fixed
            // safely?
            // Strict logic: If components exist, trust them.
            if (fixedSalary.compareTo(BigDecimal.ZERO) == 0 && variableSalary.compareTo(BigDecimal.ZERO) == 0
                    && payslip.getNetSalary().compareTo(BigDecimal.ZERO) > 0) {
                fixedSalary = payslip.getNetSalary();
            }

            int daysOnLeave = 0;
            if (payslip.getLossOfPayDays() != null) {
                daysOnLeave = payslip.getLossOfPayDays().intValue();
            }

            // EDR
            String edrRecord = String.format("EDR,%s,%s,%s,%s,%s,%d,%.2f,%.2f,%d",
                    employeeId14Digit,
                    agentId,
                    accountNo,
                    startDate.format(DATE_FORMATTER),
                    endDate.format(DATE_FORMATTER),
                    daysInPeriod,
                    fixedSalary,
                    variableSalary,
                    daysOnLeave);

            sifContent.append(edrRecord).append("\n");
            recordCount++; // Count EDR (and EVPs? No, usually EDR count)
                           // NOTE: SCR EDRCount typically counts EDR records only.

            totalSalary = totalSalary.add(fixedSalary).add(variableSalary);

            // EVP Records
            for (com.example.multi_tanent.tenant.payroll.entity.PayslipComponent vc : variableComponents) {
                if (vc.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                    // EVP, EmployeeId14, AgentId, AccountNo, Type, Amount
                    // Standard variation: EVP,LaborCard,Routing,Account,Code,Amount
                    String evpRecord = String.format("EVP,%s,%s,%s,%s,%.2f",
                            employeeId14Digit,
                            agentId,
                            accountNo, // Some specs omit this in EVP if in EDR, but safe to include if unsure?
                                       // The user image says "listed by type".
                                       // Let's assume: EVP, ID, Routing, Account, Code, Amount.
                            vc.getSalaryComponent().getCode(), // Or Name? Code is safer.
                            vc.getAmount());
                    sifContent.append(evpRecord).append("\n");
                    // Does EVP count towards record count? Usually No, EDR count is distinct.
                }
            }
        }

        // --- Generate SCR (Salary Control Record) ---
        String employerId = String.format("%013d", Long.parseLong(companyInfo.getMohreEstablishmentId()));
        String employerAgentId = companyInfo.getEmployerBankRoutingCode();
        LocalDate now = LocalDate.now();
        String creationDate = now.format(DATE_FORMATTER);
        String creationTime = LocalDateTime.now().format(TIME_FORMATTER);

        String scrRecord = String.format("SCR,%s,%s,%s,%s,%s,%d,%.2f,AED,",
                employerId,
                employerAgentId,
                creationDate,
                creationTime,
                salaryMonth,
                recordCount,
                totalSalary);
        sifContent.append(scrRecord);

        // Filename: EEEEEEEEEEEEEYYMMDDHHMMSS.SIF
        String timeForFilename = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
        String fileName = String.format("%s%s%s.SIF", employerId, now.format(DateTimeFormatter.ofPattern("yyMMdd")),
                timeForFilename);

        return Map.of(
                "fileName", fileName,
                "content", sifContent.toString());
    }

    public com.example.multi_tanent.tenant.payroll.dto.WpsComplianceReportDto generateComplianceReport(int year,
            int month) {
        String tenantId = TenantContext.getTenantId();

        // 1. Get Payroll Run for the period
        PayrollRun payrollRun = payrollRunRepository.findByYearAndMonth(year, month)
                .orElse(null); // Report can still be generated showing 0 paid if no run exists

        // 2. Get All Active Employees
        List<com.example.multi_tanent.spersusers.enitity.Employee> activeEmployees = employeeRepository
                .findByStatus(com.example.multi_tanent.spersusers.enums.EmployeeStatus.ACTIVE);

        // 3. Get Payslips if run exists
        List<Payslip> payslips = (payrollRun != null)
                ? payslipRepository.findByPayrollRunId(payrollRun.getId())
                : List.of();

        Map<Long, Payslip> payslipMap = payslips.stream()
                .collect(Collectors.toMap(p -> p.getEmployee().getId(), p -> p));

        int totalEmployees = activeEmployees.size();
        int paidEmployees = 0;
        int unpaidEmployees = 0;
        List<com.example.multi_tanent.tenant.payroll.dto.WpsComplianceReportDto.EmployeeComplianceDetail> details = new ArrayList<>();

        for (com.example.multi_tanent.spersusers.enitity.Employee emp : activeEmployees) {
            Payslip payslip = payslipMap.get(emp.getId());
            boolean isPaid = false;
            Long delayDays = 0L;
            LocalDate payDate = null;
            String status = "UNPAID";
            String remarks = "No payslip generated";

            if (payslip != null) {
                payDate = payslip.getPayDate();
                // Check if paid (Assuming PAID or COMPLETED means paid)
                if (payslip.getStatus() == com.example.multi_tanent.tenant.payroll.enums.PayrollStatus.PAID
                        || payslip
                                .getStatus() == com.example.multi_tanent.tenant.payroll.enums.PayrollStatus.COMPLETED) {
                    isPaid = true;
                    status = "PAID";

                    if (payrollRun != null && payrollRun.getPayPeriodEnd() != null && payDate != null) {
                        // Calculate delay relative to period end
                        // Delay = PayDate - PayPeriodEnd. If PayDate <= PayPeriodEnd, delay is 0 (or
                        // negative which we clamp to 0).
                        // Usually allow a grace period, but strict calculation matches "Delay
                        // Tracking".
                        long diff = java.time.temporal.ChronoUnit.DAYS.between(payrollRun.getPayPeriodEnd(), payDate);
                        delayDays = Math.max(0, diff);

                        if (delayDays > 0) {
                            remarks = "Delayed by " + delayDays + " days";
                        } else {
                            remarks = "On Time";
                        }
                    }
                } else {
                    status = payslip.getStatus().name();
                    remarks = "Payslip exists but not marked PAID";
                }
            }

            if (isPaid) {
                paidEmployees++;
            } else {
                unpaidEmployees++;
            }

            details.add(com.example.multi_tanent.tenant.payroll.dto.WpsComplianceReportDto.EmployeeComplianceDetail
                    .builder()
                    .employeeId(emp.getId())
                    .employeeCode(emp.getEmployeeCode())
                    .employeeName(emp.getName()) // Assuming getName() exists or firstName+lastName
                    .status(status)
                    .payDate(payDate)
                    .delayDays(delayDays)
                    .remarks(remarks)
                    .build());
        }

        BigDecimal compliantPercentage = (totalEmployees > 0)
                ? BigDecimal.valueOf(paidEmployees).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalEmployees), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return com.example.multi_tanent.tenant.payroll.dto.WpsComplianceReportDto.builder()
                .totalEmployees(totalEmployees)
                .paidEmployees(paidEmployees)
                .unpaidEmployees(unpaidEmployees)
                .compliantPercentage(compliantPercentage)
                .details(details)
                .build();
    }

    public java.io.ByteArrayInputStream generateComplianceReportExcel(int year, int month) {
        com.example.multi_tanent.tenant.payroll.dto.WpsComplianceReportDto report = generateComplianceReport(year,
                month);

        try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {

            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("WPS Compliance " + month + "-" + year);

            // Header Row
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            String[] headers = { "Employee Code", "Employee Name", "Status", "Pay Date", "Delay Days", "Remarks" };
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
            for (com.example.multi_tanent.tenant.payroll.dto.WpsComplianceReportDto.EmployeeComplianceDetail detail : report
                    .getDetails()) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(detail.getEmployeeCode());
                row.createCell(1).setCellValue(detail.getEmployeeName());
                row.createCell(2).setCellValue(detail.getStatus());
                row.createCell(3).setCellValue(detail.getPayDate() != null ? detail.getPayDate().toString() : "");
                row.createCell(4).setCellValue(detail.getDelayDays());
                row.createCell(5).setCellValue(detail.getRemarks());
            }

            // Summary Row at the top or bottom? Let's add stats at the end
            rowIdx++;
            org.apache.poi.ss.usermodel.Row summaryTitleRow = sheet.createRow(rowIdx++);
            summaryTitleRow.createCell(0).setCellValue("Summary Stats");

            org.apache.poi.ss.usermodel.Row statsRow1 = sheet.createRow(rowIdx++);
            statsRow1.createCell(0).setCellValue("Total Employees: " + report.getTotalEmployees());

            org.apache.poi.ss.usermodel.Row statsRow2 = sheet.createRow(rowIdx++);
            statsRow2.createCell(0).setCellValue("Paid: " + report.getPaidEmployees());

            org.apache.poi.ss.usermodel.Row statsRow3 = sheet.createRow(rowIdx++);
            statsRow3.createCell(0).setCellValue("Unpaid: " + report.getUnpaidEmployees());

            org.apache.poi.ss.usermodel.Row statsRow4 = sheet.createRow(rowIdx++);
            statsRow4.createCell(0)
                    .setCellValue("Compliance %: " + String.format("%.2f", report.getCompliantPercentage()) + "%");

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
}
