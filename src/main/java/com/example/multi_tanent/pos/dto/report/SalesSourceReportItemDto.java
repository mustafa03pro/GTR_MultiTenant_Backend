package com.example.multi_tanent.pos.dto.report;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SalesSourceReportItemDto {
    private String orderNo;
    private String reference;
    private String source;
    private String salesSourceReference;
    private String customerName;
    private LocalDateTime date;
    private BigDecimal salesTotalAmount;
}
