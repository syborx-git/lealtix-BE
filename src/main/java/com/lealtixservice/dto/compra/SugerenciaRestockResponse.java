package com.lealtixservice.dto.compra;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SugerenciaRestockResponse {
    private Long tenantId;
    private String folioSugerido;
    private Double factor;
    private Integer totalInsumosBajos;
    private BigDecimal totalEstimado;
    private List<OrdenCompraItemDTO> items;
}

