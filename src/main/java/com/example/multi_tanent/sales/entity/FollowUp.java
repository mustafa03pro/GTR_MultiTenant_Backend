package com.example.multi_tanent.sales.entity;

import com.example.multi_tanent.sales.enums.SalesStatus;
import com.example.multi_tanent.sales.entity.RentalQuotation;
import com.example.multi_tanent.spersusers.enitity.Employee;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rental_quotation_followups")
@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
public class FollowUp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(optional = true)
    @JoinColumn(name = "quotation_id")
    private Quotation quotation;

    @ManyToOne
    @JoinColumn(name = "rental_quotation_id")
    private RentalQuotation rentalQuotation;

    @Column(name = "next_followup_date")
    private LocalDate nextFollowupDate;

    @Column(name = "next_followup_time")
    private LocalTime nextFollowupTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "quotation_status")
    private SalesStatus quotationStatus;

    @Lob
    @Column(name = "comment")
    private String comment;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(name = "created_by")
    @org.springframework.data.annotation.CreatedBy
    private String createdBy;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

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
