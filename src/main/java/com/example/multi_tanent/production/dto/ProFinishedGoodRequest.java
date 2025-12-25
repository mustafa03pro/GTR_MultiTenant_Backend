package com.example.multi_tanent.production.dto;

import com.example.multi_tanent.production.enums.ItemType;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Data
public class ProFinishedGoodRequest {
    private Long id;
    private Long locationId;

    // Basic Details
    private String name;
    private String itemCode;
    private String barcode;
    private String hsnSacCode;
    private String description;

    // Enums & Flags
    private ItemType itemType;
    private boolean forPurchase;
    private boolean forSales;
    private boolean isTaxInclusive;

    // Relationships (IDs)
    private Long categoryId;
    private Long subCategoryId;
    private Long issueUnitId;
    private Long purchaseUnitId;
    private Long taxId;

    // Inventory & Pricing
    private BigDecimal unitRelation;
    private BigDecimal tolerancePercentage;
    private BigDecimal reorderLimit;
    private BigDecimal purchasePrice;
    private BigDecimal salesPrice;

    // File Upload
    private MultipartFile imageFile;
}
