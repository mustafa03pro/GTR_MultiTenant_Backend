package com.example.multi_tanent.sales.service;

import com.example.multi_tanent.crm.services.ResourceNotFoundException;
import com.example.multi_tanent.sales.dto.CreditNotesRequest;
import com.example.multi_tanent.sales.dto.CreditNotesResponse;
import com.example.multi_tanent.sales.entity.CreditNotes;
import com.example.multi_tanent.sales.enums.CreditNoteStatus;
import com.example.multi_tanent.sales.repository.CreditNotesRepository;
import com.example.multi_tanent.spersusers.enitity.BaseCustomer;
import com.example.multi_tanent.spersusers.enitity.Location;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import com.example.multi_tanent.spersusers.repository.BaseCustomerRepository;
import com.example.multi_tanent.spersusers.repository.LocationRepository;
import com.example.multi_tanent.pos.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CreditNotesService {

    private final CreditNotesRepository creditNotesRepository;
    private final TenantService tenantService;
    private final BaseCustomerRepository customerRepository;
    private final LocationRepository locationRepository;

    public List<CreditNotesResponse> getAll(HttpServletRequest request) {
        Tenant tenant = tenantService.getTenantFromRequest(request);
        return creditNotesRepository.findByTenant_Id(tenant.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public CreditNotesResponse getById(Long id) {
        CreditNotes creditNotes = creditNotesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Credit Note not found with id: " + id));
        return mapToResponse(creditNotes);
    }

    @Transactional
    public CreditNotesResponse create(CreditNotesRequest request, HttpServletRequest httpRequest) {
        Tenant tenant = tenantService.getTenantFromRequest(httpRequest);
        CreditNotes creditNotes = mapToEntity(request, tenant);
        creditNotes.setStatus(CreditNoteStatus.OPEN); // Default status
        creditNotes.setBalanceDue(request.getAmount()); // Initial balance due
        if (creditNotes.getCreditNoteDate() == null) {
            creditNotes.setCreditNoteDate(LocalDate.now());
        }
        CreditNotes savedCreditNotes = creditNotesRepository.save(creditNotes);
        return mapToResponse(savedCreditNotes);
    }

    @Transactional
    public CreditNotesResponse update(Long id, CreditNotesRequest request) {
        CreditNotes existingCreditNotes = creditNotesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Credit Note not found with id: " + id));

        updateEntity(existingCreditNotes, request);
        CreditNotes updatedCreditNotes = creditNotesRepository.save(existingCreditNotes);
        return mapToResponse(updatedCreditNotes);
    }

    public void delete(Long id) {
        if (!creditNotesRepository.existsById(id)) {
            throw new ResourceNotFoundException("Credit Note not found with id: " + id);
        }
        creditNotesRepository.deleteById(id);
    }

    private CreditNotes mapToEntity(CreditNotesRequest request, Tenant tenant) {
        BaseCustomer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Customer not found with id: " + request.getCustomerId()));
        }

        Location location = null;
        if (request.getLocationId() != null) {
            location = locationRepository.findById(request.getLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Location not found with id: " + request.getLocationId()));
        }

        return CreditNotes.builder()
                .tenant(tenant)
                .location(location)
                .customer(customer)
                .creditNoteNumber(request.getCreditNoteNumber())
                .invoiceNumber(request.getInvoiceNumber())
                .creditNoteDate(request.getCreditNoteDate())
                .amount(request.getAmount())
                .taxPercentage(request.getTaxPercentage())
                .termsAndConditions(request.getTermsAndConditions())
                .notes(request.getNotes())
                .template(request.getTemplate())
                .emailTo(request.getEmailTo())
                .attachments(request.getAttachments())
                .build();
    }

    private void updateEntity(CreditNotes creditNotes, CreditNotesRequest request) {
        if (request.getCustomerId() != null) {
            BaseCustomer customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Customer not found with id: " + request.getCustomerId()));
            creditNotes.setCustomer(customer);
        }
        if (request.getLocationId() != null) {
            Location location = locationRepository.findById(request.getLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Location not found with id: " + request.getLocationId()));
            creditNotes.setLocation(location);
        }

        creditNotes.setCreditNoteNumber(request.getCreditNoteNumber());
        creditNotes.setInvoiceNumber(request.getInvoiceNumber());
        creditNotes.setCreditNoteDate(request.getCreditNoteDate());
        creditNotes.setAmount(request.getAmount());
        creditNotes.setTaxPercentage(request.getTaxPercentage());
        creditNotes.setTermsAndConditions(request.getTermsAndConditions());
        creditNotes.setNotes(request.getNotes());
        creditNotes.setTemplate(request.getTemplate());
        creditNotes.setEmailTo(request.getEmailTo());
        creditNotes.setAttachments(request.getAttachments());
    }

    private CreditNotesResponse mapToResponse(CreditNotes creditNotes) {
        return CreditNotesResponse.builder()
                .id(creditNotes.getId())
                .locationId(creditNotes.getLocation() != null ? creditNotes.getLocation().getId() : null)
                .locationName(creditNotes.getLocation() != null ? creditNotes.getLocation().getName() : null)
                .customerId(creditNotes.getCustomer() != null ? creditNotes.getCustomer().getId() : null)
                .customerName(creditNotes.getCustomer() != null ? creditNotes.getCustomer().getCompanyName() : null)
                .creditNoteNumber(creditNotes.getCreditNoteNumber())
                .invoiceNumber(creditNotes.getInvoiceNumber())
                .creditNoteDate(creditNotes.getCreditNoteDate())
                .amount(creditNotes.getAmount())
                .taxPercentage(creditNotes.getTaxPercentage())
                .termsAndConditions(creditNotes.getTermsAndConditions())
                .notes(creditNotes.getNotes())
                .template(creditNotes.getTemplate())
                .emailTo(creditNotes.getEmailTo())
                .status(creditNotes.getStatus())
                .balanceDue(creditNotes.getBalanceDue())
                .attachments(creditNotes.getAttachments())
                .createdBy(creditNotes.getCreatedBy())
                .updatedBy(creditNotes.getUpdatedBy())
                .createdAt(creditNotes.getCreatedAt())
                .updatedAt(creditNotes.getUpdatedAt())
                .build();
    }
}
