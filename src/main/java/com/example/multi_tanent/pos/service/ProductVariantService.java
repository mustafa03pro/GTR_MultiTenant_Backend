package com.example.multi_tanent.pos.service;

import com.example.multi_tanent.config.TenantContext;
import com.example.multi_tanent.master.entity.MasterTenant;
import com.example.multi_tanent.master.repository.MasterTenantRepository;
import com.example.multi_tanent.pos.dto.ProductVariantDto;
import com.example.multi_tanent.pos.dto.ProductVariantRequest;
import com.example.multi_tanent.pos.entity.*;
import com.example.multi_tanent.pos.repository.ProductRepository;
import com.example.multi_tanent.pos.repository.ProductVariantRepository;
import com.example.multi_tanent.pos.repository.SaleItemRepository;
import com.example.multi_tanent.pos.repository.TaxRateRepository;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import com.example.multi_tanent.spersusers.repository.TenantRepository;
import com.google.zxing.WriterException;
import java.io.IOException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional("tenantTx")
public class ProductVariantService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final TenantRepository tenantRepository;
    private final TaxRateRepository taxRateRepository;
    private final SaleItemRepository saleItemRepository;
    private final BarCodeService barCodeService;
    private final MasterTenantRepository masterTenantRepository;

    public ProductVariantService(ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            TenantRepository tenantRepository,
            TaxRateRepository taxRateRepository,
            SaleItemRepository saleItemRepository,
            BarCodeService barCodeService,
            MasterTenantRepository masterTenantRepository) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.tenantRepository = tenantRepository;
        this.taxRateRepository = taxRateRepository;
        this.saleItemRepository = saleItemRepository;
        this.barCodeService = barCodeService;
        this.masterTenantRepository = masterTenantRepository;
    }

    private Tenant getCurrentTenant() {
        return tenantRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException(
                        "Tenant context not found. Cannot perform variant operations."));
    }

    private Product getProductForCurrentTenant(Long productId) {
        Tenant currentTenant = getCurrentTenant();
        return productRepository.findByIdAndTenantId(productId, currentTenant.getId())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
    }

    public ProductVariantDto addVariant(Long productId, ProductVariantRequest request) {
        Product product = getProductForCurrentTenant(productId);
        ProductVariant variant = mapRequestToEntity(request, new ProductVariant(), product);
        // First save to get an ID
        productVariantRepository.save(variant);
        // Generate and save barcode, then update the entity
        ProductVariant savedVariant = generateAndSetBarcodeUrl(variant);
        return toDto(savedVariant);
    }

    @Transactional(readOnly = true)
    public List<ProductVariantDto> getVariantsForProduct(Long productId) {
        Product product = getProductForCurrentTenant(productId);
        return product.getVariants().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<ProductVariantDto> getVariantById(Long productId, Long variantId) {
        getProductForCurrentTenant(productId); // Ensures product exists for the tenant
        return productVariantRepository.findByIdAndProductId(variantId, productId).map(this::toDto);
    }

    public ProductVariantDto updateVariant(Long productId, Long variantId, ProductVariantRequest request) {
        Product product = getProductForCurrentTenant(productId);
        ProductVariant existingVariant = productVariantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new RuntimeException(
                        "ProductVariant not found with id: " + variantId + " for product " + productId));

        String oldSku = existingVariant.getSku();
        ProductVariant updatedVariant = mapRequestToEntity(request, existingVariant, product);

        // If SKU has changed, regenerate the barcode
        if (!oldSku.equals(updatedVariant.getSku())) {
            updatedVariant = generateAndSetBarcodeUrl(updatedVariant);
        }

        return toDto(productVariantRepository.save(updatedVariant));
    }

    public void deleteVariant(Long productId, Long variantId) {
        Product product = getProductForCurrentTenant(productId); // Ensure product exists for the tenant
        ProductVariant variant = productVariantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new RuntimeException("ProductVariant not found with id: " + variantId));

        boolean hasBeenSold = saleItemRepository.existsByProductVariantId(variant.getId());
        if (hasBeenSold) {
            variant.setActive(false); // Soft delete by marking as inactive
            productVariantRepository.save(variant);
        } else {
            productVariantRepository.delete(variant); // Hard delete if it has never been sold
        }
    }

    /**
     * Finds a product variant by its SKU across all tenants.
     * This is useful for public-facing pages, like QR code scans.
     * 
     * @param sku The SKU to search for.
     * @return An Optional containing the ProductVariantDto if found.
     */
    @Transactional(transactionManager = "tenantTx", propagation = Propagation.NEVER)
    public Optional<ProductVariantDto> findVariantBySkuGlobally(String sku) {
        List<String> tenantIds = masterTenantRepository.findAll().stream().map(MasterTenant::getTenantId)
                .collect(Collectors.toList());
        for (String tenantId : tenantIds) {
            try {
                TenantContext.setTenantId(tenantId);
                Optional<ProductVariant> variant = productVariantRepository.findBySku(sku);
                if (variant.isPresent()) {
                    return variant.map(this::toDto);
                }
            } finally {
                TenantContext.clear();
            }
        }
        return Optional.empty();
    }

    @Transactional(readOnly = true)
    public Optional<ProductVariantDto> getVariantByBarcode(String barcode) {
        // Ensure tenant context is active (handled by @Transactional("tenantTx") and
        // existing filters)
        // But we might need to be careful if this is a public scan vs authenticated
        // scan.
        // Assuming authenticated scan for now as per controller logic.
        return productVariantRepository.findByBarcode(barcode).map(this::toDto);
    }

    public ProductVariant generateAndSetBarcodeUrl(ProductVariant variant) {
        try {
            String barcodePath = barCodeService.generateAndSaveProductVariantBarcode(variant);
            variant.setBarcodeImageUrl(barcodePath);
            return productVariantRepository.save(variant);
        } catch (WriterException | IOException e) {
            // Log the error. For now, we'll rethrow as a runtime exception.
            throw new RuntimeException("Could not generate or save barcode for SKU: " + variant.getSku(), e);
        }
    }

    private ProductVariant mapRequestToEntity(ProductVariantRequest request, ProductVariant variant, Product product) {
        variant.setProduct(product);
        variant.setSku(request.getSku());

        if (request.getBarcode() == null || request.getBarcode().trim().isEmpty()) {
            // Auto-generate barcode if not provided
            variant.setBarcode(generateRandomBarcode());
        } else {
            variant.setBarcode(request.getBarcode());
        }

        variant.setAttributes(request.getAttributes());
        variant.setPriceCents(request.getPriceCents());

        // If active flag is not provided in request, default to true for new/updated
        // variants
        if (request.getActive() != null) {
            variant.setActive(request.getActive());
        } else if (variant.getId() == null) { // New variant
            variant.setActive(true);
        }
        variant.setCostCents(request.getCostCents());
        variant.setImageUrl(request.getImageUrl());

        if (request.getTaxRateId() != null) {
            TaxRate taxRate = taxRateRepository.findByIdAndTenantId(request.getTaxRateId(), product.getTenant().getId())
                    .orElseThrow(() -> new RuntimeException("TaxRate not found with id: " + request.getTaxRateId()));
            variant.setTaxRate(taxRate);
        } else {
            variant.setTaxRate(null);
        }
        return variant;
    }

    ProductVariantDto toDto(ProductVariant variant) {
        ProductVariantDto dto = new ProductVariantDto();
        dto.setId(variant.getId());
        dto.setSku(variant.getSku());
        dto.setBarcode(variant.getBarcode());
        dto.setAttributes(variant.getAttributes());
        dto.setPriceCents(variant.getPriceCents());
        dto.setActive(variant.isActive());
        dto.setCostCents(variant.getCostCents());
        if (variant.getImageUrl() != null && !variant.getImageUrl().isBlank()) {
            dto.setImageUrl(buildImageUrl(variant.getImageUrl()));
        }
        if (variant.getBarcodeImageUrl() != null && !variant.getBarcodeImageUrl().isBlank()) {
            dto.setBarcodeImageUrl(buildImageUrl(variant.getBarcodeImageUrl()));
        }
        if (variant.getTaxRate() != null) {
            dto.setTaxRateId(variant.getTaxRate().getId());
            dto.setTaxRateName(variant.getTaxRate().getName());
            dto.setTaxRatePercent(variant.getTaxRate().getPercent());
        }
        return dto;
    }

    private String buildImageUrl(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
            return relativePath;
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/pos/uploads/view/") // This path needs to match the public view endpoint
                .path(relativePath)
                .build()
                .toUriString()
                .replace("\\", "/"); // Ensure forward slashes for URL
    }

    public String generateRandomBarcode() {
        // Generate a random 12-digit number for the barcode
        // This is a simple implementation. In a real scenario, you might want to ensure
        // uniqueness or follow a specific standard like EAN-13.
        long number = (long) (Math.random() * 1_000_000_000_000L);
        return String.format("%012d", number);
    }
}