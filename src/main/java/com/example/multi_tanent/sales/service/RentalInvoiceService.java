package com.example.multi_tanent.sales.service;

import com.example.multi_tanent.config.TenantContext;
import com.example.multi_tanent.crm.repository.CrmSalesProductRepository;
import com.example.multi_tanent.tenant.service.FileStorageService;
import com.example.multi_tanent.production.repository.ProCategoryRepository;
import com.example.multi_tanent.production.repository.ProSubCategoryRepository;
import com.example.multi_tanent.sales.dto.RentalInvoiceItemRequest;
import com.example.multi_tanent.sales.dto.RentalInvoiceItemResponse;
import com.example.multi_tanent.sales.dto.RentalInvoiceRequest;
import com.example.multi_tanent.sales.dto.RentalInvoiceResponse;
import com.example.multi_tanent.sales.entity.RentalInvoice;
import com.example.multi_tanent.sales.entity.RentalInvoiceItem;
import com.example.multi_tanent.sales.repository.RentalInvoiceRepository;
import com.example.multi_tanent.spersusers.enitity.Employee;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import com.example.multi_tanent.spersusers.repository.BaseCustomerRepository;
import com.example.multi_tanent.spersusers.repository.TenantRepository;
import com.example.multi_tanent.tenant.employee.repository.EmployeeRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RentalInvoiceService {

    private final RentalInvoiceRepository rentalInvoiceRepository;
    private final TenantRepository tenantRepository;
    private final BaseCustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final ProCategoryRepository categoryRepository;
    private final ProSubCategoryRepository subCategoryRepository;
    private final CrmSalesProductRepository productRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public RentalInvoiceResponse createRentalInvoice(RentalInvoiceRequest request, MultipartFile[] attachments) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        RentalInvoice rentalInvoice = mapRequestToEntity(request, tenant);
        rentalInvoice = rentalInvoiceRepository.save(rentalInvoice);

        // Handle attachments if any
        if (attachments != null && attachments.length > 0) {
            List<String> attachmentUrls = java.util.Arrays.stream(attachments)
                    .map(file -> fileStorageService.storeFile(file, "rental_invoices", false))
                    .collect(Collectors.toList());
            rentalInvoice.setAttachments(attachmentUrls);
            rentalInvoice = rentalInvoiceRepository.save(rentalInvoice);
        }

        return mapEntityToResponse(rentalInvoice);
    }

    @Transactional
    public RentalInvoiceResponse updateRentalInvoice(Long id, RentalInvoiceRequest request,
            MultipartFile[] newAttachments) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        RentalInvoice rentalInvoice = rentalInvoiceRepository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("Rental Invoice not found"));

        updateEntityFromRequest(rentalInvoice, request);

        if (newAttachments != null && newAttachments.length > 0) {
            List<String> attachmentUrls = java.util.Arrays.stream(newAttachments)
                    .map(file -> fileStorageService.storeFile(file, "rental_invoices", false))
                    .collect(Collectors.toList());
            rentalInvoice.getAttachments().addAll(attachmentUrls);
        }

        return mapEntityToResponse(rentalInvoiceRepository.save(rentalInvoice));
    }

    @Transactional(readOnly = true)
    public RentalInvoiceResponse getRentalInvoiceById(Long id) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        return rentalInvoiceRepository.findByIdAndTenantId(id, tenant.getId())
                .map(this::mapEntityToResponse)
                .orElseThrow(() -> new EntityNotFoundException("Rental Invoice not found"));
    }

    @Transactional(readOnly = true)
    public Page<RentalInvoiceResponse> getAllRentalInvoices(String search, java.time.LocalDate fromDate,
            java.time.LocalDate toDate, Long salespersonId, Pageable pageable) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        org.springframework.data.jpa.domain.Specification<RentalInvoice> spec = (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            predicates.add(cb.equal(root.get("tenant").get("id"), tenant.getId()));

            if (search != null && !search.isEmpty()) {
                String searchLike = "%" + search.toLowerCase() + "%";
                jakarta.persistence.criteria.Predicate customerName = cb
                        .like(cb.lower(root.get("customer").get("companyName")), searchLike);
                jakarta.persistence.criteria.Predicate invoiceNumber = cb.like(cb.lower(root.get("invoiceNumber")),
                        searchLike);
                jakarta.persistence.criteria.Predicate reference = cb.like(cb.lower(root.get("reference")), searchLike);
                predicates.add(cb.or(customerName, invoiceNumber, reference));
            }

            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("invoiceDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("invoiceDate"), toDate));
            }
            if (salespersonId != null) {
                predicates.add(cb.equal(root.get("salesperson").get("id"), salespersonId));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<RentalInvoice> page = rentalInvoiceRepository.findAll(spec, pageable);
        // Items are now EAGER fetched, no need for manual init

        return page.map(this::mapEntityToResponse);
    }

    @Transactional
    public void deleteRentalInvoice(Long id) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        RentalInvoice rentalInvoice = rentalInvoiceRepository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("Rental Invoice not found"));

        rentalInvoiceRepository.delete(rentalInvoice);
    }

    private RentalInvoice mapRequestToEntity(RentalInvoiceRequest request, Tenant tenant) {
        RentalInvoice rentalInvoice = new RentalInvoice();
        rentalInvoice.setTenant(tenant);
        updateEntityFromRequest(rentalInvoice, request);

        // Generate number if not provided (simple logic, likely needs a sequence
        // generator in real app)
        if (rentalInvoice.getInvoiceNumber() == null) {
            rentalInvoice.setInvoiceNumber("INVR-" + System.currentTimeMillis()); // Placeholder
        }

        return rentalInvoice;
    }

    private void updateEntityFromRequest(RentalInvoice rentalInvoice, RentalInvoiceRequest request) {
        rentalInvoice.setInvoiceLedger(request.getInvoiceLedger());
        rentalInvoice.setInvoiceDate(request.getInvoiceDate());

        if (request.getCustomerId() != null) {
            rentalInvoice.setCustomer(customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new EntityNotFoundException("Customer not found")));
        }

        rentalInvoice.setDoNumber(request.getDoNumber());
        rentalInvoice.setLpoNumber(request.getLpoNumber());
        rentalInvoice.setRequiredDate(request.getRequiredDate());
        rentalInvoice.setDueDate(request.getDueDate());

        if (request.getSalespersonId() != null) {
            rentalInvoice.setSalesperson(employeeRepository.findById(request.getSalespersonId())
                    .orElseThrow(() -> new EntityNotFoundException("Salesperson not found")));
        }

        rentalInvoice.setPoNumber(request.getPoNumber());
        rentalInvoice.setReference(request.getReference());
        rentalInvoice.setInvoiceType(request.getInvoiceType());
        rentalInvoice.setEnableGrossNetWeight(request.getEnableGrossNetWeight());

        rentalInvoice.setSubTotal(request.getSubTotal());
        rentalInvoice.setTotalDiscount(request.getTotalDiscount());
        rentalInvoice.setGrossTotal(request.getGrossTotal());
        rentalInvoice.setTotalTax(request.getTotalTax());
        rentalInvoice.setOtherCharges(request.getOtherCharges());
        rentalInvoice.setNetTotal(request.getNetTotal());
        rentalInvoice.setStatus(request.getStatus());
        rentalInvoice.setTermsAndConditions(request.getTermsAndConditions());
        rentalInvoice.setNotes(request.getNotes());
        rentalInvoice.setTemplate(request.getTemplate());
        rentalInvoice.setEmailTo(request.getEmailTo());

        // Handle items
        if (request.getItems() != null) {
            rentalInvoice.getItems().clear();
            List<RentalInvoiceItem> items = request.getItems().stream()
                    .map(itemRequest -> mapItemRequestToEntity(itemRequest, rentalInvoice))
                    .collect(Collectors.toList());
            rentalInvoice.getItems().addAll(items);
        }
    }

    private RentalInvoiceItem mapItemRequestToEntity(RentalInvoiceItemRequest request, RentalInvoice rentalInvoice) {
        RentalInvoiceItem item = new RentalInvoiceItem();
        item.setRentalInvoice(rentalInvoice);

        if (request.getCrmProductId() != null) {
            item.setCrmProduct(productRepository.findById(request.getCrmProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Product not found")));
        }

        item.setItemName(request.getItemName());
        item.setDescription(request.getDescription());

        if (request.getCategoryId() != null) {
            item.setCategory(categoryRepository.findById(request.getCategoryId()).orElse(null));
        }
        if (request.getSubcategoryId() != null) {
            item.setSubcategory(subCategoryRepository.findById(request.getSubcategoryId()).orElse(null));
        }

        item.setQuantity(request.getQuantity());
        item.setDuration(request.getDuration());
        item.setRentalValue(request.getRentalValue());
        item.setAmount(request.getAmount());
        item.setTaxValue(request.getTaxValue());
        item.setTaxExempt(request.isTaxExempt());
        item.setTaxPercentage(request.getTaxPercentage());

        return item;
    }

    private RentalInvoiceResponse mapEntityToResponse(RentalInvoice entity) {
        RentalInvoiceResponse response = new RentalInvoiceResponse();
        response.setId(entity.getId());
        response.setInvoiceLedger(entity.getInvoiceLedger());
        response.setInvoiceDate(entity.getInvoiceDate());

        if (entity.getCustomer() != null) {
            response.setCustomerId(entity.getCustomer().getId());
            response.setCustomerName(entity.getCustomer().getCompanyName());
        }

        response.setInvoiceNumber(entity.getInvoiceNumber());
        response.setDoNumber(entity.getDoNumber());
        response.setLpoNumber(entity.getLpoNumber());
        response.setRequiredDate(entity.getRequiredDate());
        response.setDueDate(entity.getDueDate());

        if (entity.getSalesperson() != null) {
            response.setSalespersonId(entity.getSalesperson().getId());
            response.setSalespersonName(entity.getSalesperson().getName());
        }

        response.setPoNumber(entity.getPoNumber());
        response.setReference(entity.getReference());
        response.setInvoiceType(entity.getInvoiceType());
        response.setEnableGrossNetWeight(entity.getEnableGrossNetWeight());

        response.setSubTotal(entity.getSubTotal());
        response.setTotalDiscount(entity.getTotalDiscount());
        response.setGrossTotal(entity.getGrossTotal());
        response.setTotalTax(entity.getTotalTax());
        response.setOtherCharges(entity.getOtherCharges());
        response.setNetTotal(entity.getNetTotal());
        response.setStatus(entity.getStatus());
        response.setTermsAndConditions(entity.getTermsAndConditions());
        response.setNotes(entity.getNotes());
        response.setAttachments(entity.getAttachments());
        response.setTemplate(entity.getTemplate());
        response.setEmailTo(entity.getEmailTo());
        response.setCreatedBy(entity.getCreatedBy());
        response.setCreatedAt(entity.getCreatedAt());

        if (entity.getItems() != null) {
            response.setItems(entity.getItems().stream()
                    .map(this::mapItemEntityToResponse)
                    .collect(Collectors.toList()));
        }

        return response;
    }

    private RentalInvoiceItemResponse mapItemEntityToResponse(RentalInvoiceItem entity) {
        RentalInvoiceItemResponse response = new RentalInvoiceItemResponse();
        response.setId(entity.getId());

        if (entity.getCrmProduct() != null) {
            response.setCrmProductId(entity.getCrmProduct().getId());
            response.setItemCode(entity.getCrmProduct().getItemCode()); // Assuming code exists
        }

        response.setItemName(entity.getItemName());
        response.setDescription(entity.getDescription());

        if (entity.getCategory() != null) {
            response.setCategoryId(entity.getCategory().getId());
            response.setCategoryName(entity.getCategory().getName());
        }

        if (entity.getSubcategory() != null) {
            response.setSubcategoryId(entity.getSubcategory().getId());
            response.setSubcategoryName(entity.getSubcategory().getName());
        }

        response.setQuantity(entity.getQuantity());
        response.setDuration(entity.getDuration());
        response.setRentalValue(entity.getRentalValue());
        response.setAmount(entity.getAmount());
        response.setTaxValue(entity.getTaxValue());
        response.setTaxExempt(entity.isTaxExempt());
        response.setTaxPercentage(entity.getTaxPercentage());

        return response;
    }
}
