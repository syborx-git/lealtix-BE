package com.lealtixservice.service.impl;

import com.lealtixservice.dto.reportes.*;
import com.lealtixservice.repository.ReporteTicketsCanceladosRepository;
import com.lealtixservice.service.ReporteExcelService;
import com.lealtixservice.service.ReporteTicketsCanceladosService;
import com.lealtixservice.util.DateRangeResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteTicketsCanceladosServiceImpl implements ReporteTicketsCanceladosService {

    private static final String REPORTE_KEY = "4.1_tickets_cancelados";
    private static final String PILAR = "4. Operacion y Staff";
    private static final String NOMBRE = "Auditoría de Tickets Cancelados";
    private static final int LIMITE_DETALLE = 300;

    private static final DateTimeFormatter FORMATO_FECHA_TABLA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final List<ColumnaDTO> COLUMNAS_MOTIVO = List.of(
            new ColumnaDTO("motivo", "Motivo de cancelación", TipoColumna.TEXTO, 280),
            new ColumnaDTO("operaciones", "Tickets cancelados", TipoColumna.NUMERO, 140),
            new ColumnaDTO("total", "Monto cancelado", TipoColumna.MONEDA, 160),
            new ColumnaDTO("pct", "% del total", TipoColumna.PORCENTAJE, 120)
    );

    private static final List<ColumnaDTO> COLUMNAS_RESPONSABLE = List.of(
            new ColumnaDTO("responsable", "Usuario responsable", TipoColumna.TEXTO, 260),
            new ColumnaDTO("operaciones", "Tickets cancelados", TipoColumna.NUMERO, 140),
            new ColumnaDTO("total", "Monto cancelado", TipoColumna.MONEDA, 160),
            new ColumnaDTO("pct", "% del total", TipoColumna.PORCENTAJE, 120)
    );

    private static final List<ColumnaDTO> COLUMNAS_DETALLE = List.of(
            new ColumnaDTO("id", "ID", TipoColumna.TEXTO, 120),
            new ColumnaDTO("folio", "Folio / Ticket", TipoColumna.TEXTO, 120),
            new ColumnaDTO("fechaCancelacion", "Fecha Cancelación", TipoColumna.FECHA, 150),
            new ColumnaDTO("mesa", "Mesa", TipoColumna.TEXTO, 130),
            new ColumnaDTO("mesero", "Mesero", TipoColumna.TEXTO, 160),
            new ColumnaDTO("responsable", "Cancelado por", TipoColumna.TEXTO, 180),
            new ColumnaDTO("motivo", "Motivo de Cancelación", TipoColumna.TEXTO, 280),
            new ColumnaDTO("total", "Total", TipoColumna.MONEDA, 120),
            new ColumnaDTO("itemsCount", "Items", TipoColumna.NUMERO, 90),
            new ColumnaDTO("itemsResumen", "Productos", TipoColumna.TEXTO, 260)
    );

    private final ReporteTicketsCanceladosRepository repository;
    private final DateRangeResolver dateRangeResolver;
    private final ReporteExcelService excelService;

    @Override
    public AuditoriaTicketsCanceladosDTO obtener(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to) {
        PeriodoComparativo periodo = dateRangeResolver.resolver(preset, from, to);
        return construir(tenantId, periodo);
    }

    @Override
    public byte[] exportar(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to) {
        PeriodoComparativo periodo = dateRangeResolver.resolver(preset, from, to);
        AuditoriaTicketsCanceladosDTO datos = construir(tenantId, periodo);

        String subtitulo = "Periodo actual: " + periodo.actual().etiqueta()
                + " (" + periodo.actual().from().toLocalDate() + " a " + periodo.actual().to().toLocalDate() + ")"
                + " | Comparado contra: " + periodo.anterior().etiqueta()
                + " (" + periodo.anterior().from().toLocalDate() + " a " + periodo.anterior().to().toLocalDate() + ")";

        List<HojaExcelDTO> hojas = new ArrayList<>();

        // 1. Resumen comparativo de KPIs
        List<List<TipoColumna>> tiposPorFila = new ArrayList<>();
        for (KpiDTO k : datos.kpis()) {
            TipoColumna formatoValor = "numero".equals(k.formato()) ? TipoColumna.NUMERO : TipoColumna.MONEDA;
            tiposPorFila.add(List.of(TipoColumna.TEXTO, formatoValor, formatoValor, TipoColumna.PORCENTAJE, TipoColumna.TEXTO));
        }

        hojas.add(HojaExcelDTO.deTiposPorFila(
                "Resumen comparativo",
                NOMBRE,
                subtitulo,
                List.of("Indicador", "Periodo actual", "Periodo anterior", "Variacion %", "Tendencia"),
                tiposPorFila,
                datos.kpis().stream().map(k -> List.<Object>of(
                        k.label(),
                        k.actual(),
                        k.anterior(),
                        k.variacionPct() == null ? "Sin base" : k.variacionPct(),
                        etiquetaDireccion(k.direccion())
                )).toList()
        ));

        // 2. Por motivo
        hojas.add(hojaDesdeTabla(datos.porMotivo(), "Por motivo", "Cancelaciones por motivo",
                "Desglose de tickets cancelados agrupados por justificación"));

        // 3. Por responsable
        hojas.add(hojaDesdeTabla(datos.porResponsable(), "Por responsable", "Cancelaciones por responsable",
                "Trazabilidad de cancelaciones por cajero / usuario"));

        // 4. Detalle cronológico
        hojas.add(hojaDesdeTabla(datos.detalle(), "Detalle cancelaciones", "Historial de tickets cancelados",
                "Auditoría cronológica de cancelaciones en el periodo"));

        return excelService.generar(NOMBRE, hojas);
    }

    private AuditoriaTicketsCanceladosDTO construir(Long tenantId, PeriodoComparativo periodo) {
        LocalDateTime actualToEx = dateRangeResolver.aExclusivo(periodo.actual().to());
        LocalDateTime anteriorToEx = dateRangeResolver.aExclusivo(periodo.anterior().to());

        List<Object[]> resumenAct = repository.resumenCancelaciones(tenantId, periodo.actual().from(), actualToEx);
        List<Object[]> resumenAnt = repository.resumenCancelaciones(tenantId, periodo.anterior().from(), anteriorToEx);

        Object[] act = resumenAct.isEmpty() ? new Object[]{0L, BigDecimal.ZERO, BigDecimal.ZERO} : resumenAct.get(0);
        Object[] ant = resumenAnt.isEmpty() ? new Object[]{0L, BigDecimal.ZERO, BigDecimal.ZERO} : resumenAnt.get(0);

        long countAct = ((Number) (act[0] != null ? act[0] : 0)).longValue();
        long countAnt = ((Number) (ant[0] != null ? ant[0] : 0)).longValue();

        double montoAct = ((Number) (act[1] != null ? act[1] : 0)).doubleValue();
        double montoAnt = ((Number) (ant[1] != null ? ant[1] : 0)).doubleValue();

        double promAct = ((Number) (act[2] != null ? act[2] : 0)).doubleValue();
        double promAnt = ((Number) (ant[2] != null ? ant[2] : 0)).doubleValue();

        Long totalOrdAct = repository.totalOrdenesGeneradas(tenantId, periodo.actual().from(), actualToEx);
        Long totalOrdAnt = repository.totalOrdenesGeneradas(tenantId, periodo.anterior().from(), anteriorToEx);

        double totalOrdActD = totalOrdAct != null && totalOrdAct > 0 ? totalOrdAct : (countAct > 0 ? countAct : 1);
        double totalOrdAntD = totalOrdAnt != null && totalOrdAnt > 0 ? totalOrdAnt : (countAnt > 0 ? countAnt : 1);

        double tasaAct = (countAct / totalOrdActD) * 100.0;
        double tasaAnt = (countAnt / totalOrdAntD) * 100.0;

        List<KpiDTO> kpis = List.of(
                KpiDTO.de("total_cancelaciones", "Tickets Cancelados", BigDecimal.valueOf(countAct), BigDecimal.valueOf(countAnt), "numero"),
                KpiDTO.de("monto_cancelado", "Monto Total Cancelado", BigDecimal.valueOf(montoAct), BigDecimal.valueOf(montoAnt), "moneda"),
                KpiDTO.de("ticket_promedio", "Ticket Promedio Cancelado", BigDecimal.valueOf(promAct), BigDecimal.valueOf(promAnt), "moneda"),
                KpiDTO.de("tasa_cancelacion", "Tasa de Cancelación (%)", BigDecimal.valueOf(redondear(tasaAct)), BigDecimal.valueOf(redondear(tasaAnt)), "numero")
        );

        TablaReporteDTO porMotivo = construirTablaPorMotivo(tenantId, periodo.actual().from(), actualToEx, montoAct);
        TablaReporteDTO porResponsable = construirTablaPorResponsable(tenantId, periodo.actual().from(), actualToEx, montoAct);
        TablaReporteDTO detalle = construirTablaDetalle(tenantId, periodo.actual().from(), actualToEx);

        ReporteMetaDTO meta = ReporteMetaDTO.de(
                REPORTE_KEY,
                PILAR,
                NOMBRE,
                periodo,
                "day"
        );

        return AuditoriaTicketsCanceladosDTO.de(meta, kpis, porMotivo, porResponsable, detalle);
    }

    private TablaReporteDTO construirTablaPorMotivo(Long tenantId, LocalDateTime from, LocalDateTime toEx, double montoTotal) {
        List<Object[]> datos = repository.cancelacionesPorMotivo(tenantId, from, toEx);
        List<Map<String, Object>> filas = new ArrayList<>();

        for (Object[] fila : datos) {
            String motivo = fila[0] != null ? fila[0].toString() : "Sin motivo registrado";
            long operaciones = ((Number) (fila[1] != null ? fila[1] : 0)).longValue();
            double total = ((Number) (fila[2] != null ? fila[2] : 0)).doubleValue();
            double pct = montoTotal > 0 ? (total / montoTotal) * 100.0 : 0.0;

            Map<String, Object> mapa = new LinkedHashMap<>();
            mapa.put("motivo", motivo);
            mapa.put("operaciones", operaciones);
            mapa.put("total", total);
            mapa.put("pct", redondear(pct));
            filas.add(mapa);
        }
        return TablaReporteDTO.de(COLUMNAS_MOTIVO, filas);
    }

    private TablaReporteDTO construirTablaPorResponsable(Long tenantId, LocalDateTime from, LocalDateTime toEx, double montoTotal) {
        List<Object[]> datos = repository.cancelacionesPorResponsable(tenantId, from, toEx);
        List<Map<String, Object>> filas = new ArrayList<>();

        for (Object[] fila : datos) {
            String responsable = fila[0] != null ? fila[0].toString() : "No especificado";
            long operaciones = ((Number) (fila[1] != null ? fila[1] : 0)).longValue();
            double total = ((Number) (fila[2] != null ? fila[2] : 0)).doubleValue();
            double pct = montoTotal > 0 ? (total / montoTotal) * 100.0 : 0.0;

            Map<String, Object> mapa = new LinkedHashMap<>();
            mapa.put("responsable", responsable);
            mapa.put("operaciones", operaciones);
            mapa.put("total", total);
            mapa.put("pct", redondear(pct));
            filas.add(mapa);
        }
        return TablaReporteDTO.de(COLUMNAS_RESPONSABLE, filas);
    }

    private TablaReporteDTO construirTablaDetalle(Long tenantId, LocalDateTime from, LocalDateTime toEx) {
        List<Object[]> datos = repository.detalleCancelaciones(tenantId, from, toEx, LIMITE_DETALLE);
        List<Map<String, Object>> filas = new ArrayList<>();

        for (Object[] fila : datos) {
            String idStr = fila[0] != null ? fila[0].toString() : "";
            String folio = idStr.length() > 8 ? idStr.substring(0, 8).toUpperCase() : idStr.toUpperCase();
            String fechaCanc = formatearFecha(fila[1]);
            String motivo = fila[3] != null ? fila[3].toString() : "Sin motivo registrado";
            String responsable = fila[4] != null ? fila[4].toString() : "No especificado";
            String mesa = fila[5] != null ? fila[5].toString() : "Sin mesa";
            String mesero = fila[6] != null ? fila[6].toString() : "Sin mesero";
            double total = ((Number) (fila[7] != null ? fila[7] : 0)).doubleValue();
            long itemsCount = ((Number) (fila[8] != null ? fila[8] : 0)).longValue();
            String itemsResumen = fila[9] != null ? fila[9].toString() : "Sin items";

            Map<String, Object> mapa = new LinkedHashMap<>();
            mapa.put("id", idStr);
            mapa.put("folio", folio);
            mapa.put("fechaCancelacion", fechaCanc);
            mapa.put("mesa", mesa);
            mapa.put("mesero", mesero);
            mapa.put("responsable", responsable);
            mapa.put("motivo", motivo);
            mapa.put("total", total);
            mapa.put("itemsCount", itemsCount);
            mapa.put("itemsResumen", itemsResumen);
            filas.add(mapa);
        }
        return TablaReporteDTO.de(COLUMNAS_DETALLE, filas);
    }

    private HojaExcelDTO hojaDesdeTabla(TablaReporteDTO tabla, String nombreHoja, String titulo, String nota) {
        List<String> encabezados = tabla.columnas().stream().map(ColumnaDTO::label).toList();
        List<TipoColumna> tipos = tabla.columnas().stream().map(ColumnaDTO::tipo).toList();
        List<String> claves = tabla.columnas().stream().map(ColumnaDTO::key).toList();

        List<List<Object>> filas = new ArrayList<>();
        for (Map<String, Object> fila : tabla.filas()) {
            List<Object> f = new ArrayList<>();
            for (String c : claves) {
                f.add(fila.get(c));
            }
            filas.add(f);
        }

        return HojaExcelDTO.de(nombreHoja, titulo, nota, encabezados, tipos, filas);
    }

    private double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private String formatearFecha(Object valor) {
        if (valor == null) return "-";
        if (valor instanceof LocalDateTime ldt) {
            return ldt.format(FORMATO_FECHA_TABLA);
        }
        if (valor instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime().format(FORMATO_FECHA_TABLA);
        }
        return valor.toString();
    }

    private String etiquetaDireccion(String direccion) {
        if (direccion == null) return "Sin cambio";
        return switch (direccion) {
            case "SUBE" -> "Incremento";
            case "BAJA" -> "Reduccion";
            case "NUEVO" -> "Nuevo indicador";
            default -> "Sin cambio";
        };
    }
}
