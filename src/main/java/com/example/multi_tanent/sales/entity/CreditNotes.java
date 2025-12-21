package com.example.multi_tanent.sales.entity;

import com.example.multi_tanent.sales.enums.CreditNoteStatus;
import com.example.multi_tanent.spersusers.enitity.BaseCustomer;
import com.example.multi_tanent.spersusers.enitity.Location;
import com.example.multi_tanent.spersusers.enitity.Tenant;
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
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "credit_notes")
@EntityListeners(AuditingEntityListener.class)
public class CreditNotes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location location;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private BaseCustomer customer;

    @Column(name = "credit_note_number")
    private String creditNoteNumber;

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Column(name = "credit_note_date")
    private LocalDate creditNoteDate;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "tax_percentage")
    private BigDecimal taxPercentage;

    @Lob
    @Column(name = "terms_and_conditions")
    private String termsAndConditions;

    @Lob
    @Column(name = "notes")
    private String notes;

    @Column(name = "template")
    private String template;

    @Column(name = "email_to")
    private String emailTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CreditNoteStatus status;

    @Column(name = "balance_due")
    private BigDecimal balanceDue;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "credit_note_attachments", joinColumns = @JoinColumn(name = "credit_note_id"))
    @Column(name = "attachment_url")
    private List<String> attachments = new ArrayList<>();

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
