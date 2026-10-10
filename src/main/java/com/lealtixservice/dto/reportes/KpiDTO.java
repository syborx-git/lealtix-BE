package com.lealtixservice.dto.reportes;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * KPI con comparativa contra el periodo anterior equivalente.
 * direccion: SUBE, BAJA, IGUAL o NUEVO (sin base de comparacion).
 */
public record KpiDTO(
        String key,
        String label,
        BigDecimal actual,
        BigDecimal anterior,
        BigDecimal variacionPct,
        String direccion,
        String formato
) {

    public static final String SUBE = "SUBE";
    public static final String BAJA = "BAJA";
    public static final String IGUAL = "IGUAL";
    public static final String NUEVO = "NUEVO";

    /**
     * Calcula la variacion porcentual. Si el periodo anterior fue cero, la
     * variacion no es un numero finito: se marca NUEVO y se devuelve null
     * para que la UI no muestre un Infinity ni un 0 enganoso.
     */
    public static KpiDTO de(String key, String label, BigDecimal actual, BigDecimal anterior, String formato) {
        BigDecimal actualSafe = actual == null ? BigDecimal.ZERO : actual;
        BigDecimal anteriorSafe = anterior == null ? BigDecimal.ZERO : anterior;

        if (anteriorSafe.compareTo(BigDecimal.ZERO) == 0) {
            String direccion = actualSafe.compareTo(BigDecimal.ZERO) == 0 ? IGUAL : NUEVO;
            return new KpiDTO(key, label, actualSafe, anteriorSafe, null, direccion, formato);
        }

        BigDecimal variacion = actualSafe.subtract(anteriorSafe)
                .multiply(BigDecimal.valueOf(100))
                .divide(anteriorSafe, 2, RoundingMode.HALF_UP);

        int cmp = actualSafe.compareTo(anteriorSafe);
        String direccion = cmp > 0 ? SUBE : (cmp < 0 ? BAJA : IGUAL);
        return new KpiDTO(key, label, actualSafe, anteriorSafe, variacion, direccion, formato);
    }

    public static KpiDTO de(String key, String label, double actual, double anterior, String formato) {
        return de(key, label, BigDecimal.valueOf(actual), BigDecimal.valueOf(anterior), formato);
    }

    public static KpiDTO de(String key, String label, int actual, int anterior, String formato) {
        return de(key, label, BigDecimal.valueOf(actual), BigDecimal.valueOf(anterior), formato);
    }
}
