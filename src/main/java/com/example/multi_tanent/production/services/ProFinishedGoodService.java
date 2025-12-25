package com.example.multi_tanent.production.services;

import com.example.multi_tanent.config.TenantContext;
import com.example.multi_tanent.production.dto.ProFinishedGoodRequest;
import com.example.multi_tanent.production.dto.ProFinishedGoodResponse;
import com.example.multi_tanent.production.entity.*;
import com.example.multi_tanent.production.repository.*;
import com.example.multi_tanent.production.enums.ItemType;
import com.example.multi_tanent.tenant.service.FileStorageService;
import com.example.multi_tanent.spersusers.enitity.Location;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import com.example.multi_tanent.spersusers.repository.LocationRepository;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProFinishedGoodService {

    private final ProFinishedGoodRepository proFinishedGoodRepository;
    private final ProCategoryRepository proCategoryRepository;
    private final ProSubCategoryRepository proSubCategoryRepository;
    private final ProUnitRepository proUnitRepository;
    private final ProTaxRepository proTaxRepository;
    private final LocationRepository locationRepository;
    private final com.example.multi_tanent.spersusers.repository.TenantRepository tenantRepository;
    private final FileStorageService fileStorageService;
    private final BarcodeService barcodeService;

    @Transactional
    public ProFinishedGoodResponse createFinishedGood(ProFinishedGoodRequest request) {
        String tenantIdentifier = TenantContext.getTenantId();
        Tenant tenant = null;
        try {
            // Try to parse as ID first (for backward compatibility if some contexts use
            // numeric ID strings)
            Long tId = Long.parseLong(tenantIdentifier);
            tenant = new Tenant();
            tenant.setId(tId);
        } catch (NumberFormatException e) {
            // If alphanumeric (e.g. "tata5"), find by tenantId string
            // Assuming we have access to TenantRepository, but we need to inject it first.
            // Wait, I need to check if I have TenantRepository injected. Steps below will
            // handle injection.
            // For now, I'll rely on a helper method I'll add.
            tenant = getTenantByIdentifier(tenantIdentifier);
        }
        Long tenantId = tenant.getId();

        // Check duplicate
        if (proFinishedGoodRepository.existsByTenantIdAndItemCodeIgnoreCase(tenantId, request.getItemCode())) {
            throw new IllegalArgumentException("Item code already exists: " + request.getItemCode());
        }

        ProFinishedGood entity = new ProFinishedGood();
        Tenant tenantRef = new Tenant();
        tenantRef.setId(tenantId);
        entity.setTenant(tenantRef);

        updateEntityFromRequest(entity, request, tenantId);

        // Handle Barcode
        String barcode = request.getBarcode();
        if (barcode == null || barcode.trim().isEmpty()) {
            barcode = generateAutoBarcode(request.getItemCode());
        }
        entity.setBarcode(barcode);

        // Generate and Save Barcode Image
        String barcodeImgUrl = generateAndSaveBarcodeImage(barcode, request.getName());
        entity.setBarcodeImgUrl(barcodeImgUrl);

        // Handle Image Upload
        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            String subDir = "finishedGood/" + System.currentTimeMillis();
            String path = fileStorageService.storeFile(request.getImageFile(), subDir, true);
            entity.setPicturePath(path);
        }

        ProFinishedGood saved = proFinishedGoodRepository.save(entity);
        return ProFinishedGoodResponse.fromEntity(saved);
    }

    @Transactional
    public ProFinishedGoodResponse updateFinishedGood(Long id, ProFinishedGoodRequest request) {
        Long tenantId = getCurrentTenantId();
        ProFinishedGood entity = proFinishedGoodRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new RuntimeException("Finished Good not found"));

        if (!entity.getItemCode().equalsIgnoreCase(request.getItemCode()) &&
                proFinishedGoodRepository.existsByTenantIdAndItemCodeIgnoreCase(tenantId, request.getItemCode())) {
            throw new IllegalArgumentException("Item code already exists: " + request.getItemCode());
        }

        updateEntityFromRequest(entity, request, tenantId);

        // Handle Image Upload (Update)
        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            // Optional: Delete old image if needed
            String subDir = "finishedGood/" + System.currentTimeMillis();
            String path = fileStorageService.storeFile(request.getImageFile(), subDir, true);
            entity.setPicturePath(path);
        }

        ProFinishedGood saved = proFinishedGoodRepository.save(entity);
        return ProFinishedGoodResponse.fromEntity(saved);
    }

    public List<ProFinishedGoodResponse> getAllFinishedGoods() {
        Long tenantId = getCurrentTenantId();
        return proFinishedGoodRepository.findByTenantId(tenantId).stream()
                .map(ProFinishedGoodResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public ProFinishedGoodResponse getFinishedGoodById(Long id) {
        Long tenantId = getCurrentTenantId();
        return proFinishedGoodRepository.findByTenantIdAndId(tenantId, id)
                .map(ProFinishedGoodResponse::fromEntity)
                .orElseThrow(() -> new RuntimeException("Finished Good not found"));
    }

    public void deleteFinishedGood(Long id) {
        Long tenantId = getCurrentTenantId();
        ProFinishedGood entity = proFinishedGoodRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new RuntimeException("Finished Good not found"));
        proFinishedGoodRepository.delete(entity);
    }

    public org.springframework.core.io.Resource getFinishedGoodImage(Long id) {
        Long tenantId = getCurrentTenantId();
        ProFinishedGood entity = proFinishedGoodRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new RuntimeException("Finished Good not found"));

        String path = entity.getPicturePath();
        if (path == null || path.isEmpty()) {
            throw new RuntimeException("No image found for this finished good");
        }

        return fileStorageService.loadFileAsResource(path, true);
    }

    public org.springframework.core.io.Resource getFinishedGoodBarcodeImage(Long id) {
        Long tenantId = getCurrentTenantId();
        ProFinishedGood entity = proFinishedGoodRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new RuntimeException("Finished Good not found"));

        String path = entity.getBarcodeImgUrl();
        if (path == null || path.isEmpty()) {
            throw new RuntimeException("No barcode image found for this finished good");
        }

        return fileStorageService.loadFileAsResource(path, true);
    }

    // Helper Methods

    private void updateEntityFromRequest(ProFinishedGood entity, ProFinishedGoodRequest request, Long tenantId) {
        entity.setName(request.getName());
        entity.setItemCode(request.getItemCode());
        entity.setHsnSacCode(request.getHsnSacCode());
        entity.setDescription(request.getDescription());

        entity.setItemType(request.getItemType() != null ? request.getItemType() : ItemType.PRODUCT);
        entity.setForPurchase(request.isForPurchase());
        entity.setForSales(request.isForSales());
        entity.setTaxInclusive(request.isTaxInclusive());

        entity.setUnitRelation(request.getUnitRelation());
        entity.setTolerancePercentage(request.getTolerancePercentage());
        entity.setReorderLimit(request.getReorderLimit());
        entity.setPurchasePrice(request.getPurchasePrice());
        entity.setSalesPrice(request.getSalesPrice());

        if (request.getLocationId() != null) {
            Location loc = new Location();
            loc.setId(request.getLocationId());
            entity.setLocation(loc);
        }

        if (request.getCategoryId() != null) {
            ProCategory cat = proCategoryRepository.findByTenantIdAndId(tenantId, request.getCategoryId())
                    .orElse(null);
            entity.setCategory(cat);
        }
        if (request.getSubCategoryId() != null) {
            ProSubCategory sub = proSubCategoryRepository.findByTenantIdAndId(tenantId, request.getSubCategoryId())
                    .orElse(null);
            entity.setSubCategory(sub);
        }
        if (request.getIssueUnitId() != null) {
            ProUnit unit = proUnitRepository.findByTenantIdAndId(tenantId, request.getIssueUnitId())
                    .orElse(null);
            entity.setIssueUnit(unit);
        }
        if (request.getPurchaseUnitId() != null) {
            ProUnit unit = proUnitRepository.findByTenantIdAndId(tenantId, request.getPurchaseUnitId())
                    .orElse(null);
            entity.setPurchaseUnit(unit);
        }
        if (request.getTaxId() != null) {
            ProTax tax = proTaxRepository.findByTenantIdAndId(tenantId, request.getTaxId())
                    .orElse(null);
            entity.setTax(tax);
        }
    }

    private String generateAutoBarcode(String itemCode) {
        return "BC-" + itemCode + "-" + System.currentTimeMillis();
    }

    private String generateAndSaveBarcodeImage(String barcodeText, String productName) {
        byte[] barcodeImage = barcodeService.generateBarcodeImage(barcodeText, 300, 100);
        String subDir = "finishedGood/barcode";
        String filename = productName.replaceAll("[^a-zA-Z0-9.-]", "_") + "_" + barcodeText + "_"
                + System.currentTimeMillis() + ".png";
        return fileStorageService.storeFile(barcodeImage, subDir, filename);
    }

    // Excel Features

    public ByteArrayInputStream downloadTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Finished Goods Import");
            Row header = sheet.createRow(0);
            String[] headers = { "Item Code", "Name", "Category", "Sub Category", "Sales Price", "Purchase Price",
                    "Description" };
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate excel template", e);
        }
    }

    public ByteArrayInputStream exportToExcel() {
        List<ProFinishedGoodResponse> goods = getAllFinishedGoods();
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Finished Goods");
            Row header = sheet.createRow(0);
            String[] headers = { "Code", "Name", "Category", "Sub Category", "Barcode", "Sales Price",
                    "Purchase Price" };
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            int rowIdx = 1;
            for (ProFinishedGoodResponse good : goods) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(good.getItemCode());
                row.createCell(1).setCellValue(good.getName());
                row.createCell(2).setCellValue(good.getCategoryName());
                row.createCell(3).setCellValue(good.getSubCategoryName());
                row.createCell(4).setCellValue(good.getBarcode());
                row.createCell(5).setCellValue(good.getSalesPrice() != null ? good.getSalesPrice().doubleValue() : 0.0);
                row.createCell(6)
                        .setCellValue(good.getPurchasePrice() != null ? good.getPurchasePrice().doubleValue() : 0.0);
            }
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to export excel", e);
        }
    }

    @Transactional
    public void importFromExcel(MultipartFile file) {
        Long tenantId = getCurrentTenantId();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0)
                    continue; // Skip header

                try {
                    String itemCode = getCellValue(row.getCell(0));
                    String name = getCellValue(row.getCell(1));

                    if (itemCode == null || name == null)
                        continue;

                    // Simple logic: Create if not exists
                    if (!proFinishedGoodRepository.existsByTenantIdAndItemCodeIgnoreCase(tenantId, itemCode)) {
                        ProFinishedGood entity = new ProFinishedGood();
                        Tenant tenant = new Tenant();
                        tenant.setId(tenantId);
                        entity.setTenant(tenant);

                        entity.setItemCode(itemCode);
                        entity.setName(name);
                        entity.setDescription(getCellValue(row.getCell(6)));

                        // Parse numbers safely
                        try {
                            entity.setSalesPrice(new java.math.BigDecimal(getCellValue(row.getCell(4))));
                        } catch (Exception e) {
                        }
                        try {
                            entity.setPurchasePrice(new java.math.BigDecimal(getCellValue(row.getCell(5))));
                        } catch (Exception e) {
                        }

                        // Auto-gen barcode
                        String bc = generateAutoBarcode(itemCode);
                        entity.setBarcode(bc);
                        entity.setBarcodeImgUrl(generateAndSaveBarcodeImage(bc, name));

                        proFinishedGoodRepository.save(entity);
                    }
                } catch (Exception e) {
                    // Log error for this row and continue
                    System.err.println("Error importing row " + row.getRowNum() + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse excel file", e);
        }
    }

    private Long getCurrentTenantId() {
        String tenantIdentifier = TenantContext.getTenantId();
        try {
            return Long.parseLong(tenantIdentifier);
        } catch (NumberFormatException e) {
            return getTenantByIdentifier(tenantIdentifier).getId();
        }
    }

    private Tenant getTenantByIdentifier(String identifier) {
        return tenantRepository.findByTenantId(identifier)
                .or(() -> tenantRepository.findByName(identifier))
                .orElseThrow(() -> new RuntimeException("Tenant not found for identifier: " + identifier));
    }

    public org.springframework.core.io.Resource loadFile(String path) {
        return fileStorageService.loadFileAsResource(path, true);
    }

    private String getCellValue(Cell cell) {
        if (cell == null)
            return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}
