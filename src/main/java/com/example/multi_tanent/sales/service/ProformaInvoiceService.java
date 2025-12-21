package com.example.multi_tanent.sales.service;

import com.example.multi_tanent.crm.entity.CrmSalesProduct;
import com.example.multi_tanent.crm.repository.CrmSalesProductRepository;
import com.example.multi_tanent.production.repository.ProCategoryRepository;
import com.example.multi_tanent.production.repository.ProSubCategoryRepository;
import com.example.multi_tanent.sales.dto.*;
import com.example.multi_tanent.sales.entity.ProformaInvoice;
import com.example.multi_tanent.sales.enums.SalesStatus;
import com.example.multi_tanent.sales.entity.ProformaInvoiceItem;
import com.example.multi_tanent.sales.repository.ProformaInvoiceRepository;
import com.example.multi_tanent.spersusers.enitity.BaseCustomer;
import com.example.multi_tanent.spersusers.enitity.Employee;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import com.example.multi_tanent.tenant.employee.repository.EmployeeRepository;
import com.example.multi_tanent.spersusers.repository.PartyRepository;
import com.example.multi_tanent.spersusers.repository.TenantRepository;
import com.example.multi_tanent.config.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProformaInvoiceService {

    private final ProformaInvoiceRepository proformaInvoiceRepository;
    private final TenantRepository tenantRepository;
    private final PartyRepository partyRepository;
    private final EmployeeRepository employeeRepository;
    private final CrmSalesProductRepository crmSalesProductRepository;
    private final ProCategoryRepository proCategoryRepository;
    private final ProSubCategoryRepository proSubCategoryRepository;
    private final com.example.multi_tanent.tenant.service.FileStorageService fileStorageService;
    private final ProformaInvoicePdfService proformaInvoicePdfService;

    @Transactional
    public ProformaInvoiceResponse createProformaInvoice(ProformaInvoiceRequest request, List<MultipartFile> files) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        ProformaInvoice invoice = new ProformaInvoice();
        invoice.setTenant(tenant);

        if (request.getCustomerId() != null) {
            BaseCustomer customer = partyRepository.findByTenantIdAndId(tenant.getId(), request.getCustomerId())
                    .orElseThrow(() -> new EntityNotFoundException("Customer not found"));
            invoice.setCustomer(customer);
        }

        invoice.setInvoiceNumber("INVP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        mapRequestToEntity(request, invoice, tenant.getId());

        if (files != null && !files.isEmpty()) {
            List<String> attachmentUrls = new ArrayList<>();
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String fileName = fileStorageService.storeFile(file, "proforma_invoices", false);
                    attachmentUrls.add(fileName);
                }
            }
            invoice.setAttachments(attachmentUrls);
        }

        calculateTotals(invoice);

        invoice = proformaInvoiceRepository.save(invoice);
        return mapEntityToResponse(invoice);
    }

    @Transactional
    public ProformaInvoiceResponse updateProformaInvoice(Long id, ProformaInvoiceRequest request,
            List<MultipartFile> files) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        ProformaInvoice invoice = proformaInvoiceRepository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("Proforma Invoice not found"));

        if (request.getCustomerId() != null) {
            BaseCustomer customer = partyRepository.findByTenantIdAndId(tenant.getId(), request.getCustomerId())
                    .orElseThrow(() -> new EntityNotFoundException("Customer not found"));
            invoice.setCustomer(customer);
        }

        mapRequestToEntity(request, invoice, tenant.getId());

        if (files != null && !files.isEmpty()) {
            if (invoice.getAttachments() == null) {
                invoice.setAttachments(new ArrayList<>());
            }
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String fileName = fileStorageService.storeFile(file, "proforma_invoices", false);
                    invoice.getAttachments().add(fileName);
                }
            }
        }

        calculateTotals(invoice);

        invoice = proformaInvoiceRepository.save(invoice);
        return mapEntityToResponse(invoice);
    }

    @Transactional(readOnly = true)
    public ProformaInvoiceResponse getProformaInvoiceById(Long id) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        ProformaInvoice invoice = proformaInvoiceRepository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("Proforma Invoice not found"));
        return mapEntityToResponse(invoice);
    }

    @Transactional(readOnly = true)
    public Page<ProformaInvoiceResponse> getAllProformaInvoices(String search, LocalDate fromDate, LocalDate toDate,
            Long salespersonId, Pageable pageable) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        Page<ProformaInvoice> invoices = proformaInvoiceRepository.searchProformaInvoices(tenant.getId(), search,
                fromDate, toDate, salespersonId, pageable);
        return invoices.map(this::mapEntityToResponse);
    }

    @Transactional(readOnly = true)
    public byte[] generatePdf(Long id) {
        ProformaInvoiceResponse invoice = getProformaInvoiceById(id);
        return proformaInvoicePdfService.generateProformaInvoicePdf(invoice);
    }

    @Transactional
    public void deleteProformaInvoice(Long id) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        ProformaInvoice invoice = proformaInvoiceRepository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("Proforma Invoice not found"));
        proformaInvoiceRepository.delete(invoice);
    }

    @Transactional
    public ProformaInvoiceResponse updateStatus(Long id, SalesStatus status) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        ProformaInvoice invoice = proformaInvoiceRepository.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new EntityNotFoundException("Proforma Invoice not found"));

        invoice.setStatus(status);
        ProformaInvoice updatedInvoice = proformaInvoiceRepository.save(invoice);
        return mapEntityToResponse(updatedInvoice);
    }

    // @Transactional
    // public ProformaInvoiceResponse updateStatusByNumber(String invoiceNumber, SalesStatus status) {
    //     String tenantIdentifier = TenantContext.getTenantId();
    //     Tenant tenant = tenantRepository.findByTenantId(tenantIdentifier)
    //             .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

    //     ProformaInvoice invoice = proformaInvoiceRepository.findByInvoiceNumberAndTenantId(invoiceNumber, tenant.getId())
    //             .orElseThrow(() -> new EntityNotFoundException("Proforma Invoice not found with number: " + invoiceNumber));

    //     invoice.setStatus(status);
    //     ProformaInvoice updatedInvoice = proformaInvoiceRepository.save(invoice);
    //     return mapEntityToResponse(updatedInvoice);
    // }

    private void mapRequestToEntity(ProformaInvoiceRequest request, ProformaInvoice invoice, Long tenantId) {
        if (request.getInvoiceLedger() != null)
            invoice.setInvoiceLedger(request.getInvoiceLedger());
        if (request.getInvoiceDate() != null)
            invoice.setInvoiceDate(request.getInvoiceDate());
        if (request.getReference() != null)
            invoice.setReference(request.getReference());
        if (request.getDueDate() != null)
            invoice.setDueDate(request.getDueDate());
        if (request.getDateOfSupply() != null)
            invoice.setDateOfSupply(request.getDateOfSupply());
        if (request.getPoNumber() != null)
            invoice.setPoNumber(request.getPoNumber());

        if (request.getSubTotal() != null)
            invoice.setSubTotal(request.getSubTotal());
        if (request.getTotalDiscount() != null)
            invoice.setTotalDiscount(request.getTotalDiscount());
        if (request.getOtherCharges() != null)
            invoice.setOtherCharges(request.getOtherCharges());

        if (request.getTermsAndConditions() != null)
            invoice.setTermsAndConditions(request.getTermsAndConditions());
        if (request.getNotes() != null)
            invoice.setNotes(request.getNotes());
        if (request.getBankDetails() != null)
            invoice.setBankDetails(request.getBankDetails());
        if (request.getTemplate() != null)
            invoice.setTemplate(request.getTemplate());
        if (request.getEmailTo() != null)
            invoice.setEmailTo(request.getEmailTo());
        if (request.getStatus() != null)
            invoice.setStatus(request.getStatus());

        if (request.getSalespersonId() != null) {
            Employee salesperson = employeeRepository.findById(request.getSalespersonId())
                    .orElse(null);
            invoice.setSalesperson(salesperson);
        }

        if (request.getItems() != null) {
            if (invoice.getItems() != null) {
                invoice.getItems().clear();
            } else {
                invoice.setItems(new ArrayList<>());
            }

            for (ProformaInvoiceItemRequest itemRequest : request.getItems()) {
                ProformaInvoiceItem item = new ProformaInvoiceItem();
                item.setProformaInvoice(invoice);

                if (itemRequest.getCrmProductId() != null) {
                    CrmSalesProduct product = crmSalesProductRepository
                            .findByIdAndTenantId(itemRequest.getCrmProductId(), tenantId)
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Product not found: " + itemRequest.getCrmProductId()));
                    item.setCrmProduct(product);
                    item.setItemCode(
                            itemRequest.getItemCode() != null ? itemRequest.getItemCode() : product.getItemCode());
                    item.setItemName(itemRequest.getItemName() != null ? itemRequest.getItemName() : product.getName());
                } else {
                    item.setItemCode(itemRequest.getItemCode());
                    item.setItemName(itemRequest.getItemName());
                }

                item.setDescription(itemRequest.getDescription());

                if (itemRequest.getCategoryId() != null) {
                    item.setCategory(proCategoryRepository.findById(itemRequest.getCategoryId()).orElse(null));
                }
                if (itemRequest.getSubcategoryId() != null) {
                    item.setSubcategory(proSubCategoryRepository.findById(itemRequest.getSubcategoryId()).orElse(null));
                }

                item.setQuantity(itemRequest.getQuantity());
                item.setRate(itemRequest.getRate());
                item.setTaxValue(itemRequest.getTaxValue());
                item.setTaxPercentage(itemRequest.getTaxPercentage());
                item.setTaxExempt(itemRequest.isTaxExempt());

                BigDecimal amount = BigDecimal.ZERO;
                if (item.getRate() != null && item.getQuantity() != null) {
                    amount = item.getRate().multiply(BigDecimal.valueOf(item.getQuantity()));
                }
                item.setAmount(amount);

                invoice.getItems().add(item);
            }
        }
    }

    private void calculateTotals(ProformaInvoice invoice) {
        BigDecimal subTotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        if (invoice.getItems() != null) {
            for (ProformaInvoiceItem item : invoice.getItems()) {
                if (item.getAmount() != null) {
                    subTotal = subTotal.add(item.getAmount());
                }
                if (!item.isTaxExempt() && item.getTaxValue() != null) {
                    totalTax = totalTax.add(item.getTaxValue());
                }
            }
        }

        invoice.setSubTotal(subTotal);

        BigDecimal discount = invoice.getTotalDiscount() != null ? invoice.getTotalDiscount() : BigDecimal.ZERO;

        BigDecimal grossTotal = subTotal.subtract(discount);
        invoice.setGrossTotal(grossTotal);

        invoice.setTotalTax(totalTax);

        BigDecimal otherCharges = invoice.getOtherCharges() != null ? invoice.getOtherCharges() : BigDecimal.ZERO;

        invoice.setNetTotal(grossTotal.add(totalTax).add(otherCharges));
    }

    private ProformaInvoiceResponse mapEntityToResponse(ProformaInvoice invoice) {
        ProformaInvoiceResponse response = new ProformaInvoiceResponse();
        response.setId(invoice.getId());
        response.setInvoiceLedger(invoice.getInvoiceLedger());
        response.setInvoiceDate(invoice.getInvoiceDate());

        if (invoice.getCustomer() != null) {
            response.setCustomerId(invoice.getCustomer().getId());
            response.setCustomerName(invoice.getCustomer().getCompanyName());
        }

        response.setInvoiceNumber(invoice.getInvoiceNumber());
        response.setReference(invoice.getReference());
        response.setDueDate(invoice.getDueDate());
        response.setDateOfSupply(invoice.getDateOfSupply());

        if (invoice.getSalesperson() != null) {
            response.setSalespersonId(invoice.getSalesperson().getId());
            response.setSalespersonName(
                    invoice.getSalesperson().getFirstName() + " " + invoice.getSalesperson().getLastName());
        }

        response.setPoNumber(invoice.getPoNumber());

        response.setSubTotal(invoice.getSubTotal());
        response.setTotalDiscount(invoice.getTotalDiscount());
        response.setGrossTotal(invoice.getGrossTotal());
        response.setTotalTax(invoice.getTotalTax());
        response.setOtherCharges(invoice.getOtherCharges());
        response.setNetTotal(invoice.getNetTotal());

        response.setAttachments(invoice.getAttachments());
        response.setTermsAndConditions(invoice.getTermsAndConditions());
        response.setNotes(invoice.getNotes());
        response.setBankDetails(invoice.getBankDetails());
        response.setTemplate(invoice.getTemplate());
        response.setEmailTo(invoice.getEmailTo());
        response.setStatus(invoice.getStatus());

        response.setCreatedBy(invoice.getCreatedBy());
        response.setUpdatedBy(invoice.getUpdatedBy());

        if (invoice.getItems() != null) {
            response.setItems(invoice.getItems().stream().map(item -> {
                ProformaInvoiceItemResponse itemResponse = new ProformaInvoiceItemResponse();
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
                itemResponse.setRate(item.getRate());
                itemResponse.setAmount(item.getAmount());
                itemResponse.setTaxValue(item.getTaxValue());
                itemResponse.setTaxExempt(item.isTaxExempt());
                itemResponse.setTaxPercentage(item.getTaxPercentage());

                return itemResponse;
            }).collect(Collectors.toList()));
        }

        return response;
    }
}
