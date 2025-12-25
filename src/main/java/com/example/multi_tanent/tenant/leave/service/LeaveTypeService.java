package com.example.multi_tanent.tenant.leave.service;

import com.example.multi_tanent.tenant.leave.dto.LeaveTypeRequest;
import com.example.multi_tanent.tenant.leave.entity.*;
import com.example.multi_tanent.tenant.leave.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(transactionManager = "tenantTx")
public class LeaveTypeService {

    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveAllocationRepository leaveAllocationRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTypePolicyRepository leaveTypePolicyRepository;

    public LeaveTypeService(LeaveTypeRepository leaveTypeRepository,
            LeaveBalanceRepository leaveBalanceRepository,
            LeaveAllocationRepository leaveAllocationRepository,
            LeaveRequestRepository leaveRequestRepository,
            LeaveTypePolicyRepository leaveTypePolicyRepository) {
        this.leaveTypeRepository = leaveTypeRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveAllocationRepository = leaveAllocationRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveTypePolicyRepository = leaveTypePolicyRepository;
    }

    public LeaveType createLeaveType(LeaveTypeRequest request) {
        Optional<LeaveType> existing = leaveTypeRepository.findByLeaveTypeIgnoreCase(request.getLeaveType());
        if (existing.isPresent()) {
            if (existing.get().isActive()) {
                throw new IllegalStateException(
                        "An active leave type with name '" + request.getLeaveType() + "' already exists.");
            }
            // If it's inactive, reactivate and update it directly.
            LeaveType leaveTypeToUpdate = existing.get();
            mapRequestToEntity(request, leaveTypeToUpdate);
            leaveTypeToUpdate.setActive(true);
            return leaveTypeRepository.save(leaveTypeToUpdate);
        }

        LeaveType leaveType = new LeaveType();
        mapRequestToEntity(request, leaveType);
        return leaveTypeRepository.save(leaveType);
    }

    @Transactional(readOnly = true)
    public List<LeaveType> getAllLeaveTypes() {
        return leaveTypeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<LeaveType> getActiveLeaveTypes() {
        return leaveTypeRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public Optional<LeaveType> getLeaveTypeById(Long id) {
        return leaveTypeRepository.findById(id);
    }

    public LeaveType updateLeaveType(Long id, LeaveTypeRequest request) {
        LeaveType leaveType = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave type not found with id: " + id));

        // If name is being changed, check if the new name already exists
        if (!leaveType.getLeaveType().equalsIgnoreCase(request.getLeaveType())) {
            leaveTypeRepository.findByLeaveTypeIgnoreCase(request.getLeaveType()).ifPresent(lt -> {
                throw new IllegalStateException(
                        "Another leave type with name '" + request.getLeaveType() + "' already exists.");
            });
        }

        mapRequestToEntity(request, leaveType);
        leaveType.setActive(true); // Ensure it's active on update
        return leaveTypeRepository.save(leaveType);
    }

    public void deleteLeaveType(Long id) {
        LeaveType leaveType = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("LeaveType not found with id: " + id));

        if (leaveTypePolicyRepository.existsByLeaveTypeId(id)) {
            throw new DataIntegrityViolationException(
                    "Cannot deactivate this leave type. It is currently in use in one or more leave policies.");
        }

        leaveType.setActive(false);
        leaveTypeRepository.save(leaveType);
    }

    private void mapRequestToEntity(LeaveTypeRequest request, LeaveType leaveType) {
        leaveType.setLeaveType(request.getLeaveType());
        leaveType.setDescription(request.getDescription());
        leaveType.setIsPaid(request.getIsPaid());
        leaveType.setMaxDaysPerYear(request.getMaxDaysPerYear());
        leaveType.setStartTime(request.getStartTime());
        leaveType.setEndTime(request.getEndTime());
    }

    // public List<LeaveType> getActiveLeaveTypes() {
    // // TODO Auto-generated method stub
    // throw new UnsupportedOperationException("Unimplemented method
    // 'getActiveLeaveTypes'");
    // }
}