package com.example.multi_tanent.pos.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemSalesReportItemDto {
    private String stockId;
    private String itemName;
    private Long quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalWithoutDiscount;
    private BigDecimal discountAmount;
    private BigDecimal totalWithDiscount;
}
