package com.example.multi_tanent.sales.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DeliveryOrderItemResponse {
    private Long id;
    private Long crmProductId;
    private Long categoryId;
    private String categoryName;
    private Long subcategoryId;
    private String subcategoryName;
    private String itemCode;
    private String itemName;
    private Integer quantity;
    private BigDecimal rate;
    private BigDecimal amount;
    private BigDecimal taxValue;
    private Double taxPercentage;
    private boolean taxExempt;
}
