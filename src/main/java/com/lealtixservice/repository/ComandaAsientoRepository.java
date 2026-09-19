package com.lealtixservice.repository;

import com.lealtixservice.entity.ComandaAsiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComandaAsientoRepository extends JpaRepository<ComandaAsiento, UUID> {

    /**
     * Listar los asientos de una comanda ordenados por número de asiento.
     */
    List<ComandaAsiento> findByOrderIdOrderByNumeroAsc(UUID orderId);

    /**
     * Último asiento creado en una comanda (para auto-incrementar el número).
     */
    Optional<ComandaAsiento> findFirstByOrderIdOrderByNumeroDesc(UUID orderId);
}