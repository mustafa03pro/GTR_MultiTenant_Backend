package com.example.multi_tanent.sales.repository;

import com.example.multi_tanent.sales.entity.Recieved;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecievedRepository extends JpaRepository<Recieved, Long> {
    List<Recieved> findByTenant_Id(Long tenantId);
}
