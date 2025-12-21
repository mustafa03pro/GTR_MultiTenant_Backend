package com.example.multi_tanent.pos.service;

import com.example.multi_tanent.pos.dto.StockMovementRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.multi_tanent.pos.dto.ProductDto;
import com.example.multi_tanent.pos.dto.ProductRequest;
import com.example.multi_tanent.pos.dto.ProductVariantRequest;
import com.example.multi_tanent.pos.entity.*;
import com.example.multi_tanent.pos.repository.*;
import com.example.multi_tanent.spersusers.enitity.Store;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import com.example.multi_tanent.spersusers.repository.StoreRepository;
import com.example.multi_tanent.spersusers.repository.TenantRepository;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.stream.Collectors;

@Service
@Transactional("tenantTx")
public class ProductService {

    private final ProductRepository productRepository;
    private final TenantRepository tenantRepository;
    private final TaxRateRepository taxRateRepository;
    private final ProductVariantService productVariantService;
    private final SaleItemRepository saleItemRepository;
    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;
    private final StoreRepository storeRepository;
    private final StockMovementService stockMovementService;
    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;

    public ProductService(ProductRepository productRepository,
            TenantRepository tenantRepository,
            TaxRateRepository taxRateRepository,
            ProductVariantService productVariantService,
            SaleItemRepository saleItemRepository,
            CategoryRepository categoryRepository,
            ObjectMapper objectMapper,
            StoreRepository storeRepository,
            StockMovementService stockMovementService,
            InventoryRepository inventoryRepository,
            StockMovementRepository stockMovementRepository) {
        this.productRepository = productRepository;
        this.tenantRepository = tenantRepository;
        this.taxRateRepository = taxRateRepository;
        this.productVariantService = productVariantService;
        this.saleItemRepository = saleItemRepository;
        this.categoryRepository = categoryRepository;
        this.objectMapper = objectMapper;
        this.storeRepository = storeRepository;
        this.stockMovementService = stockMovementService;
        this.inventoryRepository = inventoryRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    private Tenant getCurrentTenant() {
        return tenantRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException(
                        "Tenant context not found. Cannot perform product operations."));
    }

