package com.example.multi_tanent.pos.repository;

import com.example.multi_tanent.pos.entity.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {
    Optional<SaleItem> findByIdAndSaleId(Long itemId, Long saleId);

    boolean existsByProductVariantId(Long productVariantId);

    java.util.List<SaleItem> findByProductVariantId(Long productVariantId);
}