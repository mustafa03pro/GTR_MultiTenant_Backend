package com.example.multi_tanent.sales.service;

import com.example.multi_tanent.config.TenantContext;
import com.example.multi_tanent.crm.entity.CrmSalesProduct;
import com.example.multi_tanent.crm.repository.CrmSalesProductRepository;
import com.example.multi_tanent.sales.dto.DeliveryOrderItemRequest;
import com.example.multi_tanent.sales.dto.DeliveryOrderItemResponse;
import com.example.multi_tanent.sales.dto.DeliveryOrderRequest;
import com.example.multi_tanent.sales.dto.DeliveryOrderResponse;
import com.example.multi_tanent.sales.entity.DeliveryOrder;
import com.example.multi_tanent.sales.entity.DeliveryOrderItem;
import com.example.multi_tanent.sales.entity.SalesOrder;
import com.example.multi_tanent.sales.enums.SalesStatus;
import com.example.multi_tanent.sales.repository.DeliveryOrderRepository;
import com.example.multi_tanent.sales.repository.SalesOrderRepository;
import com.example.multi_tanent.spersusers.enitity.BaseCustomer;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import com.example.multi_tanent.spersusers.repository.BaseCustomerRepository;
import com.example.multi_tanent.spersusers.repository.TenantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryOrderService {

    private final DeliveryOrderRepository deliveryOrderRepository;
    private final BaseCustomerRepository customerRepository;
    private final CrmSalesProductRepository productRepository;
    private final TenantRepository tenantRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final com.example.multi_tanent.production.repository.ProCategoryRepository categoryRepository;
    private final com.example.multi_tanent.production.repository.ProSubCategoryRepository subCategoryRepository;
    private final com.example.multi_tanent.tenant.employee.repository.EmployeeRepository employeeRepository;
    private final com.example.multi_tanent.tenant.service.FileStorageService fileStorageService;

    @Transactional(transactionManager = "tenantTx")
    public DeliveryOrderResponse createDeliveryOrder(DeliveryOrderRequest request,
            org.springframework.web.multipart.MultipartFile[] attachments) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        BaseCustomer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

        DeliveryOrder deliveryOrder = new DeliveryOrder();
        deliveryOrder.setTenant(tenant);
        deliveryOrder.setCustomer(customer);
        deliveryOrder.setDeliveryOrderNumber("DO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        deliveryOrder.setStatus(SalesStatus.DRAFT);

        mapRequestToEntity(request, deliveryOrder, tenant.getId());

        if (attachments != null && attachments.length > 0) {
            for (org.springframework.web.multipart.MultipartFile file : attachments) {
                if (!file.isEmpty()) {
                    String fileUrl = fileStorageService.storeFile(file, "delivery_orders", false);
                    deliveryOrder.getAttachments().add(fileUrl);
                }
            }
        }

        calculateTotals(deliveryOrder);

        DeliveryOrder savedOrder = deliveryOrderRepository.save(deliveryOrder);
        return mapEntityToResponse(savedOrder);
    }

    @Transactional(transactionManager = "tenantTx")
    public DeliveryOrderResponse updateDeliveryOrder(Long id, DeliveryOrderRequest request,
            org.springframework.web.multipart.MultipartFile[] attachments) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        DeliveryOrder deliveryOrder = deliveryOrderRepository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("Delivery Order not found"));

        if (request.getCustomerId() != null) {
            BaseCustomer customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new EntityNotFoundException("Customer not found"));
            deliveryOrder.setCustomer(customer);
        }

        mapRequestToEntity(request, deliveryOrder, tenant.getId());

        if (attachments != null && attachments.length > 0) {
            for (org.springframework.web.multipart.MultipartFile file : attachments) {
                if (!file.isEmpty()) {
                    String fileUrl = fileStorageService.storeFile(file, "delivery_orders", false);
                    deliveryOrder.getAttachments().add(fileUrl);
                }
            }
        }

        calculateTotals(deliveryOrder);

        DeliveryOrder updatedOrder = deliveryOrderRepository.save(deliveryOrder);
        return mapEntityToResponse(updatedOrder);
    }

    @Transactional(transactionManager = "tenantTx", readOnly = true)
    public DeliveryOrderResponse getDeliveryOrderById(Long id) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        DeliveryOrder deliveryOrder = deliveryOrderRepository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("Delivery Order not found"));

        return mapEntityToResponse(deliveryOrder);
    }

    @Transactional(transactionManager = "tenantTx", readOnly = true)
    public Page<DeliveryOrderResponse> getAllDeliveryOrders(String search, java.time.LocalDate fromDate,
            java.time.LocalDate toDate, Long salespersonId, Pageable pageable) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        org.springframework.data.jpa.domain.Specification<DeliveryOrder> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenant").get("id"), tenant.getId()));

            if (search != null && !search.isEmpty()) {
                String searchLike = "%" + search.toLowerCase() + "%";
                jakarta.persistence.criteria.Join<DeliveryOrder, BaseCustomer> customerJoin = root.join("customer",
                        jakarta.persistence.criteria.JoinType.LEFT);

                jakarta.persistence.criteria.Predicate customerName = cb
                        .like(cb.lower(customerJoin.get("companyName")), searchLike);
                jakarta.persistence.criteria.Predicate orderNumber = cb.like(cb.lower(root.get("deliveryOrderNumber")),
                        searchLike);
                jakarta.persistence.criteria.Predicate reference = cb.like(cb.lower(root.get("reference")), searchLike);
                predicates.add(cb.or(customerName, orderNumber, reference));
            }

            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("deliveryOrderDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("deliveryOrderDate"), toDate));
            }
            if (salespersonId != null) {
                predicates.add(cb.equal(root.get("salesperson").get("id"), salespersonId));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<DeliveryOrder> page = deliveryOrderRepository.findAll(spec, pageable);
        page.getContent().forEach(o -> o.getItems().size()); // Force initialization

        return page.map(this::mapEntityToResponse);
    }

    @Transactional(transactionManager = "tenantTx")
    public void deleteDeliveryOrder(Long id) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        DeliveryOrder deliveryOrder = deliveryOrderRepository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("Delivery Order not found"));

        deliveryOrderRepository.delete(deliveryOrder);
    }

    @Transactional(transactionManager = "tenantTx")
    public DeliveryOrderResponse updateStatus(Long id, SalesStatus status) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        DeliveryOrder deliveryOrder = deliveryOrderRepository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("Delivery Order not found"));

        deliveryOrder.setStatus(status);
        DeliveryOrder updatedOrder = deliveryOrderRepository.save(deliveryOrder);
        return mapEntityToResponse(updatedOrder);
    }

    private void mapRequestToEntity(DeliveryOrderRequest request, DeliveryOrder deliveryOrder, Long tenantId) {
        if (request.getDeliveryOrderDate() != null)
            deliveryOrder.setDeliveryOrderDate(request.getDeliveryOrderDate());
        if (request.getShipmentDate() != null)
            deliveryOrder.setShipmentDate(request.getShipmentDate());
        if (request.getReference() != null)
            deliveryOrder.setReference(request.getReference());
        if (request.getPoNumber() != null)
            deliveryOrder.setPoNumber(request.getPoNumber());
        if (request.getSalesOrderId() != null) {
            SalesOrder salesOrder = salesOrderRepository.findByIdAndTenantId(request.getSalesOrderId(), tenantId)
                    .orElseThrow(() -> new EntityNotFoundException("Sales Order not found"));
            deliveryOrder.setSalesOrder(salesOrder);
        }
        if (request.getSalespersonId() != null) {
            com.example.multi_tanent.spersusers.enitity.Employee salesperson = employeeRepository
                    .findById(request.getSalespersonId())
                    .orElseThrow(() -> new EntityNotFoundException("Salesperson not found"));
            deliveryOrder.setSalesperson(salesperson);
        }
        if (request.getTermsAndConditions() != null)
            deliveryOrder.setTermsAndConditions(request.getTermsAndConditions());
        if (request.getNotes() != null)
            deliveryOrder.setNotes(request.getNotes());
        if (request.getEmailTo() != null)
            deliveryOrder.setEmailTo(request.getEmailTo());
        if (request.getTemplate() != null)
            deliveryOrder.setTemplate(request.getTemplate());
        if (request.getTotalDiscount() != null)
            deliveryOrder.setTotalDiscount(request.getTotalDiscount());
        if (request.getOtherCharges() != null)
            deliveryOrder.setOtherCharges(request.getOtherCharges());
        if (request.getStatus() != null) {
            try {
                deliveryOrder.setStatus(SalesStatus.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }

        if (request.getItems() != null) {
            deliveryOrder.getItems().clear();
            for (DeliveryOrderItemRequest itemRequest : request.getItems()) {
                DeliveryOrderItem item = new DeliveryOrderItem();
                item.setDeliveryOrder(deliveryOrder);

                CrmSalesProduct product = productRepository.findByIdAndTenantId(itemRequest.getCrmProductId(), tenantId)
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Product not found: " + itemRequest.getCrmProductId()));

                item.setCrmProduct(product);
                item.setItemCode(itemRequest.getItemCode() != null ? itemRequest.getItemCode() : product.getItemCode());
                item.setItemName(itemRequest.getItemName() != null ? itemRequest.getItemName() : product.getName());

                if (itemRequest.getCategoryId() != null) {
                    com.example.multi_tanent.production.entity.ProCategory category = categoryRepository
                            .findById(itemRequest.getCategoryId())
                            .orElseThrow(() -> new EntityNotFoundException("Category not found"));
                    item.setCategory(category);
                }

                if (itemRequest.getSubcategoryId() != null) {
                    com.example.multi_tanent.production.entity.ProSubCategory subCategory = subCategoryRepository
                            .findById(itemRequest.getSubcategoryId())
                            .orElseThrow(() -> new EntityNotFoundException("SubCategory not found"));
                    item.setSubcategory(subCategory);
                }

                item.setQuantity(itemRequest.getQuantity());
                item.setRate(itemRequest.getRate());
                item.setTaxValue(itemRequest.getTaxValue());
                item.setTaxPercentage(itemRequest.getTaxPercentage());
                item.setTaxExempt(itemRequest.isTaxExempt());

                // Calculate amount for item
                BigDecimal amount = item.getRate().multiply(BigDecimal.valueOf(item.getQuantity()));
                item.setAmount(amount);

                deliveryOrder.getItems().add(item);
            }
        }
    }

    private void calculateTotals(DeliveryOrder deliveryOrder) {
        BigDecimal subTotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (DeliveryOrderItem item : deliveryOrder.getItems()) {
            subTotal = subTotal.add(item.getAmount());
            if (!item.isTaxExempt() && item.getTaxValue() != null) {
                totalTax = totalTax.add(item.getTaxValue());
            }
        }

        deliveryOrder.setSubTotal(subTotal);
        deliveryOrder.setTotalTax(totalTax);

        BigDecimal discount = deliveryOrder.getTotalDiscount() != null ? deliveryOrder.getTotalDiscount()
                : BigDecimal.ZERO;
        deliveryOrder.setTotalDiscount(discount);

        BigDecimal otherCharges = deliveryOrder.getOtherCharges() != null ? deliveryOrder.getOtherCharges()
                : BigDecimal.ZERO;
        deliveryOrder.setOtherCharges(otherCharges);

        BigDecimal grossTotal = subTotal.subtract(discount);
        deliveryOrder.setGrossTotal(grossTotal);

        deliveryOrder.setNetTotal(grossTotal.add(totalTax).add(deliveryOrder.getOtherCharges()));
    }

    private DeliveryOrderResponse mapEntityToResponse(DeliveryOrder deliveryOrder) {
        DeliveryOrderResponse response = new DeliveryOrderResponse();
        response.setId(deliveryOrder.getId());
        response.setDeliveryOrderDate(deliveryOrder.getDeliveryOrderDate());
        response.setShipmentDate(deliveryOrder.getShipmentDate());
        if (deliveryOrder.getCustomer() != null) {
            response.setCustomerId(deliveryOrder.getCustomer().getId());
            response.setCustomerName(deliveryOrder.getCustomer().getCompanyName());
        }
        if (deliveryOrder.getSalesOrder() != null) {
            response.setSalesOrderId(deliveryOrder.getSalesOrder().getId());
            response.setSalesOrderNumber(deliveryOrder.getSalesOrder().getSalesOrderNumber());
        }
        if (deliveryOrder.getSalesperson() != null) {
            response.setSalespersonId(deliveryOrder.getSalesperson().getId());
            String fullName = deliveryOrder.getSalesperson().getFirstName();
            if (deliveryOrder.getSalesperson().getLastName() != null) {
                fullName += " " + deliveryOrder.getSalesperson().getLastName();
            }
            response.setSalespersonName(fullName);
        }
        response.setDeliveryOrderNumber(deliveryOrder.getDeliveryOrderNumber());
        response.setReference(deliveryOrder.getReference());
        response.setPoNumber(deliveryOrder.getPoNumber());

        List<DeliveryOrderItemResponse> itemResponses = deliveryOrder.getItems().stream().map(item -> {
            DeliveryOrderItemResponse itemResponse = new DeliveryOrderItemResponse();
            itemResponse.setId(item.getId());
            if (item.getCrmProduct() != null) {
                itemResponse.setCrmProductId(item.getCrmProduct().getId());
            }
            itemResponse.setItemCode(item.getItemCode());
            itemResponse.setItemName(item.getItemName());
            if (item.getCategory() != null) {
                itemResponse.setCategoryId(item.getCategory().getId());
                itemResponse.setCategoryName(item.getCategory().getName());
            }
            if (item.getSubcategory() != null) {
                itemResponse.setSubcategoryId(item.getSubcategory().getId());
                itemResponse.setSubcategoryName(item.getSubcategory().getName());
            }
            itemResponse.setQuantity(item.getQuantity());
            itemResponse.setRate(item.getRate());
            itemResponse.setAmount(item.getAmount());
            itemResponse.setTaxValue(item.getTaxValue());
            itemResponse.setTaxPercentage(item.getTaxPercentage());
            itemResponse.setTaxExempt(item.isTaxExempt());
            return itemResponse;
        }).collect(Collectors.toList());

        response.setItems(itemResponses);
        response.setSubTotal(deliveryOrder.getSubTotal());
        response.setTotalDiscount(deliveryOrder.getTotalDiscount());
        response.setGrossTotal(deliveryOrder.getGrossTotal());
        response.setTotalTax(deliveryOrder.getTotalTax());
        response.setOtherCharges(deliveryOrder.getOtherCharges());
        response.setNetTotal(deliveryOrder.getNetTotal());
        response.setTermsAndConditions(deliveryOrder.getTermsAndConditions());
        response.setNotes(deliveryOrder.getNotes());
        response.setAttachments(new ArrayList<>(deliveryOrder.getAttachments()));
        response.setEmailTo(deliveryOrder.getEmailTo());
        response.setStatus(deliveryOrder.getStatus());
        response.setTemplate(deliveryOrder.getTemplate());
        response.setCreatedBy(deliveryOrder.getCreatedBy());
        response.setUpdatedBy(deliveryOrder.getUpdatedBy());
        response.setCreatedAt(deliveryOrder.getCreatedAt());
        response.setUpdatedAt(deliveryOrder.getUpdatedAt());

        return response;
    }
}
