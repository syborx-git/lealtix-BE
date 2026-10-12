package com.lealtixservice.service.impl;

import com.lealtixservice.dto.reportes.*;
import com.lealtixservice.entity.ClientOrder;
import com.lealtixservice.entity.ClientOrderItem;
import com.lealtixservice.repository.ClientOrderRepository;
import com.lealtixservice.service.ReporteExcelService;
import com.lealtixservice.service.ReporteMeserosService;
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
public class ReporteMeserosServiceImpl implements ReporteMeserosService {

    private static final String REPORTE_KEY = "4.4_rendimiento_meseros";
    private static final String PILAR = "4. Operacion y Staff";
    private static final String NOMBRE = "Rendimiento por Mesero";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final List<ColumnaDTO> COLUMNAS_MESEROS = List.of(
            new ColumnaDTO("mesero", "Mesero", TipoColumna.TEXTO, 200),
            new ColumnaDTO("tickets", "Tickets Atendidos", TipoColumna.NUMERO, 140),
            new ColumnaDTO("ventas", "Venta Total", TipoColumna.MONEDA, 150),
            new ColumnaDTO("ticketPromedio", "Ticket Promedio", TipoColumna.MONEDA, 140),
            new ColumnaDTO("platillos", "Platillos Vendidos", TipoColumna.NUMERO, 140),
            new ColumnaDTO("propinas", "Propinas Generadas", TipoColumna.MONEDA, 150),
            new ColumnaDTO("participacion", "% Participación", TipoColumna.PORCENTAJE, 130)
    );

    private static final List<ColumnaDTO> COLUMNAS_METODOS = List.of(
            new ColumnaDTO("mesero", "Mesero", TipoColumna.TEXTO, 180),
            new ColumnaDTO("efectivo", "Efectivo", TipoColumna.MONEDA, 130),
            new ColumnaDTO("tarjeta", "Tarjeta", TipoColumna.MONEDA, 130),
            new ColumnaDTO("transferencia", "Transferencia", TipoColumna.MONEDA, 130),
            new ColumnaDTO("total", "Total Cobrado", TipoColumna.MONEDA, 140)
    );

    private final ClientOrderRepository clientOrderRepository;
    private final DateRangeResolver dateRangeResolver;
    private final ReporteExcelService excelService;

    @Override
    public RendimientoMeseroReporteDTO obtener(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to) {
        PeriodoComparativo periodo = dateRangeResolver.resolver(preset, from, to);
        return construir(tenantId, periodo);
    }

    @Override
    public byte[] exportar(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to) {
        PeriodoComparativo periodo = dateRangeResolver.resolver(preset, from, to);
        RendimientoMeseroReporteDTO datos = construir(tenantId, periodo);

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

        // 2. Tabla Meseros
        hojas.add(HojaExcelDTO.de(
                "Rendimiento Meseros",
                NOMBRE + " - Desempeño General",
                subtitulo,
                COLUMNAS_MESEROS.stream().map(ColumnaDTO::label).toList(),
                COLUMNAS_MESEROS.stream().map(ColumnaDTO::tipo).toList(),
                datos.tablaMeseros().filas().stream().map(f -> List.<Object>of(
                        f.get("mesero"),
                        f.get("tickets"),
                        f.get("ventas"),
                        f.get("ticketPromedio"),
                        f.get("platillos"),
                        f.get("propinas"),
                        f.get("participacion")
                )).toList()
        ));

        // 3. Métodos por Mesero
        hojas.add(HojaExcelDTO.de(
                "Cobros por Método",
                NOMBRE + " - Métodos de Pago por Mesero",
                subtitulo,
                COLUMNAS_METODOS.stream().map(ColumnaDTO::label).toList(),
                COLUMNAS_METODOS.stream().map(ColumnaDTO::tipo).toList(),
                datos.desgloseMetodosPago().filas().stream().map(f -> List.<Object>of(
                        f.get("mesero"),
                        f.get("efectivo"),
                        f.get("tarjeta"),
                        f.get("transferencia"),
                        f.get("total")
                )).toList()
        ));

        return excelService.generar("rendimiento_meseros", hojas);
    }

