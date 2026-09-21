package com.lealtixservice.repository;

import com.lealtixservice.entity.ComandaAsientoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComandaAsientoItemRepository extends JpaRepository<ComandaAsientoItem, UUID> {

    /**
     * Ítems asignados a un asiento.
     */
    List<ComandaAsientoItem> findBySeatId(UUID seatId);

    /**
     * Asignación de un ítem de la comanda (para evitar doble asignación).
     */
    Optional<ComandaAsientoItem> findByItemId(UUID itemId);

    /**
     * Eliminar todas las asignaciones de un asiento.
     */
    void deleteBySeatId(UUID seatId);
}