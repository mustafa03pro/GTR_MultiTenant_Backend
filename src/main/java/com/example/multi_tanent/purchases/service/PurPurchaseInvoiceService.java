// package com.example.multi_tanent.purchases.service;

// import com.example.multi_tanent.config.TenantContext;
// import com.example.multi_tanent.purchases.dto.*;
// import com.example.multi_tanent.purchases.entity.PurPurchaseInvoice;
// import com.example.multi_tanent.purchases.entity.PurPurchaseInvoiceAttachment;
// import com.example.multi_tanent.purchases.entity.PurPurchaseInvoiceItem;
// import com.example.multi_tanent.purchases.repository.PurPurchaseInvoiceRepository;
// import com.example.multi_tanent.production.repository.ProCategoryRepository;
// import com.example.multi_tanent.production.repository.ProRawMaterialsRepository;
// import com.example.multi_tanent.production.repository.ProSubCategoryRepository;
// import com.example.multi_tanent.production.repository.ProTaxRepository;
// import com.example.multi_tanent.production.repository.ProUnitRepository;
// import com.example.multi_tanent.spersusers.enitity.Tenant;
// import com.example.multi_tanent.spersusers.repository.PartyRepository;
// import com.example.multi_tanent.spersusers.repository.TenantRepository;
// import jakarta.persistence.EntityNotFoundException;
// import lombok.RequiredArgsConstructor;
// import org.springframework.data.domain.*;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import java.math.BigDecimal;
// import java.math.RoundingMode;
// import java.time.LocalDateTime;
// import java.util.*;
// import java.util.stream.Collectors;

// @Service
// @RequiredArgsConstructor
// @Transactional("tenantTx")
// public class PurPurchaseInvoiceService {

//     private final PurPurchaseInvoiceRepository repo;
//     private final TenantRepository tenantRepo;
//     private final PartyRepository supplierRepo;
//     private final ProRawMaterialsRepository rawMaterialRepo;
//     private final ProUnitRepository unitRepo;
//     private final ProTaxRepository taxRepo;
//     private final ProCategoryRepository categoryRepo;
//     private final ProSubCategoryRepository subCategoryRepo;

//     private Tenant currentTenant() {
//         String key = TenantContext.getTenantId();
//         return tenantRepo.findFirstByOrderByIdAsc()
//                 .orElseThrow(() -> new IllegalStateException("Tenant not resolved for key: " + key));
//     }

//     public PurPurchaseInvoiceResponse create(PurPurchaseInvoiceRequest req) {
//         Tenant t = currentTenant();

//         PurPurchaseInvoice invoice = new PurPurchaseInvoice();
//         invoice.setBillLedger(req.getBillLedger());
//         invoice.setBillNumber(req.getBillNumber());
//         invoice.setOrderNumber(req.getOrderNumber());
//         invoice.setBillDate(req.getBillDate());
//         invoice.setDueDate(req.getDueDate());
//         invoice.setBillType(req.getBillType());
//         invoice.setGrossNetEnabled(Optional.ofNullable(req.getGrossNetEnabled()).orElse(Boolean.FALSE));
//         invoice.setNotes(req.getNotes());
//         invoice.setOtherCharges(req.getOtherCharges() == null ? BigDecimal.ZERO : req.getOtherCharges());
//         invoice.setCreatedBy(req.getCreatedBy());
//         invoice.setCreatedAt(LocalDateTime.now());
//         invoice.setTenant(t);

//         if (req.getSupplierId() != null) {
//             invoice.setSupplier(supplierRepo.findById(req.getSupplierId())
//                     .orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + req.getSupplierId())));
//         }

//         // lines
//         if (req.getLines() != null && !req.getLines().isEmpty()) {
//             invoice.setLines(new ArrayList<>());
//             for (PurPurchaseInvoiceItemRequest lr : req.getLines()) {
//                 PurPurchaseInvoiceItem li = mapLineRequest(lr);
//                 invoice.getLines().add(li);
//                 li.setPurchaseInvoice(invoice);
//             }
//         }

//         // attachments
//         if (req.getAttachments() != null && !req.getAttachments().isEmpty()) {
//             invoice.setAttachments(new ArrayList<>());
//             for (PurPurchaseInvoiceAttachmentRequest ar : req.getAttachments()) {
//                 PurPurchaseInvoiceAttachment a = new PurPurchaseInvoiceAttachment();
//                 a.setFileName(ar.getFileName());
//                 a.setFilePath(ar.getFilePath());
//                 a.setUploadedBy(ar.getUploadedBy());
//                 a.setUploadedAt(ar.getUploadedAt());
//                 a.setPurchaseInvoice(invoice);
//                 invoice.getAttachments().add(a);
//             }
//         }

