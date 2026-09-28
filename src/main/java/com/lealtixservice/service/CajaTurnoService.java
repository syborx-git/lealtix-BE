package com.lealtixservice.service;

import com.lealtixservice.dto.caja.*;
import com.lealtixservice.entity.LiquidacionPropina;

import java.util.UUID;

public interface CajaTurnoService {

    TurnoDTO obtenerTurnoActivo(Long tenantId, Long cajeroId);

    TurnoDTO abrirTurno(AbrirTurnoRequest request);

    ResumenTurnoCorteDTO obtenerResumenTurno(Long tenantId, Long idTurno);

    TurnoDTO cerrarTurno(CerrarTurnoRequest request);

    TableroCajaDTO obtenerTablero(Long tenantId);

    TicketPrecuentaDTO imprimirTicketPrecuenta(Long tenantId, UUID orderId);

    PagoDTO cobrarComanda(UUID orderId, CobrarComandaRequest request);

    CorteMeseroDTO obtenerCorteMesero(Long tenantId, Long idMesero, Long idTurno);

    LiquidacionPropina liquidarPropinasMesero(LiquidarPropinasRequest request);
}
