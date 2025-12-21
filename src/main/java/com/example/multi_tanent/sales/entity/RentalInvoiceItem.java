package com.example.multi_tanent.sales.entity;

import com.example.multi_tanent.crm.entity.CrmSalesProduct;
import com.example.multi_tanent.production.entity.ProCategory;
import com.example.multi_tanent.production.entity.ProSubCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rental_invoice_items")
public class RentalInvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "rental_invoice_id", nullable = false)
    private RentalInvoice rentalInvoice;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private ProCategory category;

    @ManyToOne
    @JoinColumn(name = "subcategory_id")
    private ProSubCategory subcategory;

    @ManyToOne
    @JoinColumn(name = "crm_product_id")
    private CrmSalesProduct crmProduct; // "Type Here..." implies product selection

    @Column(name = "item_name")
    private String itemName;

    @Column(name = "description")
    private String description;

    private Integer quantity;

    private Integer duration; // Assuming days/hours based on context, likely days

    @Column(name = "rental_value")
    private BigDecimal rentalValue; // Rate

    private BigDecimal amount; // quantity * duration * rentalValue

    @Column(name = "tax_value")
    private BigDecimal taxValue;

    @Column(name = "is_tax_exempt")
    private boolean isTaxExempt;

    @Column(name = "tax_percentage")
    private BigDecimal taxPercentage;
}
