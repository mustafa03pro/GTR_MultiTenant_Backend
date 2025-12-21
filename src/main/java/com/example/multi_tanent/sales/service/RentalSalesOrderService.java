package com.example.multi_tanent.sales.service;

import com.example.multi_tanent.config.TenantContext;
import com.example.multi_tanent.crm.entity.CrmSalesProduct;
import com.example.multi_tanent.crm.repository.CrmSalesProductRepository;
import com.example.multi_tanent.production.entity.ProCategory;
import com.example.multi_tanent.production.entity.ProSubCategory;
import com.example.multi_tanent.production.repository.ProCategoryRepository;
import com.example.multi_tanent.production.repository.ProSubCategoryRepository;
import com.example.multi_tanent.sales.dto.*;
import com.example.multi_tanent.sales.entity.RentalSalesOrder;
import com.example.multi_tanent.sales.entity.RentalSalesOrderItem;
import com.example.multi_tanent.sales.repository.RentalSalesOrderRepository;
import com.example.multi_tanent.sales.enums.SalesStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.multi_tanent.spersusers.enitity.BaseCustomer;
import com.example.multi_tanent.spersusers.enitity.Employee;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import com.example.multi_tanent.spersusers.repository.TenantRepository;
import com.example.multi_tanent.tenant.employee.repository.EmployeeRepository;
import com.example.multi_tanent.tenant.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RentalSalesOrderService {

    private final RentalSalesOrderRepository rentalSalesOrderRepository;
    private final TenantRepository tenantRepository;
    private final com.example.multi_tanent.spersusers.repository.PartyRepository partyRepository; // Fixed
    private final EmployeeRepository employeeRepository;
    private final CrmSalesProductRepository crmSalesProductRepository;
    private final ProCategoryRepository proCategoryRepository;
    private final ProSubCategoryRepository proSubCategoryRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public RentalSalesOrderResponse createRentalSalesOrder(RentalSalesOrderRequest request, MultipartFile[] files) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Tenant not found"));

        RentalSalesOrder order = new RentalSalesOrder();
        order.setTenant(tenant);

        if (request.getCustomerId() != null) {
            BaseCustomer customer = partyRepository.findByTenantIdAndId(tenant.getId(), request.getCustomerId())
                    .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Customer not found"));
            order.setCustomer(customer);
        }

        // Auto-generate order number
        order.setOrderNumber("RSO-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        // order.setStatus(SalesStatus.DRAFT); // Assuming import is fixed or just set
        // string/enum if available

        mapRequestToEntity(request, order, tenant.getId());

        if (files != null && files.length > 0) {
            List<String> attachmentUrls = new ArrayList<>();
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String fileName = fileStorageService.storeFile(file, "rental_sales_orders", false);
                    attachmentUrls.add(fileName);
                }
            }
            order.setAttachments(attachmentUrls);
        }

        calculateTotals(order);

        order = rentalSalesOrderRepository.save(order);
        return mapEntityToResponse(order);
    }

    @Transactional
    public RentalSalesOrderResponse updateRentalSalesOrder(Long id, RentalSalesOrderRequest request,
            MultipartFile[] files) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Tenant not found"));

        RentalSalesOrder order = rentalSalesOrderRepository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Rental Sales Order not found"));

        if (request.getCustomerId() != null) {
            BaseCustomer customer = partyRepository.findByTenantIdAndId(tenant.getId(), request.getCustomerId())
                    .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Customer not found"));
            order.setCustomer(customer);
        }

        mapRequestToEntity(request, order, tenant.getId());

        if (files != null && files.length > 0) {
            if (order.getAttachments() == null) {
                order.setAttachments(new ArrayList<>());
            }
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String fileName = fileStorageService.storeFile(file, "rental_sales_orders", false);
                    order.getAttachments().add(fileName);
                }
            }
        }

        calculateTotals(order);

        order = rentalSalesOrderRepository.save(order);
        return mapEntityToResponse(order);
    }

    @Transactional(readOnly = true)
    public RentalSalesOrderResponse getRentalSalesOrderById(Long id) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Tenant not found"));

        RentalSalesOrder order = rentalSalesOrderRepository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Rental Sales Order not found"));
        return mapEntityToResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<RentalSalesOrderResponse> getAllRentalSalesOrders(String search, java.time.LocalDate fromDate,
            java.time.LocalDate toDate, Long salespersonId, Pageable pageable) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Tenant not found"));

        Page<RentalSalesOrder> orders = rentalSalesOrderRepository.searchRentalSalesOrders(tenant.getId(), search,
                fromDate, toDate, salespersonId, pageable);
        return orders.map(this::mapEntityToResponse);
    }

    @Transactional
    public void deleteRentalSalesOrder(Long id) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Tenant not found"));

        RentalSalesOrder order = rentalSalesOrderRepository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Rental Sales Order not found"));
        rentalSalesOrderRepository.delete(order);
    }

    @Transactional
    public RentalSalesOrderResponse updateStatus(Long id, SalesStatus status) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Tenant not found"));

        RentalSalesOrder order = rentalSalesOrderRepository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Rental Sales Order not found"));

        order.setStatus(status);
        RentalSalesOrder savedOrder = rentalSalesOrderRepository.save(order);
        return mapEntityToResponse(savedOrder);
    }

    private void mapRequestToEntity(RentalSalesOrderRequest request, RentalSalesOrder order, Long tenantId) {
        if (request.getOrderDate() != null)
            order.setOrderDate(request.getOrderDate());
        if (request.getReference() != null)
            order.setReference(request.getReference());
        if (request.getShipmentDate() != null)
            order.setShipmentDate(request.getShipmentDate());
        if (request.getFromDate() != null)
            order.setFromDate(request.getFromDate());
        if (request.getToDate() != null)
            order.setToDate(request.getToDate());
        if (request.getDeliveryLead() != null)
            order.setDeliveryLead(request.getDeliveryLead());
        if (request.getValidity() != null)
            order.setValidity(request.getValidity());
        if (request.getPaymentTerms() != null)
            order.setPaymentTerms(request.getPaymentTerms());
        if (request.getPriceBasis() != null)
            order.setPriceBasis(request.getPriceBasis());
        if (request.getDearSir() != null)
            order.setDearSir(request.getDearSir());
        if (request.getRentalDurationDays() != null)
            order.setRentalDurationDays(request.getRentalDurationDays());

        // Totals mapping not needed from request usually as they are calculated,
        // but keeping if FE sends overrides or for completeness as per original code
        // logic
        if (request.getSubTotalPerDay() != null)
            order.setSubTotalPerDay(request.getSubTotalPerDay());
        if (request.getTotalRentalPrice() != null)
            order.setTotalRentalPrice(request.getTotalRentalPrice());
        if (request.getTotalDiscount() != null)
            order.setTotalDiscount(request.getTotalDiscount());
        if (request.getOtherCharges() != null)
            order.setOtherCharges(request.getOtherCharges());

        if (request.getTermsAndConditions() != null)
            order.setTermsAndConditions(request.getTermsAndConditions());
        if (request.getNotes() != null)
            order.setNotes(request.getNotes());
        if (request.getManufacture() != null)
            order.setManufacture(request.getManufacture());
        if (request.getRemarks() != null)
            order.setRemarks(request.getRemarks());
        if (request.getEmailTo() != null)
            order.setEmailTo(request.getEmailTo());
        if (request.getStatus() != null)
            order.setStatus(request.getStatus());
        if (request.getTemplate() != null)
            order.setTemplate(request.getTemplate());

        if (request.getSalespersonId() != null) {
            com.example.multi_tanent.spersusers.enitity.Employee salesperson = employeeRepository
                    .findById(request.getSalespersonId())
                    .orElse(null);
            order.setSalesperson(salesperson);
        }

        // Map Items
        if (request.getItems() != null) {
            if (order.getItems() == null) {
                order.setItems(new ArrayList<>());
            }

            Map<Long, RentalSalesOrderItem> existingItemsMap = order.getItems().stream()
                    .collect(Collectors.toMap(RentalSalesOrderItem::getId, Function.identity()));

            List<RentalSalesOrderItem> updatedItems = new ArrayList<>();

            for (RentalSalesOrderItemRequest itemRequest : request.getItems()) {
                RentalSalesOrderItem item = null;

                if (itemRequest.getId() != null && existingItemsMap.containsKey(itemRequest.getId())) {
                    item = existingItemsMap.get(itemRequest.getId());
                } else {
                    item = new RentalSalesOrderItem();
                    item.setRentalSalesOrder(order);
                }

                if (itemRequest.getCrmProductId() != null) {
                    CrmSalesProduct product = crmSalesProductRepository
                            .findByIdAndTenantId(itemRequest.getCrmProductId(), tenantId)
                            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                                    "Product not found: " + itemRequest.getCrmProductId()));
                    item.setCrmProduct(product);
                    // Use product details as default if request doesn't have them
                    item.setItemCode(
                            itemRequest.getItemCode() != null ? itemRequest.getItemCode() : product.getItemCode());
                    item.setItemName(itemRequest.getItemName() != null ? itemRequest.getItemName() : product.getName());
                } else {
                    item.setItemCode(itemRequest.getItemCode());
                    item.setItemName(itemRequest.getItemName());
                }

                item.setDescription(itemRequest.getDescription());

                if (itemRequest.getCategoryId() != null) {
                    com.example.multi_tanent.production.entity.ProCategory category = proCategoryRepository
                            .findById(itemRequest.getCategoryId()).orElse(null);
                    item.setCategory(category);
                }

                if (itemRequest.getSubcategoryId() != null) {
                    com.example.multi_tanent.production.entity.ProSubCategory subcategory = proSubCategoryRepository
                            .findById(itemRequest.getSubcategoryId())
                            .orElse(null);
                    item.setSubcategory(subcategory);
                }

                item.setQuantity(itemRequest.getQuantity());
                item.setRentalValue(itemRequest.getRentalValue());
                item.setTaxValue(itemRequest.getTaxValue());
                item.setTaxPercentage(itemRequest.getTaxPercentage());
                item.setTaxExempt(itemRequest.isTaxExempt());

                // Calculate line amount
                BigDecimal amount = BigDecimal.ZERO;
                if (item.getRentalValue() != null && item.getQuantity() != null) {
                    // Adhering to RentalQuotationService pattern:
                    amount = item.getRentalValue().multiply(BigDecimal.valueOf(item.getQuantity()));
                }
                item.setAmount(amount);

                updatedItems.add(item);
            }

            order.getItems().clear();
            order.getItems().addAll(updatedItems);
        }
    }

    private void calculateTotals(RentalSalesOrder order) {
        BigDecimal subTotalPerDay = BigDecimal.ZERO;
        BigDecimal totalTaxPerDay = BigDecimal.ZERO;

        if (order.getItems() != null) {
            for (RentalSalesOrderItem item : order.getItems()) {
                if (item.getAmount() != null) {
                    subTotalPerDay = subTotalPerDay.add(item.getAmount());
                }
                if (!item.isTaxExempt() && item.getTaxValue() != null) {
                    totalTaxPerDay = totalTaxPerDay.add(item.getTaxValue());
                }
            }
        }

        order.setSubTotalPerDay(subTotalPerDay);

        BigDecimal discount = order.getTotalDiscount() != null ? order.getTotalDiscount() : BigDecimal.ZERO;

        BigDecimal grossTotal = subTotalPerDay.subtract(discount);
        order.setGrossTotal(grossTotal);

        BigDecimal duration = BigDecimal
                .valueOf(order.getRentalDurationDays() != null ? order.getRentalDurationDays() : 1);

        BigDecimal totalRentalPrice = grossTotal.multiply(duration);
        order.setTotalRentalPrice(totalRentalPrice);

        BigDecimal totalTax = totalTaxPerDay.multiply(duration);
        order.setTotalTax(totalTax);

        BigDecimal otherCharges = order.getOtherCharges() != null ? order.getOtherCharges() : BigDecimal.ZERO;

        order.setNetTotal(totalRentalPrice.add(totalTax).add(otherCharges));
    }

    private RentalSalesOrderResponse mapEntityToResponse(RentalSalesOrder order) {
        RentalSalesOrderResponse response = new RentalSalesOrderResponse();
        response.setId(order.getId());
        response.setOrderDate(order.getOrderDate());
        if (order.getCustomer() != null) {
            response.setCustomerId(order.getCustomer().getId());
            response.setCustomerName(order.getCustomer().getCompanyName());
        }
        if (order.getSalesperson() != null) {
            response.setSalespersonId(order.getSalesperson().getId());
            response.setSalespersonName(order.getSalesperson().getName());
        }
        response.setOrderNumber(order.getOrderNumber());
        response.setReference(order.getReference());
        response.setShipmentDate(order.getShipmentDate());
        response.setFromDate(order.getFromDate());
        response.setToDate(order.getToDate());
        response.setDeliveryLead(order.getDeliveryLead());
        response.setValidity(order.getValidity());
        response.setPaymentTerms(order.getPaymentTerms());
        response.setPriceBasis(order.getPriceBasis());
        response.setDearSir(order.getDearSir());

        response.setRentalDurationDays(order.getRentalDurationDays());
        response.setSubTotalPerDay(order.getSubTotalPerDay());
        response.setTotalRentalPrice(order.getTotalRentalPrice());
        response.setTotalDiscount(order.getTotalDiscount());
        response.setGrossTotal(order.getGrossTotal());
        response.setTotalTax(order.getTotalTax());
        response.setOtherCharges(order.getOtherCharges());
        response.setNetTotal(order.getNetTotal());

        response.setTermsAndConditions(order.getTermsAndConditions());
        response.setNotes(order.getNotes());
        response.setManufacture(order.getManufacture());
        response.setRemarks(order.getRemarks());
        response.setAttachments(order.getAttachments());
        response.setEmailTo(order.getEmailTo());
        response.setStatus(order.getStatus());
        response.setTemplate(order.getTemplate());

        response.setCreatedBy(order.getCreatedBy());
        response.setUpdatedBy(order.getUpdatedBy());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        if (order.getItems() != null) {
            List<RentalSalesOrderItemResponse> itemResponses = order.getItems().stream().map(item -> {
                RentalSalesOrderItemResponse itemResponse = new RentalSalesOrderItemResponse();
                itemResponse.setId(item.getId());
                if (item.getCrmProduct() != null)
                    itemResponse.setCrmProductId(item.getCrmProduct().getId());
                itemResponse.setItemCode(item.getItemCode());
                itemResponse.setItemName(item.getItemName());
                itemResponse.setDescription(item.getDescription());
                if (item.getCategory() != null) {
                    itemResponse.setCategoryId(item.getCategory().getId());
                    itemResponse.setCategoryName(item.getCategory().getName());
                }
                if (item.getSubcategory() != null) {
                    itemResponse.setSubcategoryId(item.getSubcategory().getId());
                    itemResponse.setSubcategoryName(item.getSubcategory().getName());
                }
                itemResponse.setQuantity(item.getQuantity());
                itemResponse.setRentalValue(item.getRentalValue());
                itemResponse.setAmount(item.getAmount());
                itemResponse.setTaxValue(item.getTaxValue());
                itemResponse.setTaxPercentage(item.getTaxPercentage());
                itemResponse.setTaxExempt(item.isTaxExempt());
                return itemResponse;
            }).collect(Collectors.toList());
            response.setItems(itemResponses);
        }
        return response;
    }
}
