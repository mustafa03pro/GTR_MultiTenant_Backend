package com.example.multi_tanent.sales.service;

import com.example.multi_tanent.config.TenantContext;
import com.example.multi_tanent.crm.entity.CrmSalesProduct;
import com.example.multi_tanent.crm.repository.CrmSalesProductRepository;
import com.example.multi_tanent.sales.dto.SalesOrderItemRequest;
import com.example.multi_tanent.sales.dto.SalesOrderItemResponse;
import com.example.multi_tanent.sales.dto.SalesOrderRequest;
import com.example.multi_tanent.sales.dto.SalesOrderResponse;
import com.example.multi_tanent.sales.entity.SalesOrder;
import com.example.multi_tanent.sales.entity.SalesOrderItem;
import com.example.multi_tanent.sales.enums.SalesStatus;
import com.example.multi_tanent.sales.repository.SalesOrderRepository;
import com.example.multi_tanent.spersusers.enitity.BaseCustomer;
import com.example.multi_tanent.spersusers.enitity.Employee;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import com.example.multi_tanent.spersusers.repository.PartyRepository;
import com.example.multi_tanent.tenant.employee.repository.EmployeeRepository;
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
@Transactional(transactionManager = "tenantTx")
public class SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final PartyRepository partyRepository;
    private final EmployeeRepository employeeRepository;
    private final CrmSalesProductRepository productRepository;
    private final TenantRepository tenantRepository;
    private final com.example.multi_tanent.production.repository.ProCategoryRepository categoryRepository;
    private final com.example.multi_tanent.production.repository.ProSubCategoryRepository subCategoryRepository;
    private final com.example.multi_tanent.tenant.service.FileStorageService fileStorageService;
    private final com.example.multi_tanent.sales.repository.QuotationRepository quotationRepository;

    @Transactional(transactionManager = "tenantTx")
    public SalesOrderResponse createSalesOrder(SalesOrderRequest request,
            org.springframework.web.multipart.MultipartFile[] attachments) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        BaseCustomer customer = partyRepository.findByTenantIdAndId(tenant.getId(), request.getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setTenant(tenant);
        salesOrder.setCustomer(customer);
        salesOrder.setSalesOrderNumber("SO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        salesOrder.setStatus(SalesStatus.DRAFT);

        mapRequestToEntity(request, salesOrder, tenant.getId());

        if (attachments != null && attachments.length > 0) {
            for (org.springframework.web.multipart.MultipartFile file : attachments) {
                if (!file.isEmpty()) {
                    String fileUrl = fileStorageService.storeFile(file, "sales_orders", true);
                    salesOrder.getAttachments().add(fileUrl);
                }
            }
        }

        calculateTotals(salesOrder);

        SalesOrder savedSalesOrder = salesOrderRepository.save(salesOrder);
        return mapEntityToResponse(savedSalesOrder);
    }

    @Transactional(transactionManager = "tenantTx")
    public SalesOrderResponse updateSalesOrder(Long id, SalesOrderRequest request,
            org.springframework.web.multipart.MultipartFile[] attachments) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        SalesOrder salesOrder = salesOrderRepository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("Sales Order not found"));

        if (request.getCustomerId() != null) {
            BaseCustomer customer = partyRepository.findByTenantIdAndId(tenant.getId(), request.getCustomerId())
                    .orElseThrow(() -> new EntityNotFoundException("Customer not found"));
            salesOrder.setCustomer(customer);
        }

        mapRequestToEntity(request, salesOrder, tenant.getId());

        if (attachments != null && attachments.length > 0) {
            for (org.springframework.web.multipart.MultipartFile file : attachments) {
                if (!file.isEmpty()) {
                    String fileUrl = fileStorageService.storeFile(file, "sales_orders", true);
                    salesOrder.getAttachments().add(fileUrl);
                }
            }
        }

        calculateTotals(salesOrder);

        SalesOrder updatedSalesOrder = salesOrderRepository.save(salesOrder);
        return mapEntityToResponse(updatedSalesOrder);
    }

    @Transactional(readOnly = true, transactionManager = "tenantTx")
    public SalesOrderResponse getSalesOrderById(Long id) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        SalesOrder salesOrder = salesOrderRepository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("Sales Order not found"));

        return mapEntityToResponse(salesOrder);
    }

    @Transactional(readOnly = true, transactionManager = "tenantTx")
    public Page<SalesOrderResponse> getAllSalesOrders(String customerName, java.time.LocalDate startDate,
            java.time.LocalDate endDate, SalesStatus status, Long salespersonId, Pageable pageable) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        org.springframework.data.jpa.domain.Specification<SalesOrder> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenant").get("id"), tenant.getId()));

            if (customerName != null && !customerName.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("customer").get("companyName")),
                        "%" + customerName.toLowerCase() + "%"));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("salesOrderDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("salesOrderDate"), endDate));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (salespersonId != null) {
                predicates.add(cb.equal(root.get("salesperson").get("id"), salespersonId));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return salesOrderRepository.findAll(spec, pageable)
                .map(this::mapEntityToResponse);
    }

    @Transactional(transactionManager = "tenantTx")
    public void deleteSalesOrder(Long id) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        SalesOrder salesOrder = salesOrderRepository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("Sales Order not found"));

        salesOrderRepository.delete(salesOrder);
    }

    @Transactional(transactionManager = "tenantTx")
    public SalesOrderResponse updateStatus(Long id, SalesStatus status) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        SalesOrder salesOrder = salesOrderRepository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("Sales Order not found"));

        salesOrder.setStatus(status);
        SalesOrder updatedSalesOrder = salesOrderRepository.save(salesOrder);
        return mapEntityToResponse(updatedSalesOrder);
    }

    @Transactional(transactionManager = "tenantTx")
    public SalesOrderResponse updateStatusByNumber(String salesOrderNumber, SalesStatus status) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        SalesOrder salesOrder = salesOrderRepository.findBySalesOrderNumberAndTenantId(salesOrderNumber, tenant.getId())
                .orElseThrow(
                        () -> new EntityNotFoundException("Sales Order not found with number: " + salesOrderNumber));

        salesOrder.setStatus(status);
        SalesOrder updatedSalesOrder = salesOrderRepository.save(salesOrder);
        return mapEntityToResponse(updatedSalesOrder);
    }

    @Transactional(transactionManager = "tenantTx")
    public SalesOrderResponse createSalesOrderFromQuotation(Long quotationId) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        com.example.multi_tanent.sales.entity.Quotation quotation = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new EntityNotFoundException("Quotation not found"));

        // Verify tenant ownership
        if (!quotation.getTenant().getId().equals(tenant.getId())) {
            throw new EntityNotFoundException("Quotation not found");
        }

        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setTenant(tenant);
        salesOrder.setCustomer(quotation.getCustomer());
        salesOrder.setSalesOrderNumber("SO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        salesOrder.setStatus(SalesStatus.DRAFT);
        salesOrder.setSalesOrderDate(java.time.LocalDate.now());
        salesOrder.setReference(quotation.getReference());
        salesOrder.setTermsAndConditions(quotation.getTermsAndConditions());
        salesOrder.setNotes(quotation.getNotes());
        salesOrder.setEmailTo(quotation.getEmailTo());
        salesOrder.setTemplate(quotation.getTemplate());
        salesOrder.setSalesperson(quotation.getSalesperson());
        // salesOrder.setSalesperson(quotation.getSalesperson()); // Assuming Quotation
        // has salesperson, but it wasn't in the entity view earlier?
        // Checking Quotation entity again... it doesn't seem to have salesperson field
        // in the view I saw earlier.
        // Wait, SalesOrder has salesperson. If Quotation doesn't, we skip it.

        // Map items
        salesOrder.getItems().clear(); // Should be empty anyway
        for (com.example.multi_tanent.sales.entity.QuotationItem qItem : quotation.getItems()) {
            SalesOrderItem item = new SalesOrderItem();
            item.setSalesOrder(salesOrder);
            item.setCrmProduct(qItem.getCrmProduct());
            item.setItemCode(qItem.getItemCode());
            item.setItemName(qItem.getItemName());
            item.setCategory(qItem.getCategory());
            item.setSubcategory(qItem.getSubcategory());
            item.setQuantity(qItem.getQuantity());
            item.setRate(qItem.getRate());
            item.setTaxValue(qItem.getTaxValue());
            item.setTaxExempt(qItem.isTaxExempt());
            item.setTaxPercentage(qItem.getTaxPercentage());
            item.setAmount(qItem.getAmount());

            salesOrder.getItems().add(item);
        }

        calculateTotals(salesOrder);

        SalesOrder savedSalesOrder = salesOrderRepository.save(salesOrder);

        // Optionally update quotation status
        quotation.setStatus(SalesStatus.CONVERTED); // If CONVERTED status exists
        quotationRepository.save(quotation);

        return mapEntityToResponse(savedSalesOrder);
    }

    private void mapRequestToEntity(SalesOrderRequest request, SalesOrder salesOrder, Long tenantId) {
        if (request.getSalesOrderDate() != null)
            salesOrder.setSalesOrderDate(request.getSalesOrderDate());
        if (request.getReference() != null)
            salesOrder.setReference(request.getReference());
        if (request.getCustomerPoNo() != null)
            salesOrder.setCustomerPoNo(request.getCustomerPoNo());
        if (request.getCustomerPoDate() != null)
            salesOrder.setCustomerPoDate(request.getCustomerPoDate());
        if (request.getSalespersonId() != null) {
            Employee employee = employeeRepository.findById(request.getSalespersonId())
                    .orElseThrow(() -> new EntityNotFoundException("Salesperson not found"));
            salesOrder.setSalesperson(employee);
        }
        if (request.getSaleType() != null)
            salesOrder.setSaleType(request.getSaleType());
        if (request.getTermsAndConditions() != null)
            salesOrder.setTermsAndConditions(request.getTermsAndConditions());
        if (request.getNotes() != null)
            salesOrder.setNotes(request.getNotes());
        if (request.getEmailTo() != null)
            salesOrder.setEmailTo(request.getEmailTo());
        if (request.getStatus() != null)
            salesOrder.setStatus(request.getStatus());
        if (request.getTotalDiscount() != null)
            salesOrder.setTotalDiscount(request.getTotalDiscount());
        if (request.getOtherCharges() != null)
            salesOrder.setOtherCharges(request.getOtherCharges());
        if (request.getTemplate() != null)
            salesOrder.setTemplate(request.getTemplate());

        if (request.getItems() != null) {
            salesOrder.getItems().clear();
            for (SalesOrderItemRequest itemRequest : request.getItems()) {
                SalesOrderItem item = new SalesOrderItem();
                item.setSalesOrder(salesOrder);

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
                item.setTaxExempt(itemRequest.isTaxExempt());
                item.setTaxPercentage(itemRequest.getTaxPercentage());

                // Calculate amount for item
                BigDecimal amount = item.getRate().multiply(BigDecimal.valueOf(item.getQuantity()));
                item.setAmount(amount);

                salesOrder.getItems().add(item);
            }
        }
    }

    private void calculateTotals(SalesOrder salesOrder) {
        BigDecimal subTotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (SalesOrderItem item : salesOrder.getItems()) {
            subTotal = subTotal.add(item.getAmount());
            if (!item.isTaxExempt() && item.getTaxValue() != null) {
                totalTax = totalTax.add(item.getTaxValue());
            }
        }

        salesOrder.setSubTotal(subTotal);
        salesOrder.setTotalTax(totalTax);

        BigDecimal discount = salesOrder.getTotalDiscount() != null ? salesOrder.getTotalDiscount() : BigDecimal.ZERO;
        salesOrder.setTotalDiscount(discount);

        BigDecimal otherCharges = salesOrder.getOtherCharges() != null ? salesOrder.getOtherCharges() : BigDecimal.ZERO;
        salesOrder.setOtherCharges(otherCharges);

        BigDecimal grossTotal = subTotal.subtract(discount);
        salesOrder.setGrossTotal(grossTotal);

        salesOrder.setNetTotal(grossTotal.add(totalTax).add(salesOrder.getOtherCharges()));
    }

    private SalesOrderResponse mapEntityToResponse(SalesOrder salesOrder) {
        SalesOrderResponse response = new SalesOrderResponse();
        response.setId(salesOrder.getId());
        response.setSalesOrderDate(salesOrder.getSalesOrderDate());
        if (salesOrder.getCustomer() != null) {
            response.setCustomerId(salesOrder.getCustomer().getId());
            response.setCustomerName(salesOrder.getCustomer().getCompanyName());
        }
        response.setSalesOrderNumber(salesOrder.getSalesOrderNumber());
        response.setReference(salesOrder.getReference());
        response.setCustomerPoNo(salesOrder.getCustomerPoNo());
        response.setCustomerPoDate(salesOrder.getCustomerPoDate());
        if (salesOrder.getSalesperson() != null) {
            response.setSalespersonId(salesOrder.getSalesperson().getId());
            String fullName = salesOrder.getSalesperson().getFirstName();
            if (salesOrder.getSalesperson().getLastName() != null) {
                fullName += " " + salesOrder.getSalesperson().getLastName();
            }
            response.setSalespersonName(fullName);
        }
        response.setSaleType(salesOrder.getSaleType());

        List<SalesOrderItemResponse> itemResponses = salesOrder.getItems().stream().map(item -> {
            SalesOrderItemResponse itemResponse = new SalesOrderItemResponse();
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
            itemResponse.setTaxExempt(item.isTaxExempt());
            itemResponse.setTaxPercentage(item.getTaxPercentage());
            return itemResponse;
        }).collect(Collectors.toList());

        response.setItems(itemResponses);
        response.setSubTotal(salesOrder.getSubTotal());
        response.setTotalDiscount(salesOrder.getTotalDiscount());
        response.setGrossTotal(salesOrder.getGrossTotal());
        response.setTotalTax(salesOrder.getTotalTax());
        response.setOtherCharges(salesOrder.getOtherCharges());
        response.setNetTotal(salesOrder.getNetTotal());
        response.setTermsAndConditions(salesOrder.getTermsAndConditions());
        response.setNotes(salesOrder.getNotes());
        response.setAttachments(new ArrayList<>(salesOrder.getAttachments()));
        response.setEmailTo(salesOrder.getEmailTo());
        response.setStatus(salesOrder.getStatus());
        response.setTemplate(salesOrder.getTemplate());
        response.setCreatedBy(salesOrder.getCreatedBy());
        response.setUpdatedBy(salesOrder.getUpdatedBy());
        response.setCreatedAt(salesOrder.getCreatedAt());
        response.setUpdatedAt(salesOrder.getUpdatedAt());

        return response;
    }
}
