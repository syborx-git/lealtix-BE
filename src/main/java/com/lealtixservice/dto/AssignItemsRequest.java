package com.lealtixservice.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request para asignar ítems de la comanda (client_order_item) a un asiento.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignItemsRequest {

    @NotEmpty(message = "itemIds es requerido")
    private List<UUID> itemIds;
}