package com.example.multi_tanent.tenant.attendance.service;

import com.example.multi_tanent.spersusers.enitity.Employee;
import com.example.multi_tanent.spersusers.enums.EmployeeStatus;
import com.example.multi_tanent.tenant.attendance.dto.AttendanceRecordResponse;
import com.example.multi_tanent.tenant.attendance.dto.AttendanceRecordRequest;
import com.example.multi_tanent.tenant.attendance.dto.BiometricPunchRequest;
import com.example.multi_tanent.tenant.attendance.entity.*;
import com.example.multi_tanent.tenant.attendance.enums.AttendanceStatus;
import com.example.multi_tanent.tenant.attendance.repository.AttendanceRecordRepository;
import com.example.multi_tanent.tenant.attendance.repository.AttendancePolicyRepository;
import com.example.multi_tanent.tenant.attendance.repository.AttendanceSettingRepository;
import com.example.multi_tanent.tenant.attendance.repository.BiometricDeviceRepository;
import com.example.multi_tanent.tenant.attendance.repository.EmployeeBiometricMappingRepository;
import com.example.multi_tanent.tenant.employee.entity.TimeAttendence;
import com.example.multi_tanent.tenant.employee.repository.TimeAttendenceRepository;
import com.example.multi_tanent.tenant.employee.repository.EmployeeRepository;
import com.example.multi_tanent.tenant.leave.repository.HolidayPolicyRepository;
import com.example.multi_tanent.tenant.leave.repository.LeaveRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityNotFoundException;

import java.time.Duration;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(transactionManager = "tenantTx")
public class AttendanceRecordService {

    private final AttendanceRecordRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendancePolicyRepository attendancePolicyRepository;
    private final AttendanceSettingRepository attendanceSettingRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeBiometricMappingRepository mappingRepository;
    private final BiometricDeviceRepository deviceRepository;
    private final TimeAttendenceRepository timeAttendenceRepository;
    private final HolidayPolicyRepository holidayPolicyRepository;

