package com.example.multi_tanent.sales.service;

import com.example.multi_tanent.config.TenantContext;
import com.example.multi_tanent.crm.entity.CrmSalesProduct;
import com.example.multi_tanent.crm.repository.CrmSalesProductRepository;
import com.example.multi_tanent.sales.dto.RentalQuotationItemRequest;
import com.example.multi_tanent.sales.dto.RentalQuotationItemResponse;
import com.example.multi_tanent.sales.dto.RentalQuotationRequest;
import com.example.multi_tanent.sales.dto.RentalQuotationResponse;
import com.example.multi_tanent.sales.entity.RentalQuotation;
import com.example.multi_tanent.sales.entity.RentalQuotationItem;
import com.example.multi_tanent.sales.enums.SalesStatus;
import com.example.multi_tanent.sales.repository.RentalQuotationRepository;
import com.example.multi_tanent.spersusers.enitity.BaseCustomer;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import com.example.multi_tanent.spersusers.repository.PartyRepository;
import com.example.multi_tanent.spersusers.repository.TenantRepository;
import com.example.multi_tanent.tenant.employee.repository.EmployeeRepository;
import com.example.multi_tanent.tenant.service.FileStorageService;
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
public class RentalQuotationService {

    private final RentalQuotationRepository rentalQuotationRepository;
    private final PartyRepository partyRepository;
    private final CrmSalesProductRepository productRepository;
    private final TenantRepository tenantRepository;
    private final com.example.multi_tanent.production.repository.ProCategoryRepository categoryRepository;
    private final com.example.multi_tanent.production.repository.ProSubCategoryRepository subCategoryRepository;
    private final EmployeeRepository employeeRepository;
    private final FileStorageService fileStorageService;

    @Transactional(transactionManager = "tenantTx")
    public RentalQuotationResponse createRentalQuotation(RentalQuotationRequest request,
            org.springframework.web.multipart.MultipartFile[] attachments) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        BaseCustomer customer = partyRepository.findByTenantIdAndId(tenant.getId(), request.getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

        RentalQuotation quotation = new RentalQuotation();
        quotation.setTenant(tenant);
        quotation.setCustomer(customer);
        quotation.setQuotationNumber("RQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        quotation.setStatus(SalesStatus.DRAFT);

        mapRequestToEntity(request, quotation, tenant.getId());

        if (attachments != null && attachments.length > 0) {
            for (org.springframework.web.multipart.MultipartFile file : attachments) {
                if (!file.isEmpty()) {
                    String fileUrl = fileStorageService.storeFile(file, "rental_quotations", false);
                    quotation.getAttachments().add(fileUrl);
                }
            }
        }

        calculateTotals(quotation);

        RentalQuotation savedQuotation = rentalQuotationRepository.save(quotation);
        return mapEntityToResponse(savedQuotation);
    }

    @Transactional(transactionManager = "tenantTx")
    public RentalQuotationResponse updateRentalQuotation(Long id, RentalQuotationRequest request,
            org.springframework.web.multipart.MultipartFile[] attachments) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        RentalQuotation quotation = rentalQuotationRepository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("Rental Quotation not found"));

        if (request.getCustomerId() != null) {
            BaseCustomer customer = partyRepository.findByTenantIdAndId(tenant.getId(), request.getCustomerId())
                    .orElseThrow(() -> new EntityNotFoundException("Customer not found"));
            quotation.setCustomer(customer);
        }

        mapRequestToEntity(request, quotation, tenant.getId());

        if (attachments != null && attachments.length > 0) {
            for (org.springframework.web.multipart.MultipartFile file : attachments) {
                if (!file.isEmpty()) {
                    String fileUrl = fileStorageService.storeFile(file, "rental_quotations", false);
                    quotation.getAttachments().add(fileUrl);
                }
            }
        }

        calculateTotals(quotation);

