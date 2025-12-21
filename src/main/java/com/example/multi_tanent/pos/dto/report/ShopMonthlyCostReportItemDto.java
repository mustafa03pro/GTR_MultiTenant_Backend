package com.example.multi_tanent.pos.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopMonthlyCostReportItemDto {
    private Long productId;
    private String productName;
    private String uom; // "PCS" default
    private BigDecimal unitCost; // Average or latest unit cost

    // Map of Day (1-31) to Total Cost for that day
    private Map<Integer, BigDecimal> dailyCosts;

    private BigDecimal totalCost;
}
