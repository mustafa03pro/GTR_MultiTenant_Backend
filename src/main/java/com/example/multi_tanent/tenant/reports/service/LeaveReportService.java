package com.example.multi_tanent.tenant.reports.service;

import com.example.multi_tanent.spersusers.enitity.Employee;
import com.example.multi_tanent.tenant.employee.entity.EmployeeProfile;
import com.example.multi_tanent.tenant.employee.repository.EmployeeProfileRepository;
import com.example.multi_tanent.tenant.employee.repository.EmployeeRepository;
import com.example.multi_tanent.tenant.leave.entity.LeaveRequest;
import com.example.multi_tanent.tenant.leave.enums.LeaveStatus;
import com.example.multi_tanent.tenant.employee.entity.JobDetails;
import com.example.multi_tanent.tenant.employee.repository.JobDetailsRepository;
import com.example.multi_tanent.tenant.leave.entity.LeaveBalance;
import com.example.multi_tanent.tenant.leave.entity.LeaveType;
import com.example.multi_tanent.tenant.leave.repository.LeaveBalanceRepository;
import com.example.multi_tanent.tenant.leave.repository.LeaveTypeRepository;
import com.example.multi_tanent.tenant.leave.repository.LeaveRequestRepository;
import com.example.multi_tanent.tenant.reports.dto.LeaveAccrualReportDto;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveReportService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final JobDetailsRepository jobDetailsRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;

    @Transactional(readOnly = true)
    public ByteArrayInputStream generateAccrualReportExcel(LocalDate asOfDate) {
        List<LeaveAccrualReportDto> reportData = generateAccrualReport(asOfDate);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Leave Accrual Report");

            // Header
            String[] columns = { "Employee Code", "Employee Name", "Department", "Designation", "Joining Date",
                    "Leave Type", "Accrued Days", "Taken Days", "Balance Days" };
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

            // Data
            int rowIdx = 1;
            for (LeaveAccrualReportDto dto : reportData) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(dto.getEmployeeCode());
                row.createCell(1).setCellValue(dto.getEmployeeName());
                row.createCell(2).setCellValue(dto.getDepartment());
                row.createCell(3).setCellValue(dto.getDesignation());
                row.createCell(4).setCellValue(dto.getJoiningDate() != null ? dto.getJoiningDate().toString() : "");
                row.createCell(5).setCellValue(dto.getLeaveType());
                row.createCell(6).setCellValue(dto.getAccruedDays() != null ? dto.getAccruedDays().doubleValue() : 0.0);
                row.createCell(7).setCellValue(dto.getTakenDays() != null ? dto.getTakenDays().doubleValue() : 0.0);
                row.createCell(8).setCellValue(dto.getBalanceDays() != null ? dto.getBalanceDays().doubleValue() : 0.0);
            }

            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Leave Accrual Excel Report: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<LeaveAccrualReportDto> generateAccrualReport(LocalDate asOfDate) {
        if (asOfDate == null) {
            asOfDate = LocalDate.now();
        }

        List<Employee> employees = employeeRepository.findAll();
        List<JobDetails> jobDetailsList = jobDetailsRepository.findAll();
        List<LeaveType> leaveTypes = leaveTypeRepository.findByActiveTrue();

        // Optimizing: Fetch relevant balances? Batching might be hard without custom
        // query.
        // For now, let's fetch all relevant balances or query in loop (Warning: N+1 if
        // not careful).
        // Better: Fetch all balances for these employees?
        // Since we don't have a "findAllByDate" easily, let's use what we have or
        // accept N+Queries for now given time constraints, or optimize if possible.
        // Optimization: `leaveBalanceRepository.findByEmployeeId(id)` is available.

        Map<Long, JobDetails> jobDetailsMap = jobDetailsList.stream()
                .filter(jd -> jd.getEmployee() != null)
                .collect(Collectors.toMap(jd -> jd.getEmployee().getId(), jd -> jd,
                        (existing, replacement) -> existing));

        List<LeaveRequest> allRequests = leaveRequestRepository.findAllWithDetails();

        LocalDate finalAsOfDate = asOfDate;
        List<LeaveAccrualReportDto> report = new ArrayList<>();

        for (Employee emp : employees) {
            JobDetails jobDetails = jobDetailsMap.get(emp.getId());

            // Prefer JobDetails date, fallback to hardcoded
            LocalDate joinDate = (jobDetails != null && jobDetails.getDateOfJoining() != null)
                    ? jobDetails.getDateOfJoining()
                    : LocalDate.of(2024, 1, 1);

            // Fetch balances for this employee
            List<LeaveBalance> balances = leaveBalanceRepository.findByEmployeeId(emp.getId());
            Map<Long, LeaveBalance> balanceMap = balances.stream()
                    .collect(Collectors.toMap(lb -> lb.getLeaveType().getId(), lb -> lb,
                            (existing, replacement) -> existing)); // Handle duplicates if any, take existing

            for (LeaveType type : leaveTypes) {
                BigDecimal accrued;
                BigDecimal taken;
                BigDecimal balance;

                LeaveBalance lb = balanceMap.get(type.getId());

                if (lb != null) {
                    // Use stored balance
                    accrued = lb.getTotalAllocated() != null ? lb.getTotalAllocated() : BigDecimal.ZERO;

                    // Taken usually comes from Balance "used" field which is updated by system
                    // But if we want to be recalculating from requests for robustness:
                    // taken = calculateTakenDays(emp.getId(), allRequests, finalAsOfDate, type);
                    // Let's trust logic to use Balance table if it exists as per "dynamic feature"
                    // logic.
                    taken = lb.getUsed() != null ? lb.getUsed() : BigDecimal.ZERO;

                    // Balance
                    // available() is transient, need to calc
                    BigDecimal pending = lb.getPending() != null ? lb.getPending() : BigDecimal.ZERO;
                    balance = accrued.subtract(taken).subtract(pending);
                    // NOTE: Usually reports show "Available Balance".
                } else {
                    // Fallback Logic
                    if (isAnnualLeave(type)) {
                        accrued = calculateAnnualAccrual(joinDate, finalAsOfDate);
                    } else {
                        accrued = BigDecimal.ZERO;
                    }

                    taken = calculateTakenDays(emp.getId(), allRequests, finalAsOfDate, type);
                    balance = accrued.subtract(taken);
                }

                report.add(LeaveAccrualReportDto.builder()
                        .employeeId(emp.getId())
                        .employeeCode(emp.getEmployeeCode())
                        .employeeName(emp.getFirstName() + " " + emp.getLastName())
                        .department(jobDetails != null ? jobDetails.getDepartment() : "")
                        .designation(jobDetails != null ? jobDetails.getDesignation() : "")
                        .joiningDate(joinDate)
                        .leaveType(type.getLeaveType())
                        .accruedDays(accrued)
                        .takenDays(taken)
                        .balanceDays(balance)
                        .build());
            }
        }
        return report;
    }

    private BigDecimal calculateAnnualAccrual(LocalDate joinDate, LocalDate asOfDate) {
        if (joinDate == null || asOfDate.isBefore(joinDate)) {
            return BigDecimal.ZERO;
        }
        int year = asOfDate.getYear();
        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate calculationStartDate = joinDate.isAfter(startOfYear) ? joinDate : startOfYear;
        long daysActive = ChronoUnit.DAYS.between(calculationStartDate, asOfDate) + 1;
        if (daysActive < 0)
            return BigDecimal.ZERO;
        double daysInYear = startOfYear.lengthOfYear();
        BigDecimal accrued = BigDecimal.valueOf((daysActive / daysInYear) * 30.0);
        return accrued.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTakenDays(Long employeeId, List<LeaveRequest> allRequests, LocalDate asOfDate,
            LeaveType type) {
        int year = asOfDate.getYear();
        return allRequests.stream()
                .filter(req -> req.getEmployee().getId().equals(employeeId))
                .filter(req -> req.getStatus() == LeaveStatus.APPROVED)
                .filter(req -> req.getLeaveType() != null && req.getLeaveType().getId().equals(type.getId()))
                .filter(req -> req.getFromDate().getYear() == year)
                .map(LeaveRequest::getDaysApproved)
                .filter(d -> d != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean isAnnualLeave(LeaveType type) {
        String typeName = type.getLeaveType();
        return typeName != null && (typeName.equalsIgnoreCase("Annual Leave") || typeName.equalsIgnoreCase("Annual")
                || typeName.equalsIgnoreCase("AL"));
    }
}
