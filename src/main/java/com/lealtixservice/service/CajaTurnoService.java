package com.lealtixservice.service;

import com.lealtixservice.dto.caja.*;
import com.lealtixservice.entity.LiquidacionPropina;

import java.time.LocalDate;
import java.util.UUID;

public interface CajaTurnoService {

    TurnoDTO obtenerTurnoActivo(Long tenantId, Long cajeroId);

    TurnoDTO abrirTurno(AbrirTurnoRequest request);

    ResumenTurnoCorteDTO obtenerResumenTurno(Long tenantId, Long idTurno);

    TurnoDTO cerrarTurno(CerrarTurnoRequest request);

    TableroCajaDTO obtenerTablero(Long tenantId);

    TicketPrecuentaDTO imprimirTicketPrecuenta(Long tenantId, UUID orderId);

    PagoDTO cobrarComanda(UUID orderId, CobrarComandaRequest request);

    /**
     * Corte y rendimiento de un mesero.
     *
     * @param idTurno turno a acotar (opcional)
     * @param fecha   día a acotar (opcional). Si viene, el corte corresponde
     *                únicamente a los cobros de ese día.
     */
    CorteMeseroDTO obtenerCorteMesero(Long tenantId, Long idMesero, Long idTurno, LocalDate fecha);

    LiquidacionPropina liquidarPropinasMesero(LiquidarPropinasRequest request);
}
