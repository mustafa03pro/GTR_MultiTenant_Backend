package com.example.multi_tanent.crm.services;

import com.example.multi_tanent.config.TenantContext;
import com.example.multi_tanent.crm.dto.CrmLeadRequest;
import com.example.multi_tanent.crm.enums.CrmLeadStatus;
import com.example.multi_tanent.crm.dto.CrmLeadResponse;
import com.example.multi_tanent.crm.entity.*;
import com.example.multi_tanent.crm.repository.*;
import com.example.multi_tanent.spersusers.enitity.Employee;
import com.example.multi_tanent.spersusers.enitity.Location;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import com.example.multi_tanent.spersusers.repository.LocationRepository;
import com.example.multi_tanent.crm.repository.LeadSourceRepository;
import com.example.multi_tanent.spersusers.repository.TenantRepository;
import com.example.multi_tanent.tenant.employee.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional("tenantTx")
public class CrmLeadService {

    private final CrmLeadRepository leadRepository;
    private final TenantRepository tenantRepository;
    private final CrmCompanyRepository companyRepository;
    private final CrmIndustryRepository industryRepository;
    private final CrmProductRepository productRepository;
    private final EmployeeRepository employeeRepository;
    private final CrmLeadStageRepository leadStageRepository;
    private final LocationRepository locationRepository;
    private final ContactService contactService;
    private final LeadSourceRepository leadSourceRepository;

    private Tenant currentTenant() {
        String tenantId = TenantContext.getTenantId();
        return tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant not found for tenantId: " + tenantId));
    }

    public CrmLeadResponse createLead(CrmLeadRequest request) {
        Tenant tenant = currentTenant();
        CrmLead lead = new CrmLead();
        mapReqToEntity(request, lead);

        if (leadRepository.existsByTenantIdAndLeadNo(tenant.getId(), request.getLeadNo())) {
            throw new IllegalArgumentException(
                    "Lead with number '" + request.getLeadNo() + "' already exists for this tenant.");
        }

        lead.setTenant(tenant);
        CrmLead savedLead = leadRepository.save(lead);

        // Use the ContactService to create or update the contact from the lead
        contactService.createOrUpdateContactFromLead(savedLead);

        return toResponse(savedLead);
    }

