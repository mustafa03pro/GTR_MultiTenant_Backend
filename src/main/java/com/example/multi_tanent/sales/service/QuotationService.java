package com.example.multi_tanent.sales.service;

import com.example.multi_tanent.config.TenantContext;
import com.example.multi_tanent.crm.entity.CrmSalesProduct;
import com.example.multi_tanent.crm.repository.CrmSalesProductRepository;
import com.example.multi_tanent.sales.dto.QuotationItemRequest;
import com.example.multi_tanent.sales.dto.QuotationItemResponse;
import com.example.multi_tanent.sales.dto.QuotationRequest;
import com.example.multi_tanent.sales.dto.QuotationResponse;
import com.example.multi_tanent.sales.entity.Quotation;
import com.example.multi_tanent.sales.entity.QuotationItem;
import com.example.multi_tanent.sales.enums.SalesStatus;
import com.example.multi_tanent.sales.repository.QuotationRepository;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "tenantTx")
public class QuotationService {

    private final QuotationRepository quotationRepository;
    private final PartyRepository partyRepository;
    private final CrmSalesProductRepository productRepository;
    private final TenantRepository tenantRepository;
    private final com.example.multi_tanent.production.repository.ProCategoryRepository categoryRepository;
    private final com.example.multi_tanent.production.repository.ProSubCategoryRepository subCategoryRepository;
    private final com.example.multi_tanent.tenant.employee.repository.EmployeeRepository employeeRepository;
    private final com.example.multi_tanent.tenant.service.FileStorageService fileStorageService;

    @Transactional(transactionManager = "tenantTx")
    public QuotationResponse createQuotation(QuotationRequest request,
            org.springframework.web.multipart.MultipartFile[] attachments) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        BaseCustomer customer = partyRepository.findByTenantIdAndId(tenant.getId(), request.getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

        Quotation quotation = new Quotation();
        quotation.setTenant(tenant);
        quotation.setCustomer(customer);
        quotation.setQuotationNumber("QT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        quotation.setStatus(SalesStatus.DRAFT);

        mapRequestToEntity(request, quotation, tenant.getId());

        if (attachments != null && attachments.length > 0) {
            for (org.springframework.web.multipart.MultipartFile file : attachments) {
                if (!file.isEmpty()) {
                    String fileUrl = fileStorageService.storeFile(file, "quotations", false);
                    quotation.getAttachments().add(fileUrl);
                }
            }
        }

        calculateTotals(quotation);

        Quotation savedQuotation = quotationRepository.save(quotation);
        return mapEntityToResponse(savedQuotation);
    }

    @Transactional(transactionManager = "tenantTx")
    public QuotationResponse updateQuotation(Long id, QuotationRequest request,
            org.springframework.web.multipart.MultipartFile[] attachments) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        Quotation quotation = quotationRepository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("Quotation not found"));

        if (request.getCustomerId() != null) {
            BaseCustomer customer = partyRepository.findByTenantIdAndId(tenant.getId(), request.getCustomerId())
                    .orElseThrow(() -> new EntityNotFoundException("Customer not found"));
            quotation.setCustomer(customer);
        }

        mapRequestToEntity(request, quotation, tenant.getId());

        if (attachments != null && attachments.length > 0) {
            for (org.springframework.web.multipart.MultipartFile file : attachments) {
                if (!file.isEmpty()) {
                    String fileUrl = fileStorageService.storeFile(file, "quotations", false);
                    quotation.getAttachments().add(fileUrl);
                }
            }
        }

        calculateTotals(quotation);

