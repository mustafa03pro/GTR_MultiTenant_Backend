package com.example.multi_tanent.sales.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RentalInvoiceItemResponse {
    private Long id;
    private Long crmProductId;
    private String itemCode;
    private String itemName;
    private String description;
    private Long categoryId;
    private String categoryName;
    private Long subcategoryId;
    private String subcategoryName;
    private Integer quantity;
    private Integer duration;
    private BigDecimal rentalValue;
    private BigDecimal amount;
    private BigDecimal taxValue;
    private boolean isTaxExempt;
    private BigDecimal taxPercentage;
}
