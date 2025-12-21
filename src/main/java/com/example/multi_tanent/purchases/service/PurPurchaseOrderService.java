// package com.example.multi_tanent.purchases.service;

// import com.example.multi_tanent.config.TenantContext;
// import com.example.multi_tanent.production.repository.ProCategoryRepository;
// import com.example.multi_tanent.production.repository.ProSubCategoryRepository;
// import com.example.multi_tanent.production.repository.ProRawMaterialsRepository;
// import com.example.multi_tanent.production.repository.ProUnitRepository;
// import com.example.multi_tanent.purchases.dto.*;
// import com.example.multi_tanent.purchases.dto.PurPurchaseOrderResponse.PurPurchaseOrderResponseBuilder;
// import com.example.multi_tanent.purchases.entity.PurPurchaseOrder;
// import com.example.multi_tanent.purchases.entity.PurPurchaseOrderAttachment;
// import com.example.multi_tanent.purchases.entity.PurPurchaseOrderItem;
// import com.example.multi_tanent.purchases.repository.PurPurchaseOrderRepository;
// import com.example.multi_tanent.production.repository.ProTaxRepository;
// import com.example.multi_tanent.spersusers.enitity.Tenant;
// //import com.example.multi_tanent.spersusers.entity.Tenant;
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

// /**
//  * Service handling PurPurchaseOrder CRUD and listing.
//  */
// @Service
// @RequiredArgsConstructor
// @Transactional("tenantTx")
// public class PurPurchaseOrderService {

//     private final PurPurchaseOrderRepository repo;
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

//     public PurPurchaseOrderResponse create(PurPurchaseOrderRequest req) {
//         Tenant t = currentTenant();

//         PurPurchaseOrder po = new PurPurchaseOrder();
//         po.setOrderCategory(req.getOrderCategory());
//         po.setPoNumber(req.getPoNumber());
//         po.setReference(req.getReference());
//         po.setDate(req.getDate());
//         po.setDiscountMode(req.getDiscountMode());
//         po.setCurrency(req.getCurrency());
//         po.setRemark(req.getRemark());
//         po.setStatus(req.getStatus());
//         po.setCreatedBy(req.getCreatedBy());
//         po.setCreatedAt(LocalDateTime.now());
//         po.setTenant(t);

//         if (req.getSupplierId() != null) {
//             po.setSupplier(supplierRepo.findById(req.getSupplierId())
//                     .orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + req.getSupplierId())));
//         }

//         // Items
//         if (req.getItems() != null && !req.getItems().isEmpty()) {
//             po.setItems(new ArrayList<>());
//             for (PurPurchaseOrderItemRequest ir : req.getItems()) {
//                 PurPurchaseOrderItem it = mapItemRequest(ir);
//                 po.addItem(it);
//             }
//         }

//         // Attachments
//         if (req.getAttachments() != null && !req.getAttachments().isEmpty()) {
//             po.setAttachments(new ArrayList<>());
//             for (PurPurchaseOrderAttachmentRequest ar : req.getAttachments()) {
//                 PurPurchaseOrderAttachment a = new PurPurchaseOrderAttachment();
//                 a.setFileName(ar.getFileName());
//                 a.setFilePath(ar.getFilePath());
//                 a.setUploadedBy(ar.getUploadedBy());
//                 a.setUploadedAt(ar.getUploadedAt());
//                 po.addAttachment(a);
//             }
//         }

//         computeTotals(po);

//         PurPurchaseOrder saved = repo.save(po);
//         return toResponse(saved);
//     }

//     @Transactional(readOnly = true)
//     public Page<PurPurchaseOrderResponse> list(Pageable pageable) {
//         Tenant t = currentTenant();
//         return repo.findByTenantId(t.getId(), pageable).map(this::toResponse);
//     }

//     @Transactional(readOnly = true)
//     public PurPurchaseOrderResponse getById(Long id) {
//         Tenant t = currentTenant();
//         PurPurchaseOrder po = repo.findByIdAndTenantId(id, t.getId())
//                 .orElseThrow(() -> new EntityNotFoundException("Purchase order not found: " + id));
//         return toResponse(po);
//     }

//     public PurPurchaseOrderResponse update(Long id, PurPurchaseOrderRequest req) {
//         Tenant t = currentTenant();
//         PurPurchaseOrder po = repo.findByIdAndTenantId(id, t.getId())
//                 .orElseThrow(() -> new EntityNotFoundException("Purchase order not found: " + id));

