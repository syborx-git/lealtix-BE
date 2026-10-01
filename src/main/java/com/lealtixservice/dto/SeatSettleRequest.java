package com.lealtixservice.dto;

import com.lealtixservice.enums.PaymentMethod;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request para cobrar/dividir la cuenta por asiento: indica qué asientos
 * de la comanda se van a cobrar. Genera una sub-comanda (folio derivado)
 * por cada asiento cobrado.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatSettleRequest {

    private Long tenantId;

    @NotEmpty(message = "seatIds es requerido")
    private List<UUID> seatIds;

    @NotNull(message = "Método de pago es requerido")
    private PaymentMethod method;

    private String reference;

    @NotNull(message = "Email del usuario que registra el pago es requerido")
    private String userEmail;
}