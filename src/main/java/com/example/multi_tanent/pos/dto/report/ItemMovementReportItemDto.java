package com.example.multi_tanent.pos.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemMovementReportItemDto {
    private String stockId;
    private String itemName;
    private String description;
    private Double qtyOut;
    private BigDecimal cost;
    private LocalDate date;
}
