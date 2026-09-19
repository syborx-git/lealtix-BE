package com.lealtixservice.repository;

import com.lealtixservice.entity.ComandaPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ComandaPagoRepository extends JpaRepository<ComandaPago, UUID> {

    /**
     * Sub-comandas (pagos por asiento) de una comanda original.
     */
    List<ComandaPago> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
}