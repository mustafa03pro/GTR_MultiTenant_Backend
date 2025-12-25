package com.example.multi_tanent.production.repository;

import com.example.multi_tanent.production.entity.BomFinishedGoodDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BomFinishedGoodDetailRepository extends JpaRepository<BomFinishedGoodDetail, Long> {
    List<BomFinishedGoodDetail> findByTenantIdAndBomFinishedGoodId(Long tenantId, Long bomFinishedGoodId);
}
