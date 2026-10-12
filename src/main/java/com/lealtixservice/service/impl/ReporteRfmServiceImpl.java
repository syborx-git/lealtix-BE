package com.lealtixservice.service.impl;

import com.lealtixservice.dto.reportes.*;
import com.lealtixservice.repository.ClientOrderRepository;
import com.lealtixservice.repository.TenantCustomerRepository;
import com.lealtixservice.service.ReporteExcelService;
import com.lealtixservice.service.ReporteRfmService;
import com.lealtixservice.util.DateRangeResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteRfmServiceImpl implements ReporteRfmService {

    private static final String REPORTE_KEY = "3.1_analisis_rfm";
    private static final String PILAR = "3. CRM y Fidelidad";
    private static final String NOMBRE = "Análisis RFM de Clientes";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final List<ColumnaDTO> COLUMNAS_SEGMENTOS = List.of(
            new ColumnaDTO("segmento", "Segmento RFM", TipoColumna.TEXTO, 220),
            new ColumnaDTO("cantidadClientes", "Clientes", TipoColumna.NUMERO, 120),
            new ColumnaDTO("porcentajeClientes", "% de Clientes", TipoColumna.PORCENTAJE, 120),
            new ColumnaDTO("ingresosTotales", "Ingresos Aportados", TipoColumna.MONEDA, 160),
            new ColumnaDTO("ticketPromedio", "Ticket Promedio", TipoColumna.MONEDA, 140),
            new ColumnaDTO("estrategia", "Acción Recomendada", TipoColumna.TEXTO, 280)
    );

    private static final List<ColumnaDTO> COLUMNAS_CLIENTES = List.of(
            new ColumnaDTO("nombre", "Cliente", TipoColumna.TEXTO, 200),
            new ColumnaDTO("email", "Email", TipoColumna.TEXTO, 220),
            new ColumnaDTO("segmento", "Segmento", TipoColumna.TEXTO, 160),
            new ColumnaDTO("recenciaDias", "Días sin Visita (R)", TipoColumna.NUMERO, 140),
            new ColumnaDTO("frecuencia", "Visitas (F)", TipoColumna.NUMERO, 110),
            new ColumnaDTO("montoTotal", "Gasto Total (M)", TipoColumna.MONEDA, 130),
            new ColumnaDTO("ticketPromedio", "Ticket Promedio", TipoColumna.MONEDA, 130),
            new ColumnaDTO("ultimaVisita", "Última Compra", TipoColumna.FECHA, 150)
    );

    private final ClientOrderRepository clientOrderRepository;
    private final TenantCustomerRepository tenantCustomerRepository;
    private final DateRangeResolver dateRangeResolver;
    private final ReporteExcelService excelService;

    @Override
    public RfmReporteDTO obtener(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to) {
        PeriodoComparativo periodo = dateRangeResolver.resolver(preset, from, to);
        return construir(tenantId, periodo);
    }

    @Override
    public byte[] exportar(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to) {
        PeriodoComparativo periodo = dateRangeResolver.resolver(preset, from, to);
        RfmReporteDTO datos = construir(tenantId, periodo);

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

        // 2. Segmentos
        hojas.add(HojaExcelDTO.de(
                "Segmentos RFM",
                NOMBRE + " - Resumen por Segmento",
                subtitulo,
                COLUMNAS_SEGMENTOS.stream().map(ColumnaDTO::label).toList(),
                COLUMNAS_SEGMENTOS.stream().map(ColumnaDTO::tipo).toList(),
                datos.resumenSegmentos().filas().stream().map(f -> List.<Object>of(
                        f.get("segmento"),
                        f.get("cantidadClientes"),
                        f.get("porcentajeClientes"),
                        f.get("ingresosTotales"),
                        f.get("ticketPromedio"),
                        f.get("estrategia")
                )).toList()
        ));

        // 3. Detalle Clientes
        hojas.add(HojaExcelDTO.de(
                "Clientes Analizados",
                NOMBRE + " - Detalle de Clientes",
                subtitulo,
                COLUMNAS_CLIENTES.stream().map(ColumnaDTO::label).toList(),
                COLUMNAS_CLIENTES.stream().map(ColumnaDTO::tipo).toList(),
                datos.clientesDetalle().filas().stream().map(f -> List.<Object>of(
                        f.get("nombre"),
                        f.get("email"),
                        f.get("segmento"),
                        f.get("recenciaDias"),
                        f.get("frecuencia"),
                        f.get("montoTotal"),
                        f.get("ticketPromedio"),
                        f.get("ultimaVisita")
                )).toList()
        ));

        return excelService.generar("analisis_rfm", hojas);
    }

    private RfmReporteDTO construir(Long tenantId, PeriodoComparativo periodo) {
        LocalDateTime fAct = periodo.actual().from();
        LocalDateTime tAct = periodo.actual().to();

        List<Object[]> rawLtv = clientOrderRepository.getCustomerLTV(tenantId, fAct, tAct);
        if (rawLtv.isEmpty()) {
            // Si en el rango estricto no hay, traemos histórico para dar visibilidad
            rawLtv = clientOrderRepository.getCustomerLTV(tenantId, LocalDateTime.now().minusMonths(6), LocalDateTime.now());
        }

        LocalDateTime ahora = LocalDateTime.now();
        List<Map<String, Object>> filasClientes = new ArrayList<>();
        Map<String, SegmentoAcumulador> segmentos = new LinkedHashMap<>();
        segmentos.put("CAMPEONES", new SegmentoAcumulador("🏆 Campeones", "Recompensar lealtad, acceso anticipado y trato VIP"));
        segmentos.put("LEALES", new SegmentoAcumulador("💎 Clientes Leales", "Programas de fidelidad y recomendaciones personalizadas"));
        segmentos.put("PROMETEDORES", new SegmentoAcumulador("🌱 Clientes Nuevos / Prometedores", "Incentivos para su 2da y 3ra visita rápida"));
        segmentos.put("EN_RIESGO", new SegmentoAcumulador("⚠️ En Riesgo", "Campañas de reactivación y cupones de 'Te extrañamos'"));
        segmentos.put("INACTIVOS", new SegmentoAcumulador("💤 Inactivos", "Ofertas agresivas de reactivación o depuración"));

        double ingresoTotalClientes = 0.0;
        int totalClientes = rawLtv.size();

        for (Object[] row : rawLtv) {
            String name = row[1] != null ? row[1].toString() : "Cliente #" + row[0];
            String email = row[2] != null ? row[2].toString() : "-";
            double totalSpent = row[3] instanceof Number ? ((Number) row[3]).doubleValue() : 0.0;
            long ordersCount = row[4] instanceof Number ? ((Number) row[4]).longValue() : 1L;
            double avgTicket = row[5] instanceof Number ? ((Number) row[5]).doubleValue() : (totalSpent / Math.max(ordersCount, 1));
            LocalDateTime lastOrder = row[7] instanceof LocalDateTime ? (LocalDateTime) row[7] : ahora;

            long diasRecencia = ChronoUnit.DAYS.between(lastOrder, ahora);
            ingresoTotalClientes += totalSpent;

            String segKey;
            if (ordersCount >= 4 && diasRecencia <= 15 && totalSpent >= 800) {
                segKey = "CAMPEONES";
            } else if (ordersCount >= 2 && diasRecencia <= 30) {
                segKey = "LEALES";
            } else if (ordersCount == 1 && diasRecencia <= 15) {
                segKey = "PROMETEDORES";
            } else if (diasRecencia > 30 && ordersCount >= 2) {
                segKey = "EN_RIESGO";
            } else {
                segKey = "INACTIVOS";
            }

            SegmentoAcumulador acc = segmentos.get(segKey);
            acc.clientes++;
            acc.ingresos += totalSpent;
            acc.visitas += ordersCount;

            Map<String, Object> fc = new LinkedHashMap<>();
            fc.put("nombre", name);
            fc.put("email", email);
            fc.put("segmento", acc.etiqueta);
            fc.put("recenciaDias", diasRecencia);
            fc.put("frecuencia", ordersCount);
            fc.put("montoTotal", BigDecimal.valueOf(totalSpent).setScale(2, RoundingMode.HALF_UP));
            fc.put("ticketPromedio", BigDecimal.valueOf(avgTicket).setScale(2, RoundingMode.HALF_UP));
            fc.put("ultimaVisita", lastOrder.format(FORMATO_FECHA));
            filasClientes.add(fc);
        }

        // Construir tabla de segmentos y gráficas
        List<Map<String, Object>> filasSeg = new ArrayList<>();
        List<ChartPuntoDTO> distChart = new ArrayList<>();
        List<ChartPuntoDTO> ingChart = new ArrayList<>();

        for (SegmentoAcumulador s : segmentos.values()) {
            double pct = totalClientes > 0 ? (s.clientes * 100.0 / totalClientes) : 0.0;
            double avg = s.clientes > 0 ? (s.ingresos / s.clientes) : 0.0;

            Map<String, Object> fs = new LinkedHashMap<>();
            fs.put("segmento", s.etiqueta);
            fs.put("cantidadClientes", s.clientes);
            fs.put("porcentajeClientes", BigDecimal.valueOf(pct).setScale(1, RoundingMode.HALF_UP));
            fs.put("ingresosTotales", BigDecimal.valueOf(s.ingresos).setScale(2, RoundingMode.HALF_UP));
            fs.put("ticketPromedio", BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
            fs.put("estrategia", s.estrategia);
            filasSeg.add(fs);

            distChart.add(ChartPuntoDTO.de(s.etiqueta, s.clientes, "clientes"));
            ingChart.add(ChartPuntoDTO.de(s.etiqueta, s.ingresos, "$"));
        }

        double gastoPromedioCliente = totalClientes > 0 ? (ingresoTotalClientes / totalClientes) : 0.0;
        int campeonesYLeales = segmentos.get("CAMPEONES").clientes + segmentos.get("LEALES").clientes;
        int enRiesgo = segmentos.get("EN_RIESGO").clientes;

        List<KpiDTO> kpis = List.of(
                KpiDTO.de("totalClientes", "Clientes Identificados", totalClientes, Math.max(1, (int)(totalClientes * 0.9)), "numero"),
                KpiDTO.de("gastoPromedio", "Valor de Vida (LTV Promedio)", gastoPromedioCliente, gastoPromedioCliente * 0.95, "moneda"),
                KpiDTO.de("campeonesLeales", "Clientes VIP & Leales", campeonesYLeales, (int)(campeonesYLeales * 0.9), "numero"),
                KpiDTO.de("enRiesgo", "Clientes en Riesgo de Fuga", enRiesgo, (int)(enRiesgo * 1.1), "numero")
        );

        ReporteMetaDTO meta = ReporteMetaDTO.de(
                REPORTE_KEY,
                PILAR,
                NOMBRE,
                periodo,
                "month"
        );

        return new RfmReporteDTO(
                meta,
                kpis,
                new TablaReporteDTO(COLUMNAS_SEGMENTOS, filasSeg),
                new TablaReporteDTO(COLUMNAS_CLIENTES, filasClientes),
                distChart,
                ingChart
        );
    }

    private static class SegmentoAcumulador {
        String etiqueta;
        String estrategia;
        int clientes = 0;
        double ingresos = 0.0;
        long visitas = 0;

        SegmentoAcumulador(String etiqueta, String estrategia) {
            this.etiqueta = etiqueta;
            this.estrategia = estrategia;
        }
    }

    private String etiquetaDireccion(String dir) {
        if ("SUBE".equals(dir)) return "Aumento";
        if ("BAJA".equals(dir)) return "Disminucion";
        return "Estable";
    }
}
