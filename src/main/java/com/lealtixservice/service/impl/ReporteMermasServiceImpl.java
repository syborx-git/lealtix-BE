package com.lealtixservice.service.impl;

import com.lealtixservice.dto.reportes.*;
import com.lealtixservice.repository.ReporteMermasRepository;
import com.lealtixservice.service.ReporteExcelService;
import com.lealtixservice.service.ReporteMermasService;
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
public class ReporteMermasServiceImpl implements ReporteMermasService {

    private static final String REPORTE_KEY = "2.3_mermas";
    private static final String PILAR = "2. Menu e Inventario";
    private static final String NOMBRE = "Auditoria de Mermas";
    private static final int LIMITE_DETALLE = 300;

    private static final DateTimeFormatter FORMATO_FECHA_TABLA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ReporteMermasRepository repository;
    private final DateRangeResolver dateRangeResolver;
    private final ReporteExcelService excelService;

    @Override
    public AuditoriaMermasDTO obtener(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to) {
        PeriodoComparativo periodo = dateRangeResolver.resolver(preset, from, to);
        return construir(tenantId, periodo);
    }

    @Override
    public byte[] exportar(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to) {
        PeriodoComparativo periodo = dateRangeResolver.resolver(preset, from, to);
        AuditoriaMermasDTO datos = construir(tenantId, periodo);

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
        hojas.add(hojaDesdeTabla(datos.porMotivo(), "Por motivo", "Mermas por motivo",
                "Desglose de desperdicios agrupados por causa registrada"));

        // 3. Por responsable
        hojas.add(hojaDesdeTabla(datos.porResponsable(), "Por responsable", "Mermas por responsable",
                "Trazabilidad de mermas por colaborador y area de origen"));

        // 4. Por insumo / producto
        hojas.add(hojaDesdeTabla(datos.porInsumo(), "Top insumos mermados", "Insumos y productos mas mermados",
                "Volumen y perdida economica por articulo"));

        // 5. Detalle cronologico
        hojas.add(hojaDesdeTabla(datos.detalle(), "Detalle de mermas", "Registro cronologico de mermas",
                "Historial completo de salidas no-venta en el periodo"));

        return excelService.generar(excelService.nombreArchivo("auditoria_mermas"), hojas);
    }

    private AuditoriaMermasDTO construir(Long tenantId, PeriodoComparativo periodo) {
        LocalDateTime actualFrom = periodo.actual().from();
        LocalDateTime actualToEx = dateRangeResolver.aExclusivo(periodo.actual().to());
        LocalDateTime anteriorFrom = periodo.anterior().from();
        LocalDateTime anteriorToEx = dateRangeResolver.aExclusivo(periodo.anterior().to());

        Object[] actual = primeraFila(repository.resumenMermas(tenantId, actualFrom, actualToEx));
        Object[] anterior = primeraFila(repository.resumenMermas(tenantId, anteriorFrom, anteriorToEx));

        ReporteMetaDTO meta = ReporteMetaDTO.de(REPORTE_KEY, PILAR, NOMBRE, periodo, "day");

        return AuditoriaMermasDTO.de(
                meta,
                construirKpis(actual, anterior),
                construirTablaPorMotivo(tenantId, actualFrom, actualToEx, numero(actual, 0)),
                construirTablaPorResponsable(tenantId, actualFrom, actualToEx, numero(actual, 0)),
                construirTablaPorInsumo(tenantId, actualFrom, actualToEx),
                construirTablaDetalle(tenantId, actualFrom, actualToEx)
        );
    }

    private List<KpiDTO> construirKpis(Object[] actual, Object[] anterior) {
        List<KpiDTO> kpis = new ArrayList<>();

        BigDecimal costoActual = numero(actual, 0);
        BigDecimal costoAnterior = numero(anterior, 0);
        kpis.add(KpiDTO.de("costo_total", "Costo total de mermas", costoActual, costoAnterior, "moneda"));

        BigDecimal eventosActual = numero(actual, 2);
        BigDecimal eventosAnterior = numero(anterior, 2);
        kpis.add(KpiDTO.de("eventos_merma", "Eventos de merma", eventosActual, eventosAnterior, "numero"));

        BigDecimal cantActual = numero(actual, 1);
        BigDecimal cantAnterior = numero(anterior, 1);
        kpis.add(KpiDTO.de("unidades_mermadas", "Volumen total mermado", cantActual, cantAnterior, "numero"));

        BigDecimal operativaActual = numero(actual, 3);
        BigDecimal operativaAnterior = numero(anterior, 3);
        kpis.add(KpiDTO.de("merma_operativa", "Merma operativa (comandas)", operativaActual, operativaAnterior, "moneda"));

        BigDecimal adminActual = numero(actual, 4);
        BigDecimal adminAnterior = numero(anterior, 4);
        kpis.add(KpiDTO.de("merma_administrativa", "Merma administrativa (almacen)", adminActual, adminAnterior, "moneda"));

        return List.copyOf(kpis);
    }

