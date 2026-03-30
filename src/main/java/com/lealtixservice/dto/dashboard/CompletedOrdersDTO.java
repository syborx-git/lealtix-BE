package com.lealtixservice.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para estadísticas de órdenes completadas en el dashboard de cocina.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompletedOrdersDTO {
    private Long completedOrders;
    private Long successfulDeliveries;
}