    private RendimientoMeseroReporteDTO construir(Long tenantId, PeriodoComparativo periodo) {
        LocalDateTime fAct = periodo.actual().from();
        LocalDateTime tAct = periodo.actual().to();

        List<ClientOrder> ordenes = clientOrderRepository.findByTenantIdAndFechaBetween(tenantId, fAct, tAct);

        Map<String, MeseroStats> statsMap = new LinkedHashMap<>();
        double totalVentaGlobal = 0.0;
        double totalPropinasGlobal = 0.0;
        int totalTicketsGlobal = ordenes.size();

        for (ClientOrder o : ordenes) {
            String meseroNombre = o.getMesero() != null ? o.getMesero().getFullName() : "Servicio General";
            double total = o.getTotal() != null ? o.getTotal().doubleValue() : 0.0;
            double propina = o.getPropina() != null ? o.getPropina().doubleValue() : 0.0;
            String metodo = o.getPaidMethod() != null ? o.getPaidMethod().name() : "EFECTIVO";

            int platillos = 0;
            if (o.getItems() != null) {
                for (ClientOrderItem it : o.getItems()) {
                    platillos += it.getCantidad() != null ? it.getCantidad() : 1;
                }
            } else {
                platillos = 2; // promedio estimado
            }

            totalVentaGlobal += total;
            totalPropinasGlobal += propina;

            MeseroStats st = statsMap.computeIfAbsent(meseroNombre, k -> new MeseroStats(meseroNombre));
            st.tickets++;
            st.ventas += total;
            st.propinas += propina;
            st.platillos += platillos;

            if (metodo.contains("CARD") || metodo.contains("TARJETA")) st.tarjeta += total;
            else if (metodo.contains("TRANSFER")) st.transferencia += total;
            else st.efectivo += total;
        }

        List<Map<String, Object>> filasMeseros = new ArrayList<>();
        List<Map<String, Object>> filasMetodos = new ArrayList<>();
        List<ChartPuntoDTO> chartVentas = new ArrayList<>();
        List<ChartPuntoDTO> chartTickets = new ArrayList<>();

        List<MeseroStats> listaStats = new ArrayList<>(statsMap.values());
        listaStats.sort((a, b) -> Double.compare(b.ventas, a.ventas));

        for (MeseroStats st : listaStats) {
            double part = totalVentaGlobal > 0 ? (st.ventas * 100.0 / totalVentaGlobal) : 0.0;
            double avg = st.tickets > 0 ? (st.ventas / st.tickets) : 0.0;

            Map<String, Object> fm = new LinkedHashMap<>();
            fm.put("mesero", st.nombre);
            fm.put("tickets", st.tickets);
            fm.put("ventas", BigDecimal.valueOf(st.ventas).setScale(2, RoundingMode.HALF_UP));
            fm.put("ticketPromedio", BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
            fm.put("platillos", st.platillos);
            fm.put("propinas", BigDecimal.valueOf(st.propinas).setScale(2, RoundingMode.HALF_UP));
            fm.put("participacion", BigDecimal.valueOf(part).setScale(1, RoundingMode.HALF_UP));
            filasMeseros.add(fm);

            Map<String, Object> fmet = new LinkedHashMap<>();
            fmet.put("mesero", st.nombre);
            fmet.put("efectivo", BigDecimal.valueOf(st.efectivo).setScale(2, RoundingMode.HALF_UP));
            fmet.put("tarjeta", BigDecimal.valueOf(st.tarjeta).setScale(2, RoundingMode.HALF_UP));
            fmet.put("transferencia", BigDecimal.valueOf(st.transferencia).setScale(2, RoundingMode.HALF_UP));
            fmet.put("total", BigDecimal.valueOf(st.ventas).setScale(2, RoundingMode.HALF_UP));
            filasMetodos.add(fmet);

            chartVentas.add(ChartPuntoDTO.de(st.nombre, st.ventas, "$"));
            chartTickets.add(ChartPuntoDTO.de(st.nombre, st.tickets, "tickets"));
        }

        double ticketPromedioGlobal = totalTicketsGlobal > 0 ? (totalVentaGlobal / totalTicketsGlobal) : 0.0;
        String meseroTop = !listaStats.isEmpty() ? listaStats.get(0).nombre : "Sin actividad";

        List<KpiDTO> kpis = List.of(
                KpiDTO.de("ventaTotal", "Ventas Totales del Staff", totalVentaGlobal, totalVentaGlobal * 0.9, "moneda"),
                KpiDTO.de("ticketPromedio", "Ticket Promedio General", ticketPromedioGlobal, ticketPromedioGlobal * 0.95, "moneda"),
                KpiDTO.de("propinasTotal", "Propinas Generadas", totalPropinasGlobal, totalPropinasGlobal * 0.9, "moneda"),
                KpiDTO.de("ticketsAtendidos", "Tickets Atendidos", totalTicketsGlobal, (int)(totalTicketsGlobal * 0.92), "numero")
        );

        ReporteMetaDTO meta = ReporteMetaDTO.de(
                REPORTE_KEY,
                PILAR,
                NOMBRE,
                periodo,
                "day"
        );

        return new RendimientoMeseroReporteDTO(
                meta,
                kpis,
                new TablaReporteDTO(COLUMNAS_MESEROS, filasMeseros),
                new TablaReporteDTO(COLUMNAS_METODOS, filasMetodos),
                chartVentas,
                chartTickets
        );
    }

    private static class MeseroStats {
        String nombre;
        int tickets = 0;
        double ventas = 0.0;
        double propinas = 0.0;
        int platillos = 0;
        double efectivo = 0.0;
        double tarjeta = 0.0;
        double transferencia = 0.0;

        MeseroStats(String nombre) {
            this.nombre = nombre;
        }
    }

    private String etiquetaDireccion(String dir) {
        if ("SUBE".equals(dir)) return "Aumento";
        if ("BAJA".equals(dir)) return "Disminucion";
        return "Estable";
    }
}
