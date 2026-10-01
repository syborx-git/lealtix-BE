package com.lealtixservice.dto.reportes;

/**
 * Filtros rapidos del panel global de reportes.
 * Cada preset define el rango actual y, por diseno, el periodo anterior equivalente
 * que usa el motor de comparativas.
 */
public enum PresetReporte {

    HOY("Hoy"),
    AYER("Ayer"),
    ESTA_SEMANA("Esta semana"),
    SEMANA_PASADA("Semana pasada"),
    ESTE_MES("Este mes"),
    MES_PASADO("Mes pasado"),
    PERSONALIZADO("Personalizado");

    private final String etiqueta;

    PresetReporte(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** Convierte el texto recibido por query param en un preset, con fallback seguro. */
    public static PresetReporte from(String texto) {
        if (texto == null || texto.isBlank()) {
            return HOY;
        }
        try {
            return valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PERSONALIZADO;
        }
    }
}
