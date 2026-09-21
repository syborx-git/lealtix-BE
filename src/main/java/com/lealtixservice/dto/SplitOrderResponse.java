package com.lealtixservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta de una división de cuenta: la comanda original actualizada
 * (sin los artículos movidos) y la nueva comanda creada.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SplitOrderResponse {

    private ClientOrderDTO originalOrder;

    private ClientOrderDTO newOrder;
}