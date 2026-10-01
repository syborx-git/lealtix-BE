package com.lealtixservice.dto.caja;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoDTO {
    private Long idPago;
    private Long tenantId;
    private UUID idComanda;
    private Long idTurno;
    private Long idCajero;
    private String nombreCajero;
    private String metodoPago;
    private BigDecimal montoCuenta;
    private BigDecimal montoPropina;
    private BigDecimal montoTotal;
    private String referencia;
    private LocalDateTime fecha;
    private String estado;
}
