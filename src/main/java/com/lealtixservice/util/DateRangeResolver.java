package com.lealtixservice.util;

import com.lealtixservice.dto.reportes.PeriodoComparativo;
import com.lealtixservice.dto.reportes.PresetReporte;
import com.lealtixservice.dto.reportes.RangoDTO;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * Motor de tiempo del modulo de reportes.
 *
 * Resuelve el rango actual a partir de un filtro rapido o de un rango personalizado
 * y calcula el periodo anterior equivalente, que es contra el que se mide el
 * crecimiento. Reglas de comparacion (evitan comparaciones engañosas):
 *
 * - HOY            -> ayer
 * - AYER           -> anteayer
 * - ESTA_SEMANA    -> misma cantidad de dias transcurridos de la semana anterior
 * - ESTE_MES       -> mismos dias transcurridos del mes anterior
 * - SEMANA_PASADA  -> semana completa anterior
 * - MES_PASADO     -> mes completo anterior
 * - PERSONALIZADO  -> ventana de igual longitud inmediatamente anterior
 *
 * Cuando el periodo anterior es un mes y el actual caeria en un dia que ese mes
 * no tiene (p. ej. 31 de marzo contra febrero), se recorta al ultimo dia real
 * de ese mes en lugar de desbordar la ventana.
 */
@Component
public class DateRangeResolver {

    private static final LocalTime INICIO_DIA = LocalTime.of(0, 0, 0);
    private static final LocalTime FIN_DIA = LocalTime.of(23, 59, 59);

    /**
     * Resuelve el periodo actual y su equivalente anterior.
     *
     * @param preset filtro rapido solicitado
     * @param from   fecha inicio solo para PERSONALIZADO
     * @param to     fecha fin solo para PERSONALIZADO
     */
    public PeriodoComparativo resolver(PresetReporte preset, LocalDateTime from, LocalDateTime to) {
        LocalDate hoy = LocalDate.now();
        PresetReporte p = preset == null ? PresetReporte.HOY : preset;

        if (p == PresetReporte.HOY) {
            return new PeriodoComparativo(
                    p,
                    rango(hoy, hoy, "Hoy"),
                    rango(hoy.minusDays(1), hoy.minusDays(1), "Ayer")
            );
        } else if (p == PresetReporte.AYER) {
            return new PeriodoComparativo(
                    p,
                    rango(hoy.minusDays(1), hoy.minusDays(1), "Ayer"),
                    rango(hoy.minusDays(2), hoy.minusDays(2), "Anteayer")
            );
        } else if (p == PresetReporte.ESTA_SEMANA) {
            LocalDate inicioSemana = hoy.with(DayOfWeek.MONDAY);
            long diasTranscurridos = ChronoUnit.DAYS.between(inicioSemana, hoy) + 1;
            return new PeriodoComparativo(
                    p,
                    rango(inicioSemana, hoy, "Esta semana"),
                    rango(inicioSemana.minusWeeks(1),
                            inicioSemana.minusWeeks(1).plusDays(diasTranscurridos - 1),
                            "Semana anterior (mismos dias)")
            );
        } else if (p == PresetReporte.SEMANA_PASADA) {
            LocalDate inicio = hoy.with(DayOfWeek.MONDAY).minusWeeks(1);
            return new PeriodoComparativo(
                    p,
                    rango(inicio, inicio.plusDays(6), "Semana pasada"),
                    rango(inicio.minusWeeks(1), inicio.minusWeeks(1).plusDays(6), "Semana anterior")
            );
        } else if (p == PresetReporte.ESTE_MES) {
            LocalDate inicioMes = hoy.withDayOfMonth(1);
            int diaDelMes = hoy.getDayOfMonth();
            return new PeriodoComparativo(
                    p,
                    rango(inicioMes, hoy, "Este mes"),
                    rango(inicioMes.minusMonths(1), recortar(inicioMes.minusMonths(1), diaDelMes), "Mes anterior (mismos dias)")
            );
        } else if (p == PresetReporte.MES_PASADO) {
            LocalDate inicioMes = hoy.withDayOfMonth(1).minusMonths(1);
            return new PeriodoComparativo(
                    p,
                    rango(inicioMes, inicioMes.plusMonths(1).minusDays(1), "Mes pasado"),
                    rango(inicioMes.minusMonths(1), inicioMes.minusMonths(1).plusMonths(1).minusDays(1), "Mes anterior")
            );
        } else {
            LocalDate inicio = from != null ? from.toLocalDate() : hoy.minusDays(29);
            LocalDate fin = to != null ? to.toLocalDate() : hoy;
            if (fin.isBefore(inicio)) {
                LocalDate swap = inicio;
                inicio = fin;
                fin = swap;
            }
            long dias = ChronoUnit.DAYS.between(inicio, fin) + 1;
            return new PeriodoComparativo(
                    p,
                    rango(inicio, fin, "Personalizado"),
                    rango(inicio.minusDays(dias), inicio.minusDays(1), "Periodo anterior")
            );
        }
    }

    /**
     * Elige la granularidad de la serie temporal a partir de la Extension del rango,
     * para no graficar 400 puntos diarios en una ventana de un ano.
     */
    public String granularidadSugerida(RangoDTO rango) {
        long dias = rango.dias();
        if (dias <= 31) {
            return "day";
        }
        if (dias <= 92) {
            return "week";
        }
        return "month";
    }

    /** Normaliza el valor recibido: day, week o month. */
    public String normalizarGranularidad(String granularidad, RangoDTO rango) {
        if (granularidad == null || granularidad.isBlank()) {
            return granularidadSugerida(rango);
        }
        String g = granularidad.trim().toLowerCase();
        return switch (g) {
            case "day", "dia" -> "day";
            case "week", "semana" -> "week";
            case "month", "mes" -> "month";
            default -> granularidadSugerida(rango);
        };
    }

    private RangoDTO rango(LocalDate inicio, LocalDate fin, String etiqueta) {
        return new RangoDTO(
                inicio.atTime(INICIO_DIA),
                fin.atTime(FIN_DIA),
                etiqueta
        );
    }

    /**
     * Convierte el fin inclusivo del rango (23:59:59) en un limite EXCLUSIVO
     * (medianoche del dia siguiente).
     *
     * Las queries de reportes deben filtrar con ">= :from AND < :toExclusivo" en
     * lugar de BETWEEN. Con BETWEEN, un pago registrado a las 23:59:59.500 se
     * queda fuera del periodo, y ademas el planner no puede aprovechar igual un
     * indice sobre la columna de fecha.
     */
    public LocalDateTime aExclusivo(LocalDateTime finInclusivo) {
        if (finInclusivo == null) {
            return null;
        }
        return finInclusivo.toLocalDate().plusDays(1).atStartOfDay();
    }

    /** Si el dia solicitado no existe en el mes destino, usa el ultimo dia de ese mes. */
    private LocalDate recortar(LocalDate inicioMes, int diaObjetivo) {
        LocalDate ultimo = inicioMes.plusMonths(1).minusDays(1);
        return inicioMes.plusDays(Math.min(diaObjetivo, ultimo.getDayOfMonth()) - 1L);
    }
}