    @Transactional(readOnly = true)
    public Page<CrmLeadResponse> getAllLeads(Pageable pageable) {
        return leadRepository.findByTenantId(currentTenant().getId(), pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CrmLeadResponse getLeadById(Long id) {
        return leadRepository.findByIdAndTenantId(id, currentTenant().getId())
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Lead not found with id: " + id));
    }

    public CrmLeadResponse updateLead(Long id, CrmLeadRequest request) {
        CrmLead lead = leadRepository.findByIdAndTenantId(id, currentTenant().getId())
                .orElseThrow(() -> new EntityNotFoundException("Lead not found with id: " + id));
        mapReqToEntity(request, lead);

        if (!lead.getLeadNo().equalsIgnoreCase(request.getLeadNo())
                && leadRepository.existsByTenantIdAndLeadNo(currentTenant().getId(), request.getLeadNo())) {
            throw new IllegalArgumentException(
                    "Lead with number '" + request.getLeadNo() + "' already exists for this tenant.");
        }

        CrmLead updatedLead = leadRepository.save(lead);

        // Also update the associated contact
        contactService.createOrUpdateContactFromLead(updatedLead);

        return toResponse(updatedLead);
    }

    public void deleteLead(Long id) {
        if (!leadRepository.existsById(id)) {
            throw new EntityNotFoundException("Lead not found with id: " + id);
        }
        leadRepository.deleteById(id);
    }

    public CrmLeadResponse updateLeadStatus(Long id, CrmLeadStatus status) {
        CrmLead lead = leadRepository.findByIdAndTenantId(id, currentTenant().getId())
                .orElseThrow(() -> new EntityNotFoundException("Lead not found with id: " + id));

        lead.setStatus(status);
        return toResponse(leadRepository.save(lead));
    }

    public CrmLeadResponse updateLeadStage(Long leadId, Long stageId) {
        Tenant tenant = currentTenant();
        CrmLead lead = leadRepository.findByIdAndTenantId(leadId, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("Lead not found with id: " + leadId));

        CrmLeadStage newStage = leadStageRepository.findByIdAndTenantId(stageId, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("Lead Stage not found with id: " + stageId));

        lead.setCurrentStage(newStage);
        return toResponse(leadRepository.save(lead));
    }

    private void mapReqToEntity(CrmLeadRequest r, CrmLead e) {
        e.setFirstName(r.getFirstName());
        e.setLastName(r.getLastName());
        e.setLeadNo(r.getLeadNo());
        e.setDesignation(r.getDesignation());
        e.setPhone(r.getPhone());
        e.setEmail(r.getEmail());
        e.setWebsite(r.getWebsite());
        e.setRequirements(r.getRequirements());
        e.setAddress(r.getAddress());
        e.setNotes(r.getNotes());
        e.setForecastCategory(r.getForecastCategory());
        e.setExpectedCloseDate(r.getExpectedCloseDate());
        e.setAmount(r.getAmount());
        if (r.getStatus() != null)
            e.setStatus(r.getStatus());

        CrmCompany company = companyRepository.findById(r.getCompanyId())
                .orElseThrow(() -> new EntityNotFoundException("Company not found with id: " + r.getCompanyId()));
        e.setCompany(company);
        if (r.getIndustryId() != null)
            e.setIndustry(industryRepository.findById(r.getIndustryId()).orElse(null));
        if (r.getOwnerId() != null)
            e.setOwner(employeeRepository.findById(r.getOwnerId()).orElse(null));
        if (r.getCurrentStageId() != null)
            e.setCurrentStage(leadStageRepository.findById(r.getCurrentStageId()).orElse(null));
        if (r.getLeadSourceId() != null) {
            LeadSource leadSource = leadSourceRepository.findById(r.getLeadSourceId())
                    .orElseThrow(
                            () -> new EntityNotFoundException("LeadSource not found with id: " + r.getLeadSourceId()));
            e.setLeadSource(leadSource);
        }
        if (r.getLocationId() != null)
            e.setLocation(locationRepository.findById(r.getLocationId()).orElse(null));
        if (r.getProductIds() != null)
            e.setProducts(new HashSet<>(productRepository.findAllById(r.getProductIds())));
    }

    private CrmLeadResponse toResponse(CrmLead e) {
        return CrmLeadResponse.builder()
                .id(e.getId()).firstName(e.getFirstName()).lastName(e.getLastName())
                .leadNo(e.getLeadNo())
                .companyId(e.getCompany() != null ? e.getCompany().getId() : null)
                .companyName(e.getCompany() != null ? e.getCompany().getName() : null)
                .industryId(e.getIndustry() != null ? e.getIndustry().getId() : null)
                .industryName(e.getIndustry() != null ? e.getIndustry().getName() : null)
                .designation(e.getDesignation()).phone(e.getPhone()).email(e.getEmail()).website(e.getWebsite())
                .products(e.getProducts().stream().map(p -> new CrmLeadResponse.ProductMini(p.getId(), p.getName()))
                        .collect(Collectors.toSet()))
                .requirements(e.getRequirements()).address(e.getAddress()).notes(e.getNotes())
                .leadSourceId(e.getLeadSource() != null ? e.getLeadSource().getId() : null)
                .leadSourceName(e.getLeadSource() != null ? e.getLeadSource().getName() : null)
                .ownerId(e.getOwner() != null ? e.getOwner().getId() : null)
                .ownerName(e.getOwner() != null ? e.getOwner().getFirstName() + " " + e.getOwner().getLastName() : null)
                .currentStageId(e.getCurrentStage() != null ? e.getCurrentStage().getId() : null)
                .currentStageName(e.getCurrentStage() != null ? e.getCurrentStage().getName() : null)
                .locationId(e.getLocation() != null ? e.getLocation().getId() : null)
                .locationName(e.getLocation() != null ? e.getLocation().getName() : null)
                //
                .status(e.getStatus()).createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt()).build();
    }
}
