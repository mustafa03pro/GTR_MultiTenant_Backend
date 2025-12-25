package com.example.multi_tanent.production.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessFinishedGoodDetailResponse {
    private Long id;
    private Long processId;
    private String processName;
    private Long workGroupId;
    private String workGroupName;
    private Integer setupTime;
    private Integer cycleTime;
    private BigDecimal fixedCost;
    private BigDecimal variableCost;
    private boolean isOutsource;
    private boolean isTesting;
    private String notes;
    private Integer sequence;

    public static ProcessFinishedGoodDetailResponse fromEntity(
            com.example.multi_tanent.production.entity.ProcessFinishedGoodDetail entity) {
        return ProcessFinishedGoodDetailResponse.builder()
                .id(entity.getId())
                .processId(entity.getProcess() != null ? entity.getProcess().getId() : null)
                .processName(entity.getProcess() != null ? entity.getProcess().getName() : null)
                .workGroupId(entity.getWorkGroup() != null ? entity.getWorkGroup().getId() : null)
                .workGroupName(entity.getWorkGroup() != null ? entity.getWorkGroup().getName() : null)
                .setupTime(entity.getSetupTime())
                .cycleTime(entity.getCycleTime())
                .fixedCost(entity.getFixedCost())
                .variableCost(entity.getVariableCost())
                .isOutsource(entity.isOutsource())
                .isTesting(entity.isTesting())
                .notes(entity.getNotes())
                .sequence(entity.getSequence())
                .build();
    }
}