        Quotation updatedQuotation = quotationRepository.save(quotation);
        return mapEntityToResponse(updatedQuotation);
    }

    @Transactional(readOnly = true, transactionManager = "tenantTx")
    public QuotationResponse getQuotationById(Long id) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        Quotation quotation = quotationRepository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("Quotation not found"));

        return mapEntityToResponse(quotation);
    }

    @Transactional(readOnly = true, transactionManager = "tenantTx")
    public Page<QuotationResponse> getAllQuotations(String customerName, java.time.LocalDate startDate,
            java.time.LocalDate endDate, String quotationType, SalesStatus status, Long salespersonId,
            Pageable pageable) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        org.springframework.data.jpa.domain.Specification<Quotation> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenant").get("id"), tenant.getId()));

            if (customerName != null && !customerName.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("customer").get("companyName")),
                        "%" + customerName.toLowerCase() + "%"));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("quotationDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("quotationDate"), endDate));
            }
            if (quotationType != null && !quotationType.isEmpty() && !"All".equalsIgnoreCase(quotationType)) {
                try {
                    predicates.add(cb.equal(root.get("quotationType"),
                            com.example.multi_tanent.sales.enums.QuotationType.valueOf(quotationType.toUpperCase())));
                } catch (Exception e) {
                    // ignore invalid type
                }
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (salespersonId != null) {
                predicates.add(cb.equal(root.get("salesperson").get("id"), salespersonId));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<Quotation> page = quotationRepository.findAll(spec, pageable);

        return page.map(this::mapEntityToResponse);
    }

    @Transactional(transactionManager = "tenantTx")
    public void deleteQuotation(Long id) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        Quotation quotation = quotationRepository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("Quotation not found"));

        quotationRepository.delete(quotation);
    }

    public QuotationResponse updateStatus(Long id, SalesStatus status) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        Quotation quotation = quotationRepository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("Quotation not found"));

        quotation.setStatus(status);
        Quotation updatedQuotation = quotationRepository.save(quotation);
        return mapEntityToResponse(updatedQuotation);
    }

    @Transactional(transactionManager = "tenantTx")
    public QuotationResponse updateStatusByNumber(String quotationNumber, SalesStatus status) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        Quotation quotation = quotationRepository.findByQuotationNumberAndTenantId(quotationNumber, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("Quotation not found with number: " + quotationNumber));

        quotation.setStatus(status);
        Quotation updatedQuotation = quotationRepository.save(quotation);
        return mapEntityToResponse(updatedQuotation);
    }

    private void mapRequestToEntity(QuotationRequest request, Quotation quotation, Long tenantId) {
        if (request.getQuotationDate() != null)
            quotation.setQuotationDate(request.getQuotationDate());
        if (request.getReference() != null)
            quotation.setReference(request.getReference());
        if (request.getExpiryDate() != null)
            quotation.setExpiryDate(request.getExpiryDate());
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
            for (QuotationItemRequest itemRequest : request.getItems()) {
                QuotationItem item = new QuotationItem();
                item.setQuotation(quotation);

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

                quotation.getItems().add(item);
            }
        }
    }

    private void calculateTotals(Quotation quotation) {
        BigDecimal subTotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (QuotationItem item : quotation.getItems()) {
            subTotal = subTotal.add(item.getAmount());
            if (!item.isTaxExempt() && item.getTaxValue() != null) {
                totalTax = totalTax.add(item.getTaxValue());
            }
        }

        quotation.setSubTotal(subTotal);
        quotation.setTotalTax(totalTax);

        BigDecimal discount = quotation.getTotalDiscount() != null ? quotation.getTotalDiscount() : BigDecimal.ZERO;
        quotation.setTotalDiscount(discount);

        BigDecimal otherCharges = quotation.getOtherCharges() != null ? quotation.getOtherCharges() : BigDecimal.ZERO;
        quotation.setOtherCharges(otherCharges);

        BigDecimal grossTotal = subTotal.subtract(discount);
        quotation.setGrossTotal(grossTotal);

        quotation.setNetTotal(grossTotal.add(totalTax).add(quotation.getOtherCharges()));
    }

    private QuotationResponse mapEntityToResponse(Quotation quotation) {
        QuotationResponse response = new QuotationResponse();
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
        response.setQuotationType(quotation.getQuotationType());

        List<QuotationItemResponse> itemResponses = quotation.getItems().stream().map(item -> {
            QuotationItemResponse itemResponse = new QuotationItemResponse();
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
        response.setSubTotal(quotation.getSubTotal());
        response.setTotalDiscount(quotation.getTotalDiscount());
        response.setGrossTotal(quotation.getGrossTotal());
        response.setTotalTax(quotation.getTotalTax());
        response.setOtherCharges(quotation.getOtherCharges());
        response.setNetTotal(quotation.getNetTotal());
        response.setTermsAndConditions(quotation.getTermsAndConditions());
        response.setNotes(quotation.getNotes());
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
