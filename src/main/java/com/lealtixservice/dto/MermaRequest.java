package com.lealtixservice.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** Solicitud de registro de merma(s). Soporta mermas por comanda y administrativas. */
@Data
public class MermaRequest {

    private Long tenantId;

    /** Id de la comanda origen (solo mermas por comanda) */
    private String orderId;

    /** Categoría del registro: COMANDADA (default) o ADMINISTRATIVA */
    private String categoria;

    /** Almacén de origen en mermas administrativas: BODEGA | COCINA | BARRA */
    private String origen;

    /** Motivo de la merma (texto libre) */
    private String motivo;

    /** Usuario que registra la merma (trazabilidad) */
    private Long usuarioId;

    private String usuarioNombre;

    /** Insumos/productos marcados como merma en el modal */
    private List<MermaItemRequest> items = new ArrayList<>();

    /** Tipificación de la salida No-Venta (default OPERATIVA) */
    private String tipoMerma;

    @Data
    public static class MermaItemRequest {
        private Long insumoId;
        private String insumoNombre;
        private Long productoId;
        private String productoNombre;
        private Double cantidad;
        private String unidad;
    }
}