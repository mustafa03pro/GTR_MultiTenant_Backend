package com.example.multi_tanent.spersusers.repository;

import com.example.multi_tanent.spersusers.enitity.BaseCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BaseCustomerRepository extends JpaRepository<BaseCustomer, Long> {
    List<BaseCustomer> findByTenant_Id(Long tenantId);
}
