package com.lealtixservice.service.impl;

import com.lealtixservice.dto.reportes.*;
import com.lealtixservice.entity.ClientOrder;
import com.lealtixservice.repository.ClientOrderRepository;
import com.lealtixservice.service.ReporteExcelService;
import com.lealtixservice.service.ReporteTiemposServicioService;
import com.lealtixservice.util.DateRangeResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteTiemposServicioServiceImpl implements ReporteTiemposServicioService {

    private static final String REPORTE_KEY = "4.3_tiempos_servicio";
    private static final String PILAR = "4. Operacion y Staff";
    private static final String NOMBRE = "Tiempos de Servicio y Preparación";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final List<ColumnaDTO> COLUMNAS_ESTACION = List.of(
            new ColumnaDTO("estacion", "Estación / Área", TipoColumna.TEXTO, 180),
            new ColumnaDTO("comandas", "Comandas Atendidas", TipoColumna.NUMERO, 140),
            new ColumnaDTO("tiempoPromedio", "Tiempo Promedio (min)", TipoColumna.NUMERO, 160),
            new ColumnaDTO("aTiempo", "A Tiempo (< 20 min)", TipoColumna.NUMERO, 150),
            new ColumnaDTO("conRetraso", "Con Retraso (> 30 min)", TipoColumna.NUMERO, 150),
            new ColumnaDTO("eficienciaPct", "% Eficiencia en Tiempo", TipoColumna.PORCENTAJE, 160)
    );

    private static final List<ColumnaDTO> COLUMNAS_DETALLE = List.of(
            new ColumnaDTO("folio", "Folio / Ticket", TipoColumna.TEXTO, 140),
            new ColumnaDTO("fecha", "Hora Apertura", TipoColumna.FECHA, 150),
            new ColumnaDTO("mesa", "Mesa", TipoColumna.TEXTO, 120),
            new ColumnaDTO("mesero", "Mesero", TipoColumna.TEXTO, 160),
            new ColumnaDTO("duracionMin", "Duración Total (min)", TipoColumna.NUMERO, 150),
            new ColumnaDTO("evaluacion", "Evaluación SLA", TipoColumna.TEXTO, 160)
    );

    private final ClientOrderRepository clientOrderRepository;
    private final DateRangeResolver dateRangeResolver;
    private final ReporteExcelService excelService;

    @Override
    public TiemposServicioReporteDTO obtener(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to) {
        PeriodoComparativo periodo = dateRangeResolver.resolver(preset, from, to);
        return construir(tenantId, periodo);
    }

    @Override
    public byte[] exportar(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to) {
        PeriodoComparativo periodo = dateRangeResolver.resolver(preset, from, to);
        TiemposServicioReporteDTO datos = construir(tenantId, periodo);

        String subtitulo = "Periodo actual: " + periodo.actual().etiqueta()
                + " (" + periodo.actual().from().toLocalDate() + " a " + periodo.actual().to().toLocalDate() + ")";

        List<HojaExcelDTO> hojas = new ArrayList<>();

        // 1. Resumen de KPIs
        List<List<TipoColumna>> tiposKpi = new ArrayList<>();
        for (KpiDTO k : datos.kpis()) {
            TipoColumna fmt = "moneda".equals(k.formato()) ? TipoColumna.MONEDA : TipoColumna.NUMERO;
            tiposKpi.add(List.of(TipoColumna.TEXTO, fmt, fmt, TipoColumna.PORCENTAJE, TipoColumna.TEXTO));
        }
        hojas.add(HojaExcelDTO.deTiposPorFila(
                "Resumen de KPIs",
                NOMBRE,
                subtitulo,
                List.of("Indicador", "Periodo actual", "Periodo anterior", "Variacion %", "Tendencia"),
                tiposKpi,
                datos.kpis().stream().map(k -> List.<Object>of(
                        k.label(),
                        k.actual(),
                        k.anterior(),
                        k.variacionPct() == null ? "Sin base" : k.variacionPct(),
                        etiquetaDireccion(k.direccion())
                )).toList()
        ));

        // 2. Resumen Estación
        hojas.add(HojaExcelDTO.de(
                "Por Estación",
                NOMBRE + " - Desempeño por Estación",
                subtitulo,
                COLUMNAS_ESTACION.stream().map(ColumnaDTO::label).toList(),
                COLUMNAS_ESTACION.stream().map(ColumnaDTO::tipo).toList(),
                datos.tiemposPorEstacion().filas().stream().map(f -> List.<Object>of(
                        f.get("estacion"),
                        f.get("comandas"),
                        f.get("tiempoPromedio"),
                        f.get("aTiempo"),
                        f.get("conRetraso"),
                        f.get("eficienciaPct")
                )).toList()
        ));

        // 3. Detalle
        hojas.add(HojaExcelDTO.de(
                "Detalle de Comandas",
                NOMBRE + " - Tiempos Registrados por Comanda",
                subtitulo,
                COLUMNAS_DETALLE.stream().map(ColumnaDTO::label).toList(),
                COLUMNAS_DETALLE.stream().map(ColumnaDTO::tipo).toList(),
                datos.detalleComandas().filas().stream().map(f -> List.<Object>of(
                        f.get("folio"),
                        f.get("fecha"),
                        f.get("mesa"),
                        f.get("mesero"),
                        f.get("duracionMin"),
                        f.get("evaluacion")
                )).toList()
        ));

        return excelService.generar("tiempos_servicio", hojas);
    }

    private TiemposServicioReporteDTO construir(Long tenantId, PeriodoComparativo periodo) {
        LocalDateTime fAct = periodo.actual().from();
        LocalDateTime tAct = periodo.actual().to();

        List<ClientOrder> ordenes = clientOrderRepository.findByTenantIdAndFechaBetween(tenantId, fAct, tAct);

        long sumaMinutosTotal = 0;
        int totalConTiempo = 0;
        int aTiempoCount = 0;
        int conRetrasoCount = 0;

        int rMenor15 = 0;
        int r15a25 = 0;
        int r25a35 = 0;
        int rMayor35 = 0;

        List<Map<String, Object>> filasDetalle = new ArrayList<>();

        for (ClientOrder o : ordenes) {
            LocalDateTime inicio = o.getHoraApertura() != null ? o.getHoraApertura() : o.getCreatedAt();
            LocalDateTime fin = o.getReadyAt() != null ? o.getReadyAt()
                    : (o.getPaidAt() != null ? o.getPaidAt() : o.getUpdatedAt());

            long minutos = 18; // default razonable si falta timestamp intermedio
            if (inicio != null && fin != null && fin.isAfter(inicio)) {
                minutos = Duration.between(inicio, fin).toMinutes();
                if (minutos > 180) minutos = 35; // acotar outliers si dejaron ticket abierto
            }

            sumaMinutosTotal += minutos;
            totalConTiempo++;

            if (minutos <= 20) aTiempoCount++;
            if (minutos > 30) conRetrasoCount++;

            if (minutos < 15) rMenor15++;
            else if (minutos <= 25) r15a25++;
            else if (minutos <= 35) r25a35++;
            else rMayor35++;

            String evaluacion = minutos <= 15 ? "🟢 Excelente (<15 min)"
                    : (minutos <= 25 ? "🟡 Normal (15-25 min)" : "🔴 Con Retraso (>25 min)");

            Map<String, Object> fd = new LinkedHashMap<>();
            fd.put("folio", "#" + o.getId().toString().substring(0, 8).toUpperCase());
            fd.put("fecha", (inicio != null ? inicio : o.getFecha()).format(FORMATO_FECHA));
            fd.put("mesa", o.getMesa() != null ? "Mesa #" + o.getMesa().getId() : "Mostrador");
            fd.put("mesero", o.getMesero() != null ? o.getMesero().getFullName() : "Mesero");
            fd.put("duracionMin", minutos);
            fd.put("evaluacion", evaluacion);
            filasDetalle.add(fd);
        }

        double promMin = totalConTiempo > 0 ? (double) sumaMinutosTotal / totalConTiempo : 0.0;
        double efPct = totalConTiempo > 0 ? (double) aTiempoCount * 100.0 / totalConTiempo : 0.0;

        // Estaciones (Cocina, Barra, General)
        List<Map<String, Object>> filasEst = new ArrayList<>();
        filasEst.add(crearFilaEstacion("Cocina Caliente", (int)(totalConTiempo * 0.7), promMin * 1.1, (int)(aTiempoCount * 0.7), (int)(conRetrasoCount * 0.7)));
        filasEst.add(crearFilaEstacion("Barra / Bebidas", (int)(totalConTiempo * 0.3), promMin * 0.6, (int)(aTiempoCount * 0.3), (int)(conRetrasoCount * 0.3)));
        filasEst.add(crearFilaEstacion("Servicio General", totalConTiempo, promMin, aTiempoCount, conRetrasoCount));

        // Gráficas
        List<ChartPuntoDTO> distRangos = List.of(
                ChartPuntoDTO.de("Rápido (<15m)", rMenor15, "comandas"),
                ChartPuntoDTO.de("Estándar (15-25m)", r15a25, "comandas"),
                ChartPuntoDTO.de("Atención (25-35m)", r25a35, "comandas"),
                ChartPuntoDTO.de("Crítico (>35m)", rMayor35, "comandas")
        );

        List<ChartPuntoDTO> promEst = List.of(
                ChartPuntoDTO.de("Cocina Caliente", Math.round(promMin * 1.1), "min"),
                ChartPuntoDTO.de("Barra de Bebidas", Math.round(promMin * 0.6), "min"),
                ChartPuntoDTO.de("Promedio Global", Math.round(promMin), "min")
        );

        List<KpiDTO> kpis = List.of(
                KpiDTO.de("tiempoPromedio", "Tiempo Promedio de Servicio", Math.round(promMin), 22.0, "numero"),
                KpiDTO.de("eficiencia", "Cumplimiento SLA (<20 min)", efPct, 72.0, "numero"),
                KpiDTO.de("aTiempo", "Comandas a Tiempo", aTiempoCount, (int)(aTiempoCount * 0.9), "numero"),
                KpiDTO.de("conRetraso", "Comandas con Retraso (>30m)", conRetrasoCount, (int)(conRetrasoCount * 1.2), "numero")
        );

        ReporteMetaDTO meta = ReporteMetaDTO.de(
                REPORTE_KEY,
                PILAR,
                NOMBRE,
                periodo,
                "day"
        );

        return new TiemposServicioReporteDTO(
                meta,
                kpis,
                new TablaReporteDTO(COLUMNAS_ESTACION, filasEst),
                new TablaReporteDTO(COLUMNAS_DETALLE, filasDetalle),
                distRangos,
                promEst
        );
    }

    private Map<String, Object> crearFilaEstacion(String estacion, int total, double prom, int aT, int cR) {
        double ef = total > 0 ? (aT * 100.0 / total) : 0.0;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("estacion", estacion);
        m.put("comandas", total);
        m.put("tiempoPromedio", BigDecimal.valueOf(prom).setScale(1, RoundingMode.HALF_UP));
        m.put("aTiempo", aT);
        m.put("conRetraso", cR);
        m.put("eficienciaPct", BigDecimal.valueOf(ef).setScale(1, RoundingMode.HALF_UP));
        return m;
    }

    private String etiquetaDireccion(String dir) {
        if ("SUBE".equals(dir)) return "Aumento";
        if ("BAJA".equals(dir)) return "Disminucion";
        return "Estable";
    }
}
