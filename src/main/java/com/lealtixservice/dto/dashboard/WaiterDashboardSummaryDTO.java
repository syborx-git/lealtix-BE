package com.lealtixservice.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para el resumen del dashboard del mesero.
 * Contiene métricas clave de desempeño diario.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaiterDashboardSummaryDTO {
    
    /**
     * Porcentaje de ventas identificadas vs totales
     */
    private Double salesIdentifiedPercentage;
    
    /**
     * Cantidad de nuevos clientes registrados hoy
     */
    private Long newClientsToday;
    
    /**
     * Total de órdenes procesadas hoy
     */
    private Long ordersToday;
    
    /**
     * Porcentaje de clientes que realizaron más de una compra
     */
    private Double repurchaseRate;
    
    /**
     * Total de ventas de clientes identificados
     */
    private BigDecimal totalSalesIdentified;
    
    /**
     * Total de ventas generales (incluye anónimas + identificadas)
     */
    private BigDecimal totalSalesGeneral;
}
