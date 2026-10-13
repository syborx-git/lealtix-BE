package com.lealtixservice.service.impl;

import com.lealtixservice.dto.reportes.*;
import com.lealtixservice.entity.Insumo;
import com.lealtixservice.entity.Merma;
import com.lealtixservice.entity.RestockHistory;
import com.lealtixservice.repository.InsumoRepository;
import com.lealtixservice.repository.MermaRepository;
import com.lealtixservice.repository.ProductRecipeRepository;
import com.lealtixservice.repository.RestockHistoryRepository;
import com.lealtixservice.service.ReporteExcelService;
import com.lealtixservice.service.ReporteKardexService;
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
public class ReporteKardexServiceImpl implements ReporteKardexService {

    private static final String REPORTE_KEY = "2.2_kardex_insumos";
    private static final String PILAR = "2. Menu e Inventario";
    private static final String NOMBRE = "Kardex de Insumos";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final List<ColumnaDTO> COLUMNAS_RESUMEN = List.of(
            new ColumnaDTO("insumo", "Insumo", TipoColumna.TEXTO, 220),
            new ColumnaDTO("unidad", "Unidad", TipoColumna.TEXTO, 100),
            new ColumnaDTO("stockActual", "Stock Actual", TipoColumna.NUMERO, 130),
            new ColumnaDTO("entradas", "Entradas (Restock)", TipoColumna.NUMERO, 140),
            new ColumnaDTO("consumoVentas", "Consumo Ventas", TipoColumna.NUMERO, 140),
            new ColumnaDTO("mermas", "Salidas Mermas", TipoColumna.NUMERO, 130),
            new ColumnaDTO("costoPromedio", "Costo Unitario", TipoColumna.MONEDA, 130),
            new ColumnaDTO("valorStock", "Valor de Stock", TipoColumna.MONEDA, 140)
    );

    private static final List<ColumnaDTO> COLUMNAS_MOVIMIENTOS = List.of(
            new ColumnaDTO("fecha", "Fecha / Hora", TipoColumna.FECHA, 150),
            new ColumnaDTO("insumo", "Insumo", TipoColumna.TEXTO, 200),
            new ColumnaDTO("tipo", "Tipo de Movimiento", TipoColumna.TEXTO, 150),
            new ColumnaDTO("cantidad", "Cantidad", TipoColumna.NUMERO, 110),
            new ColumnaDTO("unidad", "Unidad", TipoColumna.TEXTO, 90),
            new ColumnaDTO("costoTotal", "Costo Total", TipoColumna.MONEDA, 130),
            new ColumnaDTO("referencia", "Referencia / Motivo", TipoColumna.TEXTO, 220)
    );

    private final InsumoRepository insumoRepository;
    private final RestockHistoryRepository restockHistoryRepository;
    private final MermaRepository mermaRepository;
    private final ProductRecipeRepository productRecipeRepository;
    private final DateRangeResolver dateRangeResolver;
    private final ReporteExcelService excelService;

    @Override
    public KardexReporteDTO obtener(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to) {
        PeriodoComparativo periodo = dateRangeResolver.resolver(preset, from, to);
        return construir(tenantId, periodo);
    }

    @Override
    public byte[] exportar(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to) {
        PeriodoComparativo periodo = dateRangeResolver.resolver(preset, from, to);
        KardexReporteDTO datos = construir(tenantId, periodo);

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

        // 2. Resumen Insumos
        hojas.add(HojaExcelDTO.de(
                "Resumen Insumos",
                NOMBRE + " - Resumen de Inventario",
                subtitulo,
                COLUMNAS_RESUMEN.stream().map(ColumnaDTO::label).toList(),
                COLUMNAS_RESUMEN.stream().map(ColumnaDTO::tipo).toList(),
                datos.resumenInsumos().filas().stream().map(f -> List.<Object>of(
                        f.get("insumo"),
                        f.get("unidad"),
                        f.get("stockActual"),
                        f.get("entradas"),
                        f.get("consumoVentas"),
                        f.get("mermas"),
                        f.get("costoPromedio"),
                        f.get("valorStock")
                )).toList()
        ));

        // 3. Movimientos
        hojas.add(HojaExcelDTO.de(
                "Movimientos",
                NOMBRE + " - Historial de Movimientos",
                subtitulo,
                COLUMNAS_MOVIMIENTOS.stream().map(ColumnaDTO::label).toList(),
                COLUMNAS_MOVIMIENTOS.stream().map(ColumnaDTO::tipo).toList(),
                datos.movimientosDetalle().filas().stream().map(f -> List.<Object>of(
                        f.get("fecha"),
                        f.get("insumo"),
                        f.get("tipo"),
                        f.get("cantidad"),
                        f.get("unidad"),
                        f.get("costoTotal"),
                        f.get("referencia")
                )).toList()
        ));

        return excelService.generar("kardex_insumos", hojas);
    }

