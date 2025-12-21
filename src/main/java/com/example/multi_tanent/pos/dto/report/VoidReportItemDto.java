package com.example.multi_tanent.pos.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoidReportItemDto {
    private String code;
    private String orderType;
    private String pos;
    private String orderTakenBy;
    private String name;
    private Long quantity;
    private BigDecimal amount;
}
