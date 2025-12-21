package com.example.multi_tanent.pos.entity;

import com.example.multi_tanent.spersusers.enitity.Store;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "inventory", uniqueConstraints = @UniqueConstraint(columnNames = { "store_id", "product_variant_id" }))
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @OneToOne(optional = false)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @Column(nullable = false)
    private Long quantity = 0L;

    // Could add fields like `reorderLevel`, `lastRestockedAt` etc. in the future
}