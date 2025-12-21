package com.example.multi_tanent.sales.repository;

import com.example.multi_tanent.sales.entity.CreditNotes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreditNotesRepository extends JpaRepository<CreditNotes, Long> {
    List<CreditNotes> findByTenant_Id(Long tenantId);
}
