package com.example.multi_tanent.tenant.reports.service;

import com.example.multi_tanent.tenant.attendance.entity.AttendanceRecord;
import com.example.multi_tanent.tenant.attendance.enums.AttendanceStatus;
import com.example.multi_tanent.tenant.attendance.repository.AttendanceRecordRepository;
import com.example.multi_tanent.spersusers.enitity.Employee;
import com.example.multi_tanent.tenant.employee.repository.EmployeeRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AttendenceReportService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final EmployeeRepository employeeRepository;

    public AttendenceReportService(AttendanceRecordRepository attendanceRecordRepository,
            EmployeeRepository employeeRepository) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public ByteArrayInputStream generateDailyAttendanceReport(LocalDate date, String tenantId) {
        List<AttendanceRecord> records = attendanceRecordRepository.findAllByTenantIdAndDateWithDetails(tenantId, date);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Daily Attendance - " + date);

            String[] columns = { "Employee Code", "Name", "Department", "Status", "Check-In", "Check-Out", "Is Late" };
            createHeader(workbook, sheet, columns);

            int rowIdx = 1;
            for (AttendanceRecord record : records) {
                Row row = sheet.createRow(rowIdx++);
                Employee emp = record.getEmployee();

                row.createCell(0).setCellValue(emp != null ? emp.getEmployeeCode() : "N/A");
                row.createCell(1).setCellValue(emp != null ? emp.getName() : "N/A");
                // Assuming Department is not directly available on Employee but via JobDetails
                // or similar, skipping for now or adding if available.
                // Checking Employee entity, department is not direct.
                row.createCell(2).setCellValue("N/A");
                row.createCell(3).setCellValue(record.getStatus() != null ? record.getStatus().toString() : "UNKNOWN");
                row.createCell(4).setCellValue(record.getCheckIn() != null ? record.getCheckIn().toString() : "");
                row.createCell(5).setCellValue(record.getCheckOut() != null ? record.getCheckOut().toString() : "");
                row.createCell(6).setCellValue(record.getIsLate() ? "Yes" : "No");
            }
            autosizeColumns(sheet, columns.length);
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Daily Attendance Report: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public ByteArrayInputStream generateMonthlyAttendanceSummary(YearMonth month, String tenantId) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();
        List<AttendanceRecord> records = attendanceRecordRepository.findAllByTenantIdAndDateBetweenWithDetails(tenantId,
                startDate, endDate);

        Map<Employee, List<AttendanceRecord>> employeeRecordsMap = records.stream()
                .filter(r -> r.getEmployee() != null)
                .collect(Collectors.groupingBy(AttendanceRecord::getEmployee));

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Monthly Summary - " + month);
            String[] columns = { "Employee Code", "Name", "Total Days", "Working Days", "Present", "Absent", "On Leave",
                    "Unpaid Leave", "Absconding Potential" };
            createHeader(workbook, sheet, columns);

            int rowIdx = 1;
            for (Map.Entry<Employee, List<AttendanceRecord>> entry : employeeRecordsMap.entrySet()) {
                Employee emp = entry.getKey();
                List<AttendanceRecord> empRecords = entry.getValue();

                int workingDays = month.lengthOfMonth(); // Simplified, should exclude weekends/holidays ideally
                long presentCount = empRecords.stream().filter(
                        r -> r.getStatus() == AttendanceStatus.PRESENT || r.getStatus() == AttendanceStatus.HALF_DAY)
                        .count();
                long absentCount = empRecords.stream().filter(r -> r.getStatus() == AttendanceStatus.ABSENT).count();
                long leaveCount = empRecords.stream().filter(r -> r.getStatus() == AttendanceStatus.ON_LEAVE).count();

                // Placeholder logic for unpaid/absconding
                long unpaidLeave = 0;
                String absconding = absentCount > 3 ? "Yes" : "No"; // Basic rule

                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(emp.getEmployeeCode());
                row.createCell(1).setCellValue(emp.getName());
                row.createCell(2).setCellValue(month.lengthOfMonth());
                row.createCell(3).setCellValue(workingDays);
                row.createCell(4).setCellValue(presentCount);
                row.createCell(5).setCellValue(absentCount);
                row.createCell(6).setCellValue(leaveCount);
                row.createCell(7).setCellValue(unpaidLeave);
                row.createCell(8).setCellValue(absconding);
            }
            autosizeColumns(sheet, columns.length);
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Monthly Summary Report: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public ByteArrayInputStream generateOvertimeReport(YearMonth month, String tenantId) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();
        List<AttendanceRecord> records = attendanceRecordRepository.findAllByTenantIdAndDateBetweenWithDetails(tenantId,
                startDate, endDate);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Overtime Report - " + month);
            String[] columns = { "Employee Code", "Name", "Date", "Check-In", "Check-Out", "Normal OT (Mins)",
                    "Friday/Holiday OT" };
            createHeader(workbook, sheet, columns);

            int rowIdx = 1;
            for (AttendanceRecord r : records) {
                if (r.getOvertimeMinutes() != null && r.getOvertimeMinutes() > 0) {
                    Row row = sheet.createRow(rowIdx++);
                    Employee emp = r.getEmployee();
                    row.createCell(0).setCellValue(emp != null ? emp.getEmployeeCode() : "N/A");
                    row.createCell(1).setCellValue(emp != null ? emp.getName() : "N/A");
                    row.createCell(2).setCellValue(r.getAttendanceDate().toString());
                    row.createCell(3).setCellValue(r.getCheckIn() != null ? r.getCheckIn().toString() : "");
                    row.createCell(4).setCellValue(r.getCheckOut() != null ? r.getCheckOut().toString() : "");

                    // Logic for OT splitting
                    boolean isFridayOrHoliday = r.getStatus() == AttendanceStatus.WEEKLY_OFF
                            || r.getStatus() == AttendanceStatus.HOLIDAY;
                    if (isFridayOrHoliday) {
                        row.createCell(5).setCellValue(0);
                        row.createCell(6).setCellValue(r.getOvertimeMinutes());
                    } else {
                        row.createCell(5).setCellValue(r.getOvertimeMinutes());
                        row.createCell(6).setCellValue(0);
                    }
                }
            }
            autosizeColumns(sheet, columns.length);
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Overtime Report: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public ByteArrayInputStream generateWorkingHoursComplianceReport(YearMonth month, String tenantId) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();
        List<AttendanceRecord> records = attendanceRecordRepository.findAllByTenantIdAndDateBetweenWithDetails(tenantId,
                startDate, endDate);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Compliance Report - " + month);
            String[] columns = { "Employee Code", "Name", "Date", "Total Hours", "State", "Daily Limit Exceeded",
                    "Weekly Limit Status" };
            createHeader(workbook, sheet, columns);

            int rowIdx = 1;
            for (AttendanceRecord r : records) {
                // Calculate duration
                double hours = 0;
                if (r.getCheckIn() != null && r.getCheckOut() != null) {
                    long diff = java.time.Duration.between(r.getCheckIn(), r.getCheckOut()).toMinutes();
                    hours = diff / 60.0;
                }

                Row row = sheet.createRow(rowIdx++);
                Employee emp = r.getEmployee();
                row.createCell(0).setCellValue(emp != null ? emp.getEmployeeCode() : "N/A");
                row.createCell(1).setCellValue(emp != null ? emp.getName() : "N/A");
                row.createCell(2).setCellValue(r.getAttendanceDate().toString());
                row.createCell(3).setCellValue(String.format("%.2f", hours));
                row.createCell(4).setCellValue(r.getStatus().toString());

                // Compliance logic (simplified)
                boolean dailyExceeded = hours > 9; // Basic UAE rule
                row.createCell(5).setCellValue(dailyExceeded ? "YES" : "NO");
                row.createCell(6).setCellValue("N/A"); // Weekly calculation requires aggregation logic
            }
            autosizeColumns(sheet, columns.length);
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Compliance Report: " + e.getMessage());
        }
    }

    private void createHeader(Workbook workbook, Sheet sheet, String[] columns) {
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
    }

    private void autosizeColumns(Sheet sheet, int count) {
        for (int i = 0; i < count; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
