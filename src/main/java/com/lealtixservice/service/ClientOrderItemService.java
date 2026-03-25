package com.lealtixservice.service;

import com.lealtixservice.dto.ClientOrderItemDTO;
import com.lealtixservice.entity.ClientOrderItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface de servicio para gestionar items de órdenes
 */
public interface ClientOrderItemService {

    /**
     * Obtiene un item por su ID
     */
    Optional<ClientOrderItemDTO> getItemById(UUID itemId);

    /**
     * Obtiene todos los items de una orden
     */
    List<ClientOrderItemDTO> getItemsByOrder(UUID orderId);

    /**
     * Actualiza un item
     */
    ClientOrderItemDTO updateItem(UUID itemId, ClientOrderItemDTO dto);

    /**
     * Elimina un item
     */
    void deleteItem(UUID itemId);

    /**
     * Obtiene items por product_id (para análisis de productos más vendidos)
     */
    List<ClientOrderItemDTO> getItemsByProduct(Long productId);
}
