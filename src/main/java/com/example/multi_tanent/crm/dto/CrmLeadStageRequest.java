package com.example.multi_tanent.crm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CrmLeadStageRequest {
    @NotBlank(message = "Stage name is required")
    private String name;

    private Integer sortOrder;
    private boolean isDefault;

    private Long locationId; // Optional

    private String moveTo;
}