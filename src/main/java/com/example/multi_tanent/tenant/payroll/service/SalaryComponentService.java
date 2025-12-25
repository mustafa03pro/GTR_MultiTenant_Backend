package com.example.multi_tanent.tenant.payroll.service;

import com.example.multi_tanent.tenant.payroll.dto.SalaryComponentRequest;
import com.example.multi_tanent.tenant.payroll.entity.SalaryComponent;
import com.example.multi_tanent.tenant.payroll.repository.SalaryComponentRepository;
import com.example.multi_tanent.tenant.payroll.repository.SalaryStructureComponentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(transactionManager = "tenantTx")
public class SalaryComponentService {

    private final SalaryComponentRepository salaryComponentRepository;
    private final SalaryStructureComponentRepository salaryStructureComponentRepository;

    public SalaryComponentService(SalaryComponentRepository salaryComponentRepository,
            SalaryStructureComponentRepository salaryStructureComponentRepository) {
        this.salaryComponentRepository = salaryComponentRepository;
        this.salaryStructureComponentRepository = salaryStructureComponentRepository;
    }

    public SalaryComponent createSalaryComponent(SalaryComponentRequest request) {
        SalaryComponent component = new SalaryComponent();
        component.setName(request.getName());
        component.setCode(request.getCode());
        component.setType(request.getType());
        component.setCalculationType(request.getCalculationType());
        component.setIsTaxable(request.getIsTaxable());
        component.setIsPartOfGrossSalary(request.getIsPartOfGrossSalary());
        component.setIsWpsIncluded(request.getIsWpsIncluded() != null ? request.getIsWpsIncluded() : true);
        component.setIsVariable(request.getIsVariable() != null ? request.getIsVariable() : false);
        return salaryComponentRepository.save(component);
    }

    @Transactional(readOnly = true)
    public List<SalaryComponent> getAllSalaryComponents() {
        return salaryComponentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<SalaryComponent> getSalaryComponentById(Long id) {
        return salaryComponentRepository.findById(id);
    }

    public SalaryComponent updateSalaryComponent(Long id, SalaryComponentRequest request) {
        SalaryComponent component = salaryComponentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SalaryComponent not found with id: " + id));
        component.setName(request.getName());
        component.setCode(request.getCode());
        component.setType(request.getType());
        component.setCalculationType(request.getCalculationType());
        component.setIsTaxable(request.getIsTaxable());
        component.setIsPartOfGrossSalary(request.getIsPartOfGrossSalary());
        component.setIsWpsIncluded(request.getIsWpsIncluded() != null ? request.getIsWpsIncluded() : true);
        component.setIsVariable(request.getIsVariable() != null ? request.getIsVariable() : false);
        return salaryComponentRepository.save(component);
    }

    public void deleteSalaryComponent(Long id) {
        if (salaryStructureComponentRepository.existsBySalaryComponentId(id)) {
            throw new IllegalStateException(
                    "Cannot delete salary component. It is currently used in one or more salary structures.");
        }
        salaryComponentRepository.deleteById(id);
    }
}