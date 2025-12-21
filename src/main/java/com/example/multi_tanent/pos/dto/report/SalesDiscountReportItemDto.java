package com.example.multi_tanent.pos.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesDiscountReportItemDto {
    private String branch;
    private String discountType;
    private Long count; // Number of orders with this discount (aggregated) or 1 (detail)
    private BigDecimal orderAmountAfterDiscount;
    private BigDecimal discountAmount;
    private LocalDate orderDate;
    private String orderHour;
}
