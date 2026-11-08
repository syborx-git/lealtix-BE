package com.lealtixservice.service;

import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.dto.compra.*;

public interface OrdenCompraService {

    SugerenciaRestockResponse obtenerSugerenciaRestock(Long tenantId, Double factor);

    GenericResponse generarOrdenCompra(GenerarOrdenCompraRequest request);

    GenericResponse listarOrdenes(Long tenantId, String estado);

    GenericResponse obtenerOrden(Long id);

    GenericResponse surtirOrden(Long id, SurtirOrdenCompraRequest request);

    GenericResponse restockDirecto(RestockDirectoRequest request);

    GenericResponse cancelarOrden(Long id, String motivo);
}