//         computeTotals(invoice);

//         PurPurchaseInvoice saved = repo.save(invoice);
//         return toResponse(saved);
//     }

//     @Transactional(readOnly = true)
//     public Page<PurPurchaseInvoiceResponse> list(Pageable pageable) {
//         Tenant t = currentTenant();
//         return repo.findByTenantId(t.getId(), pageable).map(this::toResponse);
//     }

//     @Transactional(readOnly = true)
//     public PurPurchaseInvoiceResponse getById(Long id) {
//         Tenant t = currentTenant();
//         PurPurchaseInvoice inv = repo.findByIdAndTenantId(id, t.getId())
//                 .orElseThrow(() -> new EntityNotFoundException("Purchase invoice not found: " + id));
//         return toResponse(inv);
//     }

//     public PurPurchaseInvoiceResponse update(Long id, PurPurchaseInvoiceRequest req) {
//         Tenant t = currentTenant();
//         PurPurchaseInvoice invoice = repo.findByIdAndTenantId(id, t.getId())
//                 .orElseThrow(() -> new EntityNotFoundException("Purchase invoice not found: " + id));

//         if (req.getBillLedger() != null) invoice.setBillLedger(req.getBillLedger());
//         if (req.getBillNumber() != null) invoice.setBillNumber(req.getBillNumber());
//         if (req.getOrderNumber() != null) invoice.setOrderNumber(req.getOrderNumber());
//         if (req.getBillDate() != null) invoice.setBillDate(req.getBillDate());
//         if (req.getDueDate() != null) invoice.setDueDate(req.getDueDate());
//         if (req.getBillType() != null) invoice.setBillType(req.getBillType());
//         if (req.getGrossNetEnabled() != null) invoice.setGrossNetEnabled(req.getGrossNetEnabled());
//         if (req.getNotes() != null) invoice.setNotes(req.getNotes());
//         if (req.getOtherCharges() != null) invoice.setOtherCharges(req.getOtherCharges());
//         if (req.getCreatedBy() != null) invoice.setCreatedBy(req.getCreatedBy());

//         // supplier update/clear
//         if (req.getSupplierId() != null) {
//             invoice.setSupplier(supplierRepo.findById(req.getSupplierId())
//                     .orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + req.getSupplierId())));
//         } else {
//             invoice.setSupplier(null);
//         }

//         // Replace lines if provided
//         if (req.getLines() != null) {
//             invoice.getLines().clear();
//             for (PurPurchaseInvoiceItemRequest lr : req.getLines()) {
//                 PurPurchaseInvoiceItem li = mapLineRequest(lr);
//                 li.setPurchaseInvoice(invoice);
//                 invoice.getLines().add(li);
//             }
//         }

//         // Replace attachments if provided
//         if (req.getAttachments() != null) {
//             invoice.getAttachments().clear();
//             for (PurPurchaseInvoiceAttachmentRequest ar : req.getAttachments()) {
//                 PurPurchaseInvoiceAttachment a = new PurPurchaseInvoiceAttachment();
//                 a.setFileName(ar.getFileName());
//                 a.setFilePath(ar.getFilePath());
//                 a.setUploadedBy(ar.getUploadedBy());
//                 a.setUploadedAt(ar.getUploadedAt());
//                 a.setPurchaseInvoice(invoice);
//                 invoice.getAttachments().add(a);
//             }
//         }

//         computeTotals(invoice);

//         PurPurchaseInvoice updated = repo.save(invoice);
//         return toResponse(updated);
//     }

//     public void delete(Long id) {
//         Tenant t = currentTenant();
//         PurPurchaseInvoice inv = repo.findByIdAndTenantId(id, t.getId())
//                 .orElseThrow(() -> new EntityNotFoundException("Purchase invoice not found: " + id));
//         repo.delete(inv);
//     }

//     /* ----- helpers ----- */

//     private PurPurchaseInvoiceItem mapLineRequest(PurPurchaseInvoiceItemRequest r) {
//         PurPurchaseInvoiceItem li = new PurPurchaseInvoiceItem();
//         li.setLineNumber(r.getLineNumber());
//         li.setDescription(r.getDescription());
//         li.setQuantityGross(r.getQuantityGross());
//         li.setQuantityNet(r.getQuantityNet());
//         li.setRate(r.getRate());
//         li.setAmount(r.getAmount());
//         li.setTaxPercent(r.getTaxPercent());
//         li.setLineDiscount(Optional.ofNullable(r.getLineDiscount()).orElse(BigDecimal.ZERO));

