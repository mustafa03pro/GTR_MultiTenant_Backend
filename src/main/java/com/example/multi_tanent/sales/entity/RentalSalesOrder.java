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
@Table(name = "rental_sales_orders")
@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
public class RentalSalesOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "order_date")
    private LocalDate orderDate;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private BaseCustomer customer;

    @Column(name = "order_number", unique = true)
    private String orderNumber;

    private String reference;

    @Column(name = "shipment_date")
    private LocalDate shipmentDate;

    @Column(name = "from_date")
    private LocalDate fromDate;

    @Column(name = "to_date")
    private LocalDate toDate;

    @Column(name = "delivery_lead")
    private String deliveryLead;

    private String validity;

    @Column(name = "payment_terms")
    private String paymentTerms;

    @Column(name = "price_basis")
    private String priceBasis;

    @Column(name = "dear_sir")
    private String dearSir;

    @ManyToOne
    @JoinColumn(name = "salesperson_id")
    private Employee salesperson;

    @OneToMany(mappedBy = "rentalSalesOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RentalSalesOrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "rentalSalesOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RentalItemRecieved> rentalItemsReceived = new ArrayList<>();

    @Column(name = "rental_duration_days")
    private Integer rentalDurationDays;

    @Column(name = "sub_total_per_day")
    private BigDecimal subTotalPerDay;

    @Column(name = "total_discount")
    private BigDecimal totalDiscount;

    @Column(name = "gross_total")
    private BigDecimal grossTotal;

    @Column(name = "total_rental_price")
    private BigDecimal totalRentalPrice;

    @Column(name = "total_tax")
    private BigDecimal totalTax;

    @Column(name = "other_charges")
    private BigDecimal otherCharges;

    @Column(name = "net_total")
    private BigDecimal netTotal;

    @Lob
    @Column(name = "terms_and_conditions")
    private String termsAndConditions;

    @Lob
    private String notes;

    @Lob
    private String manufacture;

    @Lob
    private String remarks;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rental_sales_order_attachments", joinColumns = @JoinColumn(name = "rental_sales_order_id"))
    @Column(name = "attachment_url")
    private List<String> attachments = new ArrayList<>();

    @Column(name = "template")
    private String template;

    @Column(name = "email_to")
    private String emailTo;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private SalesStatus status;

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
