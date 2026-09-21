package com.lealtixservice.repository;

import com.lealtixservice.entity.Merma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MermaRepository extends JpaRepository<Merma, Long> {

    List<Merma> findByTenantIdOrderByFechaDesc(Long tenantId);

    List<Merma> findByOrderIdOrderByFechaDesc(UUID orderId);
}