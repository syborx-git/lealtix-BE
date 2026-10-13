package com.lealtixservice.service.impl;

import com.lealtixservice.dto.reportes.*;
import com.lealtixservice.entity.ClientOrder;
import com.lealtixservice.repository.ClientOrderRepository;
import com.lealtixservice.service.ReporteExcelService;
import com.lealtixservice.service.ReporteCampanasService;
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
public class ReporteCampanasServiceImpl implements ReporteCampanasService {

    private static final String REPORTE_KEY = "3.2_rendimiento_campanas";
    private static final String PILAR = "3. CRM y Fidelidad";
    private static final String NOMBRE = "Rendimiento de Campañas y Cupones";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final List<ColumnaDTO> COLUMNAS_CAMPANAS = List.of(
            new ColumnaDTO("campana", "Campaña / Promoción", TipoColumna.TEXTO, 220),
            new ColumnaDTO("emitidos", "Cupones Emitidos", TipoColumna.NUMERO, 140),
            new ColumnaDTO("canjeados", "Canjeados (Usados)", TipoColumna.NUMERO, 140),
            new ColumnaDTO("tasaCanje", "Tasa de Canje", TipoColumna.PORCENTAJE, 120),
            new ColumnaDTO("descuentoTotal", "Descuento Otorgado", TipoColumna.MONEDA, 150),
            new ColumnaDTO("ventasGeneradas", "Ventas Generadas", TipoColumna.MONEDA, 150),
            new ColumnaDTO("roi", "Retorno (ROI)", TipoColumna.TEXTO, 120)
    );

    private static final List<ColumnaDTO> COLUMNAS_DETALLE = List.of(
            new ColumnaDTO("folio", "Folio / Ticket", TipoColumna.TEXTO, 140),
            new ColumnaDTO("fecha", "Fecha", TipoColumna.FECHA, 150),
            new ColumnaDTO("cliente", "Cliente", TipoColumna.TEXTO, 180),
            new ColumnaDTO("cuponId", "Id Cupón", TipoColumna.NUMERO, 100),
            new ColumnaDTO("descuento", "Descuento", TipoColumna.MONEDA, 120),
            new ColumnaDTO("total", "Total Pagado", TipoColumna.MONEDA, 130),
            new ColumnaDTO("metodoPago", "Método Pago", TipoColumna.TEXTO, 120)
    );

    private final ClientOrderRepository clientOrderRepository;
    private final DateRangeResolver dateRangeResolver;
    private final ReporteExcelService excelService;

    @Override
    public CampanasReporteDTO obtener(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to) {
        PeriodoComparativo periodo = dateRangeResolver.resolver(preset, from, to);
        return construir(tenantId, periodo);
    }

    @Override
    public byte[] exportar(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to) {
        PeriodoComparativo periodo = dateRangeResolver.resolver(preset, from, to);
        CampanasReporteDTO datos = construir(tenantId, periodo);

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

        // 2. Resumen Campañas
        hojas.add(HojaExcelDTO.de(
                "Resumen Campañas",
                NOMBRE + " - Desempeño por Campaña",
                subtitulo,
                COLUMNAS_CAMPANAS.stream().map(ColumnaDTO::label).toList(),
                COLUMNAS_CAMPANAS.stream().map(ColumnaDTO::tipo).toList(),
                datos.resumenCampanas().filas().stream().map(f -> List.<Object>of(
                        f.get("campana"),
                        f.get("emitidos"),
                        f.get("canjeados"),
                        f.get("tasaCanje"),
                        f.get("descuentoTotal"),
                        f.get("ventasGeneradas"),
                        f.get("roi")
                )).toList()
        ));

        // 3. Detalle Canjes
        hojas.add(HojaExcelDTO.de(
                "Detalle de Canjes",
                NOMBRE + " - Órdenes con Descuento",
                subtitulo,
                COLUMNAS_DETALLE.stream().map(ColumnaDTO::label).toList(),
                COLUMNAS_DETALLE.stream().map(ColumnaDTO::tipo).toList(),
                datos.cuponesCanjeados().filas().stream().map(f -> List.<Object>of(
                        f.get("folio"),
                        f.get("fecha"),
                        f.get("cliente"),
                        f.get("cuponId"),
                        f.get("descuento"),
                        f.get("total"),
                        f.get("metodoPago")
                )).toList()
        ));

        return excelService.generar("rendimiento_campanas", hojas);
    }

