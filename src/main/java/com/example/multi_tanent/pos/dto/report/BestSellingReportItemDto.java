package com.example.multi_tanent.pos.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BestSellingReportItemDto {
    private String stockId;
    private String itemName;
    private Long quantity;
    private BigDecimal totalPrice;
    private Long salesCount;
    private Long dateDifference;
}
