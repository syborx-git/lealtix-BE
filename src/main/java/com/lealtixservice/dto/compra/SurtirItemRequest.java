package com.lealtixservice.dto.compra;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurtirItemRequest {
    private Long insumoId;
    private BigDecimal cantidad;
    private BigDecimal costoTotal;
    private String areaDestino;
}

