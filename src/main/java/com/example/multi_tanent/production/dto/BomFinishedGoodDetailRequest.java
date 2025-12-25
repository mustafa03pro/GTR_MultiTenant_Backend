package com.example.multi_tanent.production.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BomFinishedGoodDetailRequest {
    private Long processId;

    // One of these should be set
    private Long rawMaterialId;
    private Long semiFinishedId;

    private BigDecimal quantity;
    private Long uomId;
    private BigDecimal rate;
    private BigDecimal amount;
    private String notes;
    private Integer sequence;
}
