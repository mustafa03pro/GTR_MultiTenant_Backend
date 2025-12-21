package com.example.multi_tanent.sales.service;

import com.example.multi_tanent.config.TenantContext;
import com.example.multi_tanent.sales.dto.PaymentSheduleRequest;
import com.example.multi_tanent.sales.dto.PaymentSheduleResponse;
import com.example.multi_tanent.sales.entity.PaymentShedule;
import com.example.multi_tanent.sales.entity.RentalSalesOrder;
import com.example.multi_tanent.sales.repository.PaymentSheduleRepository;
import com.example.multi_tanent.sales.repository.RentalSalesOrderRepository;
import com.example.multi_tanent.spersusers.enitity.BaseCustomer;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import com.example.multi_tanent.spersusers.repository.PartyRepository;
import com.example.multi_tanent.spersusers.repository.TenantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class PaymentSheduleService {

    private final PaymentSheduleRepository paymentSheduleRepository;
    private final TenantRepository tenantRepository;
    private final PartyRepository partyRepository;
    private final RentalSalesOrderRepository rentalSalesOrderRepository;

    @Transactional
    public PaymentSheduleResponse createPaymentShedule(PaymentSheduleRequest request) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        PaymentShedule paymentShedule = new PaymentShedule();
        paymentShedule.setTenant(tenant);

        mapRequestToEntity(request, paymentShedule, tenant.getId());

        paymentShedule = paymentSheduleRepository.save(paymentShedule);
        return mapEntityToResponse(paymentShedule);
    }

    @Transactional
    public PaymentSheduleResponse updatePaymentShedule(Long id, PaymentSheduleRequest request) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        PaymentShedule paymentShedule = paymentSheduleRepository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("Payment Schedule not found"));

        mapRequestToEntity(request, paymentShedule, tenant.getId());

        paymentShedule = paymentSheduleRepository.save(paymentShedule);
        return mapEntityToResponse(paymentShedule);
    }

    @Transactional(readOnly = true)
    public PaymentSheduleResponse getPaymentSheduleById(Long id) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        PaymentShedule paymentShedule = paymentSheduleRepository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("Payment Schedule not found"));
        return mapEntityToResponse(paymentShedule);
    }

    @Transactional(readOnly = true)
    public Page<PaymentSheduleResponse> getAllPaymentSchedules(String search, LocalDate fromDate, LocalDate toDate,
            Long customerId, Long rentalSalesOrderId, Pageable pageable) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        Page<PaymentShedule> schedules = paymentSheduleRepository.searchPaymentSchedules(tenant.getId(), search,
                fromDate, toDate, customerId, rentalSalesOrderId, pageable);
        return schedules.map(this::mapEntityToResponse);
    }

    @Transactional
    public void deletePaymentShedule(Long id) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        PaymentShedule paymentShedule = paymentSheduleRepository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("Payment Schedule not found"));
        paymentSheduleRepository.delete(paymentShedule);
    }

    @Transactional
    public void updateStatus(Long id, com.example.multi_tanent.sales.enums.SalesStatus status) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        PaymentShedule paymentShedule = paymentSheduleRepository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("Payment Schedule not found"));

        paymentShedule.setStatus(status);
        paymentSheduleRepository.save(paymentShedule);
    }

    private void mapRequestToEntity(PaymentSheduleRequest request, PaymentShedule paymentShedule, Long tenantId) {
        if (request.getCustomerId() != null) {
            BaseCustomer customer = partyRepository.findByTenantIdAndId(tenantId, request.getCustomerId())
                    .orElseThrow(() -> new EntityNotFoundException("Customer not found"));
            paymentShedule.setCustomer(customer);
        }

        if (request.getRentalSalesOrderId() != null) {
            RentalSalesOrder rentalSalesOrder = rentalSalesOrderRepository
                    .findByIdAndTenantId(request.getRentalSalesOrderId(), tenantId)
                    .orElseThrow(() -> new EntityNotFoundException("Rental Sales Order not found"));
            paymentShedule.setRentalSalesOrder(rentalSalesOrder);
        }

        if (request.getDueDate() != null) {
            paymentShedule.setDueDate(request.getDueDate());
        }
        if (request.getAmount() != null) {
            paymentShedule.setAmount(request.getAmount());
        }
        if (request.getStatus() != null) {
            paymentShedule.setStatus(request.getStatus());
        }
        if (request.getNote() != null) {
            paymentShedule.setNote(request.getNote());
        }
    }

    private PaymentSheduleResponse mapEntityToResponse(PaymentShedule paymentShedule) {
        PaymentSheduleResponse response = new PaymentSheduleResponse();
        response.setId(paymentShedule.getId());

        if (paymentShedule.getCustomer() != null) {
            response.setCustomerId(paymentShedule.getCustomer().getId());
            response.setCustomerName(paymentShedule.getCustomer().getCompanyName());
        }

        if (paymentShedule.getRentalSalesOrder() != null) {
            response.setRentalSalesOrderId(paymentShedule.getRentalSalesOrder().getId());
            response.setRentalSalesOrderNumber(paymentShedule.getRentalSalesOrder().getOrderNumber());
        }

        response.setDueDate(paymentShedule.getDueDate());
        response.setAmount(paymentShedule.getAmount());
        response.setStatus(paymentShedule.getStatus());
        response.setNote(paymentShedule.getNote());
        response.setCreatedBy(paymentShedule.getCreatedBy());
        response.setUpdatedBy(paymentShedule.getUpdatedBy());
        response.setCreatedAt(paymentShedule.getCreatedAt());
        response.setUpdatedAt(paymentShedule.getUpdatedAt());

        return response;
    }
}
