package com.lealtixservice.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO resumen del dashboard de cocina.
 * Agrupa métricas operativas y motivacionales para equipos de cocina.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KitchenDashboardSummaryDTO {
    private String tenantName;
    private List<TopDishDTO> topDishes;
    private RepeatPurchaseRateDTO repeatPurchaseRate;
    private CompletedOrdersDTO completedOrders;
    private List<CustomizationAnalysisDTO> customizationAnalysis;
    private VIPAlertDTO vipAlert;
}