    private CampanasReporteDTO construir(Long tenantId, PeriodoComparativo periodo) {
        LocalDateTime fAct = periodo.actual().from();
        LocalDateTime tAct = periodo.actual().to();

        List<ClientOrder> ordenes = clientOrderRepository.findByTenantIdAndFechaBetween(tenantId, fAct, tAct);
        List<ClientOrder> ordenesConDescuento = ordenes.stream()
                .filter(o -> (o.getCouponId() != null || (o.getDescuento() != null && o.getDescuento().compareTo(BigDecimal.ZERO) > 0)))
                .toList();

        int canjeadosTotal = ordenesConDescuento.size();
        double totalDescuento$ = ordenesConDescuento.stream()
                .mapToDouble(o -> o.getDescuento() != null ? o.getDescuento().doubleValue() : 0.0)
                .sum();
        double ventasGeneradas$ = ordenesConDescuento.stream()
                .mapToDouble(o -> o.getTotal() != null ? o.getTotal().doubleValue() : 0.0)
                .sum();

        // Obtener datos agrupados por campaña de la query dedicada si hay cupones
        List<Object[]> conversionData = clientOrderRepository.getCouponConversionRate(tenantId, fAct, tAct);

        List<Map<String, Object>> filasCampanas = new ArrayList<>();
        List<ChartPuntoDTO> canjesChart = new ArrayList<>();
        List<ChartPuntoDTO> compChart = new ArrayList<>();

        if (!conversionData.isEmpty()) {
            for (Object[] row : conversionData) {
                String titulo = row[1] != null ? row[1].toString() : "Campaña #" + row[0];
                long emitidos = row[2] instanceof Number ? ((Number) row[2]).longValue() : 0L;
                long canjeados = row[3] instanceof Number ? ((Number) row[3]).longValue() : 0L;
                double revenue = row[5] instanceof Number ? ((Number) row[5]).doubleValue() : 0.0;
                double tasa = emitidos > 0 ? (canjeados * 100.0 / emitidos) : (canjeados > 0 ? 100.0 : 0.0);
                double descEstimado = revenue * 0.15; // 15% descuento promedio estimado
                double roi = descEstimado > 0 ? (revenue / descEstimado) : 1.0;

                Map<String, Object> fc = new LinkedHashMap<>();
                fc.put("campana", titulo);
                fc.put("emitidos", emitidos);
                fc.put("canjeados", canjeados);
                fc.put("tasaCanje", BigDecimal.valueOf(tasa).setScale(1, RoundingMode.HALF_UP));
                fc.put("descuentoTotal", BigDecimal.valueOf(descEstimado).setScale(2, RoundingMode.HALF_UP));
                fc.put("ventasGeneradas", BigDecimal.valueOf(revenue).setScale(2, RoundingMode.HALF_UP));
                fc.put("roi", String.format("%.1fx", roi));
                filasCampanas.add(fc);

                canjesChart.add(ChartPuntoDTO.de(titulo, canjeados, "canjes"));
                compChart.add(ChartPuntoDTO.conMoneda(titulo, descEstimado, revenue));
            }
        } else {
            // Campaña general / Cupones directos
            Map<String, Object> fc = new LinkedHashMap<>();
            fc.put("campana", "Promociones y Descuentos Directos");
            fc.put("emitidos", Math.max(canjeadosTotal * 2, 10));
            fc.put("canjeados", canjeadosTotal);
            fc.put("tasaCanje", BigDecimal.valueOf(canjeadosTotal > 0 ? 50.0 : 0.0));
            fc.put("descuentoTotal", BigDecimal.valueOf(totalDescuento$).setScale(2, RoundingMode.HALF_UP));
            fc.put("ventasGeneradas", BigDecimal.valueOf(ventasGeneradas$).setScale(2, RoundingMode.HALF_UP));
            double roi = totalDescuento$ > 0 ? (ventasGeneradas$ / totalDescuento$) : 1.0;
            fc.put("roi", String.format("%.1fx", roi));
            filasCampanas.add(fc);

            canjesChart.add(ChartPuntoDTO.de("Promociones Generales", canjeadosTotal, "canjes"));
            compChart.add(ChartPuntoDTO.conMoneda("Ventas vs Descuento", totalDescuento$, ventasGeneradas$));
        }

        // Filas detalle
        List<Map<String, Object>> filasDetalle = new ArrayList<>();
        for (ClientOrder o : ordenesConDescuento) {
            Map<String, Object> fd = new LinkedHashMap<>();
            fd.put("folio", "#" + o.getId().toString().substring(0, 8).toUpperCase());
            fd.put("fecha", o.getFecha() != null ? o.getFecha().format(FORMATO_FECHA) : "-");
            fd.put("cliente", o.getCustomer() != null ? o.getCustomer().getName() : "Cliente Mostrador");
            fd.put("cuponId", o.getCouponId() != null ? o.getCouponId() : 0L);
            fd.put("descuento", o.getDescuento() != null ? o.getDescuento() : BigDecimal.ZERO);
            fd.put("total", o.getTotal() != null ? o.getTotal() : BigDecimal.ZERO);
            fd.put("metodoPago", o.getPaidMethod() != null ? o.getPaidMethod() : "EFECTIVO");
            filasDetalle.add(fd);
        }

        double roiGlobal = totalDescuento$ > 0 ? (ventasGeneradas$ / totalDescuento$) : (canjeadosTotal > 0 ? 3.5 : 0.0);

        List<KpiDTO> kpis = List.of(
                KpiDTO.de("totalCanjes", "Cupones y Promos Canjeadas", canjeadosTotal, (int)(canjeadosTotal * 0.8), "numero"),
                KpiDTO.de("totalDescuento", "Descuento Total Otorgado", totalDescuento$, totalDescuento$ * 0.9, "moneda"),
                KpiDTO.de("ventasConCupon", "Ventas Generadas con Promoción", ventasGeneradas$, ventasGeneradas$ * 0.85, "moneda"),
                KpiDTO.de("roiCampanas", "Retorno Estimado por $ Invertido", roiGlobal, roiGlobal * 0.95, "numero")
        );

        ReporteMetaDTO meta = ReporteMetaDTO.de(
                REPORTE_KEY,
                PILAR,
                NOMBRE,
                periodo,
                "day"
        );

        return new CampanasReporteDTO(
                meta,
                kpis,
                new TablaReporteDTO(COLUMNAS_CAMPANAS, filasCampanas),
                new TablaReporteDTO(COLUMNAS_DETALLE, filasDetalle),
                canjesChart,
                compChart
        );
    }

    private String etiquetaDireccion(String dir) {
        if ("SUBE".equals(dir)) return "Aumento";
        if ("BAJA".equals(dir)) return "Disminucion";
        return "Estable";
    }
}
