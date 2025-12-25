package com.example.multi_tanent.production.entity;

import com.example.multi_tanent.spersusers.enitity.Location;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "process_finished_good_details")
public class ProcessFinishedGoodDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "process_finished_good_id", nullable = false)
    private ProcessFinishedGood processFinishedGood;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_id")
    private ProProcess process;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_group_id")
    private ProWorkGroup workGroup;

    @Column(name = "setup_time")
    private Integer setupTime;

    @Column(name = "cycle_time")
    private Integer cycleTime;

    @Column(name = "fixed_cost", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal fixedCost = BigDecimal.ZERO;

    @Column(name = "variable_cost", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal variableCost = BigDecimal.ZERO;

    @Column(name = "is_outsource")
    @Builder.Default
    private boolean isOutsource = false;

    @Column(name = "is_testing")
    @Builder.Default
    private boolean isTesting = false;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "sequence", nullable = false)
    private Integer sequence;
}
