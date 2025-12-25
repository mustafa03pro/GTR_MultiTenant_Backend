package com.example.multi_tanent.production.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BomFinishedGoodRequest {
    private Long itemId; // ProFinishedGood ID
    private String bomName;
    private BigDecimal quantity;
    private Long routingId; // ProcessFinishedGood ID
    private BigDecimal approximateCost;
    private boolean isActive;

    private List<BomFinishedGoodDetailRequest> details;
}