//         if (req.getOrderCategory() != null)
//             po.setOrderCategory(req.getOrderCategory());
//         if (req.getPoNumber() != null)
//             po.setPoNumber(req.getPoNumber());
//         if (req.getReference() != null)
//             po.setReference(req.getReference());
//         if (req.getDate() != null)
//             po.setDate(req.getDate());
//         if (req.getDiscountMode() != null)
//             po.setDiscountMode(req.getDiscountMode());
//         if (req.getCurrency() != null)
//             po.setCurrency(req.getCurrency());
//         if (req.getRemark() != null)
//             po.setRemark(req.getRemark());
//         if (req.getStatus() != null)
//             po.setStatus(req.getStatus());
//         if (req.getCreatedBy() != null)
//             po.setCreatedBy(req.getCreatedBy());

//         // supplier: if provided set; if null (and request included supplier field)
//         // clear
//         if (req.getSupplierId() != null) {
//             po.setSupplier(supplierRepo.findById(req.getSupplierId())
//                     .orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + req.getSupplierId())));
//         } else {
//             // if the caller intended to clear supplier they can send supplierId=null; we'll
//             // clear it
//             po.setSupplier(null);
//         }

//         // Replace items when provided (clear and add)
//         if (req.getItems() != null) {
//             // Use a while loop to safely remove items and trigger orphanRemoval
//             while (!po.getItems().isEmpty()) {
//                 po.removeItem(po.getItems().get(0));
//             }
//             for (PurPurchaseOrderItemRequest ir : req.getItems()) {
//                 PurPurchaseOrderItem it = mapItemRequest(ir);
//                 po.addItem(it);
//             }
//         }

//         // Replace attachments when provided
//         if (req.getAttachments() != null) {
//             while (!po.getAttachments().isEmpty()) {
//                 po.removeAttachment(po.getAttachments().get(0));
//             }
//             for (PurPurchaseOrderAttachmentRequest ar : req.getAttachments()) {
//                 PurPurchaseOrderAttachment a = new PurPurchaseOrderAttachment();
//                 a.setFileName(ar.getFileName());
//                 a.setFilePath(ar.getFilePath());
//                 a.setUploadedBy(ar.getUploadedBy());
//                 a.setUploadedAt(ar.getUploadedAt());
//                 po.addAttachment(a);
//             }
//         }

//         computeTotals(po);

//         PurPurchaseOrder updated = repo.save(po);
//         return toResponse(updated);
//     }

//     public void delete(Long id) {
//         Tenant t = currentTenant();
//         PurPurchaseOrder po = repo.findByIdAndTenantId(id, t.getId())
//                 .orElseThrow(() -> new EntityNotFoundException("Purchase order not found: " + id));
//         repo.delete(po);
//     }

//     /* ---------------- helpers ---------------- */

//     private PurPurchaseOrderItem mapItemRequest(PurPurchaseOrderItemRequest r) {
//         PurPurchaseOrderItem it = new PurPurchaseOrderItem();
//         it.setLineNumber(r.getLineNumber());
//         it.setDescription(r.getDescription());
//         it.setQuantity(Optional.ofNullable(r.getQuantity()).orElse(BigDecimal.ZERO));
//         it.setRate(r.getRate());
//         it.setLineDiscount(Optional.ofNullable(r.getLineDiscount()).orElse(BigDecimal.ZERO));
//         it.setTaxExempt(Optional.ofNullable(r.getTaxExempt()).orElse(Boolean.FALSE));
//         it.setTaxPercent(r.getTaxPercent());

//         if (r.getItemId() != null) {
//             it.setItem(rawMaterialRepo.findById(r.getItemId())
//                     .orElseThrow(() -> new EntityNotFoundException("Item not found: " + r.getItemId())));
//         }

//         if (r.getUnitId() != null) {
//             it.setUnit(unitRepo.findById(r.getUnitId())
//                     .orElseThrow(() -> new EntityNotFoundException("Unit not found: " + r.getUnitId())));
//         }

//         if (r.getTaxId() != null) {
//             it.setTax(taxRepo.findById(r.getTaxId())
//                     .orElseThrow(() -> new EntityNotFoundException("Tax not found: " + r.getTaxId())));
//         }

//         if (r.getCategoryId() != null) {
//             it.setCategory(categoryRepo.findById(r.getCategoryId())
//                     .orElseThrow(() -> new EntityNotFoundException("Category not found: " + r.getCategoryId())));
//         }
//         if (r.getSubCategoryId() != null) {
//             it.setSubCategory(subCategoryRepo.findById(r.getSubCategoryId())
//                     .orElseThrow(() -> new EntityNotFoundException("Subcategory not found: " + r.getSubCategoryId())));
//         }

