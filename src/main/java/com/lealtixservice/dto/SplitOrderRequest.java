package com.lealtixservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Request para dividir una comanda: los artículos indicados se mueven de la
 * comanda original a una comanda nueva lista para pagar.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SplitOrderRequest {

    @NotNull(message = "tenantId es requerido")
    private Long tenantId;

    private Long customerId;

    @NotNull(message = "items es requerido")
    private List<CreateClientOrderRequest.OrderItemRequest> items;

    private String source;
}