    public AttendanceRecordService(AttendanceRecordRepository attendanceRepository,
            EmployeeRepository employeeRepository,
            AttendancePolicyRepository attendancePolicyRepository,
            AttendanceSettingRepository attendanceSettingRepository,
            LeaveRequestRepository leaveRequestRepository,
            EmployeeBiometricMappingRepository mappingRepository,
            BiometricDeviceRepository deviceRepository,
            TimeAttendenceRepository timeAttendenceRepository,
            HolidayPolicyRepository holidayPolicyRepository) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.attendancePolicyRepository = attendancePolicyRepository;
        this.attendanceSettingRepository = attendanceSettingRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.mappingRepository = mappingRepository;
        this.deviceRepository = deviceRepository;
        this.timeAttendenceRepository = timeAttendenceRepository;
        this.holidayPolicyRepository = holidayPolicyRepository;
    }

    public AttendanceRecordResponse markAttendance(AttendanceRecordRequest request) {
        attendanceRepository
                .findByEmployeeEmployeeCodeAndAttendanceDate(request.getEmployeeCode(), request.getAttendanceDate())
                .ifPresent(rec -> {
                    throw new RuntimeException("Attendance record for employee " + request.getEmployeeCode() + " on "
                            + request.getAttendanceDate() + " already exists.");
                });

        Employee employee = employeeRepository.findByEmployeeCode(request.getEmployeeCode())
                .orElseThrow(() -> new RuntimeException("Employee not found with code: " + request.getEmployeeCode()));

        AttendanceRecord record = new AttendanceRecord();
        record.setEmployee(employee);
        record.setAttendanceDate(request.getAttendanceDate());
        record.setCheckIn(request.getCheckIn());
        record.setCheckOut(request.getCheckOut());
        record.setRemarks(request.getRemarks());

        // If status is provided manually (e.g., ON_LEAVE, ABSENT), use it. Otherwise,
        // determine from check-in.
        if (request.getStatus() != null) {
            record.setStatus(request.getStatus());
            updatePayableDays(record);
        } else {
            record.setStatus(request.getCheckIn() != null ? AttendanceStatus.PRESENT : AttendanceStatus.ABSENT);
        }

        // Apply shift logic only if the employee is present
        if (record.getStatus() == AttendanceStatus.PRESENT) {
            applyAttendancePolicyLogic(record, request.getAttendancePolicyId());
        }

        AttendanceRecord savedRecord = attendanceRepository.save(record);
        return AttendanceRecordResponse.fromEntity(savedRecord);
    }

    public AttendanceRecordResponse updateAttendance(Long id, AttendanceRecordRequest request) {
        AttendanceRecord record = attendanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attendance record not found with id: " + id));

        record.setCheckIn(request.getCheckIn());
        record.setCheckOut(request.getCheckOut());
        record.setRemarks(request.getRemarks());

        if (request.getStatus() != null) {
            record.setStatus(request.getStatus());
            updatePayableDays(record);
        } else {
            record.setStatus(request.getCheckIn() != null ? AttendanceStatus.PRESENT : AttendanceStatus.ABSENT);
        }

        // Reset calculated fields before reapplying logic
        record.setIsLate(false);
        record.setOvertimeMinutes(0);
        // Also reset status to a baseline before policy logic, which might override it
        // (e.g., to HALF_DAY).
        if (record.getStatus() != AttendanceStatus.ABSENT && record.getStatus() != AttendanceStatus.ON_LEAVE) {
            record.setStatus(AttendanceStatus.PRESENT);
        }

        if (record.getStatus() == AttendanceStatus.PRESENT) {
            applyAttendancePolicyLogic(record, request.getAttendancePolicyId());
            updatePayableDays(record);
        } else {
            record.setAttendancePolicy(null);
        }

        AttendanceRecord updatedRecord = attendanceRepository.save(record);
        return AttendanceRecordResponse.fromEntity(updatedRecord);
    }

    private void applyAttendancePolicyLogic(AttendanceRecord record, Long overrideAttendancePolicyId) {
        AttendancePolicy attendancePolicy = findAttendancePolicy(overrideAttendancePolicyId, record.getEmployee());

        if (attendancePolicy == null || attendancePolicy.getShiftPolicy() == null
                || attendancePolicy.getCapturingPolicy() == null) {
            // Not enough policy information to calculate late status or overtime.
            return;
        }

        record.setAttendancePolicy(attendancePolicy);
        ShiftPolicy shiftPolicy = attendancePolicy.getShiftPolicy();
        AttendanceCapturingPolicy capturingPolicy = attendancePolicy.getCapturingPolicy();
        LeaveDeductionConfig leaveConfig = attendancePolicy.getLeaveDeductionConfig();

        // Reset status to PRESENT before checks. This ensures that if a record was
        // previously
        // HALF_DAY and is now corrected, it reverts to PRESENT unless the half-day
        // condition still applies.
        // We only do this if the status is not manually set to something else like
        // ABSENT.
        record.setStatus(AttendanceStatus.PRESENT);

        // Calculate Late Status
        if (record.getCheckIn() != null) {
            LocalTime lateThreshold = shiftPolicy.getShiftStartTime()
                    .plusMinutes(capturingPolicy.getGraceTimeMinutes());
            if (record.getCheckIn().isAfter(lateThreshold)) {
                record.setIsLate(true);
            }
        }

        // Calculate Half_day Status
        if (record.getCheckIn() != null) {
            LocalTime halfDayThreshold = shiftPolicy.getShiftStartTime()
                    .plusMinutes(capturingPolicy.getHalfDayThresholdMinutes());
            if (record.getCheckIn().isAfter(halfDayThreshold)) {
                record.setStatus(AttendanceStatus.HALF_DAY);
            }
        }

        // Calculate Early Going (if configured)
        if (leaveConfig != null && Boolean.TRUE.equals(leaveConfig.getPenalizeEarlyGoing())
                && record.getCheckOut() != null) {
            if (record.getCheckOut().isBefore(shiftPolicy.getShiftEndTime())) {
                // You can add more specific logic here, like marking it as a half-day
                // or just flagging it. For now, we'll add a remark.
                String remarks = record.getRemarks() == null ? "" : record.getRemarks() + " ";
                record.setRemarks(remarks + "Marked for early going.");
            }
        }

        // Calculate Overtime
        if (record.getCheckOut() != null && record.getCheckOut().isAfter(shiftPolicy.getShiftEndTime())) {
            long overtime = Duration.between(shiftPolicy.getShiftEndTime(), record.getCheckOut()).toMinutes();
            record.setOvertimeMinutes((int) overtime);
        }
    }

    private void updatePayableDays(AttendanceRecord record) {
        switch (record.getStatus()) {
            case PRESENT:
            case ON_LEAVE: // Assuming ON_LEAVE is paid, handled by payroll logic later
            case HOLIDAY:
            case WEEKLY_OFF:
                record.setPayableDays(BigDecimal.ONE);
                break;
            case HALF_DAY:
                record.setPayableDays(new BigDecimal("0.5"));
                break;
            case ABSENT:
                // Check if it's an unpaid leave day
                boolean isUnpaidLeave = leaveRequestRepository.findByEmployeeId(record.getEmployee().getId()).stream()
                        .anyMatch(leave -> leave
                                .getStatus() == com.example.multi_tanent.tenant.leave.enums.LeaveStatus.APPROVED &&
                                !record.getAttendanceDate().isBefore(leave.getFromDate())
                                && !record.getAttendanceDate().isAfter(leave.getToDate()) &&
                                leave.getLeaveType() != null && !leave.getLeaveType().getIsPaid());
                record.setPayableDays(isUnpaidLeave ? BigDecimal.ZERO : BigDecimal.ONE); // If absent but not on unpaid
                                                                                         // leave, it might be a
                                                                                         // different issue. Payroll can
                                                                                         // decide. For now, let's
                                                                                         // assume it's payable unless
                                                                                         // explicitly unpaid.
                break;
            default:
                record.setPayableDays(BigDecimal.ZERO);
        }
    }

    private AttendancePolicy findAttendancePolicy(Long overrideAttendancePolicyId, Employee employee) {
        // Priority: Override ID > Employee's assigned policy > Default policy
        return Optional.ofNullable(overrideAttendancePolicyId)
                .flatMap(attendancePolicyRepository::findById)
                .or(() -> timeAttendenceRepository.findByEmployeeId(employee.getId())
                        .map(TimeAttendence::getAttendancePolicy))
                .or(attendancePolicyRepository::findByIsDefaultTrue)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Optional<AttendanceRecord> getAttendanceRecordById(Long id) {
        return attendanceRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<AttendanceRecord> getAttendanceForEmployee(String employeeCode, LocalDate startDate,
            LocalDate endDate) {
        employeeRepository.findByEmployeeCode(employeeCode)
                .orElseThrow(() -> new RuntimeException("Employee not found with code: " + employeeCode));
        return attendanceRepository.findByEmployeeEmployeeCodeAndAttendanceDateBetweenWithDetails(employeeCode,
                startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<AttendanceRecord> getAllAttendanceRecords() {
        return attendanceRepository.findAllWithDetails();
    }

    public void deleteAttendanceRecord(Long id) {
        if (!attendanceRepository.existsById(id)) {
            throw new RuntimeException("Attendance record not found with id: " + id);
        }
        attendanceRepository.deleteById(id);
    }

    /**
     * Processes an attendance punch from a biometric device.
     * It finds the corresponding employee and creates or updates their attendance
     * record for the day.
     *
     * @param punchRequest The data from the biometric device.
     * @return The saved AttendanceRecord.
     */
    public AttendanceRecord processBiometricPunch(BiometricPunchRequest punchRequest) {
        // 1. Find device
        BiometricDevice device = deviceRepository.findByDeviceIdentifier(punchRequest.getDeviceIdentifier())
                .orElseThrow(() -> new RuntimeException(
                        "Biometric device with identifier '" + punchRequest.getDeviceIdentifier() + "' not found."));

        // 2. Find employee mapping for the given device
        EmployeeBiometricMapping mapping = mappingRepository
                .findByBiometricIdentifierAndDeviceId(punchRequest.getBiometricIdentifier(), device.getId())
                .orElseThrow(() -> new RuntimeException(
                        "No employee mapping found for biometric ID '" + punchRequest.getBiometricIdentifier()
                                + "' on device '" + device.getDeviceIdentifier() + "'."));

        if (!Boolean.TRUE.equals(mapping.getActive())) {
            throw new RuntimeException(
                    "Employee mapping is inactive for biometric ID: " + punchRequest.getBiometricIdentifier());
        }

        Employee employee = mapping.getEmployee();
        LocalDateTime punchTime = punchRequest.getPunchTime();
        LocalTime punchLocalTime = punchTime.toLocalTime();

        // Determine the correct attendance date, accounting for overnight shifts.
        // We'll use a "day cutoff" time. Punches before this time belong to the
        // previous day.
        // This could be made configurable in AttendanceSetting in the future.
        final LocalTime DAY_CUTOFF = LocalTime.of(5, 0); // 5:00 AM

        LocalDate punchDate;
        if (punchLocalTime.isBefore(DAY_CUTOFF)) {
            // This punch belongs to the previous day's shift
            punchDate = punchTime.toLocalDate().minusDays(1);
        } else {
            punchDate = punchTime.toLocalDate();
        }

        boolean onLeave = leaveRequestRepository.findByEmployeeId(employee.getId()).stream()
                .anyMatch(leave -> leave.getStatus() == com.example.multi_tanent.tenant.leave.enums.LeaveStatus.APPROVED
                        &&
                        !punchDate.isBefore(leave.getFromDate()) && !punchDate.isAfter(leave.getToDate()));

        if (onLeave) {
            throw new IllegalStateException("Cannot process punch. Employee " + employee.getEmployeeCode()
                    + " is on approved leave for " + punchDate);
        }
        // Find or create an attendance record for the determined attendance date
        AttendanceRecord record = attendanceRepository
                .findByEmployeeEmployeeCodeAndAttendanceDate(employee.getEmployeeCode(), punchDate)
                .orElse(new AttendanceRecord());

        // 4. Logic for check-in vs check-out. First punch is check-in, subsequent
        // punches update check-out.
        if (record.getId() == null) { // This is a new record for the day (first punch)
            record.setEmployee(employee);
            record.setAttendanceDate(punchDate);
            record.setCheckIn(punchLocalTime);
            record.setStatus(AttendanceStatus.PRESENT);
            applyAttendancePolicyLogic(record, null); // Apply default attendance policy
            updatePayableDays(record);
        } else { // This is a subsequent punch for the day
            record.setCheckOut(punchLocalTime);
            // Re-apply logic to calculate overtime if applicable
            applyAttendancePolicyLogic(record,
                    record.getAttendancePolicy() != null ? record.getAttendancePolicy().getId() : null);
        }

        return attendanceRepository.save(record);
    }

    /**
     * Scheduled task to automatically mark employees as absent if they haven't
     * checked in.
     * This runs daily at a configured time (e.g., 6 PM).
     * Note: For this to work in a multi-tenant environment, the scheduler must be
     * adapted to
     * iterate over all tenants and execute this logic within each tenant's context.
     */
    public void autoMarkAbsentEmployees() {
        autoMarkAbsentEmployees(LocalDate.now());
    }

    /**
     * Scheduled task to automatically mark employees as absent if they haven't
     * checked in for a specific date.
     * This can be run manually or by a scheduler.
     * Note: For this to work in a multi-tenant environment, the scheduler must be
     * adapted to
     * iterate over all tenants and execute this logic within each tenant's context.
     *
     * @param date The date for which to mark absentees.
     */
    public void autoMarkAbsentEmployees(LocalDate date) {
        Optional<AttendanceSetting> settingOpt = attendanceSettingRepository.findAll().stream().findFirst();
        if (settingOpt.isEmpty() || !Boolean.TRUE.equals(settingOpt.get().getAutoMarkAbsentAfter())) {
            throw new IllegalStateException("Auto-marking absent employees is disabled for this tenant.");
        }

        List<Employee> activeEmployees = employeeRepository.findByStatus(EmployeeStatus.ACTIVE);

        for (Employee employee : activeEmployees) {
            boolean recordExists = attendanceRepository
                    .findByEmployeeEmployeeCodeAndAttendanceDate(employee.getEmployeeCode(), date).isPresent();

            if (recordExists) {
                continue; // Skip if a record already exists for this employee on this date.
            }

            // Before marking absent, check if the employee is on an approved leave.
            boolean onPaidLeave = leaveRequestRepository.findByEmployeeId(employee.getId()).stream()
                    .anyMatch(leave -> leave
                            .getStatus() == com.example.multi_tanent.tenant.leave.enums.LeaveStatus.APPROVED &&
                            leave.getLeaveType() != null && Boolean.TRUE.equals(leave.getLeaveType().getIsPaid()) &&
                            !date.isBefore(leave.getFromDate()) && !date.isAfter(leave.getToDate()));

            // Check if today is a weekly off day for the employee.
            Optional<TimeAttendence> timeAttendenceOpt = timeAttendenceRepository.findByEmployeeId(employee.getId());
            boolean isWeeklyOff = timeAttendenceOpt.map(TimeAttendence::getWeeklyOffPolicy)
                    .map(policy -> policy.getOffDays().contains(date.getDayOfWeek()))
                    .orElse(false);

            // Check if today is a holiday for the employee.
            boolean isHoliday = timeAttendenceOpt.map(TimeAttendence::getHolidayList)
                    .flatMap(holidayPolicyRepository::findByName)
                    .map(policy -> policy.getHolidays().stream()
                            .anyMatch(holiday -> holiday.getDate().equals(date) && !holiday.isOptional()))
                    .orElse(false);

            AttendanceRecord newRecord = new AttendanceRecord();
            newRecord.setEmployee(employee);
            newRecord.setAttendanceDate(date);

            if (onPaidLeave) {
                newRecord.setStatus(AttendanceStatus.ON_LEAVE);
                newRecord.setRemarks("Auto-marked as ON LEAVE based on approved leave.");
            } else if (isWeeklyOff) {
                newRecord.setStatus(AttendanceStatus.WEEKLY_OFF);
                newRecord.setRemarks("Auto-marked as WEEKLY OFF.");
            } else if (isHoliday) {
                newRecord.setStatus(AttendanceStatus.HOLIDAY);
                newRecord.setRemarks("Auto-marked as HOLIDAY.");
            } else {
                newRecord.setStatus(AttendanceStatus.ABSENT);
                newRecord.setRemarks("Auto-marked as ABSENT. No check-in recorded.");
            }
            updatePayableDays(newRecord);
            attendanceRepository.save(newRecord);
        }
    }

    public List<AttendanceRecord> getAttendanceForDate(LocalDate date) {
        String tenantId = com.example.multi_tanent.config.TenantContext.getTenantId();
        return attendanceRepository.findAllByTenantIdAndDateWithDetails(tenantId, date);
    }

    /**
     * Recalculates all derived fields for an existing attendance record, such as
     * late status,
     * overtime, and payable days, based on its current check-in/check-out times and
     * the
     * applicable attendance policies.
     *
     * @param attendanceRecordId The ID of the AttendanceRecord to recalculate.
     * @return The updated and saved AttendanceRecord.
     */
    public AttendanceRecord recalculateAttendance(Long attendanceRecordId) {
        AttendanceRecord record = attendanceRepository.findById(attendanceRecordId)
                .orElseThrow(
                        () -> new EntityNotFoundException("AttendanceRecord not found with id: " + attendanceRecordId));

        // Reset all calculated fields to their default state before re-evaluation.
        record.setIsLate(false);
        record.setOvertimeMinutes(0);

        // Determine the base status. If there's no check-in, it's ABSENT.
        // Otherwise, start with PRESENT, and let the policy logic potentially change it
        // to HALF_DAY.
        if (record.getCheckIn() == null) {
            record.setStatus(AttendanceStatus.ABSENT);
            record.setAttendancePolicy(null); // No policy applies if absent
        } else {
            record.setStatus(AttendanceStatus.PRESENT);
            // Re-apply all attendance policies (late, half-day, overtime, etc.)
            applyAttendancePolicyLogic(record, null);
        }

        updatePayableDays(record);
        return attendanceRepository.save(record);
    }
}