    private TablaReporteDTO construirTablaPorMotivo(Long tenantId, LocalDateTime from, LocalDateTime toExclusivo, BigDecimal costoTotalPeriodo) {
        List<ColumnaDTO> columnas = List.of(
                ColumnaDTO.de("motivo", "Motivo de la merma", TipoColumna.TEXTO),
                ColumnaDTO.de("eventos", "Registros", TipoColumna.NUMERO),
                ColumnaDTO.de("cantidad", "Cantidad", TipoColumna.NUMERO),
                ColumnaDTO.de("costoTotal", "Costo total ($)", TipoColumna.MONEDA),
                ColumnaDTO.de("participacion", "% del costo", TipoColumna.PORCENTAJE)
        );

        List<Object[]> raw = repository.mermasPorMotivo(tenantId, from, toExclusivo);
        List<Map<String, Object>> filas = new ArrayList<>();

        for (Object[] r : raw) {
            String motivo = r[0] != null ? r[0].toString() : "Sin motivo";
            BigDecimal eventos = numero(r, 1);
            BigDecimal cantidad = numero(r, 2);
            BigDecimal costo = numero(r, 3);
            BigDecimal part = porcentaje(costo, costoTotalPeriodo);

            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("motivo", motivo);
            fila.put("eventos", eventos);
            fila.put("cantidad", cantidad);
            fila.put("costoTotal", costo);
            fila.put("participacion", part);
            filas.add(fila);
        }

        return TablaReporteDTO.de(columnas, filas);
    }

    private TablaReporteDTO construirTablaPorResponsable(Long tenantId, LocalDateTime from, LocalDateTime toExclusivo, BigDecimal costoTotalPeriodo) {
        List<ColumnaDTO> columnas = List.of(
                ColumnaDTO.de("usuario", "Responsable", TipoColumna.TEXTO),
                ColumnaDTO.de("origen", "Origen", TipoColumna.TEXTO),
                ColumnaDTO.de("eventos", "Registros", TipoColumna.NUMERO),
                ColumnaDTO.de("costoTotal", "Costo total ($)", TipoColumna.MONEDA),
                ColumnaDTO.de("participacion", "% del costo", TipoColumna.PORCENTAJE)
        );

        List<Object[]> raw = repository.mermasPorResponsable(tenantId, from, toExclusivo);
        List<Map<String, Object>> filas = new ArrayList<>();

        for (Object[] r : raw) {
            String usuario = r[0] != null ? r[0].toString() : "Sin asignar";
            String origen = r[1] != null ? r[1].toString() : "General";
            BigDecimal eventos = numero(r, 2);
            BigDecimal costo = numero(r, 3);
            BigDecimal part = porcentaje(costo, costoTotalPeriodo);

            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("usuario", usuario);
            fila.put("origen", origen);
            fila.put("eventos", eventos);
            fila.put("costoTotal", costo);
            fila.put("participacion", part);
            filas.add(fila);
        }

        return TablaReporteDTO.de(columnas, filas);
    }

    private TablaReporteDTO construirTablaPorInsumo(Long tenantId, LocalDateTime from, LocalDateTime toExclusivo) {
        List<ColumnaDTO> columnas = List.of(
                ColumnaDTO.de("item", "Insumo / Producto", TipoColumna.TEXTO),
                ColumnaDTO.de("unidad", "Unidad", TipoColumna.TEXTO),
                ColumnaDTO.de("eventos", "Registros", TipoColumna.NUMERO),
                ColumnaDTO.de("cantidad", "Total mermado", TipoColumna.NUMERO),
                ColumnaDTO.de("costoTotal", "Perdida ($)", TipoColumna.MONEDA)
        );

        List<Object[]> raw = repository.mermasPorInsumo(tenantId, from, toExclusivo);
        List<Map<String, Object>> filas = new ArrayList<>();

        for (Object[] r : raw) {
            String item = r[0] != null ? r[0].toString() : "Sin nombre";
            String unidad = r[1] != null ? r[1].toString() : "pieza";
            BigDecimal eventos = numero(r, 2);
            BigDecimal cantidad = numero(r, 3);
            BigDecimal costo = numero(r, 4);

            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("item", item);
            fila.put("unidad", unidad);
            fila.put("eventos", eventos);
            fila.put("cantidad", cantidad);
            fila.put("costoTotal", costo);
            filas.add(fila);
        }

        return TablaReporteDTO.de(columnas, filas);
    }

