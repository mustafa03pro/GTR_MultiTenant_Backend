package com.example.multi_tanent.production.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.example.multi_tanent.production.entity.ProcessFinishedGood;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessFinishedGoodResponse {
    private Long id;
    private Long itemId;
    private String itemName;
    private String processFlowName;
    private BigDecimal otherFixedCost;
    private BigDecimal otherVariableCost;
    private boolean isLocked;
    private List<ProcessFinishedGoodDetailResponse> details;
    private Long locationId;
    private String locationName;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static ProcessFinishedGoodResponse fromEntity(ProcessFinishedGood entity) {
        return ProcessFinishedGoodResponse.builder()
                .id(entity.getId())
                .itemId(entity.getFinishedGood() != null ? entity.getFinishedGood().getId() : null)
                .itemName(entity.getFinishedGood() != null ? entity.getFinishedGood().getName() : null)
                .processFlowName(entity.getProcessFlowName())
                .otherFixedCost(entity.getOtherFixedCost())
                .otherVariableCost(entity.getOtherVariableCost())
                .isLocked(entity.isLocked())
                .details(entity.getDetails() != null ? entity.getDetails().stream()
                        .map(ProcessFinishedGoodDetailResponse::fromEntity)
                        .collect(Collectors.toList()) : null)
                .locationId(entity.getLocation() != null ? entity.getLocation().getId() : null)
                .locationName(entity.getLocation() != null ? entity.getLocation().getName() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