//         // compute amount (quantity * rate - discount)
//         BigDecimal q = Optional.ofNullable(it.getQuantity()).orElse(BigDecimal.ZERO);
//         BigDecimal rate = Optional.ofNullable(it.getRate()).orElse(BigDecimal.ZERO);
//         BigDecimal lineDiscount = Optional.ofNullable(it.getLineDiscount()).orElse(BigDecimal.ZERO);
//         BigDecimal amount = q.multiply(rate).subtract(lineDiscount);
//         it.setAmount(amount.max(BigDecimal.ZERO));
//         return it;
//     }

//     private void computeTotals(PurPurchaseOrder po) {
//         BigDecimal subTotal = BigDecimal.ZERO;
//         BigDecimal totalDiscount = BigDecimal.ZERO;
//         BigDecimal totalTax = BigDecimal.ZERO;

//         if (po.getItems() != null) {
//             for (PurPurchaseOrderItem it : po.getItems()) {
//                 BigDecimal amount = Optional.ofNullable(it.getAmount()).orElse(BigDecimal.ZERO);
//                 BigDecimal discount = Optional.ofNullable(it.getLineDiscount()).orElse(BigDecimal.ZERO);
//                 subTotal = subTotal.add(amount);
//                 totalDiscount = totalDiscount.add(discount);

//                 boolean taxExempt = Optional.ofNullable(it.getTaxExempt()).orElse(Boolean.FALSE);
//                 if (!taxExempt) {
//                     BigDecimal taxPercent = Optional.ofNullable(it.getTaxPercent())
//                             .orElse(Optional.ofNullable(it.getTax() != null ? it.getTax().getRate() : null)
//                                     .orElse(BigDecimal.ZERO));
//                     BigDecimal taxValue = amount.multiply(taxPercent).divide(BigDecimal.valueOf(100), 2,
//                             RoundingMode.HALF_UP);
//                     totalTax = totalTax.add(taxValue);
//                 }
//             }
//         }

//         po.setSubTotal(subTotal);
//         po.setTotalDiscount(totalDiscount);
//         po.setTotalTax(totalTax);
//         if (po.getOtherCharges() == null)
//             po.setOtherCharges(BigDecimal.ZERO);

//         BigDecimal total = subTotal.subtract(totalDiscount).add(totalTax)
//                 .add(Optional.ofNullable(po.getOtherCharges()).orElse(BigDecimal.ZERO));
//         po.setTotalAmount(total.max(BigDecimal.ZERO));
//     }

//     /* ---------------- response mapping ---------------- */

//     private PurPurchaseOrderResponse toResponse(PurPurchaseOrder po) {
//         PurPurchaseOrderResponseBuilder rb = PurPurchaseOrderResponse.builder()
//                 .id(po.getId())
//                 .orderCategory(po.getOrderCategory())
//                 .poNumber(po.getPoNumber())
//                 .reference(po.getReference())
//                 .date(po.getDate())
//                 .discountMode(po.getDiscountMode())
//                 .currency(po.getCurrency())
//                 .remark(po.getRemark())
//                 .status(po.getStatus())
//                 .createdBy(po.getCreatedBy())
//                 .createdAt(po.getCreatedAt())
//                 .subTotal(po.getSubTotal())
//                 .totalDiscount(po.getTotalDiscount())
//                 .totalTax(po.getTotalTax())
//                 .otherCharges(po.getOtherCharges())
//                 .totalAmount(po.getTotalAmount())
//                 .tenantId(po.getTenant() != null ? po.getTenant().getId() : null);

//         if (po.getSupplier() != null) {
//             rb.supplierId(po.getSupplier().getId()).supplierName(po.getSupplier().getCompanyName());
//         }

//         List<PurPurchaseOrderItemResponse> items = Optional.ofNullable(po.getItems()).orElse(Collections.emptyList())
//                 .stream()
//                 .map(it -> {
//                     var category = it.getCategory();
//                     var subCategory = it.getSubCategory();
//                     var item = it.getItem();
//                     var unit = it.getUnit();
//                     var tax = it.getTax();

//                     return PurPurchaseOrderItemResponse.builder()
//                             .id(it.getId())
//                             .lineNumber(it.getLineNumber())
//                             .categoryId(category != null ? category.getId() : null)
//                             .categoryName(category != null ? category.getName() : null)
//                             .subCategoryId(subCategory != null ? subCategory.getId() : null)
//                             .subCategoryName(subCategory != null ? subCategory.getName() : null)
//                             .itemId(item != null ? item.getId() : null)
//                             .itemName(item != null ? item.getName() : null)
//                             .description(it.getDescription())
//                             .quantity(it.getQuantity())
//                             .unitId(unit != null ? unit.getId() : null)
//                             .unitName(unit != null ? unit.getName() : null)
//                             .rate(it.getRate())
//                             .amount(it.getAmount())
//                             .taxId(tax != null ? tax.getId() : null)
//                             .taxName(tax != null ? tax.getCode() : null)
//                             .taxExempt(it.getTaxExempt())
//                             .taxPercent(it.getTaxPercent())
//                             .lineDiscount(it.getLineDiscount())
//                             .build();
//                 }).collect(Collectors.toList());

