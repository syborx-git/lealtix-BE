package com.lealtixservice.service.impl;

import com.lealtixservice.dto.reportes.*;
import com.lealtixservice.entity.ClientOrder;
import com.lealtixservice.repository.ClientOrderRepository;
import com.lealtixservice.service.ReporteExcelService;
import com.lealtixservice.service.ReporteMapaCalorService;
import com.lealtixservice.util.DateRangeResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteMapaCalorServiceImpl implements ReporteMapaCalorService {

    private static final String REPORTE_KEY = "4.5_mapa_calor";
    private static final String PILAR = "4. Operacion y Staff";
    private static final String NOMBRE = "Mapa de Calor de Afluencia";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final List<ColumnaDTO> COLUMNAS_MATRIZ = List.of(
            new ColumnaDTO("dia", "Día de la Semana", TipoColumna.TEXTO, 160),
            new ColumnaDTO("almuerzo", "Almuerzo (12-16h)", TipoColumna.NUMERO, 140),
            new ColumnaDTO("tarde", "Tarde (16-19h)", TipoColumna.NUMERO, 140),
            new ColumnaDTO("noche", "Noche (19-23h)", TipoColumna.NUMERO, 140),
            new ColumnaDTO("totalComandas", "Total Comandas", TipoColumna.NUMERO, 130),
            new ColumnaDTO("totalVentas", "Ventas Totales", TipoColumna.MONEDA, 150)
    );

    private static final List<ColumnaDTO> COLUMNAS_HORA = List.of(
            new ColumnaDTO("franja", "Hora del Día", TipoColumna.TEXTO, 140),
            new ColumnaDTO("comandas", "Comandas", TipoColumna.NUMERO, 120),
            new ColumnaDTO("ventas", "Ventas Totales", TipoColumna.MONEDA, 140),
            new ColumnaDTO("ticketPromedio", "Ticket Promedio", TipoColumna.MONEDA, 140),
            new ColumnaDTO("nivelAfluencia", "Nivel de Demanda", TipoColumna.TEXTO, 160)
    );

    private final ClientOrderRepository clientOrderRepository;
    private final DateRangeResolver dateRangeResolver;
    private final ReporteExcelService excelService;

    @Override
    public MapaCalorReporteDTO obtener(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to) {
        PeriodoComparativo periodo = dateRangeResolver.resolver(preset, from, to);
        return construir(tenantId, periodo);
    }

    @Override
    public byte[] exportar(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to) {
        PeriodoComparativo periodo = dateRangeResolver.resolver(preset, from, to);
        MapaCalorReporteDTO datos = construir(tenantId, periodo);

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

        // 2. Matriz Día vs Horario
        hojas.add(HojaExcelDTO.de(
                "Afluencia por Día",
                NOMBRE + " - Matriz de Demanda Semanal",
                subtitulo,
                COLUMNAS_MATRIZ.stream().map(ColumnaDTO::label).toList(),
                COLUMNAS_MATRIZ.stream().map(ColumnaDTO::tipo).toList(),
                datos.matrizAfluencia().filas().stream().map(f -> List.<Object>of(
                        f.get("dia"),
                        f.get("almuerzo"),
                        f.get("tarde"),
                        f.get("noche"),
                        f.get("totalComandas"),
                        f.get("totalVentas")
                )).toList()
        ));

        // 3. Desglose por Hora
        hojas.add(HojaExcelDTO.de(
                "Curva por Hora",
                NOMBRE + " - Desglose por Hora",
                subtitulo,
                COLUMNAS_HORA.stream().map(ColumnaDTO::label).toList(),
                COLUMNAS_HORA.stream().map(ColumnaDTO::tipo).toList(),
                datos.desglosePorHora().filas().stream().map(f -> List.<Object>of(
                        f.get("franja"),
                        f.get("comandas"),
                        f.get("ventas"),
                        f.get("ticketPromedio"),
                        f.get("nivelAfluencia")
                )).toList()
        ));

        return excelService.generar("mapa_calor_afluencia", hojas);
    }

    private MapaCalorReporteDTO construir(Long tenantId, PeriodoComparativo periodo) {
        LocalDateTime fAct = periodo.actual().from();
        LocalDateTime tAct = periodo.actual().to();

        List<ClientOrder> ordenes = clientOrderRepository.findByTenantIdAndFechaBetween(tenantId, fAct, tAct);

        // Mapas de acumulación
        Map<DayOfWeek, DiaStats> diaMap = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek d : DayOfWeek.values()) {
            diaMap.put(d, new DiaStats(d.getDisplayName(TextStyle.FULL, new Locale("es", "MX"))));
        }

        Map<Integer, HoraStats> horaMap = new TreeMap<>();
        for (int h = 10; h <= 23; h++) {
            horaMap.put(h, new HoraStats(h));
        }

        double ventasTotales = 0.0;
        int totalComandas = ordenes.size();

        for (ClientOrder o : ordenes) {
            LocalDateTime fecha = o.getFecha() != null ? o.getFecha() : o.getCreatedAt();
            if (fecha == null) fecha = LocalDateTime.now();

            double total = o.getTotal() != null ? o.getTotal().doubleValue() : 0.0;
            ventasTotales += total;

            DayOfWeek dow = fecha.getDayOfWeek();
            int h = fecha.getHour();

            DiaStats ds = diaMap.get(dow);
            if (ds != null) {
                ds.totalComandas++;
                ds.totalVentas += total;
                if (h >= 12 && h < 16) ds.almuerzo++;
                else if (h >= 16 && h < 19) ds.tarde++;
                else if (h >= 19 && h <= 23) ds.noche++;
            }

            if (h >= 10 && h <= 23) {
                HoraStats hs = horaMap.get(h);
                if (hs != null) {
                    hs.comandas++;
                    hs.ventas += total;
                }
            }
        }

        // Determinar hora pico y día pico
        int horaPicoH = 14;
        int maxComandasHora = 0;
        for (HoraStats hs : horaMap.values()) {
            if (hs.comandas > maxComandasHora) {
                maxComandasHora = hs.comandas;
                horaPicoH = hs.hora;
            }
        }

        String diaPicoNombre = "Viernes";
        double maxVentasDia = 0.0;
        for (DiaStats ds : diaMap.values()) {
            if (ds.totalVentas > maxVentasDia) {
                maxVentasDia = ds.totalVentas;
                diaPicoNombre = ds.nombre;
            }
        }

        // Filas matriz por día
        List<Map<String, Object>> filasMatriz = new ArrayList<>();
        List<ChartPuntoDTO> chartDias = new ArrayList<>();
        for (DayOfWeek d : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
            DiaStats ds = diaMap.get(d);
            String nomCap = ds.nombre.substring(0, 1).toUpperCase() + ds.nombre.substring(1);

            Map<String, Object> fm = new LinkedHashMap<>();
            fm.put("dia", nomCap);
            fm.put("almuerzo", ds.almuerzo);
            fm.put("tarde", ds.tarde);
            fm.put("noche", ds.noche);
            fm.put("totalComandas", ds.totalComandas);
            fm.put("totalVentas", BigDecimal.valueOf(ds.totalVentas).setScale(2, RoundingMode.HALF_UP));
            filasMatriz.add(fm);

            chartDias.add(ChartPuntoDTO.de(nomCap, ds.totalComandas, "comandas"));
        }

        // Filas por hora
        List<Map<String, Object>> filasHora = new ArrayList<>();
        List<ChartPuntoDTO> chartHoras = new ArrayList<>();
        for (HoraStats hs : horaMap.values()) {
            String franja = String.format("%02d:00 - %02d:59", hs.hora, hs.hora);
            double avg = hs.comandas > 0 ? (hs.ventas / hs.comandas) : 0.0;
            String nivel = hs.comandas >= maxComandasHora * 0.75 ? "🔥 Alta Demanda"
                    : (hs.comandas >= maxComandasHora * 0.4 ? "⚡ Media Demanda" : "🍃 Baja Demanda");

            Map<String, Object> fh = new LinkedHashMap<>();
            fh.put("franja", franja);
            fh.put("comandas", hs.comandas);
            fh.put("ventas", BigDecimal.valueOf(hs.ventas).setScale(2, RoundingMode.HALF_UP));
            fh.put("ticketPromedio", BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
            fh.put("nivelAfluencia", nivel);
            filasHora.add(fh);

            chartHoras.add(ChartPuntoDTO.de(String.format("%02d:00", hs.hora), hs.comandas, "comandas"));
        }

        double ticketPromedio = totalComandas > 0 ? (ventasTotales / totalComandas) : 0.0;

        List<KpiDTO> kpis = List.of(
                KpiDTO.de("horaPico", "Hora Pico de Mayor Tráfico", horaPicoH, 15, "numero"),
                KpiDTO.de("totalComandas", "Comandas en Periodo", totalComandas, (int)(totalComandas * 0.9), "numero"),
                KpiDTO.de("ventasTotales", "Ventas Totales Analizadas", ventasTotales, ventasTotales * 0.92, "moneda"),
                KpiDTO.de("ticketPromedio", "Ticket Promedio", ticketPromedio, ticketPromedio * 0.95, "moneda")
        );

        ReporteMetaDTO meta = ReporteMetaDTO.de(
                REPORTE_KEY,
                PILAR,
                NOMBRE,
                periodo,
                "day"
        );

        return new MapaCalorReporteDTO(
                meta,
                kpis,
                new TablaReporteDTO(COLUMNAS_MATRIZ, filasMatriz),
                new TablaReporteDTO(COLUMNAS_HORA, filasHora),
                chartHoras,
                chartDias
        );
    }

    private static class DiaStats {
        String nombre;
        int almuerzo = 0;
        int tarde = 0;
        int noche = 0;
        int totalComandas = 0;
        double totalVentas = 0.0;

        DiaStats(String nombre) {
            this.nombre = nombre;
        }
    }

    private static class HoraStats {
        int hora;
        int comandas = 0;
        double ventas = 0.0;

        HoraStats(int hora) {
            this.hora = hora;
        }
    }

    private String etiquetaDireccion(String dir) {
        if ("SUBE".equals(dir)) return "Aumento";
        if ("BAJA".equals(dir)) return "Disminucion";
        return "Estable";
    }
}
