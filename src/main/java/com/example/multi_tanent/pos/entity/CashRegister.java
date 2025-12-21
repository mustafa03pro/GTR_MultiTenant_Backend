package com.example.multi_tanent.pos.entity;

import com.example.multi_tanent.spersusers.enitity.Tenant;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "cash_registers")
public class CashRegister {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    private OffsetDateTime openingTime;
    private OffsetDateTime closingTime;

    private BigDecimal openingFloat;
    private BigDecimal expectedCashAmount; // Calculated from Sales
    private BigDecimal countedCashAmount; // Manually entered
    private BigDecimal closedCashDifference; // Counted - Expected
    private String notes;

    private String status; // "OPEN", "CLOSED"
}
