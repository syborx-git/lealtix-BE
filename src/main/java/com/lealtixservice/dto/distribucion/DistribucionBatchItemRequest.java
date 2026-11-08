package com.lealtixservice.dto.distribucion;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistribucionBatchItemRequest {
    private Long insumoId;
    private Double cantidad;
    private String destino; // 'cocina' o 'barra'
}

