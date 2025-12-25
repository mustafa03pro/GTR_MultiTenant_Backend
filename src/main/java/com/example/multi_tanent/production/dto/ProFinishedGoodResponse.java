package com.example.multi_tanent.production.dto;

import com.example.multi_tanent.production.entity.ProFinishedGood;
import com.example.multi_tanent.production.enums.InventoryType;
import com.example.multi_tanent.production.enums.ItemType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProFinishedGoodResponse {
    private Long id;
    private String tenantId; // Just ID/Name if needed
    private Long locationId;
    private String locationName;

    private InventoryType inventoryType;
    private ItemType itemType;

    private boolean forPurchase;
    private boolean forSales;
    private boolean isTaxInclusive;

    private Long categoryId;
    private String categoryName;

    private Long subCategoryId;
    private String subCategoryName;

    private String itemCode;
    private String name;
    private String barcode;
    private String barcodeImgUrl;
    private String hsnSacCode;
    private String description;
    private String picturePath;

    private Long issueUnitId;
    private String issueUnitName;

    private Long purchaseUnitId;
    private String purchaseUnitName;

    private BigDecimal unitRelation;
    private BigDecimal tolerancePercentage;
    private BigDecimal reorderLimit;

    private Long taxId;
    private String taxCode;
    private BigDecimal taxRate;

    private BigDecimal purchasePrice;
    private BigDecimal salesPrice;

    public static ProFinishedGoodResponse fromEntity(ProFinishedGood entity) {
        ProFinishedGoodResponse dto = new ProFinishedGoodResponse();
        dto.setId(entity.getId());
        // Tenant is usually handled by context, but for response might be useful
        // dto.setTenantId(entity.getTenant().getId());

        if (entity.getLocation() != null) {
            dto.setLocationId(entity.getLocation().getId());
            dto.setLocationName(entity.getLocation().getName());
        }

        dto.setInventoryType(entity.getInventoryType());
        dto.setItemType(entity.getItemType());
        dto.setForPurchase(entity.isForPurchase());
        dto.setForSales(entity.isForSales());
        dto.setTaxInclusive(entity.isTaxInclusive());

        if (entity.getCategory() != null) {
            dto.setCategoryId(entity.getCategory().getId());
            dto.setCategoryName(entity.getCategory().getName());
        }

        if (entity.getSubCategory() != null) {
            dto.setSubCategoryId(entity.getSubCategory().getId());
            dto.setSubCategoryName(entity.getSubCategory().getName());
        }

        dto.setItemCode(entity.getItemCode());
        dto.setName(entity.getName());
        dto.setBarcode(entity.getBarcode());
        dto.setBarcodeImgUrl(entity.getBarcodeImgUrl());
        dto.setHsnSacCode(entity.getHsnSacCode());
        dto.setDescription(entity.getDescription());
        dto.setPicturePath(entity.getPicturePath());

        if (entity.getIssueUnit() != null) {
            dto.setIssueUnitId(entity.getIssueUnit().getId());
            dto.setIssueUnitName(entity.getIssueUnit().getName());
        }

        if (entity.getPurchaseUnit() != null) {
            dto.setPurchaseUnitId(entity.getPurchaseUnit().getId());
            dto.setPurchaseUnitName(entity.getPurchaseUnit().getName());
        }

        dto.setUnitRelation(entity.getUnitRelation());
        dto.setTolerancePercentage(entity.getTolerancePercentage());
        dto.setReorderLimit(entity.getReorderLimit());

        if (entity.getTax() != null) {
            dto.setTaxId(entity.getTax().getId());
            dto.setTaxCode(entity.getTax().getCode());
            dto.setTaxRate(entity.getTax().getRate());
        }

        dto.setPurchasePrice(entity.getPurchasePrice());
        dto.setSalesPrice(entity.getSalesPrice());

        return dto;
    }
}