//         if (r.getItemId() != null) {
//             li.setItem(rawMaterialRepo.findById(r.getItemId())
//                     .orElseThrow(() -> new EntityNotFoundException("Item not found: " + r.getItemId())));
//         }
//         if (r.getUnitId() != null) {
//             li.setUnit(unitRepo.findById(r.getUnitId())
//                     .orElseThrow(() -> new EntityNotFoundException("Unit not found: " + r.getUnitId())));
//         }
//         if (r.getTaxId() != null) {
//             li.setTax(taxRepo.findById(r.getTaxId())
//                     .orElseThrow(() -> new EntityNotFoundException("Tax not found: " + r.getTaxId())));
//         }
//         if (r.getCategoryId() != null) {
//             li.setCategory(categoryRepo.findById(r.getCategoryId())
//                     .orElseThrow(() -> new EntityNotFoundException("Category not found: " + r.getCategoryId())));
//         }
//         if (r.getSubCategoryId() != null) {
//             li.setSubCategory(subCategoryRepo.findById(r.getSubCategoryId())
//                     .orElseThrow(() -> new EntityNotFoundException("Subcategory not found: " + r.getSubCategoryId())));
//         }

//         // compute amount if not provided: prefer provided amount, else quantityNet * rate - discount
//         if (li.getAmount() == null) {
//             BigDecimal q = Optional.ofNullable(li.getQuantityNet()).orElse(li.getQuantityGross());
//             BigDecimal rate = Optional.ofNullable(li.getRate()).orElse(BigDecimal.ZERO);
//             BigDecimal discount = Optional.ofNullable(li.getLineDiscount()).orElse(BigDecimal.ZERO);
//             BigDecimal amount = Optional.ofNullable(q).orElse(BigDecimal.ZERO).multiply(rate).subtract(discount);
//             li.setAmount(amount.max(BigDecimal.ZERO));
//         }
//         return li;
//     }

//     private void computeTotals(PurPurchaseInvoice invoice) {
//         BigDecimal subTotal = BigDecimal.ZERO;
//         BigDecimal totalDiscount = BigDecimal.ZERO;
//         BigDecimal totalTax = BigDecimal.ZERO;
//         BigDecimal grossTotal = BigDecimal.ZERO;

//         if (invoice.getLines() != null) {
//             for (PurPurchaseInvoiceItem li : invoice.getLines()) {
//                 BigDecimal amount = Optional.ofNullable(li.getAmount()).orElse(BigDecimal.ZERO);
//                 BigDecimal discount = Optional.ofNullable(li.getLineDiscount()).orElse(BigDecimal.ZERO);
//                 subTotal = subTotal.add(amount);
//                 totalDiscount = totalDiscount.add(discount);

//                 BigDecimal taxPercent = Optional.ofNullable(li.getTaxPercent())
//                         .orElse(Optional.ofNullable(li.getTax() != null ? li.getTax().getRate() : null).orElse(BigDecimal.ZERO));
//                 BigDecimal taxValue = amount.multiply(taxPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
//                 totalTax = totalTax.add(taxValue);

//                 grossTotal = grossTotal.add(amount);
//             }
//         }

//         invoice.setSubTotal(subTotal);
//         invoice.setTotalDiscount(totalDiscount);
//         invoice.setGrossTotal(grossTotal);
//         invoice.setTotalTax(totalTax);
//         if (invoice.getOtherCharges() == null) invoice.setOtherCharges(BigDecimal.ZERO);

//         BigDecimal net = subTotal.subtract(totalDiscount).add(totalTax).add(invoice.getOtherCharges());
//         invoice.setNetTotal(net.max(BigDecimal.ZERO));
//     }

//     private PurPurchaseInvoiceResponse toResponse(PurPurchaseInvoice inv) {
//         PurPurchaseInvoiceResponse.PurPurchaseInvoiceResponseBuilder rb = PurPurchaseInvoiceResponse.builder()
//                 .id(inv.getId())
//                 .billLedger(inv.getBillLedger())
//                 .billNumber(inv.getBillNumber())
//                 .orderNumber(inv.getOrderNumber())
//                 .billDate(inv.getBillDate())
//                 .dueDate(inv.getDueDate())
//                 .billType(inv.getBillType())
//                 .grossNetEnabled(inv.getGrossNetEnabled())
//                 .notes(inv.getNotes())
//                 .subTotal(inv.getSubTotal())
//                 .totalDiscount(inv.getTotalDiscount())
//                 .grossTotal(inv.getGrossTotal())
//                 .totalTax(inv.getTotalTax())
//                 .otherCharges(inv.getOtherCharges())
//                 .netTotal(inv.getNetTotal())
//                 .createdBy(inv.getCreatedBy())
//                 .createdAt(inv.getCreatedAt())
//                 .tenantId(inv.getTenant() != null ? inv.getTenant().getId() : null);

