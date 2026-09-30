package com.lealtixservice.dto.reportes;

/**
 * Tipo de dato de una columna. Decide el formato aplicado en el Excel
 * y el pipe de presentacion en el frontend.
 */
public enum TipoColumna {
    TEXTO,
    MONEDA,
    NUMERO,
    PORCENTAJE,
    FECHA
}
