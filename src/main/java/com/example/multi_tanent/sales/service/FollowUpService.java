package com.example.multi_tanent.sales.service;

import com.example.multi_tanent.config.TenantContext;
import com.example.multi_tanent.sales.dto.FollowUpRequest;
import com.example.multi_tanent.sales.dto.FollowUpResponse;
import com.example.multi_tanent.sales.entity.FollowUp;
import com.example.multi_tanent.sales.entity.Quotation;
import com.example.multi_tanent.sales.enums.SalesStatus;
import com.example.multi_tanent.sales.repository.FollowUpRepository;
import com.example.multi_tanent.sales.repository.QuotationRepository;
import com.example.multi_tanent.sales.entity.RentalQuotation;
import com.example.multi_tanent.sales.repository.RentalQuotationRepository;
import com.example.multi_tanent.spersusers.enitity.Employee;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import com.example.multi_tanent.spersusers.repository.TenantRepository;
import com.example.multi_tanent.tenant.employee.repository.EmployeeRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowUpService {

    private final FollowUpRepository repository;
    private final QuotationRepository quotationRepository;
    private final RentalQuotationRepository rentalQuotationRepository;
    private final EmployeeRepository employeeRepository;
    private final TenantRepository tenantRepository;

    @Transactional
    public FollowUpResponse create(FollowUpRequest request) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        FollowUp entity = new FollowUp();
        entity.setTenant(tenant);
        mapRequestToEntity(request, entity, tenant.getId());

        FollowUp saved = repository.save(entity);
        return mapEntityToResponse(saved);
    }

    @Transactional
    public FollowUpResponse update(Long id, FollowUpRequest request) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        FollowUp entity = repository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("FollowUp not found with id: " + id));

        mapRequestToEntity(request, entity, tenant.getId());

        FollowUp saved = repository.save(entity);
        return mapEntityToResponse(saved);
    }

    public List<FollowUpResponse> getAll() {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        return repository.findByTenant_Id(tenant.getId()).stream()
                .map(this::mapEntityToResponse)
                .collect(Collectors.toList());
    }

    public FollowUpResponse getById(Long id) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        FollowUp entity = repository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("FollowUp not found with id: " + id));
        return mapEntityToResponse(entity);
    }

    public List<FollowUpResponse> getByQuotationId(Long quotationId) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        return repository.findByQuotation_Id(quotationId).stream()
                .filter(f -> f.getTenant().getId().equals(tenant.getId()))
                .map(this::mapEntityToResponse)
                .collect(Collectors.toList());
    }

    public List<FollowUpResponse> getByRentalQuotationId(Long rentalQuotationId) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        return repository.findByRentalQuotation_Id(rentalQuotationId).stream()
                .filter(f -> f.getTenant().getId().equals(tenant.getId()))
                .map(this::mapEntityToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        FollowUp entity = repository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("FollowUp not found with id: " + id));

        repository.delete(entity);
    }

    private void mapRequestToEntity(FollowUpRequest req, FollowUp entity, Long tenantId) {
        entity.setNextFollowupDate(req.getNextFollowupDate());
        entity.setNextFollowupTime(req.getNextFollowupTime());
        entity.setComment(req.getComment());

        if (req.getQuotationStatus() != null) {
            try {
                entity.setQuotationStatus(SalesStatus.valueOf(req.getQuotationStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                entity.setQuotationStatus(SalesStatus.DRAFT);
            }
        }

        if (req.getQuotationId() != null) {
            Quotation quotation = quotationRepository.findByIdAndTenantId(req.getQuotationId(), tenantId)
                    .orElseThrow(() -> new EntityNotFoundException("Quotation not found"));
            entity.setQuotation(quotation);
        }

        if (req.getRentalQuotationId() != null) {
            RentalQuotation rentalQuotation = rentalQuotationRepository
                    .findByIdAndTenantId(req.getRentalQuotationId(), tenantId)
                    .orElseThrow(() -> new EntityNotFoundException("Rental Quotation not found"));
            entity.setRentalQuotation(rentalQuotation);
        }

        if (req.getEmployeeId() != null) {
            Employee employee = employeeRepository.findByIdAndUser_Tenant_Id(req.getEmployeeId(), tenantId)
                    .orElse(null);
            entity.setEmployee(employee);
        }
    }

    private FollowUpResponse mapEntityToResponse(FollowUp entity) {
        return FollowUpResponse.builder()
                .id(entity.getId())
                .tenantId(entity.getTenant().getId())
                .quotationId(entity.getQuotation() != null ? entity.getQuotation().getId() : null)
                .quotationNumber(entity.getQuotation() != null ? entity.getQuotation().getQuotationNumber() : null)
                .rentalQuotationId(entity.getRentalQuotation() != null ? entity.getRentalQuotation().getId() : null)
                .rentalQuotationNumber(
                        entity.getRentalQuotation() != null ? entity.getRentalQuotation().getQuotationNumber() : null)
                .nextFollowupDate(entity.getNextFollowupDate())
                .nextFollowupTime(entity.getNextFollowupTime())
                .quotationStatus(entity.getQuotationStatus())
                .comment(entity.getComment())
                .employeeId(entity.getEmployee() != null ? entity.getEmployee().getId() : null)
                .employeeName(entity.getEmployee() != null ? entity.getEmployee().getName() : null)
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