//         if (inv.getSupplier() != null) {
//             rb.supplierId(inv.getSupplier().getId()).supplierName(inv.getSupplier().getName());
//         }

//         List<PurPurchaseInvoiceItemResponse> lines = Optional.ofNullable(inv.getLines()).orElse(Collections.emptyList())
//                 .stream()
//                 .map(li -> {
//                     var cat = li.getCategory();
//                     var sub = li.getSubCategory();
//                     var item = li.getItem();
//                     var unit = li.getUnit();
//                     var tax = li.getTax();
//                     return PurPurchaseInvoiceItemResponse.builder()
//                             .id(li.getId())
//                             .lineNumber(li.getLineNumber())
//                             .categoryId(cat != null ? cat.getId() : null)
//                             .categoryName(cat != null ? cat.getName() : null)
//                             .subCategoryId(sub != null ? sub.getId() : null)
//                             .subCategoryName(sub != null ? sub.getName() : null)
//                             .itemId(item != null ? item.getId() : null)
//                             .itemName(item != null ? item.getName() : null)
//                             .description(li.getDescription())
//                             .quantityGross(li.getQuantityGross())
//                             .quantityNet(li.getQuantityNet())
//                             .unitId(unit != null ? unit.getId() : null)
//                             .unitName(unit != null ? unit.getName() : null)
//                             .rate(li.getRate())
//                             .amount(li.getAmount())
//                             .taxId(tax != null ? tax.getId() : null)
//                             .taxName(tax != null ? tax.getName() : null)
//                             .taxPercent(li.getTaxPercent())
//                             .lineDiscount(li.getLineDiscount())
//                             .build();
//                 }).collect(Collectors.toList());
//         rb.lines(lines);

//         List<PurPurchaseInvoiceAttachmentResponse> atts = Optional.ofNullable(inv.getAttachments()).orElse(Collections.emptyList())
//                 .stream()
//                 .map(a -> PurPurchaseInvoiceAttachmentResponse.builder()
//                         .id(a.getId())
//                         .fileName(a.getFileName())
//                         .filePath(a.getFilePath())
//                         .uploadedBy(a.getUploadedBy())
//                         .uploadedAt(a.getUploadedAt())
//                         .build())
//                 .collect(Collectors.toList());
//         rb.attachments(atts);

//         return rb.build();
//     }
// }

package com.example.multi_tanent.purchases.service;

import com.example.multi_tanent.config.TenantContext;
import com.example.multi_tanent.purchases.dto.*;
import com.example.multi_tanent.purchases.entity.*;
import com.example.multi_tanent.purchases.repository.PurPurchaseInvoiceRepository;
import com.example.multi_tanent.production.entity.ProCategory;
import com.example.multi_tanent.production.entity.ProRawMaterials;
import com.example.multi_tanent.production.entity.ProSubCategory;
import com.example.multi_tanent.production.entity.ProTax;
import com.example.multi_tanent.production.entity.ProUnit;
import com.example.multi_tanent.production.repository.ProCategoryRepository;
import com.example.multi_tanent.production.repository.ProRawMaterialsRepository;
import com.example.multi_tanent.production.repository.ProSubCategoryRepository;
import com.example.multi_tanent.production.repository.ProTaxRepository;
import com.example.multi_tanent.production.repository.ProUnitRepository;
import com.example.multi_tanent.spersusers.enitity.BaseCustomer;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import com.example.multi_tanent.spersusers.repository.PartyRepository;
import com.example.multi_tanent.spersusers.repository.TenantRepository;
import com.example.multi_tanent.tenant.service.FileStorageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional("tenantTx")
public class PurPurchaseInvoiceService {

    private final PurPurchaseInvoiceRepository repo;
    private final TenantRepository tenantRepo;
    private final PartyRepository supplierRepo; // repository for BaseCustomer / suppliers
    private final ProRawMaterialsRepository rawMaterialRepo;
    private final ProUnitRepository unitRepo;
    private final ProTaxRepository taxRepo;
    private final ProCategoryRepository categoryRepo;
    private final ProSubCategoryRepository subCategoryRepo;
    private final FileStorageService fileStorageService;
    private final PurPurchasePaymentService paymentService;

