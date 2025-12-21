package com.example.multi_tanent.sales.entity;

import com.example.multi_tanent.spersusers.enitity.BaseCustomer;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import com.example.multi_tanent.spersusers.enitity.Location;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "recieved_amounts")
@EntityListeners(AuditingEntityListener.class)
public class Recieved {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "entry_type")
    private String entryType;

    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location location;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private BaseCustomer customer;

    @Column(name = "pi_number")
    private String piNumber;

    @Column(name = "manual_pi_number")
    private String manualPiNumber;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "received_full_amount")
    private Boolean isReceivedFullAmount;

    @Column(name = "tax_deducted")
    private Boolean isTaxDeducted;

    @Column(name = "deposit_date")
    private LocalDate depositDate;

    @Column(name = "deposit_mode")
    private String depositMode;

    @Column(name = "reference")
    private String reference;

    @Column(name = "cheque_number")
    private String chequeNumber;

    @Column(name = "receiving_date")
    private LocalDate receivingDate;

    @Column(name = "received_number")
    private String receivedNumber;

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Column(name = "tds")
    private BigDecimal tds;

    @Column(name = "advance_amount")
    private BigDecimal advanceAmount;

    @Column(name = "total_pi_amount")
    private BigDecimal totalPiAmount;

    @Column(name = "fbc")
    private BigDecimal fbc;

    @Column(name = "expected_in_fc")
    private BigDecimal expectedInFc;

    @Column(name = "bank_charges")
    private BigDecimal bankCharges;

    @Column(name = "fine_and_penalty")
    private BigDecimal fineAndPenalty;

    @Column(name = "rebate_and_discount")
    private BigDecimal rebateAndDiscount;

    @Column(name = "created_by")
    @CreatedBy
    private String createdBy;

    @Column(name = "updated_by")
    @LastModifiedBy
    private String updatedBy;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
