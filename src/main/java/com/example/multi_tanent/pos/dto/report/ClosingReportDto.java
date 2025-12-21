package com.example.multi_tanent.pos.dto.report;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClosingReportDto {
    private Long id;
    private String openingDate;
    private BigDecimal openingFloat;
    private String runningDate; // Format: YYYY-MM-DD
    private String closingDate;
    private BigDecimal expectedCashAmount;
    private BigDecimal countedCashAmount;
    private BigDecimal closedCashDifference;
    private String notes;
}
