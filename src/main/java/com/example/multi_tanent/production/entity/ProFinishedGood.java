package com.example.multi_tanent.production.entity;

import com.example.multi_tanent.production.enums.InventoryType;
import com.example.multi_tanent.production.enums.ItemType;
import com.example.multi_tanent.spersusers.enitity.Location;
import com.example.multi_tanent.spersusers.enitity.Tenant;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "pro_finished_goods", uniqueConstraints = {
        @UniqueConstraint(name = "uk_finished_good_tenant_item_code", columnNames = { "tenant_id", "item_code" })
})
public class ProFinishedGood {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    /* enums */
    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_type", length = 50)
    @Builder.Default
    private InventoryType inventoryType = InventoryType.FINISHED_GOOD;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", length = 20, nullable = false)
    @Builder.Default
    private ItemType itemType = ItemType.PRODUCT;

    /* flags */
    @Column(name = "for_purchase")
    @Builder.Default
    private boolean forPurchase = true;

    @Column(name = "for_sales")
    @Builder.Default
    private boolean forSales = true;

    @Column(name = "is_tax_inclusive")
    @Builder.Default
    private boolean isTaxInclusive = false;

    /* relationships */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ProCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_category_id")
    private ProSubCategory subCategory;

    /* identifiers */
    @NotBlank
    @Column(name = "item_code", nullable = false, length = 100)
    private String itemCode;

    @NotBlank
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "barcode", length = 128)
    private String barcode;

    @Column(name = "barcode_img_url")
    private String barcodeImgUrl;

    @Column(name = "hsn_sac_code", length = 50)
    private String hsnSacCode;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "picture_path", length = 512)
    private String picturePath;

    /* units & conversion */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_unit_id")
    private ProUnit issueUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_unit_id")
    private ProUnit purchaseUnit;

    @PositiveOrZero
    @Column(name = "unit_relation", precision = 12, scale = 4)
    @Builder.Default
    private BigDecimal unitRelation = BigDecimal.ONE;

    /* inventory & pricing */
    @PositiveOrZero
    @Column(name = "tolerance_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal tolerancePercentage = BigDecimal.ZERO;

    @Column(name = "reorder_limit", precision = 12, scale = 4)
    @Builder.Default
    private BigDecimal reorderLimit = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_id")
    private ProTax tax;

    @Column(name = "purchase_price", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    @Column(name = "sales_price", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal salesPrice = BigDecimal.ZERO;

    // Note: Parameter values and Price lists would typically be separate entities
    // (e.g., OneToMany ProFinishedGoodParameterValue, OneToMany
    // ProFinishedGoodPrice)
    // linked to this entity.
}
