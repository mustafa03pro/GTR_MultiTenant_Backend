package com.example.multi_tanent.sales.service;

import com.example.multi_tanent.crm.services.ResourceNotFoundException;
import com.example.multi_tanent.sales.dto.RecievedRequest;
import com.example.multi_tanent.sales.dto.RecievedResponse;
import com.example.multi_tanent.sales.entity.Recieved;
import com.example.multi_tanent.sales.repository.RecievedRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecievedService {

    private final RecievedRepository recievedRepository;
    private final TenantService tenantService;
    private final BaseCustomerRepository customerRepository;
    private final LocationRepository locationRepository;

    public List<RecievedResponse> getAll(HttpServletRequest request) {
        Tenant tenant = tenantService.getTenantFromRequest(request);
        return recievedRepository.findByTenant_Id(tenant.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public RecievedResponse getById(Long id) {
        Recieved recieved = recievedRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recieved record not found with id: " + id));
        return mapToResponse(recieved);
    }

    @Transactional
    public RecievedResponse create(RecievedRequest request, HttpServletRequest httpRequest) {
        Tenant tenant = tenantService.getTenantFromRequest(httpRequest);
        Recieved recieved = mapToEntity(request, tenant);
        Recieved savedRecieved = recievedRepository.save(recieved);
        return mapToResponse(savedRecieved);
    }

    @Transactional
    public RecievedResponse update(Long id, RecievedRequest request) {
        Recieved existingRecieved = recievedRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recieved record not found with id: " + id));

        updateEntity(existingRecieved, request);
        Recieved updatedRecieved = recievedRepository.save(existingRecieved);
        return mapToResponse(updatedRecieved);
    }

    public void delete(Long id) {
        if (!recievedRepository.existsById(id)) {
            throw new ResourceNotFoundException("Recieved record not found with id: " + id);
        }
        recievedRepository.deleteById(id);
    }

    private Recieved mapToEntity(RecievedRequest request, Tenant tenant) {
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

        return Recieved.builder()
                .tenant(tenant)
                .entryType(request.getEntryType())
                .location(location)
                .customer(customer)
                .piNumber(request.getPiNumber())
                .manualPiNumber(request.getManualPiNumber())
                .amount(request.getAmount())
                .isReceivedFullAmount(request.getIsReceivedFullAmount())
                .isTaxDeducted(request.getIsTaxDeducted())
                .depositDate(request.getDepositDate())
                .depositMode(request.getDepositMode())
                .reference(request.getReference())
                .chequeNumber(request.getChequeNumber())
                .receivingDate(request.getReceivingDate() != null ? request.getReceivingDate() : LocalDate.now())
                .receivedNumber(request.getReceivedNumber())
                .invoiceNumber(request.getInvoiceNumber())
                .tds(request.getTds())
                .advanceAmount(request.getAdvanceAmount())
                .totalPiAmount(request.getTotalPiAmount())
                .fbc(request.getFbc())
                .expectedInFc(request.getExpectedInFc())
                .bankCharges(request.getBankCharges())
                .fineAndPenalty(request.getFineAndPenalty())
                .rebateAndDiscount(request.getRebateAndDiscount())
                .build();
    }

    private void updateEntity(Recieved recieved, RecievedRequest request) {
        if (request.getCustomerId() != null) {
            BaseCustomer customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Customer not found with id: " + request.getCustomerId()));
            recieved.setCustomer(customer);
        }
        if (request.getLocationId() != null) {
            Location location = locationRepository.findById(request.getLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Location not found with id: " + request.getLocationId()));
            recieved.setLocation(location);
        }

        recieved.setEntryType(request.getEntryType());
        recieved.setPiNumber(request.getPiNumber());
        recieved.setManualPiNumber(request.getManualPiNumber());
        recieved.setAmount(request.getAmount());
        recieved.setIsReceivedFullAmount(request.getIsReceivedFullAmount());
        recieved.setIsTaxDeducted(request.getIsTaxDeducted());
        recieved.setDepositDate(request.getDepositDate());
        recieved.setDepositMode(request.getDepositMode());
        recieved.setReference(request.getReference());
        recieved.setChequeNumber(request.getChequeNumber());
        if (request.getReceivingDate() != null) {
            recieved.setReceivingDate(request.getReceivingDate());
        }
        recieved.setReceivedNumber(request.getReceivedNumber());
        recieved.setInvoiceNumber(request.getInvoiceNumber());
        recieved.setTds(request.getTds());
        recieved.setAdvanceAmount(request.getAdvanceAmount());
        recieved.setTotalPiAmount(request.getTotalPiAmount());
        recieved.setFbc(request.getFbc());
        recieved.setExpectedInFc(request.getExpectedInFc());
        recieved.setBankCharges(request.getBankCharges());
        recieved.setFineAndPenalty(request.getFineAndPenalty());
        recieved.setRebateAndDiscount(request.getRebateAndDiscount());
    }

    private RecievedResponse mapToResponse(Recieved recieved) {
        return RecievedResponse.builder()
                .id(recieved.getId())
                .entryType(recieved.getEntryType())
                .locationId(recieved.getLocation() != null ? recieved.getLocation().getId() : null)
                .locationName(recieved.getLocation() != null ? recieved.getLocation().getName() : null)
                .customerId(recieved.getCustomer() != null ? recieved.getCustomer().getId() : null)
                .customerName(recieved.getCustomer() != null ? recieved.getCustomer().getCompanyName() : null)
                .piNumber(recieved.getPiNumber())
                .manualPiNumber(recieved.getManualPiNumber())
                .amount(recieved.getAmount())
                .isReceivedFullAmount(recieved.getIsReceivedFullAmount())
                .isTaxDeducted(recieved.getIsTaxDeducted())
                .depositDate(recieved.getDepositDate())
                .depositMode(recieved.getDepositMode())
                .reference(recieved.getReference())
                .chequeNumber(recieved.getChequeNumber())
                .receivingDate(recieved.getReceivingDate())
                .receivedNumber(recieved.getReceivedNumber())
                .invoiceNumber(recieved.getInvoiceNumber())
                .tds(recieved.getTds())
                .advanceAmount(recieved.getAdvanceAmount())
                .totalPiAmount(recieved.getTotalPiAmount())
                .fbc(recieved.getFbc())
                .expectedInFc(recieved.getExpectedInFc())
                .bankCharges(recieved.getBankCharges())
                .fineAndPenalty(recieved.getFineAndPenalty())
                .rebateAndDiscount(recieved.getRebateAndDiscount())
                .createdBy(recieved.getCreatedBy())
                .updatedBy(recieved.getUpdatedBy())
                .createdAt(recieved.getCreatedAt())
                .updatedAt(recieved.getUpdatedAt())
                .build();
    }
}
