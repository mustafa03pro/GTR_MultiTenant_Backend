package com.example.multi_tanent.tenant.reports.service;

import com.example.multi_tanent.spersusers.enitity.Employee;
import com.example.multi_tanent.tenant.employee.entity.EmployeeProfile;
import com.example.multi_tanent.tenant.employee.repository.EmployeeProfileRepository;
import com.example.multi_tanent.tenant.employee.repository.EmployeeRepository;
import com.example.multi_tanent.tenant.employee.repository.JobDetailsRepository;
import com.example.multi_tanent.tenant.employee.entity.JobDetails;
import com.example.multi_tanent.spersusers.enums.EmployeeStatus;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HrmsReportService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final JobDetailsRepository jobDetailsRepository;
    private final com.example.multi_tanent.tenant.employee.repository.EmployeeDocumentRepository employeeDocumentRepository;

    @Transactional(readOnly = true)
    public ByteArrayInputStream generateEmployeeDocumentReport(String tenantId) {
        List<com.example.multi_tanent.tenant.employee.entity.EmployeeDocument> documents = employeeDocumentRepository
                .findAllByTenantIdWithDetails(tenantId);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Employee Documents");

            // Header
            String[] columns = { "Sr.No", "Employee Name", "Employee Code", "Document Type", "Document Number",
                    "Issue Date", "Expiry Date", "Days to Expiry", "Status" };
            Row headerRow = sheet.createRow(0);
            CellStyle headerCellStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerCellStyle.setFont(headerFont);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerCellStyle);
            }

            int rowIdx = 1;
            int srNo = 1;

            for (com.example.multi_tanent.tenant.employee.entity.EmployeeDocument doc : documents) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(srNo++);
                row.createCell(1)
                        .setCellValue(doc.getEmployee() != null
                                ? doc.getEmployee().getFirstName() + " " + doc.getEmployee().getLastName()
                                : "N/A");
                row.createCell(2).setCellValue(doc.getEmployee() != null ? doc.getEmployee().getEmployeeCode() : "N/A");
                row.createCell(3).setCellValue(doc.getDocumentType() != null ? doc.getDocumentType().getName() : "N/A");
                row.createCell(4).setCellValue(doc.getDocumentId());
                row.createCell(5)
                        .setCellValue(doc.getRegistrationDate() != null ? doc.getRegistrationDate().toString() : "");
                row.createCell(6).setCellValue(doc.getEndDate() != null ? doc.getEndDate().toString() : "");

                // Days to Expiry and Status
                if (doc.getEndDate() != null) {
                    long daysToExpiry = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), doc.getEndDate());
                    row.createCell(7).setCellValue(daysToExpiry);

                    String status;
                    if (daysToExpiry < 0) {
                        status = "Expired";
                    } else if (daysToExpiry <= 30) {
                        status = "Expiring Soon";
                    } else {
                        status = "Valid";
                    }
                    row.createCell(8).setCellValue(status);
                } else {
                    row.createCell(7).setCellValue("N/A");
                    row.createCell(8).setCellValue("N/A");
                }
            }

            // Autosize columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to export employee document data to Excel file: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public ByteArrayInputStream generateEmployeeMasterReport(String statusFilter) {
        List<Employee> employees;
        if (statusFilter != null && !statusFilter.isEmpty() && !statusFilter.equalsIgnoreCase("ALL")) {
            try {
                EmployeeStatus status = EmployeeStatus.valueOf(statusFilter.toUpperCase());
                employees = employeeRepository.findByStatus(status);
            } catch (IllegalArgumentException e) {
                employees = employeeRepository.findAll();
            }
        } else {
            employees = employeeRepository.findAll();
        }
        List<EmployeeProfile> profiles = employeeProfileRepository.findAll();
        Map<Long, EmployeeProfile> profileMap = profiles.stream()
                .collect(Collectors.toMap(p -> p.getEmployee().getId(), p -> p, (p1, p2) -> p1));

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Employee Master");

            String[] columns = {
                    "Employee Code", "First Name", "Last Name", "Email", "Phone", "Gender", "Marital Status", "Status",
                    "Job Title", "Department", "Job Type", "Office", "Start Date",
                    "Labor Card No", "Mol ID", "WPS Registered", "Payment Method",
                    "Bank Name", "Account No", "IBAN", "Routing Code",
                    "Address", "City", "Country"
            };

            Row headerRow = sheet.createRow(0);
            CellStyle headerCellStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerCellStyle.setFont(headerFont);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerCellStyle);
            }

            int rowIdx = 1;
            for (Employee emp : employees) {
                Row row = sheet.createRow(rowIdx++);
                EmployeeProfile profile = profileMap.get(emp.getId());

                row.createCell(0).setCellValue(emp.getEmployeeCode() != null ? emp.getEmployeeCode() : "");
                row.createCell(1).setCellValue(emp.getFirstName() != null ? emp.getFirstName() : "");
                row.createCell(2).setCellValue(emp.getLastName() != null ? emp.getLastName() : "");
                row.createCell(3).setCellValue(emp.getEmailWork() != null ? emp.getEmailWork() : "");
                row.createCell(4).setCellValue(emp.getPhonePrimary() != null ? emp.getPhonePrimary() : "");
                row.createCell(5).setCellValue(emp.getGender() != null ? emp.getGender().name() : "");
                row.createCell(6).setCellValue(emp.getMartialStatus() != null ? emp.getMartialStatus().name() : "");
                row.createCell(7).setCellValue(emp.getStatus() != null ? emp.getStatus().name() : "");

                if (profile != null) {
                    row.createCell(8).setCellValue(profile.getJobTitle() != null ? profile.getJobTitle() : "");
                    row.createCell(9).setCellValue(profile.getDepartment() != null ? profile.getDepartment() : "");
                    row.createCell(10).setCellValue(profile.getJobType() != null ? profile.getJobType() : "");
                    row.createCell(11).setCellValue(profile.getOffice() != null ? profile.getOffice() : "");
                    row.createCell(12)
                            .setCellValue(profile.getHireDate() != null ? profile.getHireDate().toString() : "");
                    row.createCell(13)
                            .setCellValue(profile.getLaborCardNumber() != null ? profile.getLaborCardNumber() : "");
                    row.createCell(14).setCellValue(profile.getMolId() != null ? profile.getMolId() : "");
                    row.createCell(15).setCellValue(profile.isWpsRegistered() ? "Yes" : "No");
                    row.createCell(16)
                            .setCellValue(profile.getPaymentMethod() != null ? profile.getPaymentMethod() : "");
                    row.createCell(17).setCellValue(profile.getBankName() != null ? profile.getBankName() : "");
                    row.createCell(18)
                            .setCellValue(profile.getBankAccountNumber() != null ? profile.getBankAccountNumber() : "");
                    row.createCell(19).setCellValue(profile.getIban() != null ? profile.getIban() : "");
                    row.createCell(20).setCellValue(profile.getRoutingCode() != null ? profile.getRoutingCode() : "");
                    row.createCell(21).setCellValue(profile.getAddress() != null ? profile.getAddress() : "");
                    row.createCell(22).setCellValue(profile.getCity() != null ? profile.getCity() : "");
                    row.createCell(23).setCellValue(profile.getCountry() != null ? profile.getCountry() : "");
                }
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to export Employee Master data: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public ByteArrayInputStream generateHeadcountReport() {
        List<Employee> employees = employeeRepository.findByStatus(EmployeeStatus.ACTIVE);
        List<JobDetails> jobDetailsList = jobDetailsRepository.findAll();
        List<EmployeeProfile> profiles = employeeProfileRepository.findAll();

        // Maps for quick access
        Map<Long, JobDetails> jobDetailsMap = jobDetailsList.stream()
                .filter(jd -> jd.getEmployee() != null)
                .collect(Collectors.toMap(jd -> jd.getEmployee().getId(), jd -> jd, (j1, j2) -> j1));

        Map<Long, EmployeeProfile> profileMap = profiles.stream()
                .collect(Collectors.toMap(p -> p.getEmployee().getId(), p -> p, (p1, p2) -> p1));

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // 1. Total Headcount Sheet
            Sheet sheet1 = workbook.createSheet("Total Headcount");
            Row row1 = sheet1.createRow(0);
            row1.createCell(0).setCellValue("Total Active Employees");
            row1.createCell(1).setCellValue(employees.size());
            sheet1.autoSizeColumn(0);

            // 2. Department-wise Sheet
            Sheet sheet2 = workbook.createSheet("Department Wise");
            Map<String, Long> deptCount = employees.stream()
                    .map(e -> {
                        JobDetails jd = jobDetailsMap.get(e.getId());
                        if (jd != null && jd.getDepartment() != null)
                            return jd.getDepartment();
                        EmployeeProfile p = profileMap.get(e.getId());
                        if (p != null && p.getDepartment() != null)
                            return p.getDepartment();
                        return "Unassigned";
                    })
                    .collect(Collectors.groupingBy(d -> d, Collectors.counting()));

            createSummarySheet(sheet2, deptCount, "Department", "Count");

            // 3. Location-wise Sheet
            Sheet sheet3 = workbook.createSheet("Location Wise");
            Map<String, Long> locCount = employees.stream()
                    .map(e -> {
                        JobDetails jd = jobDetailsMap.get(e.getId());
                        if (jd != null && jd.getLocation() != null)
                            return jd.getLocation().getName();
                        if (jd != null && jd.getActualLocation() != null)
                            return jd.getActualLocation();
                        EmployeeProfile p = profileMap.get(e.getId());
                        if (p != null && p.getOffice() != null)
                            return p.getOffice();
                        return "Unassigned";
                    })
                    .collect(Collectors.groupingBy(l -> l, Collectors.counting()));

            createSummarySheet(sheet3, locCount, "Location", "Count");

            // 4. Role (Designation) Wise Sheet
            Sheet sheet4 = workbook.createSheet("Role Wise");
            Map<String, Long> roleCount = employees.stream()
                    .map(e -> {
                        JobDetails jd = jobDetailsMap.get(e.getId());
                        if (jd != null && jd.getDesignation() != null)
                            return jd.getDesignation();
                        EmployeeProfile p = profileMap.get(e.getId());
                        if (p != null && p.getJobTitle() != null)
                            return p.getJobTitle();
                        return "Unassigned";
                    })
                    .collect(Collectors.groupingBy(r -> r, Collectors.counting()));

            createSummarySheet(sheet4, roleCount, "Role", "Count");

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to export Headcount data: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public ByteArrayInputStream generateDemographicReport() {
        List<Employee> employees = employeeRepository.findByStatus(EmployeeStatus.ACTIVE);
        List<JobDetails> jobDetailsList = jobDetailsRepository.findAll();
        Map<Long, JobDetails> jobDetailsMap = jobDetailsList.stream()
                .filter(jd -> jd.getEmployee() != null)
                .collect(Collectors.toMap(jd -> jd.getEmployee().getId(), jd -> jd, (j1, j2) -> j1));

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // 1. Age Group Sheet
            Sheet sheet1 = workbook.createSheet("Age Group");
            Map<String, Long> ageGroupCount = employees.stream()
                    .map(e -> {
                        if (e.getDob() == null)
                            return "Unknown";
                        int age = java.time.Period.between(e.getDob(), LocalDate.now()).getYears();
                        if (age < 20)
                            return "Under 20";
                        if (age < 30)
                            return "20-29";
                        if (age < 40)
                            return "30-39";
                        if (age < 50)
                            return "40-49";
                        if (age < 60)
                            return "50-59";
                        return "60+";
                    })
                    .collect(Collectors.groupingBy(a -> a, Collectors.counting()));
            createSummarySheet(sheet1, ageGroupCount, "Age Group", "Count");

            // 2. Gender Sheet
            Sheet sheet2 = workbook.createSheet("Gender");
            Map<String, Long> genderCount = employees.stream()
                    .map(e -> e.getGender() != null ? e.getGender().name() : "Unspecified")
                    .collect(Collectors.groupingBy(g -> g, Collectors.counting()));
            createSummarySheet(sheet2, genderCount, "Gender", "Count");

            // 3. Experience Band Sheet (Tenure)
            Sheet sheet3 = workbook.createSheet("Experience Band");
            Map<String, Long> expBandCount = employees.stream()
                    .map(e -> {
                        JobDetails jd = jobDetailsMap.get(e.getId());
                        LocalDate doj = (jd != null && jd.getDateOfJoining() != null) ? jd.getDateOfJoining() : null;
                        // Fallback to createdAt if DOJ is missing, or skip
                        if (doj == null && e.getCreatedAt() != null)
                            doj = e.getCreatedAt().toLocalDate();

                        if (doj == null)
                            return "Unknown";

                        int years = java.time.Period.between(doj, LocalDate.now()).getYears();
                        if (years < 1)
                            return "< 1 Year";
                        if (years < 3)
                            return "1-3 Years";
                        if (years < 5)
                            return "3-5 Years";
                        if (years < 10)
                            return "5-10 Years";
                        return "10+ Years";
                    })
                    .collect(Collectors.groupingBy(b -> b, Collectors.counting()));
            createSummarySheet(sheet3, expBandCount, "Experience Band", "Count");

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to export Demographic data: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public ByteArrayInputStream generateEmploymentStatusReport() {
        List<Employee> employees = employeeRepository.findByStatus(EmployeeStatus.ACTIVE);
        List<JobDetails> jobDetailsList = jobDetailsRepository.findAll();
        List<EmployeeProfile> profiles = employeeProfileRepository.findAll();

        Map<Long, JobDetails> jobDetailsMap = jobDetailsList.stream()
                .filter(jd -> jd.getEmployee() != null)
                .collect(Collectors.toMap(jd -> jd.getEmployee().getId(), jd -> jd, (j1, j2) -> j1));

        Map<Long, EmployeeProfile> profileMap = profiles.stream()
                .collect(Collectors.toMap(p -> p.getEmployee().getId(), p -> p, (p1, p2) -> p1));

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // 1. Employment Type (Permanent/Contract/Intern)
            Sheet sheet1 = workbook.createSheet("Employment Type");
            Map<String, Long> typeCount = employees.stream()
                    .map(e -> {
                        JobDetails jd = jobDetailsMap.get(e.getId());
                        EmployeeProfile p = profileMap.get(e.getId());

                        // Check for Intern first
                        if (p != null && "Intern".equalsIgnoreCase(p.getJobType()))
                            return "Intern";
                        if (jd != null && "Intern".equalsIgnoreCase(jd.getProfileName()))
                            return "Intern";

                        // Check Contract Type
                        if (jd != null && jd.getContractType() != null)
                            return jd.getContractType().name();

                        return "Permanent"; // Default assumption if not defined
                    })
                    .collect(Collectors.groupingBy(t -> t, Collectors.counting()));
            createSummarySheet(sheet1, typeCount, "Employment Type", "Count");

            // 2. Probation vs Confirmed
            Sheet sheet2 = workbook.createSheet("Probation Status");
            Map<String, Long> probationCount = employees.stream()
                    .map(e -> {
                        JobDetails jd = jobDetailsMap.get(e.getId());
                        if (jd != null && jd.getProbationEndDate() != null) {
                            return jd.getProbationEndDate().isAfter(LocalDate.now()) ? "Probation" : "Confirmed";
                        }
                        return "Confirmed"; // Default if no probation date set
                    })
                    .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
            createSummarySheet(sheet2, probationCount, "Status", "Count");

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to export Employment Status data: " + e.getMessage());
        }
    }

    private void createSummarySheet(Sheet sheet, Map<String, Long> data, String keyHeader, String valueHeader) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue(keyHeader);
        header.createCell(1).setCellValue(valueHeader);

        int rowIdx = 1;
        for (Map.Entry<String, Long> entry : data.entrySet()) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue());
        }
        sheet.autoSizeColumn(0);
    }
}
