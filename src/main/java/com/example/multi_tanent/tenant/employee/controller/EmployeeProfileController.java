package com.example.multi_tanent.tenant.employee.controller;

import com.example.multi_tanent.tenant.employee.dto.EmployeeProfileRequest;
import com.example.multi_tanent.tenant.employee.entity.EmployeeProfile;
import com.example.multi_tanent.tenant.employee.repository.EmployeeProfileRepository;
import com.example.multi_tanent.tenant.employee.repository.EmployeeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/employee-profiles")
@CrossOrigin(origins = "*")
@Transactional(transactionManager = "tenantTx")
public class EmployeeProfileController {

    private final EmployeeRepository employeeRepository;
    private final EmployeeProfileRepository employeeProfileRepository;

    public EmployeeProfileController(EmployeeRepository employeeRepository,
            EmployeeProfileRepository employeeProfileRepository) {
        this.employeeRepository = employeeRepository;
        this.employeeProfileRepository = employeeProfileRepository;
    }

    @PutMapping("/{employeeCode}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HRMS_ADMIN','HR','MANAGER')")
    public ResponseEntity<EmployeeProfile> createOrUpdateEmployeeProfile(@PathVariable String employeeCode,
            @RequestBody EmployeeProfileRequest request) {
        return employeeRepository.findByEmployeeCode(employeeCode)
                .map(employee -> {
                    EmployeeProfile profile = employeeProfileRepository.findByEmployeeId(employee.getId())
                            .orElse(new EmployeeProfile());

                    boolean isNew = profile.getId() == null;
                    if (isNew) {
                        profile.setEmployee(employee);
                    }

                    updateProfileFromRequest(profile, request);
                    EmployeeProfile savedProfile = employeeProfileRepository.save(profile);

                    if (isNew) {
                        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                                .path("/api/employee-profiles/{employeeCode}")
                                .buildAndExpand(employeeCode).toUri();
                        return ResponseEntity.created(location).body(savedProfile);
                    } else {
                        return ResponseEntity.ok(savedProfile);
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{employeeCode}")
    public ResponseEntity<EmployeeProfile> getEmployeeProfile(@PathVariable String employeeCode) {
        return employeeRepository.findByEmployeeCode(employeeCode)
                .flatMap(employee -> employeeProfileRepository.findByEmployeeId(employee.getId()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private void updateProfileFromRequest(EmployeeProfile profile, EmployeeProfileRequest request) {
        profile.setAddress(request.getAddress());
        profile.setCity(request.getCity());
        profile.setState(request.getState());
        profile.setCountry(request.getCountry());
        profile.setPostalCode(request.getPostalCode());
        profile.setEmergencyContactName(request.getEmergencyContactName());
        profile.setEmergencyContactRelation(request.getEmergencyContactRelation());
        profile.setEmergencyContactPhone(request.getEmergencyContactPhone());
        profile.setLaborCardNumber(request.getLaborCardNumber());
        profile.setJobTitle(request.getJobTitle());
        profile.setDepartment(request.getDepartment());
        profile.setHireDate(request.getHireDate());
        profile.setWpsRegistered(request.isWpsRegistered());
        profile.setIban(request.getIban());
        profile.setRoutingCode(request.getRoutingCode());
        profile.setBankName(request.getBankName());
        profile.setBankAccountNumber(request.getBankAccountNumber());
        profile.setIfscCode(request.getIfscCode());
        profile.setBloodGroup(request.getBloodGroup());
        profile.setNotes(request.getNotes());
        profile.setPreferredName(request.getPreferredName());
        profile.setJobType(request.getJobType());
        profile.setOffice(request.getOffice());
        profile.setMolId(request.getMolId());
        profile.setPaymentMethod(request.getPaymentMethod());
        profile.setLaborCardExpiry(request.getLaborCardExpiry());
        profile.setNationality(request.getNationality());
    }
}
