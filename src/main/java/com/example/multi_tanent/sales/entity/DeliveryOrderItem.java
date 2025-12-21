package com.example.multi_tanent.sales.entity;

import com.example.multi_tanent.crm.entity.CrmSalesProduct;
import com.example.multi_tanent.production.entity.ProCategory;
import com.example.multi_tanent.production.entity.ProSubCategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "delivery_order_items")
public class DeliveryOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "delivery_order_id", nullable = false)
    private DeliveryOrder deliveryOrder;

    @ManyToOne
    @JoinColumn(name = "crm_product_id")
    private CrmSalesProduct crmProduct;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private ProCategory category;

    @ManyToOne
    @JoinColumn(name = "subcategory_id")
    private ProSubCategory subcategory;

    private String itemCode;
    private String itemName;

    private Integer quantity;
    private BigDecimal rate;
    private BigDecimal amount;
    private BigDecimal taxValue;
    private Double taxPercentage;
    private boolean isTaxExempt;
}
