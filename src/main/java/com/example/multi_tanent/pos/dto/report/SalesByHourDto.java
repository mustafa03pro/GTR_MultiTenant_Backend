package com.example.multi_tanent.pos.dto.report;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesByHourDto {
    private String hour; // e.g. "09:00"
    private Long quantity; // Total quantity of items sold in this hour
    private Long salesCount; // Number of transactions (invoices)
    private BigDecimal amount; // Total sales amount
}
