
// package com.example.multi_tanent.purchases.repository;

// import org.springframework.data.jpa.repository.JpaRepository;
// import com.example.multi_tanent.purchases.entity.PurGrnItem;

// public interface PurGrnItemRepository extends JpaRepository<PurGrnItem, Long> {
//     // custom queries if needed
// }

// package com.example.multi_tanent.purchases.repository;

// import org.springframework.data.jpa.repository.JpaRepository;
// import com.example.multi_tanent.purchases.entity.PurGrnItem;

// public interface PurGrnItemRepository extends JpaRepository<PurGrnItem, Long> {
//     // add queries if needed
// }

// src/main/java/com/example/multi_tanent/purchases/repository/PurGrnItemRepository.java
package com.example.multi_tanent.purchases.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.multi_tanent.purchases.entity.PurGrnItem;

public interface PurGrnItemRepository extends JpaRepository<PurGrnItem, Long> {
    boolean existsByPurchaseOrderItemIdIn(java.util.Collection<Long> purchaseOrderItemIds);
}
