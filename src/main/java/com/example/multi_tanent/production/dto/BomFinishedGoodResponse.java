package com.example.multi_tanent.production.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
public class BomFinishedGoodResponse {
    private Long id;
    private ProFinishedGoodResponse item;
    private String bomName;
    private BigDecimal quantity;
    private ProcessFinishedGoodResponse routing;
    private BigDecimal approximateCost;
    private boolean isActive;

    private List<BomFinishedGoodDetailResponse> details;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
