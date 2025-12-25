package com.example.multi_tanent.production.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BomFinishedGoodDetailResponse {
    private Long id;
    private ProProcessResponse process;

    private ProRawMaterialsResponse rawMaterial;
    private ProSemiFinishedResponse semiFinished;

    private BigDecimal quantity;
    private ProUnitResponse uom;
    private BigDecimal rate;
    private BigDecimal amount;
    private String notes;
    private Integer sequence;
}
