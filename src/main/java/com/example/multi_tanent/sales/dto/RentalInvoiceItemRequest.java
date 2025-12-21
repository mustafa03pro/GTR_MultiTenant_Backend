package com.example.multi_tanent.sales.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RentalInvoiceItemRequest {
    private Long id;
    private Long crmProductId;
    private String itemName;
    private String description;
    private Long categoryId;
    private Long subcategoryId;
    private Integer quantity;
    private Integer duration;
    private BigDecimal rentalValue;
    private BigDecimal amount;
    private BigDecimal taxValue;
    private boolean isTaxExempt;
    private BigDecimal taxPercentage;
}
