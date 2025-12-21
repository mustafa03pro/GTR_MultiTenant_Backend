package com.example.multi_tanent.sales.entity;

import com.example.multi_tanent.crm.entity.CrmSalesProduct;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rental_item_recieved_items")
public class RentalItemRecievedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "rental_item_recieved_id", nullable = false)
    private RentalItemRecieved rentalItemRecieved;

    @ManyToOne
    @JoinColumn(name = "crm_product_id")
    private CrmSalesProduct crmProduct;

    @Column(name = "item_name")
    private String itemName;

    @Column(name = "item_code")
    private String itemCode;

    @Column(name = "description")
    private String description;

    @Column(name = "do_quantity")
    private Integer doQuantity;

    @Column(name = "received_quantity")
    private Integer receivedQuantity;

    @Column(name = "current_receive_quantity")
    private Integer currentReceiveQuantity;

    @Column(name = "remaining_quantity")
    private Integer remainingQuantity;
}