    private TablaReporteDTO construirTablaDetalle(Long tenantId, LocalDateTime from, LocalDateTime toExclusivo) {
        List<ColumnaDTO> columnas = List.of(
                ColumnaDTO.de("fecha", "Fecha y Hora", TipoColumna.FECHA),
                ColumnaDTO.de("ticket", "Folio / Ticket", TipoColumna.TEXTO),
                ColumnaDTO.de("usuario", "Responsable", TipoColumna.TEXTO),
                ColumnaDTO.de("categoria", "Tipo Merma", TipoColumna.TEXTO),
                ColumnaDTO.de("origen", "Origen", TipoColumna.TEXTO),
                ColumnaDTO.de("item", "Insumo / Producto", TipoColumna.TEXTO),
                ColumnaDTO.de("cantidad", "Cantidad", TipoColumna.NUMERO),
                ColumnaDTO.de("unidad", "Unidad", TipoColumna.TEXTO),
                ColumnaDTO.de("costoUnitario", "Costo Unit. ($)", TipoColumna.MONEDA),
                ColumnaDTO.de("costoTotal", "Perdida ($)", TipoColumna.MONEDA),
                ColumnaDTO.de("motivo", "Motivo", TipoColumna.TEXTO)
        );

        List<Object[]> raw = repository.detalleMermas(tenantId, from, toExclusivo, LIMITE_DETALLE);
        List<Map<String, Object>> filas = new ArrayList<>();

        for (Object[] r : raw) {
            String fechaStr = fecha(r, 1);
            String ticket = r[2] != null ? r[2].toString() : "—";
            String usuario = r[3] != null ? r[3].toString() : "—";
            String categoria = r[4] != null ? r[4].toString() : "COMANDADA";
            String origen = r[5] != null ? r[5].toString() : "—";
            String item = r[6] != null ? r[6].toString() : "—";
            BigDecimal cantidad = numero(r, 7);
            String unidad = r[8] != null ? r[8].toString() : "pieza";
            BigDecimal costoUnitario = numero(r, 9);
            BigDecimal costoTotal = numero(r, 10);
            String motivo = r[11] != null ? r[11].toString() : "—";

            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("fecha", fechaStr);
            fila.put("ticket", ticket);
            fila.put("usuario", usuario);
            fila.put("categoria", "COMANDADA".equalsIgnoreCase(categoria) ? "Comanda" : "Administrativa");
            fila.put("origen", origen);
            fila.put("item", item);
            fila.put("cantidad", cantidad);
            fila.put("unidad", unidad);
            fila.put("costoUnitario", costoUnitario);
            fila.put("costoTotal", costoTotal);
            fila.put("motivo", motivo);
            filas.add(fila);
        }

        return TablaReporteDTO.de(columnas, filas);
    }

    private HojaExcelDTO hojaDesdeTabla(TablaReporteDTO tabla, String nombreHoja, String titulo, String nota) {
        List<String> claves = tabla.columnas().stream().map(ColumnaDTO::key).toList();
        List<String> encabezados = tabla.columnas().stream().map(ColumnaDTO::label).toList();
        List<TipoColumna> tipos = tabla.columnas().stream().map(ColumnaDTO::tipo).toList();

        return HojaExcelDTO.de(nombreHoja, titulo, tabla.filas().isEmpty() ? "Sin datos en el periodo" : nota,
                encabezados, tipos, excelService.aFilas(claves, tabla.filas()));
    }

    private Object[] primeraFila(Object result) {
        if (result == null) return new Object[0];
        if (result instanceof List<?> l) {
            if (l.isEmpty()) return new Object[0];
            Object first = l.get(0);
            if (first instanceof Object[] arr) return arr;
            return new Object[]{first};
        }
        if (result instanceof Object[] arr) {
            if (arr.length > 0 && arr[0] instanceof Object[] nested) {
                return nested;
            }
            return arr;
        }
        return new Object[]{result};
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
            return BigDecimal.valueOf(n.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        try {
            return new BigDecimal(valor.toString()).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private String fecha(Object[] fila, int indice) {
        if (fila == null || indice >= fila.length || fila[indice] == null) {
            return "";
        }
        Object valor = fila[indice];
        if (valor instanceof LocalDateTime ldt) {
            return ldt.format(FORMATO_FECHA_TABLA);
        }
        if (valor instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime().format(FORMATO_FECHA_TABLA);
        }
        return valor.toString();
    }

    private BigDecimal porcentaje(BigDecimal parte, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return parte.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizar(BigDecimal valor) {
        int escala = Math.min(valor.scale(), 2);
        return escala == valor.scale() ? valor : valor.setScale(escala, RoundingMode.HALF_UP);
    }

    private String etiquetaDireccion(String direccion) {
        if (direccion == null) return "";
        return switch (direccion) {
            case KpiDTO.SUBE -> "Sube";
            case KpiDTO.BAJA -> "Baja";
            case KpiDTO.NUEVO -> "Sin periodo anterior";
            default -> "Estable";
        };
    }
}
