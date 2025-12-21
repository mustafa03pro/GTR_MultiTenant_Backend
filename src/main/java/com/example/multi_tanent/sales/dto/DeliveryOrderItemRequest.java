package com.example.multi_tanent.sales.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DeliveryOrderItemRequest {
    private Long crmProductId;
    private Long categoryId;
    private Long subcategoryId;
    private String itemCode;
    private String itemName;
    private Integer quantity;
    private BigDecimal rate;
    private BigDecimal taxValue;
    private Double taxPercentage;
    private boolean taxExempt;
}