//         rb.items(items);

//         List<PurPurchaseOrderAttachmentResponse> atts = Optional.ofNullable(po.getAttachments())
//                 .orElse(Collections.emptyList())
//                 .stream()
//                 .map(a -> PurPurchaseOrderAttachmentResponse.builder()
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

//     // inside PurPurchaseOrderService class (add near other public methods)
//     @Transactional
//     public PurPurchaseOrderResponse convertToBill(Long id) {
//         Tenant t = currentTenant();
//         PurPurchaseOrder po = repo.findByIdAndTenantId(id, t.getId())
//                 .orElseThrow(() -> new EntityNotFoundException("Purchase order not found: " + id));

//         // simple behavior: set status to Billed (or "Billed" + timestamp) and save.
//         po.setStatus("Billed");

//         // If you later want to mark amounts or create invoice entity, add that logic
//         // here.
//         computeTotals(po); // ensure totals are up to date (optional)
//         PurPurchaseOrder saved = repo.save(po);

//         return toResponse(saved);
//     }
// }

package com.example.multi_tanent.purchases.service;

import com.example.multi_tanent.config.TenantContext;
import com.example.multi_tanent.purchases.dto.*;
import com.example.multi_tanent.purchases.entity.*;
import com.example.multi_tanent.purchases.repository.PurPurchaseOrderRepository;
import com.example.multi_tanent.purchases.repository.PurGrnItemRepository;
import com.example.multi_tanent.production.repository.*;
import com.example.multi_tanent.spersusers.enitity.Location;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import com.example.multi_tanent.spersusers.repository.LocationRepository;
import com.example.multi_tanent.spersusers.repository.PartyRepository;
import com.example.multi_tanent.spersusers.repository.TenantRepository;
import com.example.multi_tanent.tenant.service.FileStorageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Purchase order service: create, list, get, update, delete and attachments.
 *
 * Notes:
 * - This class expects that FileStorageService.storeFile(file, subDir, true)
 * stores
 * the file under uploads/<tenantId>/<subDir>/<generatedFilename> and returns
 * the relative path (e.g. "purchase_orders/123/169xxx-file.png").
 * - FileStorageService must also expose a public buildPublicUrl(String
 * relativePath)
 * which returns a full URL like
 * "http://host/uploads/<tenantId>/purchase_orders/123/..."
 */
@Service
@RequiredArgsConstructor
@Transactional("tenantTx")
public class PurPurchaseOrderService {

    private final PurPurchaseOrderRepository repo;
    private final TenantRepository tenantRepo;
    private final PartyRepository supplierRepo;
    private final ProRawMaterialsRepository rawMaterialRepo;
    private final ProUnitRepository unitRepo;
    private final ProTaxRepository taxRepo;
    private final ProCategoryRepository categoryRepo;
    private final ProSubCategoryRepository subCategoryRepo;

    private final FileStorageService fileStorageService;
    private final LocationRepository locationRepo;
    private final PurPurchaseInvoiceService invoiceService;
    private final PurPurchasePaymentService paymentService;

