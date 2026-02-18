package com.lealtixservice.repository;

import com.lealtixservice.entity.ClientOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClientOrderItemRepository extends JpaRepository<ClientOrderItem, UUID> {

    /**
     * Buscar todos los items de una orden
     */
    List<ClientOrderItem> findByOrderId(UUID orderId);

    /**
     * Eliminar todos los items de una orden
     */
    void deleteByOrderId(UUID orderId);

    /**
     * Buscar items por product_id (para análisis de productos más vendidos)
     */
    List<ClientOrderItem> findByProductId(Long productId);

    /**
     * Contar items en una orden
     */
    Long countByOrderId(UUID orderId);
}
