package com.example.multi_tanent.production.entity;

import com.example.multi_tanent.production.enums.ManufacturingOrderStatus;
import com.example.multi_tanent.sales.entity.SalesOrder;
import com.example.multi_tanent.spersusers.enitity.BaseCustomer;
import com.example.multi_tanent.spersusers.enitity.Employee;
import com.example.multi_tanent.spersusers.enitity.Location;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "manufacturing_orders")
public class ManufacturingOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location productionHouse;

    @Column(name = "mo_number", nullable = false, unique = true)
    private String moNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_id")
    private SalesOrder salesOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private BaseCustomer customer;

    @Column(name = "reference_no")
    private String referenceNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private ProSemifinished item;

    @Column(name = "quantity", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    @Builder.Default
    private ManufacturingOrderStatus status = ManufacturingOrderStatus.SCHEDULED;

    @Column(name = "schedule_start")
    private OffsetDateTime scheduleStart;

    @Column(name = "schedule_finish")
    private OffsetDateTime scheduleFinish;

    @Column(name = "due_date")
    private OffsetDateTime dueDate;

    @Column(name = "actual_start")
    private OffsetDateTime actualStart;

    @Column(name = "actual_finish")
    private OffsetDateTime actualFinish;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assign_to_id")
    private Employee assignTo;

    @Column(name = "batch_no")
    private String batchNo;

    @Column(name = "sampling_request_status")
    private String samplingRequestStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bom_id")
    private BomSemiFinished bom;

    @OneToMany(mappedBy = "manufacturingOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ManufacturingOrderFile> files = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
