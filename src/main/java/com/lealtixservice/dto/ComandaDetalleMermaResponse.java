package com.lealtixservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Respuesta con el desglose jerárquico de platillos e insumos de una comanda
 * para el modal de mermas intuitivo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComandaDetalleMermaResponse {

    private UUID orderId;
    private String ticket;
    private String customerName;
    private String mesaNombre;
    private String estado;
    private Double totalFinal;
    private List<ComandaItemMermaDTO> items;
}
