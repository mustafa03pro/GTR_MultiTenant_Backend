package com.example.multi_tanent.sales.entity;

import com.example.multi_tanent.sales.enums.SalesStatus;
import com.example.multi_tanent.spersusers.enitity.BaseCustomer;
import com.example.multi_tanent.spersusers.enitity.Employee;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sales_invoices")
@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
public class SalesInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "invoice_ledger")
    private String invoiceLedger;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private BaseCustomer customer;

    @Column(name = "number", unique = true)
    private String invoiceNumber;

    @Column(name = "order_number")
    private String orderNumber;

    @Column(name = "reference")
    private String reference;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "date_of_supply")
    private LocalDate dateOfSupply;

    @ManyToOne
    @JoinColumn(name = "salesperson_id")
    private Employee salesperson;

    @Column(name = "enable_gross_net_weight")
    private Boolean enableGrossNetWeight;

    @Column(name = "delay_reason")
    private String delayReason;

    @OneToMany(mappedBy = "salesInvoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<SalesInvoiceItem> items = new ArrayList<>();

    @Column(name = "sub_total")
    private BigDecimal subTotal;

    @Column(name = "total_discount")
    private BigDecimal totalDiscount;

    @Column(name = "gross_total")
    private BigDecimal grossTotal;

    @Column(name = "total_tax")
    private BigDecimal totalTax;

    @Column(name = "other_charges")
    private BigDecimal otherCharges;

    @Column(name = "net_total")
    private BigDecimal netTotal;

    @Column(name = "amount_received")
    private BigDecimal amountReceived;

    @Column(name = "balance_due")
    private BigDecimal balanceDue;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private SalesStatus status;

    @Lob
    @Column(name = "terms_and_conditions")
    private String termsAndConditions;

    @Lob
    private String notes;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "sales_invoice_attachments", joinColumns = @JoinColumn(name = "sales_invoice_id"))
    @Column(name = "attachment_url")
    private List<String> attachments = new ArrayList<>();

    @Column(name = "template")
    private String template;

    @Column(name = "email_to")
    private String emailTo;

    @Column(name = "created_by")
    @org.springframework.data.annotation.CreatedBy
    private String createdBy;

    @Column(name = "updated_by")
    @org.springframework.data.annotation.LastModifiedBy
    private String updatedBy;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null)
            createdAt = OffsetDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
