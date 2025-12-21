package com.example.multi_tanent.purchases.service;

// package com.example.multi_tanent.purchases.service;

import com.example.multi_tanent.config.TenantContext;
import com.example.multi_tanent.purchases.dto.*;
import com.example.multi_tanent.purchases.entity.*;
import com.example.multi_tanent.purchases.repository.*;
import com.example.multi_tanent.spersusers.enitity.BaseCustomer;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import com.example.multi_tanent.spersusers.repository.PartyRepository;
import com.example.multi_tanent.spersusers.repository.TenantRepository;
import com.example.multi_tanent.purchases.entity.PurPurchaseInvoice;
import com.example.multi_tanent.tenant.service.FileStorageService;
import com.example.multi_tanent.purchases.repository.PurPurchaseInvoiceRepository; // for invoice lookup

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.function.Function;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional("tenantTx")
public class PurPurchasePaymentService {

    private final PurPurchasePaymentRepository repo;
    private final PurPurchasePaymentAllocationRepository allocRepo;
    private final PurPurchasePaymentAttachmentRepository attachmentRepo;
    private final TenantRepository tenantRepo;
    private final PartyRepository partyRepo; // supplier repo
    private final PurPurchaseInvoiceRepository invoiceRepo;
    private final FileStorageService fileStorageService;