    private Tenant currentTenant() {
        String key = TenantContext.getTenantId();
        return tenantRepo.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("Tenant not resolved for key: " + key));
    }

    /* ---------------- CRUD ---------------- */

    public PurPurchaseOrderResponse create(PurPurchaseOrderRequest req) {
        Tenant t = currentTenant();

        PurPurchaseOrder po = new PurPurchaseOrder();
        po.setOrderCategory(req.getOrderCategory());
        po.setPoNumber(req.getPoNumber());
        po.setReference(req.getReference());
        po.setDate(req.getDate());
        po.setDiscountMode(req.getDiscountMode());
        po.setCurrency(req.getCurrency());
        po.setRemark(req.getRemark());
        po.setStatus(req.getStatus());
        po.setCreatedBy(req.getCreatedBy());

        po.setCreatedAt(LocalDateTime.now());
        po.setTenant(t);

        if (req.getLocationId() != null) {
            Location loc = locationRepo.findById(req.getLocationId())
                    .orElseThrow(() -> new EntityNotFoundException("Location not found: " + req.getLocationId()));
            po.setLocation(loc);
        }
        po.setProjectNumber(req.getProjectNumber());
        po.setDeliverToType(req.getDeliverToType());
        po.setDeliverToId(req.getDeliverToId());
        po.setDeliverToAddress(req.getDeliverToAddress());
        po.setTemplate(req.getTemplate());
        po.setEmailTo(req.getEmailTo());
        po.setTermsAndConditions(req.getTermsAndConditions());
        po.setNotes(req.getNotes());

        if (req.getSupplierId() != null) {
            po.setSupplier(supplierRepo.findById(req.getSupplierId())
                    .orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + req.getSupplierId())));
        }

        if (req.getItems() != null && !req.getItems().isEmpty()) {
            for (PurPurchaseOrderItemRequest ir : req.getItems()) {
                PurPurchaseOrderItem it = mapItemRequest(ir);
                po.addItem(it);
            }
        }

        if (req.getAttachments() != null && !req.getAttachments().isEmpty()) {
            for (PurPurchaseOrderAttachmentRequest ar : req.getAttachments()) {
                PurPurchaseOrderAttachment a = new PurPurchaseOrderAttachment();
                a.setFileName(ar.getFileName());
                a.setFilePath(ar.getFilePath());
                a.setUploadedAt(ar.getUploadedAt());
                a.setUploadedBy(ar.getUploadedBy());
                po.addAttachment(a);
            }
        }

        computeTotals(po);
        PurPurchaseOrder saved = repo.save(po);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<PurPurchaseOrderResponse> list(Pageable pageable) {
        Tenant t = currentTenant();
        return repo.findByTenantId(t.getId(), pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PurPurchaseOrderResponse getById(Long id) {
        Tenant t = currentTenant();
        PurPurchaseOrder po = repo.findByIdAndTenantId(id, t.getId())
                .orElseThrow(() -> new EntityNotFoundException("Purchase order not found: " + id));
        return toResponse(po);
    }

    public PurPurchaseOrderResponse update(Long id, PurPurchaseOrderRequest req) {
        Tenant t = currentTenant();
        PurPurchaseOrder po = repo.findByIdAndTenantId(id, t.getId())
                .orElseThrow(() -> new EntityNotFoundException("Purchase order not found: " + id));

        if (req.getOrderCategory() != null)
            po.setOrderCategory(req.getOrderCategory());
        if (req.getPoNumber() != null)
            po.setPoNumber(req.getPoNumber());
        if (req.getReference() != null)
            po.setReference(req.getReference());
        if (req.getDate() != null)
            po.setDate(req.getDate());
        if (req.getDiscountMode() != null)
            po.setDiscountMode(req.getDiscountMode());
        if (req.getCurrency() != null)
            po.setCurrency(req.getCurrency());
        if (req.getRemark() != null)
            po.setRemark(req.getRemark());
        if (req.getStatus() != null)
            po.setStatus(req.getStatus());
        if (req.getCreatedBy() != null)
            po.setCreatedBy(req.getCreatedBy());

        if (req.getLocationId() != null) {
            Location loc = locationRepo.findById(req.getLocationId())
                    .orElseThrow(() -> new EntityNotFoundException("Location not found: " + req.getLocationId()));
            po.setLocation(loc);
        }
        if (req.getProjectNumber() != null)
            po.setProjectNumber(req.getProjectNumber());
        if (req.getDeliverToType() != null)
            po.setDeliverToType(req.getDeliverToType());
        po.setDeliverToId(req.getDeliverToId()); // Allow nulling
        if (req.getDeliverToAddress() != null)
            po.setDeliverToAddress(req.getDeliverToAddress());
        if (req.getTemplate() != null)
            po.setTemplate(req.getTemplate());
        if (req.getEmailTo() != null)
            po.setEmailTo(req.getEmailTo());
        if (req.getTermsAndConditions() != null)
            po.setTermsAndConditions(req.getTermsAndConditions());
        if (req.getNotes() != null)
            po.setNotes(req.getNotes());

        if (req.getSupplierId() != null) {
            po.setSupplier(supplierRepo.findById(req.getSupplierId())
                    .orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + req.getSupplierId())));
        } else {
            po.setSupplier(null);
        }

        if (req.getItems() != null) {
            while (!po.getItems().isEmpty()) {
                po.removeItem(po.getItems().get(0));
            }
            for (PurPurchaseOrderItemRequest ir : req.getItems()) {
                PurPurchaseOrderItem it = mapItemRequest(ir);
                po.addItem(it);
            }
        }

        if (req.getAttachments() != null) {
            while (!po.getAttachments().isEmpty()) {
                po.removeAttachment(po.getAttachments().get(0));
            }
            for (PurPurchaseOrderAttachmentRequest ar : req.getAttachments()) {
                PurPurchaseOrderAttachment a = new PurPurchaseOrderAttachment();
                a.setFileName(ar.getFileName());
                a.setFilePath(ar.getFilePath());
                a.setUploadedAt(ar.getUploadedAt());
                a.setUploadedBy(ar.getUploadedBy());
                po.addAttachment(a);
            }
        }

        computeTotals(po);
        PurPurchaseOrder updated = repo.save(po);
        return toResponse(updated);
    }

    private final PurGrnItemRepository grnItemRepo;

    // ... (existing helper methods)

    public void delete(Long id) {
        Tenant t = currentTenant();
        PurPurchaseOrder po = repo.findByIdAndTenantId(id, t.getId())
                .orElseThrow(() -> new EntityNotFoundException("Purchase order not found: " + id));

        // Validation: Check if any PO items are linked to a GRN
        if (po.getItems() != null && !po.getItems().isEmpty()) {
            List<Long> itemIds = po.getItems().stream()
                    .map(PurPurchaseOrderItem::getId)
                    .collect(Collectors.toList());
            if (grnItemRepo.existsByPurchaseOrderItemIdIn(itemIds)) {
                throw new IllegalStateException(
                        "Cannot delete Purchase Order because one or more items are linked to a Goods Received Note (GRN). Please delete the GRN first.");
            }
        }

        repo.delete(po);
    }

    /* ---------------- attachments ---------------- */

    /**
     * Attach uploaded files to a purchase order. Files are stored on disk under
     * uploads/<tenantId>/<subDir>/<filename>.
     * The returned list contains the public URLs for each uploaded file.
     */
    public List<String> attachFiles(Long purchaseOrderId, MultipartFile[] files, String uploadedBy) {
        if (files == null || files.length == 0)
            return Collections.emptyList();

        Tenant t = currentTenant();
        PurPurchaseOrder po = repo.findByIdAndTenantId(purchaseOrderId, t.getId())
                .orElseThrow(() -> new EntityNotFoundException("Purchase order not found: " + purchaseOrderId));

        List<String> publicUrls = new ArrayList<>();
        String subDir = "purchase_orders/" + purchaseOrderId;

        for (MultipartFile file : files) {
            // store under tenant folder; returns "purchase_orders/{id}/{storedFilename}"
            String relative = fileStorageService.storeFile(file, subDir, true);

            PurPurchaseOrderAttachment a = new PurPurchaseOrderAttachment();
            a.setFileName(file.getOriginalFilename());
            a.setFilePath(relative);
            a.setUploadedAt(LocalDateTime.now());
            a.setUploadedBy(uploadedBy);
            a.setPurchaseOrder(po);
            po.getAttachments().add(a);

            // build public URL via FileStorageService
            String url = fileStorageService.buildPublicUrl(relative);
            publicUrls.add(url);
        }

        repo.save(po); // persist attachments via cascade
        return publicUrls;
    }

    /* ---------------- helpers ---------------- */

    private PurPurchaseOrderItem mapItemRequest(PurPurchaseOrderItemRequest r) {
        PurPurchaseOrderItem it = new PurPurchaseOrderItem();
        it.setLineNumber(r.getLineNumber());
        it.setDescription(r.getDescription());
        it.setQuantity(Optional.ofNullable(r.getQuantity()).orElse(BigDecimal.ZERO));
        it.setRate(r.getRate());
        it.setLineDiscount(Optional.ofNullable(r.getLineDiscount()).orElse(BigDecimal.ZERO));
        it.setDiscountPercent(r.getDiscountPercent());
        it.setTaxExempt(Optional.ofNullable(r.getTaxExempt()).orElse(Boolean.FALSE));
        it.setTaxPercent(r.getTaxPercent());

        if (r.getItemId() != null) {
            it.setItem(rawMaterialRepo.findById(r.getItemId())
                    .orElseThrow(() -> new EntityNotFoundException("Item not found: " + r.getItemId())));
        }
        if (r.getUnitId() != null) {
            it.setUnit(unitRepo.findById(r.getUnitId())
                    .orElseThrow(() -> new EntityNotFoundException("Unit not found: " + r.getUnitId())));
        }
        if (r.getTaxId() != null) {
            it.setTax(taxRepo.findById(r.getTaxId())
                    .orElseThrow(() -> new EntityNotFoundException("Tax not found: " + r.getTaxId())));
        }
        if (r.getCategoryId() != null) {
            it.setCategory(categoryRepo.findById(r.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found: " + r.getCategoryId())));
        }
        if (r.getSubCategoryId() != null) {
            it.setSubCategory(subCategoryRepo.findById(r.getSubCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Subcategory not found: " + r.getSubCategoryId())));
        }

        BigDecimal q = Optional.ofNullable(it.getQuantity()).orElse(BigDecimal.ZERO);
        BigDecimal rate = Optional.ofNullable(it.getRate()).orElse(BigDecimal.ZERO);
        BigDecimal lineDiscount = Optional.ofNullable(it.getLineDiscount()).orElse(BigDecimal.ZERO);
        BigDecimal amount = q.multiply(rate).subtract(lineDiscount);
        it.setAmount(amount.max(BigDecimal.ZERO));
        return it;
    }

    private void computeTotals(PurPurchaseOrder po) {
        BigDecimal subTotal = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        if (po.getItems() != null) {
            for (PurPurchaseOrderItem it : po.getItems()) {
                BigDecimal amount = Optional.ofNullable(it.getAmount()).orElse(BigDecimal.ZERO);
                BigDecimal discount = Optional.ofNullable(it.getLineDiscount()).orElse(BigDecimal.ZERO);
                subTotal = subTotal.add(amount);
                totalDiscount = totalDiscount.add(discount);

                boolean taxExempt = Optional.ofNullable(it.getTaxExempt()).orElse(Boolean.FALSE);
                if (!taxExempt) {
                    BigDecimal taxPercent = Optional.ofNullable(it.getTaxPercent())
                            .orElse(Optional.ofNullable(it.getTax() != null ? it.getTax().getRate() : null)
                                    .orElse(BigDecimal.ZERO));
                    BigDecimal taxValue = amount.multiply(taxPercent).divide(BigDecimal.valueOf(100), 2,
                            RoundingMode.HALF_UP);
                    totalTax = totalTax.add(taxValue);
                }
            }
        }

        po.setSubTotal(subTotal);
        po.setTotalDiscount(totalDiscount);
        po.setTotalTax(totalTax);
        if (po.getOtherCharges() == null)
            po.setOtherCharges(BigDecimal.ZERO);

        BigDecimal total = subTotal.subtract(totalDiscount).add(totalTax)
                .add(Optional.ofNullable(po.getOtherCharges()).orElse(BigDecimal.ZERO));
        po.setTotalAmount(total.max(BigDecimal.ZERO));
    }

    private PurPurchaseOrderResponse toResponse(PurPurchaseOrder po) {
        PurPurchaseOrderResponse.PurPurchaseOrderResponseBuilder rb = PurPurchaseOrderResponse.builder()
                .id(po.getId())
                .orderCategory(po.getOrderCategory())
                .poNumber(po.getPoNumber())
                .reference(po.getReference())
                .date(po.getDate())
                .discountMode(po.getDiscountMode())
                .currency(po.getCurrency())
                .remark(po.getRemark())
                .status(po.getStatus())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .subTotal(po.getSubTotal())
                .totalDiscount(po.getTotalDiscount())
                .totalTax(po.getTotalTax())
                .totalAmount(po.getTotalAmount())
                // Gross Total = SubTotal - Discount
                .grossTotal(po.getSubTotal().subtract(po.getTotalDiscount()))
                .tenantId(po.getTenant() != null ? po.getTenant().getId() : null)
                .locationId(po.getLocation() != null ? po.getLocation().getId() : null)
                .locationName(po.getLocation() != null ? po.getLocation().getName() : null)
                .projectNumber(po.getProjectNumber())
                .deliverToType(po.getDeliverToType())
                .deliverToId(po.getDeliverToId())
                .deliverToAddress(po.getDeliverToAddress())
                .template(po.getTemplate())
                .emailTo(po.getEmailTo())
                .termsAndConditions(po.getTermsAndConditions())
                .notes(po.getNotes());

        if (po.getSupplier() != null) {
            rb.supplierId(po.getSupplier().getId()).supplierName(po.getSupplier().getCompanyName());
        }

        List<PurPurchaseOrderItemResponse> items = Optional.ofNullable(po.getItems()).orElse(Collections.emptyList())
                .stream()
                .map(it -> {
                    var category = it.getCategory();
                    var subCategory = it.getSubCategory();
                    var item = it.getItem();
                    var unit = it.getUnit();
                    var tax = it.getTax();

                    return PurPurchaseOrderItemResponse.builder()
                            .id(it.getId())
                            .lineNumber(it.getLineNumber())
                            .categoryId(category != null ? category.getId() : null)
                            .categoryName(category != null ? category.getName() : null)
                            .subCategoryId(subCategory != null ? subCategory.getId() : null)
                            .subCategoryName(subCategory != null ? subCategory.getName() : null)
                            .itemId(item != null ? item.getId() : null)
                            .itemName(item != null ? item.getName() : null)
                            .description(it.getDescription())
                            .quantity(it.getQuantity())
                            .unitId(unit != null ? unit.getId() : null)
                            .unitName(unit != null ? unit.getName() : null)
                            .rate(it.getRate())
                            .amount(it.getAmount())
                            .taxId(tax != null ? tax.getId() : null)
                            .taxName(tax != null ? tax.getCode() : null)
                            .taxExempt(it.getTaxExempt())
                            .taxPercent(it.getTaxPercent())

                            .lineDiscount(it.getLineDiscount())
                            .discountPercent(it.getDiscountPercent())
                            .build();
                })
                .collect(Collectors.toList());
        rb.items(items);

        List<PurPurchaseOrderAttachmentResponse> atts = Optional.ofNullable(po.getAttachments())
                .orElse(Collections.emptyList())
                .stream()
                .map(a -> PurPurchaseOrderAttachmentResponse.builder()
                        .id(a.getId())
                        .fileName(a.getFileName())
                        .filePath(a.getFilePath())
                        .uploadedAt(a.getUploadedAt())
                        .uploadedBy(a.getUploadedBy())
                        .url(fileStorageService.buildPublicUrl(a.getFilePath()))
                        .build())
                .collect(Collectors.toList());
        rb.attachments(atts);

        return rb.build();
    }

    @Transactional
    public PurPurchaseInvoiceResponse convertToBill(Long id) {
        Tenant t = currentTenant();
        PurPurchaseOrder po = repo.findByIdAndTenantId(id, t.getId())
                .orElseThrow(() -> new EntityNotFoundException("Purchase order not found: " + id));

        // 1. Create Invoice Request from PO
        PurPurchaseInvoiceRequest req = new PurPurchaseInvoiceRequest();

        if (po.getSupplier() != null) {
            req.setSupplierId(po.getSupplier().getId());
        }

        req.setOrderNumber(po.getPoNumber());
        req.setBillDate(LocalDate.now());
        req.setDueDate(LocalDate.now().plusDays(30)); // Default due date, can be adjusted
        req.setNotes(po.getRemark());
        req.setCreatedBy(po.getCreatedBy());
        req.setGrossNetEnabled(Boolean.FALSE); // Default or derive from PO if applicable

        // Map Items
        if (po.getItems() != null) {
            List<PurPurchaseInvoiceItemRequest> lineRequests = po.getItems().stream().map(item -> {
                PurPurchaseInvoiceItemRequest line = new PurPurchaseInvoiceItemRequest();
                line.setLineNumber(item.getLineNumber());
                line.setDescription(item.getDescription());
                line.setQuantityGross(item.getQuantity());
                line.setQuantityNet(item.getQuantity()); // Assuming gross=net for now
                line.setRate(item.getRate());
                line.setLineDiscount(item.getLineDiscount());
                line.setDiscountPercent(item.getDiscountPercent());
                line.setTaxPercent(item.getTaxPercent());

                if (item.getItem() != null)
                    line.setItemId(item.getItem().getId());
                if (item.getCategory() != null)
                    line.setCategoryId(item.getCategory().getId());
                if (item.getSubCategory() != null)
                    line.setSubCategoryId(item.getSubCategory().getId());
                if (item.getUnit() != null)
                    line.setUnitId(item.getUnit().getId());
                if (item.getTax() != null)
                    line.setTaxId(item.getTax().getId());

                return line;
            }).collect(Collectors.toList());
            req.setLines(lineRequests);
        }

        // Map Attachments (Optional: if we want to copy attachments)
        // For now, we are not copying attachments to avoid duplication on disk unless
        // requested.

        // 2. Create Invoice
        PurPurchaseInvoiceResponse invoice = invoiceService.create(req, null);

        // 3. Update PO Status
        po.setStatus("Billed");
        repo.save(po);

        return invoice;
    }

    @Transactional
    public PurPurchasePaymentResponse convertToPayment(Long id) {
        Tenant t = currentTenant();
        PurPurchaseOrder po = repo.findByIdAndTenantId(id, t.getId())
                .orElseThrow(() -> new EntityNotFoundException("Purchase order not found: " + id));

        PurPurchasePaymentRequest req = new PurPurchasePaymentRequest();
        if (po.getSupplier() != null) {
            req.setSupplierId(po.getSupplier().getId());
        }

        // Default amount to PO total amount
        req.setAmount(po.getTotalAmount());
        req.setPaymentDate(LocalDate.now());
        req.setReference("Payment for PO: " + po.getPoNumber());
        req.setCreatedBy(po.getCreatedBy());

        // Note: We cannot automatically allocate because allocations link to Invoices,
        // not POs directly.
        // User will likely convert PO -> Invoice first, or this payment is an advance.

        PurPurchasePaymentResponse payment = paymentService.create(req, null);

        return payment;
    }
}
