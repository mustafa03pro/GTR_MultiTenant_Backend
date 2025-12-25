package com.example.multi_tanent.production.dto;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class ProductionScheduleRequest {
    private Long manufacturingOrderId;
    private Long workGroupId;
    private Long employeeId;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private String status;
    private String notes;
}
