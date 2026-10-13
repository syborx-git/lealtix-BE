package com.lealtixservice.dto.reportes;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record ChartPuntoDTO(
        String etiqueta,
        Double valor,
        BigDecimal valorMoneda,
        String unidad,
        String categoria,
        String area
) {
    @JsonProperty("label")
    public String label() {
        return etiqueta;
    }

    @JsonProperty("subtexto")
    public String subtexto() {
        return unidad;
    }

    public static ChartPuntoDTO de(String etiqueta, Double valor, String unidad, String categoria, String area) {
        return new ChartPuntoDTO(etiqueta, valor, BigDecimal.ZERO, unidad, categoria, area);
    }

    public static ChartPuntoDTO de(String etiqueta, Number valor) {
        return new ChartPuntoDTO(etiqueta, valor != null ? valor.doubleValue() : 0.0, BigDecimal.ZERO, null, null, null);
    }

    public static ChartPuntoDTO de(String etiqueta, Number valor, String unidad) {
        return new ChartPuntoDTO(etiqueta, valor != null ? valor.doubleValue() : 0.0, BigDecimal.ZERO, unidad, null, null);
    }

    public static ChartPuntoDTO deMoneda(String etiqueta, Double valor, BigDecimal valorMoneda, String unidad, String categoria, String area) {
        return new ChartPuntoDTO(etiqueta, valor, valorMoneda, unidad, categoria, area);
    }

    public static ChartPuntoDTO conMoneda(String etiqueta, Number valor, Number valorMoneda) {
        return new ChartPuntoDTO(
                etiqueta,
                valor != null ? valor.doubleValue() : 0.0,
                valorMoneda != null ? BigDecimal.valueOf(valorMoneda.doubleValue()) : BigDecimal.ZERO,
                null,
                null,
                null
        );
    }
}