    public Product createProduct(ProductRequest request) {
        Tenant currentTenant = getCurrentTenant();

        Product product = new Product();
        product.setTenant(currentTenant);
        product.setName(request.getName());
        product.setSku(request.getSku());
        product.setDescription(request.getDescription());
        product.setActive(request.isActive());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findByIdAndTenantId(request.getCategoryId(), currentTenant.getId())
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + request.getCategoryId()));
            product.setCategory(category);
        }

        if (request.getVariants() != null && !request.getVariants().isEmpty()) {
            List<ProductVariant> variants = request.getVariants().stream()
                    .map(variantRequest -> mapVariantRequestToEntity(variantRequest, product, currentTenant.getId()))
                    .collect(Collectors.toList());
            product.getVariants().addAll(variants);
        }

        Product savedProduct = productRepository.save(product);

        // Generate barcode images for all variants
        if (savedProduct.getVariants() != null) {
            System.out.println(
                    "DEBUG: Generating barcode images for " + savedProduct.getVariants().size() + " variants.");
            for (ProductVariant variant : savedProduct.getVariants()) {
                System.out.println("DEBUG: Generating barcode for variant SKU: " + variant.getSku());
                productVariantService.generateAndSetBarcodeUrl(variant);
            }
        } else {
            System.out.println("DEBUG: No variants found for product: " + savedProduct.getName());
        }

        return savedProduct;
    }

    @Transactional
    public String bulkAddProducts(MultipartFile file) {
        Tenant currentTenant = getCurrentTenant();
        List<String> errors = new ArrayList<>();
        Map<String, Product> productsToCreate = new HashMap<>();
        List<StockMovementRequest> stockMovementsToCreate = new ArrayList<>();
        int productsCreated = 0;
        int variantsCreated = 0;

        try (InputStream is = file.getInputStream()) {
            Workbook workbook = WorkbookFactory.create(is);
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            // Pre-fetch existing data for validation to avoid multiple DB calls in a loop
            Map<String, Category> categoryCache = categoryRepository.findByTenantId(currentTenant.getId())
                    .stream().collect(Collectors.toMap(c -> c.getName().toLowerCase(), Function.identity()));
            Map<String, TaxRate> taxRateCache = taxRateRepository.findByTenantId(currentTenant.getId())
                    .stream().collect(Collectors.toMap(t -> t.getName().toLowerCase(), Function.identity()));
            Map<String, Store> storeCache = storeRepository.findByTenantId(currentTenant.getId())
                    .stream().collect(Collectors.toMap(s -> s.getName().toLowerCase(), Function.identity()));

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (isRowEmpty(row))
                    continue;

                try {
                    String productName = formatter.formatCellValue(row.getCell(0)).trim();
                    String productDescription = formatter.formatCellValue(row.getCell(1)).trim();
                    String categoryName = formatter.formatCellValue(row.getCell(2)).trim();
                    String variantSku = formatter.formatCellValue(row.getCell(3)).trim();
                    String variantBarcode = formatter.formatCellValue(row.getCell(4)).trim();
                    String variantAttributes = formatter.formatCellValue(row.getCell(5)).trim();
                    String priceCentsStr = formatter.formatCellValue(row.getCell(6)).trim();
                    String costCentsStr = formatter.formatCellValue(row.getCell(7)).trim();
                    String taxRateName = formatter.formatCellValue(row.getCell(8)).trim();
                    String storeName = formatter.formatCellValue(row.getCell(9)).trim();
                    String initialStockStr = formatter.formatCellValue(row.getCell(10)).trim();
                    String imageUrl = formatter.formatCellValue(row.getCell(11)).trim();

                    if (productName.isEmpty()) {
                        errors.add("Row " + (i + 1) + ": Product Name (column 1) is required.");
                        continue;
                    }
                    if (variantSku.isEmpty()) {
                        errors.add("Row " + (i + 1) + ": Variant SKU (column 4) is required.");
                        continue;
                    }
                    if (priceCentsStr.isEmpty()) {
                        errors.add("Row " + (i + 1) + ": Variant Price (column 7) is required.");
                        continue;
                    }

                    // Validate Image URL format to prevent local paths.
                    // The server cannot access local file paths from your computer.
                    if (imageUrl != null && !imageUrl.isBlank()
                            && (imageUrl.startsWith("/") || imageUrl.matches("^[a-zA-Z]:\\\\.*"))) {
                        errors.add("Row " + (i + 1) + ": Invalid Image URL. Local paths like '" + imageUrl
                                + "' are not allowed. Please upload images to the server first to get a valid URL, then use that URL in the Excel file.");
                        continue;
                    }

                    // Find or create the product in our map
                    Product product = productsToCreate.computeIfAbsent(productName.toLowerCase(), pn -> {
                        Product newProduct = new Product();
                        newProduct.setTenant(currentTenant);
                        newProduct.setName(productName);
                        newProduct.setDescription(productDescription);
                        newProduct.setActive(true);

                        if (!categoryName.isEmpty()) {
                            Category category = categoryCache.get(categoryName.toLowerCase());
                            if (category == null) {
                                throw new IllegalArgumentException("Category '" + categoryName + "' not found.");
                            }
                            newProduct.setCategory(category);
                        }
                        return newProduct;
                    });

                    // Check for duplicate SKU within the file
                    boolean skuExistsInFile = product.getVariants().stream()
                            .anyMatch(v -> v.getSku().equalsIgnoreCase(variantSku));
                    if (skuExistsInFile) {
                        throw new IllegalArgumentException("Duplicate Variant SKU '" + variantSku
                                + "' found in the file for product '" + productName + "'.");
                    }

                    // Check for duplicate SKU in the database
                    if (productRepository.existsByVariantSku(variantSku, currentTenant.getId())) {
                        throw new IllegalArgumentException(
                                "Variant SKU '" + variantSku + "' already exists in the database.");
                    }

                    ProductVariant variant = new ProductVariant();
                    variant.setProduct(product);
                    variant.setSku(variantSku);
                    if (variantBarcode != null && !variantBarcode.isEmpty()) {
                        variant.setBarcode(variantBarcode);
                    } else {
                        String generated = productVariantService.generateRandomBarcode();
                        System.out.println("DEBUG: Bulk Import - Auto-generated barcode: " + generated + " for SKU: "
                                + variantSku);
                        variant.setBarcode(generated);
                    }
                    if (!variantAttributes.isEmpty()) {
                        JsonNode attributesJson = objectMapper.readTree(variantAttributes);
                        variant.setAttributes(attributesJson);
                    } else {
                        variant.setAttributes(null);
                    }
                    variant.setImageUrl(imageUrl);
                    variant.setActive(true);

                    try {
                        variant.setPriceCents(Integer.parseInt(priceCentsStr));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                                "Invalid number format for Variant Price '" + priceCentsStr + "'.");
                    }

                    if (!costCentsStr.isEmpty()) {
                        try {
                            variant.setCostCents(Integer.parseInt(costCentsStr));
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException(
                                    "Invalid number format for Variant Cost '" + costCentsStr + "'.");
                        }
                    }

                    if (!taxRateName.isEmpty()) {
                        TaxRate taxRate = taxRateCache.get(taxRateName.toLowerCase());
                        if (taxRate == null) {
                            throw new IllegalArgumentException("Tax Rate '" + taxRateName + "' not found.");
                        }
                        variant.setTaxRate(taxRate);
                    }

                    product.getVariants().add(variant);

                    // Prepare stock movement if initial stock is provided
                    if (!initialStockStr.isEmpty() && !storeName.isEmpty()) {
                        Store store = storeCache.get(storeName.toLowerCase());
                        if (store == null) {
                            throw new IllegalArgumentException("Store '" + storeName + "' not found.");
                        }
                        long quantity;
                        try {
                            quantity = Long.parseLong(initialStockStr);
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException(
                                    "Invalid number format for Initial Stock Quantity '" + initialStockStr + "'.");
                        }

                        if (quantity > 0) {
                            StockMovementRequest stockRequest = new StockMovementRequest();
                            stockRequest.setStoreId(store.getId());
                            stockRequest.setProductVariantSkuForBulk(variant.getSku()); // Use SKU as temporary link
                            stockRequest.setChangeQuantity(quantity);
                            stockRequest.setReason("Initial Stock (Bulk Upload)");
                            stockMovementsToCreate.add(stockRequest);
                        }
                    }
                } catch (Exception e) {
                    errors.add("Error on row " + (i + 1) + ": " + e.getMessage());
                }
            }

            if (!errors.isEmpty()) {
                throw new IllegalArgumentException("File processing failed with errors:\n" + String.join("\n", errors));
            }

            Collection<Product> savedProducts = productRepository.saveAll(productsToCreate.values());
            productsCreated = savedProducts.size();
            variantsCreated = savedProducts.stream().mapToInt(p -> p.getVariants().size()).sum();

            // Generate barcode images for all variants from bulk import
            for (Product savedProduct : savedProducts) {
                if (savedProduct.getVariants() != null) {
                    for (ProductVariant variant : savedProduct.getVariants()) {
                        try {
                            productVariantService.generateAndSetBarcodeUrl(variant);
                        } catch (Exception e) {
                            System.out.println(
                                    "DEBUG: Failed to generate barcode image for bulk variant: " + variant.getSku());
                            e.printStackTrace();
                        }
                    }
                }
            }

            // After products are saved, variants have IDs. Now create stock movements.
            if (!stockMovementsToCreate.isEmpty()) {
                // Create a map of SKU to variant ID from the saved products
                Map<String, Long> skuToVariantIdMap = savedProducts.stream()
                        .flatMap(p -> p.getVariants().stream())
                        .collect(Collectors.toMap(ProductVariant::getSku, ProductVariant::getId));

                for (StockMovementRequest stockRequest : stockMovementsToCreate) {
                    Long variantId = skuToVariantIdMap.get(stockRequest.getProductVariantSkuForBulk());
                    stockRequest.setProductVariantId(variantId);
                    stockRequest.setProductVariantSkuForBulk(null); // Clean up temporary field
                    stockMovementService.createStockMovement(stockRequest);
                }
            }

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to process Excel file: " + e.getMessage(), e);
        }

        return String.format("Successfully created %d products with %d variants.", productsCreated, variantsCreated);
    }

    public byte[] generateBulkAddTemplate() throws IOException {
        Tenant currentTenant = getCurrentTenant();

        try (
                Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // 1. Create main sheet for products
            Sheet productSheet = workbook.createSheet("Products");

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);

            String[] headers = {
                    "Product Name", "Product Description", "Category Name",
                    "Variant SKU", "Variant Barcode", "Variant Attributes (JSON)",
                    "Variant Price (in cents)", "Variant Cost (in cents)", "Tax Rate Name",
                    "Store Name (for initial stock)", "Initial Stock Quantity",
                    "Image URL"
            };
            Row headerRow = productSheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerCellStyle);
                productSheet.autoSizeColumn(i);
            }

            // Add an example row
            Row exampleRow = productSheet.createRow(1);
            exampleRow.createCell(0).setCellValue("T-Shirt");
            exampleRow.createCell(1).setCellValue("Comfortable cotton t-shirt");
            exampleRow.createCell(2).setCellValue("Apparel");
            exampleRow.createCell(3).setCellValue("TSHIRT-RED-S");
            exampleRow.createCell(4).setCellValue("123456789012");
            exampleRow.createCell(5).setCellValue("{\"color\":\"Red\", \"size\":\"Small\"}");
            exampleRow.createCell(6).setCellValue(1500); // e.g., $15.00
            exampleRow.createCell(7).setCellValue(700); // e.g., $7.00
            exampleRow.createCell(8).setCellValue("VAT 5%");
            exampleRow.createCell(9).setCellValue("Main Store");
            exampleRow.createCell(10).setCellValue(50);
            exampleRow.createCell(11).setCellValue("product-images/your-image-name.jpg");

            // 2. Create a helper sheet for Categories
            Sheet categorySheet = workbook.createSheet("Available Categories");
            List<Category> categories = categoryRepository.findByTenantId(currentTenant.getId());
            createHelperSheet(categorySheet, "Category Name",
                    categories.stream().map(Category::getName).collect(Collectors.toList()), headerCellStyle);

            // 3. Create a helper sheet for Tax Rates
            Sheet taxSheet = workbook.createSheet("Available Tax Rates");
            List<TaxRate> taxRates = taxRateRepository.findByTenantId(currentTenant.getId());
            createHelperSheet(taxSheet, "Tax Rate Name",
                    taxRates.stream().map(TaxRate::getName).collect(Collectors.toList()), headerCellStyle);

            // 4. Create a helper sheet for Stores
            Sheet storeSheet = workbook.createSheet("Available Stores");
            List<Store> stores = storeRepository.findByTenantId(currentTenant.getId());
            createHelperSheet(storeSheet, "Store Name",
                    stores.stream().map(Store::getName).collect(Collectors.toList()), headerCellStyle);

            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            // Consider logging the error
            throw new IOException("Failed to generate Excel template.", e);
        }
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getAllProductsForCurrentTenant() {
        Tenant currentTenant = getCurrentTenant();
        List<Product> products = productRepository.findByTenantId(currentTenant.getId());
        return products.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<ProductDto> getProductById(Long id) {
        Tenant currentTenant = getCurrentTenant();
        Optional<Product> productOpt = productRepository.findByIdAndTenantId(id, currentTenant.getId());
        return productOpt.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getProductsByCategoryName(String categoryName) {
        Tenant currentTenant = getCurrentTenant();
        List<Product> products = productRepository.findByTenantIdAndCategoryNameIgnoreCase(currentTenant.getId(),
                categoryName);
        return products.stream().map(this::toDto).collect(Collectors.toList());
    }

    public ProductDto updateProduct(Long id, ProductRequest request) {
        Tenant currentTenant = getCurrentTenant();
        Product product = productRepository.findByIdAndTenantId(id, currentTenant.getId())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        product.setName(request.getName());
        product.setSku(request.getSku());
        product.setDescription(request.getDescription());
        product.setActive(request.isActive());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findByIdAndTenantId(request.getCategoryId(), currentTenant.getId())
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + request.getCategoryId()));
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }

        // --- Safe Variant Update Logic ---
        // Use maps for efficient lookups
        java.util.Map<String, ProductVariant> existingVariantsBySku = product.getVariants().stream()
                .collect(Collectors.toMap(
                        ProductVariant::getSku,
                        v -> v,
                        (first, second) -> first // In case of duplicate SKUs in DB, pick the first one.
                ));

        List<ProductVariantRequest> incomingVariants = request.getVariants() != null ? request.getVariants()
                : new ArrayList<>();

        // 1. Update existing variants or create new ones
        for (ProductVariantRequest variantRequest : incomingVariants) {
            ProductVariant existingVariant = existingVariantsBySku.get(variantRequest.getSku());
            if (existingVariant != null) {
                // Update existing
                mapVariantRequestToEntity(variantRequest, existingVariant, product, currentTenant.getId());
                existingVariantsBySku.remove(variantRequest.getSku()); // Remove from map to track which ones are left
            } else {
                // Create new
                ProductVariant newVariant = mapVariantRequestToEntity(variantRequest, new ProductVariant(), product,
                        currentTenant.getId());
                product.getVariants().add(newVariant);
            }
        }

        // 2. The remaining variants in the map are the ones to be removed
        List<ProductVariant> variantsToRemove = new ArrayList<>(existingVariantsBySku.values());

        // 3. Soft-delete or hard-delete variants that are being removed
        for (ProductVariant variantToRemove : variantsToRemove) {
            boolean hasBeenSold = variantToRemove.getId() != null
                    && saleItemRepository.existsByProductVariantId(variantToRemove.getId());
            if (hasBeenSold) {
                variantToRemove.setActive(false); // Soft delete
            } else {
                // Hard delete by removing from the collection (thanks to orphanRemoval=true)
                product.getVariants().remove(variantToRemove);
            }
        }

        Product savedProduct = productRepository.save(product);
        return toDto(savedProduct);
    }

    public void deleteProduct(Long id) {
        Product product = productRepository.findByIdAndTenantId(id, getCurrentTenant().getId())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        // Check if any variant of this product has been sold
        boolean hasSalesHistory = false;
        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            for (ProductVariant variant : product.getVariants()) {
                if (saleItemRepository.existsByProductVariantId(variant.getId())) {
                    hasSalesHistory = true;
                    break;
                }
            }
        }

        if (hasSalesHistory) {
            // Soft delete
            product.setActive(false);
            if (product.getVariants() != null) {
                product.getVariants().forEach(v -> v.setActive(false));
            }
            productRepository.save(product);
        } else {
            // Hard delete
            // Before deleting the product, we must delete dependent records to avoid
            // foreign key constraints.
            if (product.getVariants() != null && !product.getVariants().isEmpty()) {
                List<Long> variantIds = product.getVariants().stream().map(ProductVariant::getId)
                        .collect(Collectors.toList());
                // Delete associated inventory records
                inventoryRepository.deleteByProductVariantIdIn(variantIds);
                // Delete associated stock movement records
                stockMovementRepository.deleteByProductVariantIdIn(variantIds);
            }

            productRepository.delete(product);
        }
    }

    public void hardDeleteProduct(Long id) {
        Product product = productRepository.findByIdAndTenantId(id, getCurrentTenant().getId())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            for (ProductVariant variant : product.getVariants()) {
                // Unlink from sales
                List<SaleItem> saleItems = saleItemRepository.findByProductVariantId(variant.getId());
                for (SaleItem item : saleItems) {
                    item.setProductVariant(null);
                    saleItemRepository.save(item);
                }
            }

            List<Long> variantIds = product.getVariants().stream().map(ProductVariant::getId)
                    .collect(Collectors.toList());
            // Delete associated inventory records
            inventoryRepository.deleteByProductVariantIdIn(variantIds);
            // Delete associated stock movement records
            stockMovementRepository.deleteByProductVariantIdIn(variantIds);
        }

        productRepository.delete(product);
    }

    private ProductVariant mapVariantRequestToEntity(ProductVariantRequest request, ProductVariant variant,
            Product product, Long tenantId) {
        variant.setProduct(product);
        if (request.getSku() != null && !request.getSku().isBlank()) {
            variant.setSku(request.getSku());
        } else if (variant.getSku() == null) {
            // Auto-generate SKU if missing and it's a new variant (or existing variant has
            // no SKU, which shouldn't happen)
            variant.setSku("SKU-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 1000));
        }
        if (request.getBarcode() == null || request.getBarcode().trim().isEmpty()) {
            // Auto-generate barcode if not provided
            String generated = productVariantService.generateRandomBarcode();
            System.out.println("DEBUG: Auto-generated barcode: " + generated + " for SKU: " + variant.getSku());
            variant.setBarcode(generated);
        } else {
            variant.setBarcode(request.getBarcode());
        }
        if (request.getAttributes() != null) {
            variant.setAttributes(request.getAttributes());
        } else {
            // If attributes are not provided in the request, set to null or an empty JSON
            // object
            variant.setAttributes(objectMapper.createObjectNode());
        }

        // If active flag is not provided in request, default to true for new/updated
        // variants
        if (request.getActive() != null) {
            variant.setActive(request.getActive());
        } else if (variant.getId() == null) { // New variant
            variant.setActive(true);
        }

        variant.setPriceCents(request.getPriceCents());
        variant.setCostCents(request.getCostCents());
        variant.setImageUrl(request.getImageUrl());

        if (request.getTaxRateId() != null) {
            TaxRate taxRate = taxRateRepository.findByIdAndTenantId(request.getTaxRateId(), tenantId)
                    .orElseThrow(() -> new RuntimeException("TaxRate not found with id: " + request.getTaxRateId()));
            variant.setTaxRate(taxRate);
        }
        return variant;
    }

    private ProductVariant mapVariantRequestToEntity(ProductVariantRequest request, Product product, Long tenantId) {
        return mapVariantRequestToEntity(request, new ProductVariant(), product, tenantId);
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        DataFormatter formatter = new DataFormatter();
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && !formatter.formatCellValue(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void createHelperSheet(Sheet sheet, String header, List<String> values, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        Cell headerCell = headerRow.createCell(0);
        headerCell.setCellValue(header);
        headerCell.setCellStyle(headerStyle);
        sheet.autoSizeColumn(0);

        for (int i = 0; i < values.size(); i++) {
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(values.get(i));
        }
    }

    private ProductDto toDto(Product product) {
        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setSku(product.getSku());
        dto.setDescription(product.getDescription());
        dto.setActive(product.isActive());
        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getId());
            dto.setCategoryName(product.getCategory().getName());
        }
        dto.setVariants(product.getVariants().stream().map(variant -> productVariantService.toDto(variant))
                .collect(Collectors.toList()));
        return dto;
    }
}