        RentalQuotation updatedQuotation = rentalQuotationRepository.save(quotation);
        return mapEntityToResponse(updatedQuotation);
    }

    @Transactional(readOnly = true, transactionManager = "tenantTx")
    public RentalQuotationResponse getRentalQuotationById(Long id) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        RentalQuotation quotation = rentalQuotationRepository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("Rental Quotation not found"));

        return mapEntityToResponse(quotation);
    }

    @Transactional(readOnly = true, transactionManager = "tenantTx")
    public Page<RentalQuotationResponse> getAllRentalQuotations(Pageable pageable) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        return rentalQuotationRepository.findByTenantId(tenant.getId(), pageable)
                .map(this::mapEntityToResponse);
    }

    @Transactional(readOnly = true, transactionManager = "tenantTx")
    public Page<RentalQuotationResponse> getAllRentalQuotations(String customerName, java.time.LocalDate fromDate,
            java.time.LocalDate toDate, com.example.multi_tanent.sales.enums.SalesStatus status,
            Long salespersonId, com.example.multi_tanent.sales.enums.QuotationType quotationType, Pageable pageable) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant;

        if (tenantIdentifier == null) {
            tenant = tenantRepository.findFirstByOrderByIdAsc()
                    .orElseThrow(() -> new EntityNotFoundException("No default tenant found"));
        } else {
            tenant = tenantRepository.findByTenantId(tenantIdentifier)
                    .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));
        }

        return rentalQuotationRepository
                .searchRentalQuotations(tenant.getId(), customerName, fromDate, toDate, status, salespersonId,
                        quotationType, pageable)
                .map(this::mapEntityToResponse);
    }

    @Transactional(transactionManager = "tenantTx")
    public void deleteRentalQuotation(Long id) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        RentalQuotation quotation = rentalQuotationRepository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("Rental Quotation not found"));

        rentalQuotationRepository.delete(quotation);
    }

    @Transactional(transactionManager = "tenantTx")
    public RentalQuotationResponse updateStatus(Long id, SalesStatus status) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        RentalQuotation quotation = rentalQuotationRepository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("Rental Quotation not found"));

        quotation.setStatus(status);
        RentalQuotation updatedQuotation = rentalQuotationRepository.save(quotation);
        return mapEntityToResponse(updatedQuotation);
    }

    @Transactional(transactionManager = "tenantTx")
    public RentalQuotationResponse updateStatusByQuotationNumber(String quotationNumber, SalesStatus status) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        RentalQuotation quotation = rentalQuotationRepository
                .findByQuotationNumberAndTenantId(quotationNumber, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Rental Quotation not found with number: " + quotationNumber));

        quotation.setStatus(status);
        RentalQuotation updatedQuotation = rentalQuotationRepository.save(quotation);
        return mapEntityToResponse(updatedQuotation);
    }

    private void mapRequestToEntity(RentalQuotationRequest request, RentalQuotation quotation, Long tenantId) {
        if (request.getQuotationDate() != null)
            quotation.setQuotationDate(request.getQuotationDate());
        if (request.getReference() != null)
            quotation.setReference(request.getReference());
        if (request.getExpiryDate() != null)
            quotation.setExpiryDate(request.getExpiryDate());
        if (request.getDeliveryLead() != null)
            quotation.setDeliveryLead(request.getDeliveryLead());
        if (request.getValidity() != null)
            quotation.setValidity(request.getValidity());
        if (request.getPaymentTerms() != null)
            quotation.setPaymentTerms(request.getPaymentTerms());
        if (request.getPriceBasis() != null)
            quotation.setPriceBasis(request.getPriceBasis());
        if (request.getDearSir() != null)
            quotation.setDearSir(request.getDearSir());
        if (request.getRentalDurationDays() != null)
            quotation.setRentalDurationDays(request.getRentalDurationDays());

        if (request.getSalespersonId() != null) {
            com.example.multi_tanent.spersusers.enitity.Employee salesperson = employeeRepository
                    .findById(request.getSalespersonId())
                    .orElseThrow(() -> new EntityNotFoundException("Salesperson not found"));
            quotation.setSalesperson(salesperson);
        }
        if (request.getQuotationType() != null)
            quotation.setQuotationType(request.getQuotationType());
        if (request.getTermsAndConditions() != null)
            quotation.setTermsAndConditions(request.getTermsAndConditions());
        if (request.getNotes() != null)
            quotation.setNotes(request.getNotes());
        if (request.getManufacture() != null)
            quotation.setManufacture(request.getManufacture());
        if (request.getRemarks() != null)
            quotation.setRemarks(request.getRemarks());
        if (request.getEmailTo() != null)
            quotation.setEmailTo(request.getEmailTo());
        if (request.getStatus() != null)
            quotation.setStatus(request.getStatus());
        if (request.getTemplate() != null)
            quotation.setTemplate(request.getTemplate());
        if (request.getTotalDiscount() != null)
            quotation.setTotalDiscount(request.getTotalDiscount());
        if (request.getOtherCharges() != null)
            quotation.setOtherCharges(request.getOtherCharges());

        if (request.getItems() != null) {
            quotation.getItems().clear();
            for (RentalQuotationItemRequest itemRequest : request.getItems()) {
                RentalQuotationItem item = new RentalQuotationItem();
                item.setRentalQuotation(quotation);

                CrmSalesProduct product = productRepository.findByIdAndTenantId(itemRequest.getCrmProductId(), tenantId)
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Product not found: " + itemRequest.getCrmProductId()));

                item.setCrmProduct(product);
                item.setItemCode(itemRequest.getItemCode() != null ? itemRequest.getItemCode() : product.getItemCode());
                item.setItemName(itemRequest.getItemName() != null ? itemRequest.getItemName() : product.getName());
                item.setDescription(itemRequest.getDescription());

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
                item.setRentalValue(itemRequest.getRentalValue());
                item.setTaxValue(itemRequest.getTaxValue());
                item.setTaxPercentage(itemRequest.getTaxPercentage());
                item.setTaxExempt(itemRequest.isTaxExempt());

                // Calculate amount for item
                BigDecimal amount = item.getRentalValue().multiply(BigDecimal.valueOf(item.getQuantity()));
                item.setAmount(amount);

                quotation.getItems().add(item);
            }
        }
    }

    private void calculateTotals(RentalQuotation quotation) {
        BigDecimal subTotalPerDay = BigDecimal.ZERO;
        BigDecimal totalTaxPerDay = BigDecimal.ZERO;

        for (RentalQuotationItem item : quotation.getItems()) {
            subTotalPerDay = subTotalPerDay.add(item.getAmount());
            if (!item.isTaxExempt() && item.getTaxValue() != null) {
                totalTaxPerDay = totalTaxPerDay.add(item.getTaxValue());
            }
        }

        quotation.setSubTotalPerDay(subTotalPerDay);

        BigDecimal discount = quotation.getTotalDiscount() != null ? quotation.getTotalDiscount() : BigDecimal.ZERO;
        quotation.setTotalDiscount(discount);

        BigDecimal grossTotal = subTotalPerDay.subtract(discount);
        quotation.setGrossTotal(grossTotal);

        BigDecimal duration = BigDecimal
                .valueOf(quotation.getRentalDurationDays() != null ? quotation.getRentalDurationDays() : 1);
        BigDecimal totalRentalPrice = grossTotal.multiply(duration);
        quotation.setTotalRentalPrice(totalRentalPrice);

        BigDecimal totalTax = totalTaxPerDay.multiply(duration);
        quotation.setTotalTax(totalTax);

        BigDecimal otherCharges = quotation.getOtherCharges() != null ? quotation.getOtherCharges() : BigDecimal.ZERO;
        quotation.setOtherCharges(otherCharges);

        quotation.setNetTotal(totalRentalPrice.add(totalTax).add(otherCharges));
    }

    private RentalQuotationResponse mapEntityToResponse(RentalQuotation quotation) {
        RentalQuotationResponse response = new RentalQuotationResponse();
        response.setId(quotation.getId());
        response.setQuotationDate(quotation.getQuotationDate());
        if (quotation.getCustomer() != null) {
            response.setCustomerId(quotation.getCustomer().getId());
            response.setCustomerName(quotation.getCustomer().getCompanyName());
        }
        if (quotation.getSalesperson() != null) {
            response.setSalespersonId(quotation.getSalesperson().getId());
            String fullName = quotation.getSalesperson().getFirstName();
            if (quotation.getSalesperson().getLastName() != null) {
                fullName += " " + quotation.getSalesperson().getLastName();
            }
            response.setSalespersonName(fullName);
        }
        response.setQuotationNumber(quotation.getQuotationNumber());
        response.setReference(quotation.getReference());
        response.setExpiryDate(quotation.getExpiryDate());
        response.setDeliveryLead(quotation.getDeliveryLead());
        response.setValidity(quotation.getValidity());
        response.setPaymentTerms(quotation.getPaymentTerms());
        response.setPriceBasis(quotation.getPriceBasis());
        response.setDearSir(quotation.getDearSir());
        response.setQuotationType(quotation.getQuotationType());
        response.setRentalDurationDays(quotation.getRentalDurationDays());

        List<RentalQuotationItemResponse> itemResponses = quotation.getItems().stream().map(item -> {
            RentalQuotationItemResponse itemResponse = new RentalQuotationItemResponse();
            itemResponse.setId(item.getId());
            if (item.getCrmProduct() != null) {
                itemResponse.setCrmProductId(item.getCrmProduct().getId());
            }
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
        response.setSubTotalPerDay(quotation.getSubTotalPerDay());
        response.setTotalRentalPrice(quotation.getTotalRentalPrice());
        response.setTotalDiscount(quotation.getTotalDiscount());
        response.setGrossTotal(quotation.getGrossTotal());
        response.setTotalTax(quotation.getTotalTax());
        response.setOtherCharges(quotation.getOtherCharges());
        response.setNetTotal(quotation.getNetTotal());
        response.setTermsAndConditions(quotation.getTermsAndConditions());
        response.setNotes(quotation.getNotes());
        response.setManufacture(quotation.getManufacture());
        response.setRemarks(quotation.getRemarks());
        response.setAttachments(new ArrayList<>(quotation.getAttachments()));
        response.setEmailTo(quotation.getEmailTo());
        response.setStatus(quotation.getStatus());
        response.setTemplate(quotation.getTemplate());
        response.setCreatedBy(quotation.getCreatedBy());
        response.setUpdatedBy(quotation.getUpdatedBy());
        response.setCreatedAt(quotation.getCreatedAt());
        response.setUpdatedAt(quotation.getUpdatedAt());

        return response;
    }
}
