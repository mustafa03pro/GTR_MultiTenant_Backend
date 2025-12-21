package com.example.multi_tanent.pos.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.multi_tanent.spersusers.enitity.Store;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import com.example.multi_tanent.spersusers.enitity.User;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sales")
public class Sale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne
    @JoinColumn(name = "store_id")
    private Store store;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(nullable = false, unique = true)
    private String orderId;

    @Column(nullable = false, unique = true)
    private String invoiceNo;

    private OffsetDateTime invoiceDate;

    private Long subtotalCents;

    private Long taxCents;

    private Long discountCents = 0L;

    private String discountType; // e.g., "Seasonal", "Coupon", "Test"

    @Column(columnDefinition = "TEXT")
    private String discountReason;

    @Column(columnDefinition = "TEXT")
    private String cancellationReason;

    private OffsetDateTime cancelledTime;

    private Long deliveryCharge;

    private Long totalCents;

    @Column(nullable = false)
    private String status = "completed"; // pending, completed, refunded

    @Column(nullable = false)
    private String paymentStatus = "unpaid"; // unpaid, paid, partial

    @Enumerated(EnumType.STRING)
    private com.example.multi_tanent.pos.enums.OrderType orderType;

    private Integer adultsCount;

    private Integer kidsCount;

    private String salesSource; // e.g., "POS", "Online"

    private String salesSourceReference; // e.g. referral name, ad ID

    private String carNumber;

    private OffsetDateTime deliveredTime;

    @ManyToOne
    @JoinColumn(name = "driver_id")
    private User driver;

    private String deliveryAddress;

    private OffsetDateTime dispatchedTime;

    private OffsetDateTime expectedDeliveryTime;

    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payment> payments = new ArrayList<>();

    @OneToMany(mappedBy = "relatedSale")
    private List<StockMovement> stockMovements = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (createdAt == null)
            createdAt = OffsetDateTime.now();
        if (invoiceDate == null)
            invoiceDate = OffsetDateTime.now();
    }
}