    private Tenant currentTenant() {
        String key = TenantContext.getTenantId();
        return tenantRepo.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("Tenant not resolved for key: " + key));
    }

    public PurPurchasePaymentResponse create(PurPurchasePaymentRequest req, MultipartFile[] files) {
        Tenant t = currentTenant();

        PurPurchasePayment p = new PurPurchasePayment();
        p.setSupplier(req.getSupplierId() != null ? partyRepo.findById(req.getSupplierId())
                .orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + req.getSupplierId())) : null);
        p.setAmount(req.getAmount());
        p.setPayFullAmount(Optional.ofNullable(req.getPayFullAmount()).orElse(Boolean.FALSE));
        p.setTaxDeducted(Optional.ofNullable(req.getTaxDeducted()).orElse(Boolean.FALSE));
        p.setTdsAmount(Optional.ofNullable(req.getTdsAmount()).orElse(BigDecimal.ZERO));
        p.setTdsSection(req.getTdsSection());
        p.setPaymentDate(req.getPaymentDate());
        p.setPaymentMode(req.getPaymentMode());
        p.setPaidThrough(req.getPaidThrough());
        p.setReference(req.getReference());
        p.setChequeNumber(req.getChequeNumber());
        p.setNotes(req.getNotes());
        p.setCreatedBy(req.getCreatedBy());
        p.setCreatedAt(LocalDateTime.now());
        p.setTenant(t);

        // allocations
        p.setAllocations(new ArrayList<>());
        if (req.getAllocations() != null) {
            for (PurPurchasePaymentAllocationRequest ar : req.getAllocations()) {
                PurPurchaseInvoice inv = invoiceRepo.findById(ar.getInvoiceId())
                        .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + ar.getInvoiceId()));
                PurPurchasePaymentAllocation a = new PurPurchasePaymentAllocation();
                a.setPurchaseInvoice(inv);
                a.setAllocatedAmount(ar.getAllocatedAmount());
                a.setAllocationNote(ar.getAllocationNote());
                a.setPurchasePayment(p);
                p.getAllocations().add(a);
            }
        }

        // Handle new file uploads
        if (files != null && files.length > 0) {
            // We can reuse attachFiles logic or inline it to associate with p before save
            // if needed,
            // but attachFiles does repo.save(p) at the end.
            // Since p is new and not saved yet, we should associate entities first or save
            // p first.
            // Let's save p first, then attach.
        }

        // compute simple derived fields (sum of allocations)
        recomputePaymentFields(p);

        PurPurchasePayment saved = repo.save(p);

        if (files != null && files.length > 0) {
            attachFiles(saved.getId(), files, req.getCreatedBy());
            // Reload to get attachments in response? attachFiles returns URLs, but we want
            // full response.
            // attachFiles saves updates.
            return getById(saved.getId());
        }

        return toResponse(saved);
    }

    public List<String> attachFiles(Long id, MultipartFile[] files, String uploadedBy) {
        Tenant t = currentTenant();
        PurPurchasePayment p = repo.findByIdAndTenantId(id, t.getId())
                .orElseThrow(() -> new EntityNotFoundException("Purchase payment not found: " + id));

        List<String> uploadedUrls = new ArrayList<>();
        if (files != null && files.length > 0) {
            String subDir = "purchase_payments";
            for (MultipartFile file : files) {
                if (file.isEmpty())
                    continue;

                String relativePath = fileStorageService.storeFile(file, subDir, true);
                PurPurchasePaymentAttachment a = new PurPurchasePaymentAttachment();
                a.setFileName(file.getOriginalFilename());
                a.setFilePath(relativePath);
                a.setUploadedBy(uploadedBy);
                a.setUploadedAt(LocalDateTime.now());
                p.addAttachment(a);
                uploadedUrls.add(fileStorageService.buildPublicUrl(relativePath));
            }
            repo.save(p);
        }
        return uploadedUrls;
    }

    @Transactional(readOnly = true)
    public Page<PurPurchasePaymentResponse> list(Pageable pageable) {
        Tenant t = currentTenant();
        return repo.findByTenantId(t.getId(), pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PurPurchasePaymentResponse getById(Long id) {
        Tenant t = currentTenant();
        PurPurchasePayment p = repo.findByIdAndTenantId(id, t.getId())
                .orElseThrow(() -> new EntityNotFoundException("Purchase payment not found: " + id));
        return toResponse(p);
    }

    @Transactional(readOnly = true)
    public List<PurPurchaseInvoiceResponse> getUnpaidInvoices(Long supplierId) {
        Tenant t = currentTenant();
        List<PurPurchaseInvoice> invoices = invoiceRepo.findUnpaidInvoices(supplierId, t.getId());

        return invoices.stream().map(inv -> {
            BigDecimal paid = Optional.ofNullable(inv.getPaymentAllocations())
                    .orElse(Collections.emptyList())
                    .stream()
                    .map(a -> Optional.ofNullable(a.getAllocatedAmount()).orElse(BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal net = Optional.ofNullable(inv.getNetTotal()).orElse(BigDecimal.ZERO);
            BigDecimal balance = net.subtract(paid);

            // Map manually or use a helper if available, but we need to inject the extra
            // fields
            PurPurchaseInvoiceResponse resp = mapInvoiceToResponse(inv);
            resp.setPaidAmount(paid);
            resp.setBalance(balance);
            return resp;
        }).collect(Collectors.toList());
    }

    // Helper to map invoice to response (can be moved to InvoiceService or
    // duplicated here for decoupling)
    private PurPurchaseInvoiceResponse mapInvoiceToResponse(PurPurchaseInvoice inv) {
        return PurPurchaseInvoiceResponse.builder()
                .id(inv.getId())
                .supplierId(inv.getSupplier() != null ? inv.getSupplier().getId() : null)
                .supplierName(inv.getSupplier() != null ? inv.getSupplier().getCompanyName() : null)
                .billNumber(inv.getBillNumber())
                .orderNumber(inv.getOrderNumber())
                .billDate(inv.getBillDate())
                .dueDate(inv.getDueDate())
                .netTotal(inv.getNetTotal())
                // derived fields can be set by caller
                .build();
    }

    public PurPurchasePaymentResponse update(Long id, PurPurchasePaymentRequest req, MultipartFile[] files) {
        Tenant t = currentTenant();
        PurPurchasePayment p = repo.findByIdAndTenantId(id, t.getId())
                .orElseThrow(() -> new EntityNotFoundException("Purchase payment not found: " + id));

        if (req.getSupplierId() != null) {
            p.setSupplier(partyRepo.findById(req.getSupplierId())
                    .orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + req.getSupplierId())));
        } else {
            p.setSupplier(null);
        }

        if (req.getAmount() != null)
            p.setAmount(req.getAmount());
        if (req.getPayFullAmount() != null)
            p.setPayFullAmount(req.getPayFullAmount());
        if (req.getTaxDeducted() != null)
            p.setTaxDeducted(req.getTaxDeducted());
        if (req.getTdsAmount() != null)
            p.setTdsAmount(req.getTdsAmount());
        if (req.getTdsSection() != null)
            p.setTdsSection(req.getTdsSection());
        if (req.getPaymentDate() != null)
            p.setPaymentDate(req.getPaymentDate());
        if (req.getPaymentMode() != null)
            p.setPaymentMode(req.getPaymentMode());
        if (req.getPaidThrough() != null)
            p.setPaidThrough(req.getPaidThrough());
        if (req.getReference() != null)
            p.setReference(req.getReference());
        if (req.getChequeNumber() != null)
            p.setChequeNumber(req.getChequeNumber());
        if (req.getNotes() != null)
            p.setNotes(req.getNotes());
        if (req.getCreatedBy() != null)
            p.setCreatedBy(req.getCreatedBy());

        // Replace allocations if provided
        if (req.getAllocations() != null) {
            // Smart merge to avoid constraint violation (delete-insert ordering issues)
            // 1. Map request items by Invoice ID
            Map<Long, PurPurchasePaymentAllocationRequest> incomingMap = req.getAllocations().stream()
                    .collect(Collectors.toMap(PurPurchasePaymentAllocationRequest::getInvoiceId, Function.identity(),
                            (a, b) -> a));

            // 2. Update existing items and remove orphans
            Iterator<PurPurchasePaymentAllocation> iterator = p.getAllocations().iterator();
            while (iterator.hasNext()) {
                PurPurchasePaymentAllocation existing = iterator.next();
                Long invId = existing.getPurchaseInvoice().getId();

                if (incomingMap.containsKey(invId)) {
                    // Update
                    PurPurchasePaymentAllocationRequest r = incomingMap.get(invId);
                    existing.setAllocatedAmount(r.getAllocatedAmount());
                    existing.setAllocationNote(r.getAllocationNote());
                    incomingMap.remove(invId); // Handled
                } else {
                    // Remove orphan
                    iterator.remove();
                    existing.setPurchasePayment(null);
                }
            }

            // 3. Add new items (remaining in incomingMap)
            for (PurPurchasePaymentAllocationRequest r : incomingMap.values()) {
                PurPurchaseInvoice inv = invoiceRepo.findById(r.getInvoiceId())
                        .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + r.getInvoiceId()));
                PurPurchasePaymentAllocation a = new PurPurchasePaymentAllocation();
                a.setPurchaseInvoice(inv);
                a.setAllocatedAmount(r.getAllocatedAmount());
                a.setAllocationNote(r.getAllocationNote());
                a.setPurchasePayment(p); // Set parent
                p.getAllocations().add(a);
            }
        }

        // Handle attachments on update
        if (req.getAttachments() != null || (files != null && files.length > 0)) {
            p.getAttachments().clear();

            // Re-add attachments that client sent back (existing ones to keep)
            if (req.getAttachments() != null) {
                for (PurPurchasePaymentAttachmentRequest ar : req.getAttachments()) {
                    if (ar.getFilePath() != null && !ar.getFilePath().isEmpty()) {
                        PurPurchasePaymentAttachment a = new PurPurchasePaymentAttachment();
                        a.setFileName(ar.getFileName());
                        a.setFilePath(ar.getFilePath());
                        a.setUploadedAt(ar.getUploadedAt());
                        a.setUploadedBy(ar.getUploadedBy());
                        p.addAttachment(a);
                    }
                }
            }

            // Add any newly uploaded files
            if (files != null && files.length > 0) {
                String subDir = "purchase_payments";
                for (MultipartFile file : files) {
                    if (file.isEmpty())
                        continue;
                    String relativePath = fileStorageService.storeFile(file, subDir, true);
                    PurPurchasePaymentAttachment a = new PurPurchasePaymentAttachment();
                    a.setFileName(file.getOriginalFilename());
                    a.setFilePath(relativePath);
                    a.setUploadedAt(LocalDateTime.now());
                    a.setUploadedBy(req.getCreatedBy());
                    p.addAttachment(a);
                }
            }
        }

        recomputePaymentFields(p);

        PurPurchasePayment updated = repo.save(p);
        return toResponse(updated);
    }

    public void delete(Long id) {
        Tenant t = currentTenant();
        PurPurchasePayment p = repo.findByIdAndTenantId(id, t.getId())
                .orElseThrow(() -> new EntityNotFoundException("Purchase payment not found: " + id));
        repo.delete(p);
    }

    /* ---------------- helpers ---------------- */

    private void recomputePaymentFields(PurPurchasePayment p) {
        // Basic derived values: amountPaid = sum allocated amounts (or min(amount,
        // sum))
        BigDecimal sumAllocated = Optional.ofNullable(p.getAllocations())
                .orElse(Collections.emptyList())
                .stream()
                .map(a -> Optional.ofNullable(a.getAllocatedAmount()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        p.setAmountPaid(sumAllocated);
        // amountUsedForPayments equals allocated total (business logic may differ)
        p.setAmountUsedForPayments(sumAllocated);
        // simple placeholders
        p.setAmountRefunded(Optional.ofNullable(p.getAmountRefunded()).orElse(BigDecimal.ZERO));
        BigDecimal inExcess = Optional.ofNullable(p.getAmount()).orElse(BigDecimal.ZERO).subtract(sumAllocated);
        p.setAmountInExcess(inExcess.compareTo(BigDecimal.ZERO) > 0 ? inExcess : BigDecimal.ZERO);
    }

    private PurPurchasePaymentResponse toResponse(PurPurchasePayment p) {
        PurPurchasePaymentResponse.PurPurchasePaymentResponseBuilder rb = PurPurchasePaymentResponse.builder()
                .id(p.getId())
                .supplierId(p.getSupplier() != null ? p.getSupplier().getId() : null)
                .supplierName(p.getSupplier() != null ? p.getSupplier().getCompanyName() : null)
                .amount(p.getAmount())
                .payFullAmount(p.getPayFullAmount())
                .taxDeducted(p.getTaxDeducted())
                .tdsAmount(p.getTdsAmount())
                .tdsSection(p.getTdsSection())
                .paymentDate(p.getPaymentDate())
                .paymentMode(p.getPaymentMode())
                .paidThrough(p.getPaidThrough())
                .reference(p.getReference())
                .chequeNumber(p.getChequeNumber())
                .amountPaid(p.getAmountPaid())
                .amountUsedForPayments(p.getAmountUsedForPayments())
                .amountRefunded(p.getAmountRefunded())
                .amountInExcess(p.getAmountInExcess())
                .notes(p.getNotes())
                .createdBy(p.getCreatedBy())
                .createdAt(p.getCreatedAt())
                .tenantId(p.getTenant() != null ? p.getTenant().getId() : null);

        List<PurPurchasePaymentAllocationResponse> allocs = Optional.ofNullable(p.getAllocations())
                .orElse(Collections.emptyList())
                .stream()
                .map(a -> PurPurchasePaymentAllocationResponse.builder()
                        .id(a.getId())
                        .invoiceId(a.getPurchaseInvoice() != null ? a.getPurchaseInvoice().getId() : null)
                        .invoiceNumber(a.getPurchaseInvoice() != null ? a.getPurchaseInvoice().getOrderNumber() : null)
                        .allocatedAmount(a.getAllocatedAmount())
                        .allocationNote(a.getAllocationNote())
                        .build())
                .collect(Collectors.toList());
        rb.allocations(allocs);

        List<PurPurchasePaymentAttachmentResponse> atts = Optional.ofNullable(p.getAttachments())
                .orElse(Collections.emptyList())
                .stream()
                .map(a -> PurPurchasePaymentAttachmentResponse.builder()
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
}
