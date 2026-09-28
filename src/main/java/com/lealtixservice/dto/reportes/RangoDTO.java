package com.lealtixservice.dto.reportes;

import java.time.LocalDateTime;

/**
 * Rango temporal ya normalizado a bordes de dia completo.
 * from = 00:00:00 del dia inicial, to = 23:59:59 del dia final.
 */
public record RangoDTO(
        LocalDateTime from,
        LocalDateTime to,
        String etiqueta
) {

    public static RangoDTO de(LocalDateTime from, LocalDateTime to, String etiqueta) {
        return new RangoDTO(from, to, etiqueta);
    }

    public long dias() {
        return java.time.temporal.ChronoUnit.DAYS.between(from.toLocalDate(), to.toLocalDate()) + 1;
    }
}
