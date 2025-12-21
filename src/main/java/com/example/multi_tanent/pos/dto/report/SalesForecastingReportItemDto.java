package com.example.multi_tanent.pos.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesForecastingReportItemDto {
    private String monthYear;
    private BigDecimal actualSales;
    private BigDecimal forecastedSales;
    private BigDecimal achievedPercentage;
}
