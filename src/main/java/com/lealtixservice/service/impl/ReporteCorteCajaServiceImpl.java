package com.lealtixservice.service.impl;

import com.lealtixservice.dto.reportes.ColumnaDTO;
import com.lealtixservice.dto.reportes.CorteCajaDTO;
import com.lealtixservice.dto.reportes.HojaExcelDTO;
import com.lealtixservice.dto.reportes.KpiDTO;
import com.lealtixservice.dto.reportes.PeriodoComparativo;
import com.lealtixservice.dto.reportes.PresetReporte;
import com.lealtixservice.dto.reportes.ReporteMetaDTO;
import com.lealtixservice.dto.reportes.TablaReporteDTO;
import com.lealtixservice.dto.reportes.TipoColumna;
import com.lealtixservice.repository.ReporteCorteCajaRepository;
import com.lealtixservice.service.ReporteCorteCajaService;
import com.lealtixservice.service.ReporteExcelService;
import com.lealtixservice.util.DateRangeResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteCorteCajaServiceImpl implements ReporteCorteCajaService {

    private static final String REPORTE_KEY = "corte_caja";
    private static final String PILAR = "P1_Finanzas_y_Ventas";
    private static final String NOMBRE = "Corte de Caja y Conciliacion";
    private static final int LIMITE_ANULACIONES = 200;

    private final ReporteCorteCajaRepository repository;
    private final DateRangeResolver dateRangeResolver;
    private final ReporteExcelService excelService;

    @Override
    public CorteCajaDTO obtener(Long tenantId, PresetReporte preset,
                                LocalDateTime from, LocalDateTime to) {

        PeriodoComparativo periodo = dateRangeResolver.resolver(preset, from, to);
        return construir(tenantId, periodo);
    }

    @Override
    public byte[] exportar(Long tenantId, PresetReporte preset,
                           LocalDateTime from, LocalDateTime to) {

        PeriodoComparativo periodo = dateRangeResolver.resolver(preset, from, to);
        CorteCajaDTO datos = construir(tenantId, periodo);

        String subtitulo = "Periodo actual: " + periodo.actual().etiqueta()
                + "  (" + periodo.actual().from().toLocalDate() + " a " + periodo.actual().to().toLocalDate() + ")"
                + "   |   Comparado contra: " + periodo.anterior().etiqueta()
                + "  (" + periodo.anterior().from().toLocalDate() + " a " + periodo.anterior().to().toLocalDate() + ")";

        List<HojaExcelDTO> hojas = new ArrayList<>();

        // Cada KPI lleva su propio formato: los conteos (operaciones, cajeros,
        // anulaciones) NO son moneda. Con un unico vector de tipos, Excel los
        // mostraba con formato de pesos.
        List<List<TipoColumna>> tiposPorFila = new ArrayList<>();
        for (KpiDTO k : datos.kpis()) {
            TipoColumna formatoValor = "numero".equals(k.formato()) ? TipoColumna.NUMERO : TipoColumna.MONEDA;
            tiposPorFila.add(List.of(
                    TipoColumna.TEXTO, formatoValor, formatoValor, TipoColumna.PORCENTAJE, TipoColumna.TEXTO));
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

        hojas.add(hojaDesdeTabla(datos.porMetodo(), "Por metodo de pago",
                "Cobrado por metodo",
                " Incluye pagos simples y los cobros parciales de division de cuenta, sin duplicar"));

        hojas.add(hojaDesdeTabla(datos.porCajero(), "Por cajero",
                "Conciliacion por cajero",
                " Base para cuadrar el efectivo de cada usuario contra el sistema"));

        hojas.add(hojaDesdeTabla(datos.anulaciones(), "Anulaciones",
                "Ordenes canceladas en el periodo",
                " Maximo " + LIMITE_ANULACIONES + " registros, ordenadas de la mas reciente a la mas antigua"));

        return excelService.generar(REPORTE_KEY, hojas);
    }

    // ==================== Construccion del reporte ====================

    private CorteCajaDTO construir(Long tenantId, PeriodoComparativo periodo) {
        Object[] actual = resumen(tenantId, periodo.actual().from(), dateRangeResolver.aExclusivo(periodo.actual().to()));
        Object[] anterior = resumen(tenantId, periodo.anterior().from(), dateRangeResolver.aExclusivo(periodo.anterior().to()));

        Object[] anulActual = resumenAnulaciones(tenantId, periodo.actual().from(), dateRangeResolver.aExclusivo(periodo.actual().to()));
        Object[] anulAnterior = resumenAnulaciones(tenantId, periodo.anterior().from(), dateRangeResolver.aExclusivo(periodo.anterior().to()));

        ReporteMetaDTO meta = ReporteMetaDTO.de(REPORTE_KEY, PILAR, NOMBRE, periodo, "day");

        return CorteCajaDTO.de(
                meta,
                construirKpis(actual, anterior, anulActual, anulAnterior),
                construirTablaPorMetodo(tenantId, periodo),
                construirTablaPorCajero(tenantId, periodo),
                construirTablaAnulaciones(tenantId, periodo)
        );
    }

    private Object[] resumen(Long tenantId, LocalDateTime from, LocalDateTime to) {
        return primeraFila(repository.resumenCorte(tenantId, from, to));
    }

    private Object[] resumenAnulaciones(Long tenantId, LocalDateTime from, LocalDateTime to) {
        return primeraFila(repository.resumenAnulaciones(tenantId, from, to));
    }

    /**
     * Las consultas de resumen son agregados sin GROUP BY, asi que PostgreSQL siempre
     * devuelve una fila. aun asi se protege el caso vacio para no romper el render.
     */
    private Object[] primeraFila(List<Object[]> filas) {
        if (filas == null || filas.isEmpty()) {
            return new Object[0];
        }
        Object[] fila = filas.get(0);
        return fila == null ? new Object[0] : fila;
    }

    private List<KpiDTO> construirKpis(Object[] actual, Object[] anterior,
                                       Object[] anulacionesActual, Object[] anulacionesAnterior) {
        List<KpiDTO> kpis = new ArrayList<>();

        kpis.add(KpiDTO.de("total_cobrado", "Total cobrado", numero(actual, 0), numero(anterior, 0), "moneda"));
        kpis.add(KpiDTO.de("efectivo", "Efectivo", numero(actual, 1), numero(anterior, 1), "moneda"));
        kpis.add(KpiDTO.de("tarjeta", "Tarjeta", numero(actual, 2), numero(anterior, 2), "moneda"));
        kpis.add(KpiDTO.de("transferencia", "Transferencia", numero(actual, 3), numero(anterior, 3), "moneda"));
        kpis.add(KpiDTO.de("mixto", "Pago mixto", numero(actual, 4), numero(anterior, 4), "moneda"));
        kpis.add(KpiDTO.de("operaciones", "Operaciones de cobro", numero(actual, 6), numero(anterior, 6), "numero"));
        kpis.add(KpiDTO.de("cajeros", "Cajeros con movimientos", numero(actual, 7), numero(anterior, 7), "numero"));

        BigDecimal ticketActual = ticketPromedio(numero(actual, 0), numero(actual, 6));
        BigDecimal ticketAnterior = ticketPromedio(numero(anterior, 0), numero(anterior, 6));
        kpis.add(KpiDTO.de("ticket_promedio", "Ticket promedio por cobro", ticketActual, ticketAnterior, "moneda"));

        // La propina no forma parte del total cobrado: se reporta aparte para que el
        // cierre pueda separar lo que es venta de lo que fue propina del personal.
        kpis.add(KpiDTO.de("propinas", "Propinas cobradas", numero(actual, 5), numero(anterior, 5), "moneda"));

        // El dinero que se perdio por cancelaciones es parte del cierre: sin esto
        // el total cobrado no se puede conciliar contra lo esperado.
        kpis.add(KpiDTO.de("anulaciones", "Ordenes anuladas", numero(anulacionesActual, 0), numero(anulacionesAnterior, 0), "numero"));
        kpis.add(KpiDTO.de("monto_anulado", "Monto anulado", numero(anulacionesActual, 1), numero(anulacionesAnterior, 1), "moneda"));

        kpis.add(KpiDTO.de("efectivo_real", "Efectivo en caja (cobrado - anulado)",
                numero(actual, 1).subtract(numero(anulacionesActual, 1)),
                numero(anterior, 1).subtract(numero(anulacionesAnterior, 1)),
                "moneda"));

        return List.copyOf(kpis);
    }

    private BigDecimal ticketPromedio(BigDecimal total, BigDecimal operaciones) {
        if (operaciones == null || operaciones.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return total.divide(operaciones, 2, RoundingMode.HALF_UP);
    }

    // ==================== Tablas ====================

    private static final List<ColumnaDTO> COLUMNAS_METODO = List.of(
            ColumnaDTO.de("metodo", "Metodo de pago", TipoColumna.TEXTO),
            ColumnaDTO.de("operaciones", "Operaciones", TipoColumna.NUMERO),
            ColumnaDTO.de("total", "Total cobrado", TipoColumna.MONEDA),
            ColumnaDTO.de("propinas", "Propinas", TipoColumna.MONEDA),
            ColumnaDTO.de("participacion", "Participacion %", TipoColumna.PORCENTAJE),
            ColumnaDTO.de("ticket", "Ticket promedio", TipoColumna.MONEDA)
    );

    private static final List<ColumnaDTO> COLUMNAS_CAJERO = List.of(
            ColumnaDTO.de("cajero", "Cajero", TipoColumna.TEXTO),
            ColumnaDTO.de("email", "Correo", TipoColumna.TEXTO),
            ColumnaDTO.de("operaciones", "Operaciones", TipoColumna.NUMERO),
            ColumnaDTO.de("efectivo", "Efectivo", TipoColumna.MONEDA),
            ColumnaDTO.de("tarjeta", "Tarjeta", TipoColumna.MONEDA),
            ColumnaDTO.de("transferencia", "Transferencia", TipoColumna.MONEDA),
            ColumnaDTO.de("mixto", "Mixto", TipoColumna.MONEDA),
            ColumnaDTO.de("total", "Total", TipoColumna.MONEDA),
            ColumnaDTO.de("propinas", "Propinas", TipoColumna.MONEDA)
    );

    private static final List<ColumnaDTO> COLUMNAS_ANULACION = List.of(
            ColumnaDTO.de("referencia", "Comanda", TipoColumna.TEXTO),
            ColumnaDTO.de("fecha", "Fecha", TipoColumna.FECHA),
            ColumnaDTO.de("monto", "Monto", TipoColumna.MONEDA),
            ColumnaDTO.de("motivo", "Motivo", TipoColumna.TEXTO),
            ColumnaDTO.de("usuario", "Cancelada por", TipoColumna.TEXTO)
    );

    private TablaReporteDTO construirTablaPorMetodo(Long tenantId, PeriodoComparativo periodo) {
        List<Object[]> datos = repository.pagosPorMetodo(
                tenantId, periodo.actual().from(), dateRangeResolver.aExclusivo(periodo.actual().to()));

        BigDecimal total = datos.stream()
                .map(f -> numero(f, 2))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> filas = new ArrayList<>();
        for (Object[] fila : datos) {
            BigDecimal operaciones = numero(fila, 1);
            BigDecimal cobrado = numero(fila, 2);
            Map<String, Object> mapa = new LinkedHashMap<>();
            mapa.put("metodo", etiquetaMetodo(texto(fila, 0)));
            mapa.put("operaciones", operaciones);
            mapa.put("total", cobrado);
            mapa.put("propinas", numero(fila, 3));
            mapa.put("participacion", porcentaje(cobrado, total));
            mapa.put("ticket", ticketPromedio(cobrado, operaciones));
            filas.add(mapa);
        }
        return TablaReporteDTO.de(COLUMNAS_METODO, filas);
    }

    private TablaReporteDTO construirTablaPorCajero(Long tenantId, PeriodoComparativo periodo) {
        List<Object[]> datos = repository.pagosPorCajero(
                tenantId, periodo.actual().from(), dateRangeResolver.aExclusivo(periodo.actual().to()));

        List<Map<String, Object>> filas = new ArrayList<>();
        for (Object[] fila : datos) {
            Map<String, Object> mapa = new LinkedHashMap<>();
            mapa.put("cajero", texto(fila, 0));
            mapa.put("email", texto(fila, 1));
            mapa.put("operaciones", numero(fila, 2));
            mapa.put("efectivo", numero(fila, 3));
            mapa.put("tarjeta", numero(fila, 4));
            mapa.put("transferencia", numero(fila, 5));
            mapa.put("mixto", numero(fila, 6));
            mapa.put("total", numero(fila, 7));
            mapa.put("propinas", numero(fila, 8));
            filas.add(mapa);
        }
        return TablaReporteDTO.de(COLUMNAS_CAJERO, filas);
    }

    private TablaReporteDTO construirTablaAnulaciones(Long tenantId, PeriodoComparativo periodo) {
        List<Object[]> datos = repository.anulaciones(
                tenantId, periodo.actual().from(), dateRangeResolver.aExclusivo(periodo.actual().to()), LIMITE_ANULACIONES);

        List<Map<String, Object>> filas = new ArrayList<>();
        for (Object[] fila : datos) {
            Map<String, Object> mapa = new LinkedHashMap<>();
            mapa.put("referencia", texto(fila, 0));
            mapa.put("fecha", fecha(fila, 1));
            mapa.put("monto", numero(fila, 4));
            mapa.put("motivo", texto(fila, 2));
            mapa.put("usuario", texto(fila, 3));
            filas.add(mapa);
        }
        return TablaReporteDTO.de(COLUMNAS_ANULACION, filas);
    }

    private HojaExcelDTO hojaDesdeTabla(TablaReporteDTO tabla, String nombreHoja,
                                         String titulo, String nota) {
        List<String> claves = tabla.columnas().stream().map(ColumnaDTO::key).toList();
        List<String> encabezados = tabla.columnas().stream().map(ColumnaDTO::label).toList();
        List<TipoColumna> tipos = tabla.columnas().stream().map(ColumnaDTO::tipo).toList();

        return HojaExcelDTO.de(nombreHoja, titulo, tabla.filas().isEmpty() ? "Sin datos en el periodo" : nota,
                encabezados, tipos, excelService.aFilas(claves, tabla.filas()));
    }

    // ==================== Utilidades ====================

    private String etiquetaMetodo(String metodo) {
        if (metodo == null || metodo.isBlank()) {
            return "Sin metodo registrado";
        }
        return switch (metodo) {
            case "CASH" -> "Efectivo";
            case "CARD" -> "Tarjeta";
            case "TRANSFER" -> "Transferencia";
            case "MIXED" -> "Pago mixto";
            case "SIN_METODO" -> "Sin metodo registrado";
            default -> metodo;
        };
    }

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
        if (fila == null || indice >= fila.length || fila[indice] == null) {
            return BigDecimal.ZERO;
        }
        Object valor = fila[indice];
        if (valor instanceof BigDecimal decimal) {
            return normalizar(decimal);
        }
        if (valor instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        try {
            return new BigDecimal(valor.toString());
        } catch (NumberFormatException e) {
            log.warn("No se pudo convertir '{}' a numero para el reporte de corte", valor);
            return BigDecimal.ZERO;
        }
    }

    private String fecha(Object[] fila, int indice) {
        if (fila == null || indice >= fila.length || fila[indice] == null) {
            return "";
        }
        Object valor = fila[indice];
        if (valor instanceof Timestamp t) {
            return t.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }
        if (valor instanceof Date d) {
            return d.toLocalDate().toString();
        }
        if (valor instanceof LocalDateTime ldt) {
            return ldt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }
        return valor.toString();
    }

    private String texto(Object[] fila, int indice) {
        if (fila == null || indice >= fila.length || fila[indice] == null) {
            return "";
        }
        return fila[indice].toString();
    }
}
