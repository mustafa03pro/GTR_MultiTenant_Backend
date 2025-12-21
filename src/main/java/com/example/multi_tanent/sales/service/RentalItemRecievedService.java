package com.example.multi_tanent.sales.service;

import com.example.multi_tanent.config.TenantContext;
import com.example.multi_tanent.crm.repository.CrmSalesProductRepository;
import com.example.multi_tanent.sales.dto.RentalItemRecievedItemRequest;
import com.example.multi_tanent.sales.dto.RentalItemRecievedItemResponse;
import com.example.multi_tanent.sales.dto.RentalItemRecievedRequest;
import com.example.multi_tanent.sales.dto.RentalItemRecievedResponse;
import com.example.multi_tanent.sales.entity.RentalItemRecieved;
import com.example.multi_tanent.sales.entity.RentalItemRecievedItem;
import com.example.multi_tanent.sales.entity.RentalSalesOrder;
import com.example.multi_tanent.sales.enums.SalesStatus;
import com.example.multi_tanent.sales.repository.RentalItemRecievedRepository;
import com.example.multi_tanent.sales.repository.RentalSalesOrderRepository;
import com.example.multi_tanent.spersusers.enitity.BaseCustomer;
import com.example.multi_tanent.spersusers.repository.PartyRepository;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import com.example.multi_tanent.spersusers.repository.TenantRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RentalItemRecievedService {

    private final RentalItemRecievedRepository repository;
    private final RentalSalesOrderRepository orderRepository;
    private final PartyRepository partyRepository;
    private final CrmSalesProductRepository productRepository;
    private final TenantRepository tenantRepository;

    @Transactional
    public RentalItemRecievedResponse create(RentalItemRecievedRequest request) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        RentalItemRecieved entity = new RentalItemRecieved();
        entity.setTenant(tenant);
        mapRequestToEntity(request, entity, tenant.getId());

        RentalItemRecieved saved = repository.save(entity);
        return mapEntityToResponse(saved);
    }

    @Transactional
    public RentalItemRecievedResponse update(Long id, RentalItemRecievedRequest request) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        RentalItemRecieved entity = repository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("Rental Item Recieved not found with id: " + id));

        // clean existing items to replace with new ones (simple approach)
        // OR better: update existing items logic
        entity.getItems().clear();

        mapRequestToEntity(request, entity, tenant.getId());

        RentalItemRecieved saved = repository.save(entity);
        return mapEntityToResponse(saved);
    }

    public List<RentalItemRecievedResponse> getAll() {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        return repository.findByTenant_Id(tenant.getId()).stream()
                .map(this::mapEntityToResponse)
                .collect(Collectors.toList());
    }

    public RentalItemRecievedResponse getById(Long id) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        RentalItemRecieved entity = repository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("Rental Item Recieved not found with id: " + id));
        return mapEntityToResponse(entity);
    }

    public List<RentalItemRecievedResponse> getByOrderId(Long orderId) {
        // Potentially should secure this too if necessary, but skipping for strict
        // parity unless order itself is checked?
        // Let's at least check tenant presence.
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        // Ideally should filter by tenant too, but repo plain method might return all
        // if not careful.
        // Assuming findByRentalSalesOrder_Id returns only if related to order, and
        // order is tenant bound.
        // But better safety:
        return repository.findByRentalSalesOrder_Id(orderId).stream()
                .filter(r -> r.getTenant().getId().equals(tenant.getId()))
                .map(this::mapEntityToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        RentalItemRecieved entity = repository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("Rental Item Recieved not found with id: " + id));

        repository.delete(entity);
    }

    private void mapRequestToEntity(RentalItemRecievedRequest req, RentalItemRecieved entity, Long tenantId) {
        entity.setDoDate(req.getDoDate());
        if (req.getCustomerId() != null) {
            BaseCustomer cust = partyRepository.findByTenantIdAndId(tenantId, req.getCustomerId())
                    .orElseThrow(() -> new EntityNotFoundException("Customer not found"));
            entity.setCustomer(cust);
        }
        entity.setBillingAddress(req.getBillingAddress());
        entity.setShippingAddress(req.getShippingAddress());
        entity.setDoNumber(req.getDoNumber());
        entity.setOrderNumber(req.getOrderNumber());

        if (req.getRentalSalesOrderId() != null) {
            RentalSalesOrder order = orderRepository.findByIdAndTenantId(req.getRentalSalesOrderId(), tenantId)
                    .orElseThrow(() -> new EntityNotFoundException("Order not found"));
            entity.setRentalSalesOrder(order);
        }

        if (req.getStatus() != null) {
            try {
                entity.setStatus(SalesStatus.valueOf(req.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // handle invalid status or default
                entity.setStatus(SalesStatus.DRAFT);
            }
        }

        if (req.getItems() != null) {
            for (RentalItemRecievedItemRequest itemReq : req.getItems()) {
                RentalItemRecievedItem item = new RentalItemRecievedItem();
                item.setRentalItemRecieved(entity);
                item.setItemName(itemReq.getItemName());
                item.setItemCode(itemReq.getItemCode());
                item.setDescription(itemReq.getDescription());
                item.setDoQuantity(itemReq.getDoQuantity());
                item.setReceivedQuantity(itemReq.getReceivedQuantity());
                item.setCurrentReceiveQuantity(itemReq.getCurrentReceiveQuantity());
                item.setRemainingQuantity(itemReq.getRemainingQuantity());

                if (itemReq.getCrmProductId() != null) {
                    item.setCrmProduct(
                            productRepository.findByIdAndTenantId(itemReq.getCrmProductId(), tenantId).orElse(null));
                }

                entity.getItems().add(item);
            }
        }
    }

    private RentalItemRecievedResponse mapEntityToResponse(RentalItemRecieved entity) {
        List<RentalItemRecievedItemResponse> itemResponses = entity.getItems() != null
                ? entity.getItems().stream().map(i -> RentalItemRecievedItemResponse.builder()
                        .id(i.getId())
                        .crmProductId(i.getCrmProduct() != null ? i.getCrmProduct().getId() : null)
                        .itemName(i.getItemName())
                        .itemCode(i.getItemCode())
                        .description(i.getDescription())
                        .doQuantity(i.getDoQuantity())
                        .receivedQuantity(i.getReceivedQuantity())
                        .currentReceiveQuantity(i.getCurrentReceiveQuantity())
                        .remainingQuantity(i.getRemainingQuantity())
                        .build())
                        .collect(Collectors.toList())
                : Collections.emptyList();

        return RentalItemRecievedResponse.builder()
                .id(entity.getId())
                .tenantId(entity.getTenant().getId())
                .doDate(entity.getDoDate())
                .customerId(entity.getCustomer() != null ? entity.getCustomer().getId() : null)
                .customerName(entity.getCustomer() != null ? entity.getCustomer().getCompanyName() : null)
                .billingAddress(entity.getBillingAddress())
                .shippingAddress(entity.getShippingAddress())
                .doNumber(entity.getDoNumber())
                .orderNumber(entity.getOrderNumber())
                .rentalSalesOrderId(entity.getRentalSalesOrder() != null ? entity.getRentalSalesOrder().getId() : null)
                .status(entity.getStatus())
                .items(itemResponses)
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    @Transactional
    public void updateStatus(Long id, SalesStatus status) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        RentalItemRecieved entity = repository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("Rental Item Recieved not found with id: " + id));

        entity.setStatus(status);

        repository.save(entity);
    }
}