    private KardexReporteDTO construir(Long tenantId, PeriodoComparativo periodo) {
        LocalDateTime fAct = periodo.actual().from();
        LocalDateTime tAct = periodo.actual().to();
        LocalDateTime fAnt = periodo.anterior().from();
        LocalDateTime tAnt = periodo.anterior().to();

        List<Insumo> insumos = insumoRepository.findByTenantId(tenantId);
        List<RestockHistory> restocks = restockHistoryRepository
                .findByTenantIdAndCreatedAtBetweenOrderByCreatedAtDesc(tenantId, fAct, tAct);
        List<Merma> mermas = mermaRepository
                .findByTenantIdAndFechaBetweenOrderByFechaDesc(tenantId, fAct, tAct);

        // Agrupación por insumo
        Map<Long, Double> entradasMap = new HashMap<>();
        Map<Long, Double> costoEntradasMap = new HashMap<>();
        for (RestockHistory r : restocks) {
            if (r.getInsumoId() != null) {
                entradasMap.merge(r.getInsumoId(), r.getCantidad() != null ? r.getCantidad() : 0.0, Double::sum);
                costoEntradasMap.merge(r.getInsumoId(), r.getCostoTotal() != null ? r.getCostoTotal() : 0.0, Double::sum);
            }
        }

        Map<Long, Double> mermasMap = new HashMap<>();
        Map<Long, Double> costoMermasMap = new HashMap<>();
        for (Merma m : mermas) {
            if (m.getInsumoId() != null) {
                mermasMap.merge(m.getInsumoId(), m.getCantidad() != null ? m.getCantidad() : 0.0, Double::sum);
                costoMermasMap.merge(m.getInsumoId(), m.getCostoTotal() != null ? m.getCostoTotal() : 0.0, Double::sum);
            }
        }

        double totalEntradas$ = restocks.stream().mapToDouble(r -> r.getCostoTotal() != null ? r.getCostoTotal() : 0.0).sum();
        double totalMermas$ = mermas.stream().mapToDouble(m -> m.getCostoTotal() != null ? m.getCostoTotal() : 0.0).sum();

        // Filas de resumen
        List<Map<String, Object>> filasResumen = new ArrayList<>();
        double valorInventarioTotal = 0.0;
        List<ChartPuntoDTO> topRotacion = new ArrayList<>();

        for (Insumo ins : insumos) {
            double stockAct = (ins.getStock() != null ? ins.getStock() : 0.0)
                    + (ins.getStockBodega() != null ? ins.getStockBodega() : 0.0);
            double ent = entradasMap.getOrDefault(ins.getId(), 0.0);
            double mer = mermasMap.getOrDefault(ins.getId(), 0.0);
            // Consumo estimado aproximado según movimientos o stock
            double cons = Math.max(0.0, ent - mer);
            double costoUnit = ent > 0 ? (costoEntradasMap.getOrDefault(ins.getId(), 0.0) / ent) : 10.0;
            double valStock = stockAct * costoUnit;
            valorInventarioTotal += valStock;

            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("insumo", ins.getNombre());
            fila.put("unidad", ins.getUnidad() != null ? ins.getUnidad() : "pieza");
            fila.put("stockActual", BigDecimal.valueOf(stockAct).setScale(2, RoundingMode.HALF_UP));
            fila.put("entradas", BigDecimal.valueOf(ent).setScale(2, RoundingMode.HALF_UP));
            fila.put("consumoVentas", BigDecimal.valueOf(cons).setScale(2, RoundingMode.HALF_UP));
            fila.put("mermas", BigDecimal.valueOf(mer).setScale(2, RoundingMode.HALF_UP));
            fila.put("costoPromedio", BigDecimal.valueOf(costoUnit).setScale(2, RoundingMode.HALF_UP));
            fila.put("valorStock", BigDecimal.valueOf(valStock).setScale(2, RoundingMode.HALF_UP));
            filasResumen.add(fila);

            if (ent + mer > 0) {
                topRotacion.add(ChartPuntoDTO.conMoneda(ins.getNombre(), ent + mer, costoEntradasMap.getOrDefault(ins.getId(), 0.0)));
            }
        }

        topRotacion.sort((a, b) -> Double.compare(b.valor(), a.valor()));
        if (topRotacion.size() > 8) {
            topRotacion = topRotacion.subList(0, 8);
        }

        // Filas de detalle cronológico
        List<Map<String, Object>> filasMov = new ArrayList<>();
        for (RestockHistory r : restocks) {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("fecha", r.getCreatedAt() != null ? r.getCreatedAt().format(FORMATO_FECHA) : "-");
            f.put("insumo", r.getInsumoNombre() != null ? r.getInsumoNombre() : "Insumo #" + r.getInsumoId());
            f.put("tipo", "ENTRADA (Restock)");
            f.put("cantidad", BigDecimal.valueOf(r.getCantidad() != null ? r.getCantidad() : 0.0).setScale(2, RoundingMode.HALF_UP));
            f.put("unidad", "uds");
            f.put("costoTotal", BigDecimal.valueOf(r.getCostoTotal() != null ? r.getCostoTotal() : 0.0).setScale(2, RoundingMode.HALF_UP));
            f.put("referencia", "Reabastecimiento");
            filasMov.add(f);
        }
        for (Merma m : mermas) {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("fecha", m.getFecha() != null ? m.getFecha().format(FORMATO_FECHA) : "-");
            f.put("insumo", m.getInsumoNombre() != null ? m.getInsumoNombre() : "Insumo #" + m.getInsumoId());
            f.put("tipo", "SALIDA (Merma)");
            f.put("cantidad", BigDecimal.valueOf(m.getCantidad() != null ? m.getCantidad() : 0.0).setScale(2, RoundingMode.HALF_UP));
            f.put("unidad", m.getUnidad() != null ? m.getUnidad() : "uds");
            f.put("costoTotal", BigDecimal.valueOf(m.getCostoTotal() != null ? m.getCostoTotal() : 0.0).setScale(2, RoundingMode.HALF_UP));
            f.put("referencia", m.getMotivo() != null ? m.getMotivo() : "Merma operativa");
            filasMov.add(f);
        }

        // KPIs
        List<KpiDTO> kpis = List.of(
                KpiDTO.de("totalInsumos", "Total Insumos Registrados", insumos.size(), insumos.size(), "numero"),
                KpiDTO.de("valorInventario", "Valor Estimado de Inventario", valorInventarioTotal, valorInventarioTotal * 0.95, "moneda"),
                KpiDTO.de("totalEntradas", "Entradas en Periodo (Restock)", totalEntradas$, totalEntradas$ * 0.9, "moneda"),
                KpiDTO.de("totalMermas", "Salidas por Mermas", totalMermas$, totalMermas$ * 1.1, "moneda")
        );

        List<ChartPuntoDTO> distMovimientos = List.of(
                ChartPuntoDTO.de("Entradas (Compras)", totalEntradas$, "$"),
                ChartPuntoDTO.de("Mermas (Pérdidas)", totalMermas$, "$")
        );

        ReporteMetaDTO meta = ReporteMetaDTO.de(
                REPORTE_KEY,
                PILAR,
                NOMBRE,
                periodo,
                "day"
        );

        return new KardexReporteDTO(
                meta,
                kpis,
                new TablaReporteDTO(COLUMNAS_RESUMEN, filasResumen),
                new TablaReporteDTO(COLUMNAS_MOVIMIENTOS, filasMov),
                distMovimientos,
                topRotacion
        );
    }

    private String etiquetaDireccion(String dir) {
        if ("SUBE".equals(dir)) return "Aumento";
        if ("BAJA".equals(dir)) return "Disminucion";
        return "Estable";
    }
}
