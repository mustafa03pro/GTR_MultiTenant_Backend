package com.example.multi_tanent.crm.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CrmLeadStageResponse {
    private Long id;
    private String name;
    private Integer sortOrder;
    private boolean isDefault;

    private Long locationId;
    private String locationName;

    private Long tenantId;

    private String moveTo;
}