    private Tenant currentTenant() {
        String key = TenantContext.getTenantId();
        return tenantRepo.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("Tenant not resolved for key: " + key));
    }

    public PurPurchaseInvoiceResponse create(PurPurchaseInvoiceRequest req, MultipartFile[] files) {
        Tenant t = currentTenant();

        PurPurchaseInvoice inv = new PurPurchaseInvoice();
        inv.setBillLedger(req.getBillLedger());
        inv.setBillNumber(req.getBillNumber());
        inv.setOrderNumber(req.getOrderNumber());
        inv.setBillDate(req.getBillDate());
        inv.setDueDate(req.getDueDate());
        inv.setBillType(req.getBillType());
        inv.setGrossNetEnabled(Optional.ofNullable(req.getGrossNetEnabled()).orElse(Boolean.FALSE));
        inv.setNotes(req.getNotes());
        inv.setTemplate(req.getTemplate());
        inv.setCreatedBy(req.getCreatedBy());
        inv.setCreatedAt(LocalDateTime.now());
        inv.setTenant(t);

        if (req.getSupplierId() != null) {
            BaseCustomer s = supplierRepo.findById(req.getSupplierId())
                    .orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + req.getSupplierId()));
            inv.setSupplier(s);
        }

        if (req.getLines() != null && !req.getLines().isEmpty()) {
            List<PurPurchaseInvoiceItem> lines = req.getLines().stream()
                    .map(this::mapLineRequest)
                    .peek(li -> li.setPurchaseInvoice(inv))
                    .collect(Collectors.toCollection(ArrayList::new));
            inv.setLines(lines);
        } else {
            inv.setLines(new ArrayList<>());
        }

        // Handle new file uploads
        if (files != null && files.length > 0) {
            String subDir = "purchase_invoices"; // Folder to store invoice attachments
            for (MultipartFile file : files) {
                if (file.isEmpty())
                    continue;

                String relativePath = fileStorageService.storeFile(file, subDir, true);

                PurPurchaseInvoiceAttachment a = new PurPurchaseInvoiceAttachment();
                a.setFileName(file.getOriginalFilename());
                a.setFilePath(relativePath);
                a.setUploadedBy(req.getCreatedBy()); // Or from security context
                a.setUploadedAt(LocalDateTime.now());
                a.setPurchaseInvoice(inv);
                inv.addAttachment(a);
            }
        } else {
            inv.setAttachments(new ArrayList<>());
        }

        computeTotals(inv);
        PurPurchaseInvoice saved = repo.save(inv);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<PurPurchaseInvoiceResponse> list(Pageable pageable) {
        Tenant t = currentTenant();
        return repo.findByTenantId(t.getId(), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PurPurchaseInvoiceResponse getById(Long id) {
        Tenant t = currentTenant();
        PurPurchaseInvoice inv = repo.findByIdAndTenantId(id, t.getId())
                .orElseThrow(() -> new EntityNotFoundException("Purchase invoice not found: " + id));
        return toResponse(inv);
    }

    public PurPurchaseInvoiceResponse update(Long id, PurPurchaseInvoiceRequest req, MultipartFile[] files) {
        Tenant t = currentTenant();
        PurPurchaseInvoice inv = repo.findByIdAndTenantId(id, t.getId())
                .orElseThrow(() -> new EntityNotFoundException("Purchase invoice not found: " + id));

        if (req.getBillLedger() != null)
            inv.setBillLedger(req.getBillLedger());
        if (req.getBillNumber() != null)
            inv.setBillNumber(req.getBillNumber());
        if (req.getOrderNumber() != null)
            inv.setOrderNumber(req.getOrderNumber());
        if (req.getBillDate() != null)
            inv.setBillDate(req.getBillDate());
        if (req.getDueDate() != null)
            inv.setDueDate(req.getDueDate());
        if (req.getBillType() != null)
            inv.setBillType(req.getBillType());
        if (req.getGrossNetEnabled() != null)
            inv.setGrossNetEnabled(req.getGrossNetEnabled());
        if (req.getNotes() != null)
            inv.setNotes(req.getNotes());
        if (req.getTemplate() != null)
            inv.setTemplate(req.getTemplate());
        if (req.getCreatedBy() != null)
            inv.setCreatedBy(req.getCreatedBy());

        if (req.getSupplierId() != null) {
            BaseCustomer s = supplierRepo.findById(req.getSupplierId())
                    .orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + req.getSupplierId()));
            inv.setSupplier(s);
        } else {
            inv.setSupplier(null);
        }

        // replace lines if provided
        if (req.getLines() != null) {
            List<PurPurchaseInvoiceItemRequest> newLines = req.getLines();
            List<PurPurchaseInvoiceItem> currentLines = inv.getLines();

            int reqSize = newLines.size();
            int currSize = currentLines.size();

            // Update existing ones (reuse entities)
            for (int i = 0; i < Math.min(reqSize, currSize); i++) {
                PurPurchaseInvoiceItem existing = currentLines.get(i);
                PurPurchaseInvoiceItemRequest requestLine = newLines.get(i);
                updateLineItem(existing, requestLine);
            }

            // Add new ones
            if (reqSize > currSize) {
                for (int i = currSize; i < reqSize; i++) {
                    PurPurchaseInvoiceItemRequest requestLine = newLines.get(i);
                    PurPurchaseInvoiceItem newItem = mapLineRequest(requestLine);
                    newItem.setPurchaseInvoice(inv);
                    currentLines.add(newItem);
                }
            }
            // Remove extra ones
            else if (currSize > reqSize) {
                // Remove from the end
                List<PurPurchaseInvoiceItem> toRemove = new ArrayList<>(currentLines.subList(reqSize, currSize));
                currentLines.removeAll(toRemove);
                // Orphan removal handles the deletion
            }
        }

        // Handle attachments on update
        if (req.getAttachments() != null || (files != null && files.length > 0)) {
            // ... (keep existing attachment logic or simplify if needed)
            // Existing logic seems okay for attachments as they don't have the same unique
            // constraint blockers usually
            // unless fileName is unique.
            // But let's keep the existing attachment block logic for now, verifying it
            // matches context.
            inv.getAttachments().clear();

            // Re-add attachments that client sent back (existing ones to keep)
            if (req.getAttachments() != null) {
                for (PurPurchaseInvoiceAttachmentRequest ar : req.getAttachments()) {
                    if (ar.getFilePath() != null && !ar.getFilePath().isEmpty()) {
                        PurPurchaseInvoiceAttachment a = new PurPurchaseInvoiceAttachment();
                        a.setFileName(ar.getFileName());
                        a.setFilePath(ar.getFilePath());
                        a.setUploadedAt(ar.getUploadedAt());
                        a.setUploadedBy(ar.getUploadedBy());
                        inv.addAttachment(a);
                    }
                }
            }

            // Add any newly uploaded files
            if (files != null && files.length > 0) {
                String subDir = "purchase_invoices";
                for (MultipartFile file : files) {
                    if (file.isEmpty())
                        continue;
                    String relativePath = fileStorageService.storeFile(file, subDir, true);
                    PurPurchaseInvoiceAttachment a = new PurPurchaseInvoiceAttachment();
                    a.setFileName(file.getOriginalFilename());
                    a.setFilePath(relativePath);
                    a.setUploadedAt(LocalDateTime.now());
                    a.setUploadedBy(req.getCreatedBy());
                    inv.addAttachment(a);
                }
            }
        }

        computeTotals(inv);
        PurPurchaseInvoice updated = repo.save(inv);
        return toResponse(updated);
    }

    public void delete(Long id) {
        Tenant t = currentTenant();
        PurPurchaseInvoice inv = repo.findByIdAndTenantId(id, t.getId())
                .orElseThrow(() -> new EntityNotFoundException("Purchase invoice not found: " + id));
        repo.delete(inv);
    }

    /* ---------- helpers ---------- */

    private void updateLineItem(PurPurchaseInvoiceItem existing, PurPurchaseInvoiceItemRequest requestLine) {
        existing.setLineNumber(requestLine.getLineNumber());
        existing.setDescription(requestLine.getDescription());
        existing.setQuantityGross(requestLine.getQuantityGross());
        existing.setQuantityNet(requestLine.getQuantityNet());
        existing.setRate(requestLine.getRate());
        existing.setAmount(requestLine.getAmount());
        existing.setTaxPercent(requestLine.getTaxPercent());
        existing.setLineDiscount(Optional.ofNullable(requestLine.getLineDiscount()).orElse(BigDecimal.ZERO));
        existing.setDiscountPercent(requestLine.getDiscountPercent());

        if (requestLine.getItemId() != null) {
            existing.setItem(rawMaterialRepo.findById(requestLine.getItemId())
                    .orElseThrow(() -> new EntityNotFoundException("Item not found: " + requestLine.getItemId())));
        } else {
            existing.setItem(null);
        }

        if (requestLine.getUnitId() != null) {
            existing.setUnit(unitRepo.findById(requestLine.getUnitId())
                    .orElseThrow(() -> new EntityNotFoundException("Unit not found: " + requestLine.getUnitId())));
        } else {
            existing.setUnit(null);
        }

        if (requestLine.getCategoryId() != null) {
            existing.setCategory(categoryRepo.findById(requestLine.getCategoryId())
                    .orElseThrow(
                            () -> new EntityNotFoundException("Category not found: " + requestLine.getCategoryId())));
        } else {
            existing.setCategory(null);
        }

        if (requestLine.getSubCategoryId() != null) {
            existing.setSubCategory(subCategoryRepo.findById(requestLine.getSubCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "SubCategory not found: " + requestLine.getSubCategoryId())));
        } else {
            existing.setSubCategory(null);
        }

        if (requestLine.getTaxId() != null) {
            existing.setTax(taxRepo.findById(requestLine.getTaxId())
                    .orElseThrow(() -> new EntityNotFoundException("Tax not found: " + requestLine.getTaxId())));
        } else {
            existing.setTax(null);
        }
    }

    private PurPurchaseInvoiceItem mapLineRequest(PurPurchaseInvoiceItemRequest r) {
        PurPurchaseInvoiceItem li = new PurPurchaseInvoiceItem();
        li.setLineNumber(r.getLineNumber());
        li.setDescription(r.getDescription());
        li.setQuantityGross(r.getQuantityGross());
        li.setQuantityNet(r.getQuantityNet());
        li.setRate(r.getRate());
        li.setAmount(r.getAmount());
        li.setTaxPercent(r.getTaxPercent());
        li.setTaxPercent(r.getTaxPercent());
        li.setLineDiscount(Optional.ofNullable(r.getLineDiscount()).orElse(BigDecimal.ZERO));
        li.setDiscountPercent(r.getDiscountPercent());

        if (r.getCategoryId() != null) {
            ProCategory cat = categoryRepo.findById(r.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found: " + r.getCategoryId()));
            li.setCategory(cat);
        }
        if (r.getSubCategoryId() != null) {
            ProSubCategory sub = subCategoryRepo.findById(r.getSubCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("SubCategory not found: " + r.getSubCategoryId()));
            li.setSubCategory(sub);
        }
        if (r.getItemId() != null) {
            ProRawMaterials item = rawMaterialRepo.findById(r.getItemId())
                    .orElseThrow(() -> new EntityNotFoundException("Item not found: " + r.getItemId()));
            li.setItem(item);
        }
        if (r.getUnitId() != null) {
            ProUnit unit = unitRepo.findById(r.getUnitId())
                    .orElseThrow(() -> new EntityNotFoundException("Unit not found: " + r.getUnitId()));
            li.setUnit(unit);
        }
        if (r.getTaxId() != null) {
            ProTax tax = taxRepo.findById(r.getTaxId())
                    .orElseThrow(() -> new EntityNotFoundException("Tax not found: " + r.getTaxId()));
            li.setTax(tax);
        }
        return li;
    }

    private void computeTotals(PurPurchaseInvoice inv) {
        BigDecimal subTotal = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (PurPurchaseInvoiceItem li : Optional.ofNullable(inv.getLines()).orElse(Collections.emptyList())) {
            BigDecimal amount = Optional.ofNullable(li.getAmount()).orElse(BigDecimal.ZERO);
            BigDecimal lineDiscount = Optional.ofNullable(li.getLineDiscount()).orElse(BigDecimal.ZERO);
            subTotal = subTotal.add(amount);
            totalDiscount = totalDiscount.add(lineDiscount);

            BigDecimal taxPercent = Optional.ofNullable(li.getTaxPercent())
                    .orElse(Optional.ofNullable(li.getTax() != null ? li.getTax().getRate() : null)
                            .orElse(BigDecimal.ZERO));

            if (taxPercent.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal taxValue = amount.multiply(taxPercent).divide(BigDecimal.valueOf(100), 4,
                        RoundingMode.HALF_UP);
                totalTax = totalTax.add(taxValue);
            }
        }

        inv.setSubTotal(subTotal);
        inv.setTotalDiscount(totalDiscount);
        inv.setTotalTax(totalTax);
        if (inv.getOtherCharges() == null)
            inv.setOtherCharges(BigDecimal.ZERO);

        BigDecimal gross = subTotal.subtract(totalDiscount).add(totalTax).add(inv.getOtherCharges());
        inv.setGrossTotal(gross);
        inv.setNetTotal(gross.max(BigDecimal.ZERO));
    }

    private PurPurchaseInvoiceResponse toResponse(PurPurchaseInvoice inv) {
        PurPurchaseInvoiceResponse.PurPurchaseInvoiceResponseBuilder rb = PurPurchaseInvoiceResponse.builder()
                .id(inv.getId())
                .billLedger(inv.getBillLedger())
                .billNumber(inv.getBillNumber())
                .orderNumber(inv.getOrderNumber())
                .billDate(inv.getBillDate())
                .dueDate(inv.getDueDate())
                .billType(inv.getBillType())
                .grossNetEnabled(inv.getGrossNetEnabled())
                .notes(inv.getNotes())
                .template(inv.getTemplate())
                .createdBy(inv.getCreatedBy())
                .createdAt(inv.getCreatedAt())
                .subTotal(inv.getSubTotal())
                .totalDiscount(inv.getTotalDiscount())
                .grossTotal(inv.getGrossTotal())
                .totalTax(inv.getTotalTax())
                .otherCharges(inv.getOtherCharges())
                .netTotal(inv.getNetTotal())
                .tenantId(inv.getTenant() != null ? inv.getTenant().getId() : null);

        if (inv.getSupplier() != null) {
            rb.supplierId(inv.getSupplier().getId()).supplierName(inv.getSupplier().getCompanyName());
        }

        List<PurPurchaseInvoiceItemResponse> lines = Optional.ofNullable(inv.getLines()).orElse(Collections.emptyList())
                .stream()
                .map(li -> {
                    var cat = li.getCategory();
                    var sub = li.getSubCategory();
                    var item = li.getItem();
                    var unit = li.getUnit();
                    var tax = li.getTax();
                    return PurPurchaseInvoiceItemResponse.builder()
                            .id(li.getId())
                            .lineNumber(li.getLineNumber())
                            .categoryId(cat != null ? cat.getId() : null)
                            .categoryName(cat != null ? cat.getName() : null)
                            .subCategoryId(sub != null ? sub.getId() : null)
                            .subCategoryName(sub != null ? sub.getName() : null)
                            .itemId(item != null ? item.getId() : null)
                            .itemName(item != null ? item.getName() : null)
                            .description(li.getDescription())
                            .quantityGross(li.getQuantityGross())
                            .quantityNet(li.getQuantityNet())
                            .unitId(unit != null ? unit.getId() : null)
                            .unitName(unit != null ? unit.getName() : null)
                            .rate(li.getRate())
                            .amount(li.getAmount())
                            .taxId(tax != null ? tax.getId() : null)
                            .taxName(tax != null ? tax.getCode() : null)
                            .taxPercent(li.getTaxPercent())
                            .lineDiscount(li.getLineDiscount())
                            .discountPercent(li.getDiscountPercent())
                            .build();
                }).collect(Collectors.toList());
        rb.lines(lines);

        List<PurPurchaseInvoiceAttachmentResponse> atts = Optional.ofNullable(inv.getAttachments())
                .orElse(Collections.emptyList())
                .stream().map(a -> PurPurchaseInvoiceAttachmentResponse.builder()
                        .id(a.getId())
                        .fileName(a.getFileName())
                        .filePath(a.getFilePath())
                        .uploadedBy(a.getUploadedBy())
                        .uploadedAt(a.getUploadedAt())
                        .url(fileStorageService.buildPublicUrl(a.getFilePath()))
                        .build())
                .collect(Collectors.toList());
        rb.attachments(atts);

        return rb.build();
    }

    @Transactional
    public PurPurchasePaymentResponse convertToPayment(Long id) {
        Tenant t = currentTenant();
        PurPurchaseInvoice inv = repo.findByIdAndTenantId(id, t.getId())
                .orElseThrow(() -> new EntityNotFoundException("Purchase invoice not found: " + id));

        PurPurchasePaymentRequest req = new PurPurchasePaymentRequest();
        if (inv.getSupplier() != null) {
            req.setSupplierId(inv.getSupplier().getId());
        }

        // Default amount to Invoice Net Total (or Gross if Net is missing/zero, though
        // Net is usually set)
        BigDecimal amount = Optional.ofNullable(inv.getNetTotal())
                .filter(a -> a.compareTo(BigDecimal.ZERO) > 0)
                .orElse(Optional.ofNullable(inv.getGrossTotal()).orElse(BigDecimal.ZERO));

        req.setAmount(amount);
        req.setPaymentDate(LocalDate.now());
        req.setReference("Payment for Bill: " + inv.getBillNumber());
        req.setCreatedBy(inv.getCreatedBy());

        // Note: Automatic allocation to this invoice could be done here if
        // PaymentService supports it.
        // For now, simple creation.

        return paymentService.create(req, null);
    }
}
