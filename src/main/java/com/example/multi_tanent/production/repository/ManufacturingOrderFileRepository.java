package com.example.multi_tanent.production.repository;

import com.example.multi_tanent.production.entity.ManufacturingOrderFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ManufacturingOrderFileRepository extends JpaRepository<ManufacturingOrderFile, Long> {
    Optional<ManufacturingOrderFile> findByTenantIdAndId(Long tenantId, Long id);
}
