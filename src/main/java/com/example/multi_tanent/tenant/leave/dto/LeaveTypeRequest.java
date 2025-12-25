package com.example.multi_tanent.tenant.leave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalTime;

@Data
public class LeaveTypeRequest {
    @NotBlank(message = "Leave type name is required")
    private String leaveType;
    private String description;
    @NotNull(message = "isPaid field is required")
    private Boolean isPaid;

    private Integer maxDaysPerYear;
    private LocalTime startTime;
    private LocalTime endTime;
}