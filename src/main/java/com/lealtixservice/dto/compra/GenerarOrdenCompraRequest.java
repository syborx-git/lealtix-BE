package com.lealtixservice.dto.compra;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerarOrdenCompraRequest {
    private Long tenantId;
    private String titulo;
    private String notas;
    private Double factor;
    private List<OrdenCompraItemDTO> items;
}

