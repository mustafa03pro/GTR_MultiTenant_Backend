package com.example.multi_tanent.pos.entity;

import com.example.multi_tanent.spersusers.enitity.Tenant;
import com.example.multi_tanent.spersusers.enitity.User;
import com.example.multi_tanent.spersusers.enitity.Store;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payment_change_logs")
public class PaymentChangeLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne
    @JoinColumn(name = "store_id")
    private Store store;

    @Column(nullable = false)
    private Long saleId;

    @Column(nullable = false)
    private Long paymentId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String cardAssignedUser; // Optional, if relevant for card payments

    private String fromMethod; // "From Account"
    private String toMethod;   // "To Account"

    private String changeReason;

    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
