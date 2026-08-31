package com.lealtixservice.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO agregado que contiene toda la información del dashboard mesero.
 * Combinación de resumen, clientes VIP, productos cross-sell y mensajes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaiterDashboardCompleteDTO {
    
    /**
     * Resumen de métricas del dashboard
     */
    private WaiterDashboardSummaryDTO summary;
    
    /**
     * Lista de clientes VIP
     */
    private List<VipClientDTO> vipClients;
    
    /**
     * Lista de productos para venta cruzada
     */
    private List<CrossSellProductDTO> crossSellProducts;
    
    /**
     * Mensajes/alertas para mostrar al mesero
     */
    private List<MessageDTO> messages;
}
