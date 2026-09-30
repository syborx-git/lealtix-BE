package com.lealtixservice.service.impl;

import com.lealtixservice.dto.reportes.ColumnaDTO;
import com.lealtixservice.dto.reportes.HojaExcelDTO;
import com.lealtixservice.dto.reportes.KpiDTO;
import com.lealtixservice.dto.reportes.PeriodoComparativo;
import com.lealtixservice.dto.reportes.PresetReporte;
import com.lealtixservice.dto.reportes.ReporteMetaDTO;
import com.lealtixservice.dto.reportes.SeriePuntoDTO;
import com.lealtixservice.dto.reportes.TablaReporteDTO;
import com.lealtixservice.dto.reportes.TipoColumna;
import com.lealtixservice.dto.reportes.VentasTendenciasDTO;
import com.lealtixservice.repository.ReporteVentasRepository;
import com.lealtixservice.service.ReporteExcelService;
import com.lealtixservice.service.ReporteVentasService;
import com.lealtixservice.util.DateRangeResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteVentasServiceImpl implements ReporteVentasService {

    private static final String REPORTE_KEY = "ventas_tendencias";
    private static final String PILAR = "P1_Finanzas_y_Ventas";
    private static final String NOMBRE = "Dashboard de Ventas y Tendencias";
    private static final int TOP_PRODUCTOS = 20;

    private static final String[] MESES = {
            "ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"
    };

    private final ReporteVentasRepository repository;
    private final DateRangeResolver dateRangeResolver;
    private final ReporteExcelService excelService;

    @Override
    public VentasTendenciasDTO obtener(Long tenantId, PresetReporte preset,
                                       LocalDateTime from, LocalDateTime to, String granularidad) {

        PeriodoComparativo periodo = dateRangeResolver.resolver(preset, from, to);
        String gran = dateRangeResolver.normalizarGranularidad(granularidad, periodo.actual());

        Object[] actual = resumen(tenantId, periodo.actual().from(), dateRangeResolver.aExclusivo(periodo.actual().to()));
        Object[] anterior = resumen(tenantId, periodo.anterior().from(), dateRangeResolver.aExclusivo(periodo.anterior().to()));

        ReporteMetaDTO meta = ReporteMetaDTO.de(REPORTE_KEY, PILAR, NOMBRE, periodo, gran);

        return new VentasTendenciasDTO(
                meta,
                construirKpis(actual, anterior),
                construirSerie(tenantId, periodo, gran),
                construirTablaPorCategoria(tenantId, periodo),
                construirTablaTopProductos(tenantId, periodo)
        );
    }

    @Override
    public byte[] exportar(Long tenantId, PresetReporte preset,
                           LocalDateTime from, LocalDateTime to, String granularidad) {

        PeriodoComparativo periodo = dateRangeResolver.resolver(preset, from, to);
        String gran = dateRangeResolver.normalizarGranularidad(granularidad, periodo.actual());

        Object[] actual = resumen(tenantId, periodo.actual().from(), dateRangeResolver.aExclusivo(periodo.actual().to()));
        Object[] anterior = resumen(tenantId, periodo.anterior().from(), dateRangeResolver.aExclusivo(periodo.anterior().to()));

        List<KpiDTO> kpis = construirKpis(actual, anterior);
        List<SeriePuntoDTO> serie = construirSerie(tenantId, periodo, gran);
        TablaReporteDTO categorias = construirTablaPorCategoria(tenantId, periodo);
        TablaReporteDTO top = construirTablaTopProductos(tenantId, periodo);

        StringBuilder subtitulo = new StringBuilder();
        subtitulo.append("Periodo actual: ").append(periodo.actual().etiqueta())
                .append("  (").append(periodo.actual().from().toLocalDate())
                .append(" a ").append(periodo.actual().to().toLocalDate()).append(")")
                .append("   |   Comparado contra: ").append(periodo.anterior().etiqueta())
                .append("  (").append(periodo.anterior().from().toLocalDate())
                .append(" a ").append(periodo.anterior().to().toLocalDate()).append(")");

        List<HojaExcelDTO> hojas = new ArrayList<>();

        // Cada KPI lleva su propio formato: los conteos (ordenes, comandas, tickets,
        // clientes) NO son moneda. Con un unico vector de tipos, Excel los mostraba
        // con formato de pesos.
        List<List<TipoColumna>> tiposPorFila = new ArrayList<>();
        for (KpiDTO k : kpis) {
            TipoColumna formatoValor = "numero".equals(k.formato()) ? TipoColumna.NUMERO : TipoColumna.MONEDA;
            tiposPorFila.add(List.of(
                    TipoColumna.TEXTO, formatoValor, formatoValor, TipoColumna.PORCENTAJE, TipoColumna.TEXTO));
        }

        hojas.add(HojaExcelDTO.deTiposPorFila(
                "Resumen comparativo",
                NOMBRE,
                subtitulo.toString(),
                List.of("Indicador", "Periodo actual", "Periodo anterior", "Variacion %", "Tendencia"),
                tiposPorFila,
                kpis.stream().map(k -> List.<Object>of(
                        k.label(),
                        k.actual(),
                        k.anterior(),
                        k.variacionPct() == null ? "Sin base" : k.variacionPct(),
                        etiquetaDireccion(k.direccion())
                )).toList()
        ));

        List<String> cabecerasSerie = switch (gran) {
            case "week" -> List.of("Semana inicio", "Ordenes", "Ingreso bruto", "Ingreso neto", "Ingreso neto (periodo anterior)");
            case "month" -> List.of("Mes", "Ordenes", "Ingreso bruto", "Ingreso neto", "Ingreso neto (periodo anterior)");
            default -> List.of("Dia", "Ordenes", "Ingreso bruto", "Ingreso neto", "Ingreso neto (periodo anterior)");
        };

        hojas.add(HojaExcelDTO.de(
                "Tendencia " + gran,
                "Evolucion de ventas por " + gran,
                "Cada punto compara contra el mismo desplazamiento del periodo anterior",
                cabecerasSerie,
                List.of(TipoColumna.TEXTO, TipoColumna.NUMERO, TipoColumna.MONEDA, TipoColumna.MONEDA, TipoColumna.MONEDA),
                serie.stream().map(p -> List.<Object>of(
                        p.etiqueta(),
                        p.ordenes(),
                        p.bruto(),
                        p.valor(),
                        p.valorAnterior()
                )).toList()
        ));

        hojas.add(hojaDesdeTabla(categorias, "Por categoria", "Ingresos por categoria de producto",
                " participacion se calcula sobre el total de ingresos del periodo"));

        hojas.add(hojaDesdeTabla(top, "Top productos", "Productos mas vendidos por ingresos",
                " Top " + TOP_PRODUCTOS + " del periodo"));

        return excelService.generar(REPORTE_KEY, hojas);
    }

    // ==================== KPIs ====================

    private Object[] resumen(Long tenantId, LocalDateTime from, LocalDateTime to) {
        List<Object[]> filas = repository.resumenVentas(tenantId, from, to);
        if (filas == null || filas.isEmpty() || filas.get(0) == null) {
            return new Object[0];
        }
        return filas.get(0);
    }

    private List<KpiDTO> construirKpis(Object[] actual, Object[] anterior) {
        return List.of(
                KpiDTO.de("ordenes", "Ordenes", numero(actual, 0), numero(anterior, 0), "numero"),
                KpiDTO.de("ingreso_bruto", "Ingreso bruto", numero(actual, 1), numero(anterior, 1), "moneda"),
                KpiDTO.de("descuentos", "Descuentos", numero(actual, 2), numero(anterior, 2), "moneda"),
                KpiDTO.de("ingreso_neto", "Ingreso neto", numero(actual, 3), numero(anterior, 3), "moneda"),
                KpiDTO.de("ticket_promedio", "Ticket promedio", numero(actual, 4), numero(anterior, 4), "moneda"),
                KpiDTO.de("clientes", "Clientes unicos", numero(actual, 5), numero(anterior, 5), "numero"),
                KpiDTO.de("cobrado", "Importe cobrado", numero(actual, 6), numero(anterior, 6), "moneda")
        );
    }

    // ==================== Serie temporal ====================

    private List<SeriePuntoDTO> construirSerie(Long tenantId, PeriodoComparativo periodo, String granularidad) {
        List<Object[]> actual = repository.serieVentas(
                tenantId, periodo.actual().from(), dateRangeResolver.aExclusivo(periodo.actual().to()), granularidad);

        Map<LocalDate, BigDecimal> anteriores = new HashMap<>();
        for (Object[] fila : repository.serieVentas(
                tenantId, periodo.anterior().from(), dateRangeResolver.aExclusivo(periodo.anterior().to()), granularidad)) {
            anteriores.put(fecha(fila, 0), numero(fila, 3));
        }

        LocalDate inicioActual = periodo.actual().from().toLocalDate();
        LocalDate inicioAnterior = periodo.anterior().from().toLocalDate();

        List<SeriePuntoDTO> serie = new ArrayList<>();
        for (Object[] fila : actual) {
            LocalDate periodoFecha = fecha(fila, 0);
            if (periodoFecha == null) {
                log.warn("Serie de ventas con periodo ilegible, se omite: {}", java.util.Arrays.toString(fila));
                continue;
            }

            // Se alinea por desplazamiento de dias desde el inicio de cada periodo:
            // asi "martes de esta semana" se compara con "martes de la semana anterior".
            long offset = ChronoUnit.DAYS.between(inicioActual, periodoFecha);
            BigDecimal valorAnterior = anteriores.getOrDefault(inicioAnterior.plusDays(offset), BigDecimal.ZERO);

            serie.add(SeriePuntoDTO.completo(
                    etiquetaPeriodo(periodoFecha, granularidad),
                    periodoFecha.toString(),
                    numero(fila, 1),
                    numero(fila, 2),
                    numero(fila, 3),
                    valorAnterior
            ));
        }
        return serie;
    }

    private String etiquetaPeriodo(LocalDate fecha, String granularidad) {
        return switch (granularidad) {
            case "month" -> MESES[fecha.getMonthValue() - 1] + " " + fecha.getYear();
            case "week" -> fecha.format(DateTimeFormatter.ofPattern("dd/MM")) + " - " +
                    fecha.plusDays(6).format(DateTimeFormatter.ofPattern("dd/MM"));
            default -> fecha.getDayOfMonth() + " " + MESES[fecha.getMonthValue() - 1];
        };
    }

    // ==================== Tablas ====================

    private static final List<ColumnaDTO> COLUMNAS_CATEGORIA = List.of(
            ColumnaDTO.de("categoria", "Categoria", TipoColumna.TEXTO),
            ColumnaDTO.de("unidades", "Unidades", TipoColumna.NUMERO),
            ColumnaDTO.de("ingresos", "Ingresos", TipoColumna.MONEDA),
            ColumnaDTO.de("participacion", "Participacion %", TipoColumna.PORCENTAJE)
    );

    private static final List<ColumnaDTO> COLUMNAS_PRODUCTO = List.of(
            ColumnaDTO.de("producto", "Producto", TipoColumna.TEXTO),
            ColumnaDTO.de("unidades", "Unidades", TipoColumna.NUMERO),
            ColumnaDTO.de("ingresos", "Ingresos", TipoColumna.MONEDA),
            ColumnaDTO.de("ticket", "Ticket promedio", TipoColumna.MONEDA)
    );

    private TablaReporteDTO construirTablaPorCategoria(Long tenantId, PeriodoComparativo periodo) {
        List<Object[]> datos = repository.ventasPorCategoria(
                tenantId, periodo.actual().from(), dateRangeResolver.aExclusivo(periodo.actual().to()));

        BigDecimal total = datos.stream()
                .map(f -> numero(f, 2))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> filas = new ArrayList<>();
        for (Object[] fila : datos) {
            BigDecimal ingresos = numero(fila, 2);
            Map<String, Object> mapa = new LinkedHashMap<>();
            mapa.put("categoria", texto(fila, 0));
            mapa.put("unidades", numero(fila, 1));
            mapa.put("ingresos", ingresos);
            mapa.put("participacion", porcentaje(ingresos, total));
            filas.add(mapa);
        }
        return TablaReporteDTO.de(COLUMNAS_CATEGORIA, filas);
    }

    private TablaReporteDTO construirTablaTopProductos(Long tenantId, PeriodoComparativo periodo) {
        List<Object[]> datos = repository.topProductos(
                tenantId, periodo.actual().from(), dateRangeResolver.aExclusivo(periodo.actual().to()), TOP_PRODUCTOS);

        List<Map<String, Object>> filas = new ArrayList<>();
        for (Object[] fila : datos) {
            BigDecimal unidades = numero(fila, 1);
            BigDecimal ingresos = numero(fila, 2);
            Map<String, Object> mapa = new LinkedHashMap<>();
            mapa.put("producto", texto(fila, 0));
            mapa.put("unidades", unidades);
            mapa.put("ingresos", ingresos);
            mapa.put("ticket", unidades.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : ingresos.divide(unidades, 2, RoundingMode.HALF_UP));
            filas.add(mapa);
        }
        return TablaReporteDTO.de(COLUMNAS_PRODUCTO, filas);
    }

    private HojaExcelDTO hojaDesdeTabla(TablaReporteDTO tabla, String nombreHoja,
                                         String titulo, String nota) {
        List<String> claves = tabla.columnas().stream().map(ColumnaDTO::key).toList();
        List<String> encabezados = tabla.columnas().stream().map(ColumnaDTO::label).toList();
        List<TipoColumna> tipos = tabla.columnas().stream().map(ColumnaDTO::tipo).toList();

        return HojaExcelDTO.de(nombreHoja, titulo, tabla.filas().isEmpty() ? "Sin datos en el periodo" : nota.trim(),
                encabezados, tipos, excelService.aFilas(claves, tabla.filas()));
    }

    // ==================== Utilidades ====================

    private String etiquetaDireccion(String direccion) {
        if (direccion == null) {
            return "";
        }
        return switch (direccion) {
            case KpiDTO.SUBE -> "Sube";
            case KpiDTO.BAJA -> "Baja";
            case KpiDTO.NUEVO -> "Sin periodo anterior";
            default -> "Estable";
        };
    }

    private BigDecimal porcentaje(BigDecimal parte, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return parte.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
    }

    /**
     * PostgreSQL devuelve los promedios con la escala nativa de numeric, asi que un
     * AVG de 4 ordenes puede llegar como 825.0000000000000000. Se recorta a dos
     * decimales como maximo para que el JSON y el Excel no arrastren la escala.
     * No se hace stripTrailingZeros porque en un conteo devolveria 4E+0.
     */
    private BigDecimal normalizar(BigDecimal valor) {
        int escala = Math.min(valor.scale(), 2);
        return escala == valor.scale() ? valor : valor.setScale(escala, RoundingMode.HALF_UP);
    }
    private BigDecimal numero(Object[] fila, int indice) {
        if (fila == null || indice >= fila.length) {
            return BigDecimal.ZERO;
        }
        Object valor = fila[indice];
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        if (valor instanceof BigDecimal decimal) {
            return normalizar(decimal);
        }
        if (valor instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        try {
            return new BigDecimal(valor.toString());
        } catch (NumberFormatException e) {
            log.warn("No se pudo convertir '{}' a numero para el reporte de ventas", valor);
            return BigDecimal.ZERO;
        }
    }

    private LocalDate fecha(Object[] fila, int indice) {
        if (fila == null || indice >= fila.length || fila[indice] == null) {
            return null;
        }
        Object valor = fila[indice];
        if (valor instanceof Date d) {
            return d.toLocalDate();
        }
        if (valor instanceof java.sql.Timestamp t) {
            return t.toLocalDateTime().toLocalDate();
        }
        if (valor instanceof LocalDate ld) {
            return ld;
        }
        try {
            return LocalDate.parse(valor.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private String texto(Object[] fila, int indice) {
        if (fila == null || indice >= fila.length || fila[indice] == null) {
            return "";
        }
        return fila[indice].toString();
    }
}
