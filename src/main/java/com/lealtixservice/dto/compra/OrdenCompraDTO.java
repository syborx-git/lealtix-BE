package com.lealtixservice.dto.compra;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCompraDTO {
    private Long id;
    private Long tenantId;
    private String folio;
    private String titulo;
    private String estado;
    private BigDecimal totalEstimado;
    private BigDecimal totalReal;
    private String notas;
    private Integer totalArticulos;
    private List<OrdenCompraItemDTO> items;
    private LocalDateTime createdAt;
    private LocalDateTime surtidaAt;
    private LocalDateTime updatedAt;
}

