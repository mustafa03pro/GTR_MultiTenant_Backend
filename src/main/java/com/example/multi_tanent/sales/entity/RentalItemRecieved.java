package com.example.multi_tanent.sales.entity;

import com.example.multi_tanent.sales.enums.SalesStatus;
import com.example.multi_tanent.spersusers.enitity.BaseCustomer;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rental_item_recieved")
@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
public class RentalItemRecieved {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "do_date")
    private LocalDate doDate;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private BaseCustomer customer;

    @Column(name = "billing_address", length = 1000)
    private String billingAddress;

    @Column(name = "shipping_address", length = 1000)
    private String shippingAddress;

    @Column(name = "do_number")
    private String doNumber;

    @Column(name = "order_number")
    private String orderNumber;

    @ManyToOne
    @JoinColumn(name = "rental_sales_order_id")
    private RentalSalesOrder rentalSalesOrder;

    @OneToMany(mappedBy = "rentalItemRecieved", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RentalItemRecievedItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private SalesStatus status; // e.g., PENDING, FULLY_RECEIVED

    @Column(name = "created_by")
    @org.springframework.data.annotation.CreatedBy
    private String createdBy;

    @Column(name = "updated_by")
    @org.springframework.data.annotation.LastModifiedBy
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
