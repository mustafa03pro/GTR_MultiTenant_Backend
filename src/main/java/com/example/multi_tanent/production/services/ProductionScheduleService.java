package com.example.multi_tanent.production.services;

import com.example.multi_tanent.config.TenantContext;
import com.example.multi_tanent.production.dto.ProductionScheduleRequest;
import com.example.multi_tanent.production.entity.ManufacturingOrder;
import com.example.multi_tanent.production.entity.ProWorkGroup;
import com.example.multi_tanent.production.entity.ProductionSchedule;
import com.example.multi_tanent.production.repository.ManufacturingOrderRepository;
import com.example.multi_tanent.production.repository.ProWorkGroupRepository;
import com.example.multi_tanent.production.repository.ProductionScheduleRepository;
import com.example.multi_tanent.spersusers.enitity.Employee;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import com.example.multi_tanent.tenant.employee.repository.EmployeeRepository;
import com.example.multi_tanent.spersusers.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional(transactionManager = "tenantTx")
public class ProductionScheduleService {

    private final ProductionScheduleRepository scheduleRepository;
    private final ManufacturingOrderRepository moRepository;
    private final ProWorkGroupRepository workGroupRepository;
    private final EmployeeRepository employeeRepository;
    private final TenantRepository tenantRepository;

    public ProductionScheduleService(ProductionScheduleRepository scheduleRepository,
            ManufacturingOrderRepository moRepository,
            ProWorkGroupRepository workGroupRepository,
            EmployeeRepository employeeRepository,
            TenantRepository tenantRepository) {
        this.scheduleRepository = scheduleRepository;
        this.moRepository = moRepository;
        this.workGroupRepository = workGroupRepository;
        this.employeeRepository = employeeRepository;
        this.tenantRepository = tenantRepository;
    }

    public ProductionSchedule createSchedule(ProductionScheduleRequest request) {
        String tenantId = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        ManufacturingOrder mo = moRepository.findById(request.getManufacturingOrderId())
                .orElseThrow(() -> new RuntimeException("Manufacturing Order not found"));

        ProWorkGroup workGroup = null;
        if (request.getWorkGroupId() != null) {
            workGroup = workGroupRepository.findById(request.getWorkGroupId())
                    .orElseThrow(() -> new RuntimeException("Work Group not found"));
        }

        Employee employee = null;
        if (request.getEmployeeId() != null) {
            employee = employeeRepository.findById(request.getEmployeeId())
                    .orElseThrow(() -> new RuntimeException("Employee not found"));
        }

        ProductionSchedule schedule = ProductionSchedule.builder()
                .tenant(tenant)
                .manufacturingOrder(mo)
                .workGroup(workGroup)
                .employee(employee)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(request.getStatus() != null ? request.getStatus() : "SCHEDULED")
                .notes(request.getNotes())
                .build();

        return scheduleRepository.save(schedule);
    }

    public ProductionSchedule updateSchedule(Long id, ProductionScheduleRequest request) {
        ProductionSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        if (request.getManufacturingOrderId() != null) {
            ManufacturingOrder mo = moRepository.findById(request.getManufacturingOrderId())
                    .orElseThrow(() -> new RuntimeException("Manufacturing Order not found"));
            schedule.setManufacturingOrder(mo);
        }

        if (request.getWorkGroupId() != null) {
            ProWorkGroup workGroup = workGroupRepository.findById(request.getWorkGroupId())
                    .orElseThrow(() -> new RuntimeException("Work Group not found"));
            schedule.setWorkGroup(workGroup);
        } else {
            schedule.setWorkGroup(null);
        }

        if (request.getEmployeeId() != null) {
            Employee employee = employeeRepository.findById(request.getEmployeeId())
                    .orElseThrow(() -> new RuntimeException("Employee not found"));
            schedule.setEmployee(employee);
        } else {
            schedule.setEmployee(null);
        }

        if (request.getStartTime() != null)
            schedule.setStartTime(request.getStartTime());
        if (request.getEndTime() != null)
            schedule.setEndTime(request.getEndTime());
        if (request.getStatus() != null)
            schedule.setStatus(request.getStatus());
        if (request.getNotes() != null)
            schedule.setNotes(request.getNotes());

        return scheduleRepository.save(schedule);
    }

    public void deleteSchedule(Long id) {
        if (!scheduleRepository.existsById(id)) {
            throw new RuntimeException("Schedule not found");
        }
        scheduleRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ProductionSchedule> getSchedules(OffsetDateTime start, OffsetDateTime end, Long workGroupId,
            Long employeeId) {
        String tenantId = TenantContext.getTenantId();
        if (workGroupId != null) {
            return scheduleRepository.findByTenantIdAndWorkGroupIdAndDateRange(tenantId, workGroupId, start, end);
        } else if (employeeId != null) {
            return scheduleRepository.findByTenantIdAndEmployeeIdAndDateRange(tenantId, employeeId, start, end);
        } else {
            return scheduleRepository.findAllByTenantIdAndDateRange(tenantId, start, end);
        }
    }
}
