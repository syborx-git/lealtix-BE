package com.lealtixservice.service.impl;

import com.lealtixservice.dto.reportes.*;
import com.lealtixservice.repository.ReporteStockMinimoRepository;
import com.lealtixservice.service.ReporteExcelService;
import com.lealtixservice.service.ReporteStockMinimoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteStockMinimoServiceImpl implements ReporteStockMinimoService {

    private static final String REPORTE_KEY = "2.4_stock_minimo";
    private static final String PILAR = "2. Menu e Inventario";
    private static final String NOMBRE = "Alertas de Stock Minimo";

    private final ReporteStockMinimoRepository repository;
    private final ReporteExcelService excelService;

    @Override
    public StockMinimoReporteDTO obtener(Long tenantId) {
        return construir(tenantId);
    }

    @Override
    public byte[] exportar(Long tenantId) {
        StockMinimoReporteDTO datos = construir(tenantId);

        String subtitulo = "Auditoria de inventario y lista de compras calculada al "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        List<HojaExcelDTO> hojas = new ArrayList<>();

        // 1. Resumen de KPIs
        List<List<TipoColumna>> tiposPorFila = new ArrayList<>();
        for (KpiDTO k : datos.kpis()) {
            TipoColumna formatoValor = "moneda".equals(k.formato()) ? TipoColumna.MONEDA : TipoColumna.NUMERO;
            tiposPorFila.add(List.of(TipoColumna.TEXTO, formatoValor, TipoColumna.TEXTO));
        }

        hojas.add(HojaExcelDTO.deTiposPorFila(
                "Resumen de Stock",
                NOMBRE,
                subtitulo,
                List.of("Indicador", "Valor actual", "Estado"),
                tiposPorFila,
                datos.kpis().stream().map(k -> List.<Object>of(
                        k.label(),
                        k.actual(),
                        "alertas".equals(k.key()) ? "Atencion requerida" : "Monitoreo"
                )).toList()
        ));

        // 2. Lista de compras y reposicion recomendada
        hojas.add(hojaDesdeTabla(datos.listaCompras(), "Lista de compras", "Lista de compras y reposicion recomendada",
                "Calculo automatico de unidades a comprar para alcanzar el stock de seguridad"));

        // 3. Alertas de stock critico y agotado
        hojas.add(hojaDesdeTabla(datos.alertas(), "Alertas activas", "Articulos bajo stock minimo o agotados",
                "Detalle de existencias actuales vs nivel minimo permitido"));

        // 4. Inventario general
        hojas.add(hojaDesdeTabla(datos.inventarioGeneral(), "Inventario general", "Inventario general monitoreado",
                "Todos los insumos y productos registrados"));

        return excelService.generar(excelService.nombreArchivo("alertas_stock_minimo"), hojas);
    }

    private StockMinimoReporteDTO construir(Long tenantId) {
        List<Object[]> rawInsumos = repository.listarInsumosConStock(tenantId);
        List<Object[]> rawProductos = repository.listarProductosConStockMinimo(tenantId);

        List<Map<String, Object>> filasAlertas = new ArrayList<>();
        List<Map<String, Object>> filasCompras = new ArrayList<>();
        List<Map<String, Object>> filasGeneral = new ArrayList<>();

        int totalAgotados = 0;
        int totalCriticos = 0;
        int totalMonitoreados = 0;
        BigDecimal inversionTotal = BigDecimal.ZERO;

        // Procesar Insumos
        for (Object[] r : rawInsumos) {
            totalMonitoreados++;
            String nombre = r[1] != null ? r[1].toString() : "Insumo sin nombre";
            String unidad = r[2] != null ? r[2].toString() : "pieza";
            double stockBodega = toDouble(r[3]);
            double stockDistribuido = toDouble(r[4]);
            double stockCocina = toDouble(r[5]);
            double stockBarra = toDouble(r[6]);
            double stockMinimo = toDouble(r[7]);
            boolean esBebida = r[8] != null && Boolean.parseBoolean(r[8].toString());
            BigDecimal costoUnit = toBigDecimal(r[9]);

            double stockTotal = stockBodega + stockDistribuido;
            String tipo = esBebida ? "Bebida" : "Insumo";

            String estado = "NORMAL";
            if (stockTotal <= 0) {
                estado = "AGOTADO";
                totalAgotados++;
            } else if (stockMinimo > 0 && stockTotal <= stockMinimo) {
                estado = "CRITICO";
                totalCriticos++;
            }

            // Ubicacion descriptiva
            String ubicacion = "Bodega (" + round(stockBodega) + ") + Cocina/Barra (" + round(stockDistribuido) + ")";

            // Si está en alerta (agotado o crítico)
            if (!"NORMAL".equals(estado)) {
                double targetSeguridad = stockMinimo > 0 ? stockMinimo * 1.5 : 10.0;
                double faltante = Math.max(0.0, targetSeguridad - stockTotal);
                BigDecimal inversion = costoUnit.multiply(BigDecimal.valueOf(faltante)).setScale(2, RoundingMode.HALF_UP);
                inversionTotal = inversionTotal.add(inversion);

                Map<String, Object> filaAlerta = new LinkedHashMap<>();
                filaAlerta.put("item", nombre);
                filaAlerta.put("tipo", tipo);
                filaAlerta.put("ubicacion", ubicacion);
                filaAlerta.put("stockActual", BigDecimal.valueOf(stockTotal).setScale(2, RoundingMode.HALF_UP));
                filaAlerta.put("stockMinimo", BigDecimal.valueOf(stockMinimo).setScale(2, RoundingMode.HALF_UP));
                filaAlerta.put("unidad", unidad);
                filaAlerta.put("faltante", BigDecimal.valueOf(faltante).setScale(2, RoundingMode.HALF_UP));
                filaAlerta.put("costoUnitario", costoUnit);
                filaAlerta.put("inversionEstimada", inversion);
                filaAlerta.put("estado", estado);
                filasAlertas.add(filaAlerta);

                // Fila para lista de compras
                Map<String, Object> filaCompra = new LinkedHashMap<>();
                filaCompra.put("item", nombre);
                filaCompra.put("tipo", tipo);
                filaCompra.put("unidad", unidad);
                filaCompra.put("stockActual", BigDecimal.valueOf(stockTotal).setScale(2, RoundingMode.HALF_UP));
                filaCompra.put("stockMinimo", BigDecimal.valueOf(stockMinimo).setScale(2, RoundingMode.HALF_UP));
                filaCompra.put("cantidadComprar", BigDecimal.valueOf(faltante).setScale(2, RoundingMode.HALF_UP));
                filaCompra.put("costoUnitario", costoUnit);
                filaCompra.put("inversionEstimada", inversion);
                filasCompras.add(filaCompra);
            }

            Map<String, Object> filaGen = new LinkedHashMap<>();
            filaGen.put("item", nombre);
            filaGen.put("tipo", tipo);
            filaGen.put("stockBodega", BigDecimal.valueOf(stockBodega).setScale(2, RoundingMode.HALF_UP));
            filaGen.put("stockDistribuido", BigDecimal.valueOf(stockDistribuido).setScale(2, RoundingMode.HALF_UP));
            filaGen.put("stockTotal", BigDecimal.valueOf(stockTotal).setScale(2, RoundingMode.HALF_UP));
            filaGen.put("stockMinimo", BigDecimal.valueOf(stockMinimo).setScale(2, RoundingMode.HALF_UP));
            filaGen.put("unidad", unidad);
            filaGen.put("costoUnitario", costoUnit);
            filaGen.put("estado", estado);
            filasGeneral.add(filaGen);
        }

        // Procesar Productos de Menú con stockMinimo configurado
        for (Object[] r : rawProductos) {
            totalMonitoreados++;
            String nombre = r[1] != null ? r[1].toString() : "Producto sin nombre";
            String unidad = r[2] != null ? r[2].toString() : "pieza";
            double stock = toDouble(r[3]);
            double stockMinimo = toDouble(r[4]);
            BigDecimal precio = toBigDecimal(r[5]);
            boolean esSubReceta = r[6] != null && Boolean.parseBoolean(r[6].toString());

            String tipo = esSubReceta ? "Sub-receta" : "Producto Menú";

            String estado = "NORMAL";
            if (stock <= 0) {
                estado = "AGOTADO";
                totalAgotados++;
            } else if (stockMinimo > 0 && stock <= stockMinimo) {
                estado = "CRITICO";
                totalCriticos++;
            }

            if (!"NORMAL".equals(estado)) {
                double targetSeguridad = stockMinimo > 0 ? stockMinimo * 1.5 : 10.0;
                double faltante = Math.max(0.0, targetSeguridad - stock);
                BigDecimal inversion = BigDecimal.ZERO; // sin costo registrado por omisión

                Map<String, Object> filaAlerta = new LinkedHashMap<>();
                filaAlerta.put("item", nombre);
                filaAlerta.put("tipo", tipo);
                filaAlerta.put("ubicacion", "Almacén / Cocina");
                filaAlerta.put("stockActual", BigDecimal.valueOf(stock).setScale(2, RoundingMode.HALF_UP));
                filaAlerta.put("stockMinimo", BigDecimal.valueOf(stockMinimo).setScale(2, RoundingMode.HALF_UP));
                filaAlerta.put("unidad", unidad);
                filaAlerta.put("faltante", BigDecimal.valueOf(faltante).setScale(2, RoundingMode.HALF_UP));
                filaAlerta.put("costoUnitario", inversion);
                filaAlerta.put("inversionEstimada", inversion);
                filaAlerta.put("estado", estado);
                filasAlertas.add(filaAlerta);

                Map<String, Object> filaCompra = new LinkedHashMap<>();
                filaCompra.put("item", nombre);
                filaCompra.put("tipo", tipo);
                filaCompra.put("unidad", unidad);
                filaCompra.put("stockActual", BigDecimal.valueOf(stock).setScale(2, RoundingMode.HALF_UP));
                filaCompra.put("stockMinimo", BigDecimal.valueOf(stockMinimo).setScale(2, RoundingMode.HALF_UP));
                filaCompra.put("cantidadComprar", BigDecimal.valueOf(faltante).setScale(2, RoundingMode.HALF_UP));
                filaCompra.put("costoUnitario", inversion);
                filaCompra.put("inversionEstimada", inversion);
                filasCompras.add(filaCompra);
            }

            Map<String, Object> filaGen = new LinkedHashMap<>();
            filaGen.put("item", nombre);
            filaGen.put("tipo", tipo);
            filaGen.put("stockBodega", BigDecimal.ZERO);
            filaGen.put("stockDistribuido", BigDecimal.valueOf(stock).setScale(2, RoundingMode.HALF_UP));
            filaGen.put("stockTotal", BigDecimal.valueOf(stock).setScale(2, RoundingMode.HALF_UP));
            filaGen.put("stockMinimo", BigDecimal.valueOf(stockMinimo).setScale(2, RoundingMode.HALF_UP));
            filaGen.put("unidad", unidad);
            filaGen.put("costoUnitario", BigDecimal.ZERO);
            filaGen.put("estado", estado);
            filasGeneral.add(filaGen);
        }

        int totalAlertas = totalAgotados + totalCriticos;

        // Construir KPIs
        List<KpiDTO> kpis = new ArrayList<>();
        kpis.add(KpiDTO.de("total_alertas", "Articulos en Alerta", BigDecimal.valueOf(totalAlertas), BigDecimal.ZERO, "numero"));
        kpis.add(KpiDTO.de("articulos_agotados", "Articulos Agotados (Stock 0)", BigDecimal.valueOf(totalAgotados), BigDecimal.ZERO, "numero"));
        kpis.add(KpiDTO.de("articulos_criticos", "Articulos en Stock Minimo", BigDecimal.valueOf(totalCriticos), BigDecimal.ZERO, "numero"));
        kpis.add(KpiDTO.de("inversion_sugerida", "Inversion Sugerida de Compra", inversionTotal, BigDecimal.ZERO, "moneda"));
        kpis.add(KpiDTO.de("total_monitoreados", "Total Articulos Monitoreados", BigDecimal.valueOf(totalMonitoreados), BigDecimal.ZERO, "numero"));

        // Definir columnas
        List<ColumnaDTO> colsAlertas = List.of(
                ColumnaDTO.de("item", "Articulo", TipoColumna.TEXTO),
                ColumnaDTO.de("tipo", "Tipo", TipoColumna.TEXTO),
                ColumnaDTO.de("ubicacion", "Ubicacion / Almacen", TipoColumna.TEXTO),
                ColumnaDTO.de("stockActual", "Stock Actual", TipoColumna.NUMERO),
                ColumnaDTO.de("stockMinimo", "Stock Minimo", TipoColumna.NUMERO),
                ColumnaDTO.de("unidad", "Unidad", TipoColumna.TEXTO),
                ColumnaDTO.de("faltante", "Faltante Sugerido", TipoColumna.NUMERO),
                ColumnaDTO.de("costoUnitario", "Costo Unit. ($)", TipoColumna.MONEDA),
                ColumnaDTO.de("inversionEstimada", "Inversion Sugerida ($)", TipoColumna.MONEDA),
                ColumnaDTO.de("estado", "Estado", TipoColumna.TEXTO)
        );

        List<ColumnaDTO> colsCompras = List.of(
                ColumnaDTO.de("item", "Articulo a Comprar", TipoColumna.TEXTO),
                ColumnaDTO.de("tipo", "Tipo", TipoColumna.TEXTO),
                ColumnaDTO.de("unidad", "Unidad", TipoColumna.TEXTO),
                ColumnaDTO.de("stockActual", "Stock Actual", TipoColumna.NUMERO),
                ColumnaDTO.de("stockMinimo", "Stock Minimo", TipoColumna.NUMERO),
                ColumnaDTO.de("cantidadComprar", "Cantidad a Comprar", TipoColumna.NUMERO),
                ColumnaDTO.de("costoUnitario", "Costo Unitario ($)", TipoColumna.MONEDA),
                ColumnaDTO.de("inversionEstimada", "Total Estimado ($)", TipoColumna.MONEDA)
        );

        List<ColumnaDTO> colsGeneral = List.of(
                ColumnaDTO.de("item", "Articulo", TipoColumna.TEXTO),
                ColumnaDTO.de("tipo", "Tipo", TipoColumna.TEXTO),
                ColumnaDTO.de("stockBodega", "Stock Bodega", TipoColumna.NUMERO),
                ColumnaDTO.de("stockDistribuido", "Stock Cocina/Barra", TipoColumna.NUMERO),
                ColumnaDTO.de("stockTotal", "Stock Total", TipoColumna.NUMERO),
                ColumnaDTO.de("stockMinimo", "Stock Minimo", TipoColumna.NUMERO),
                ColumnaDTO.de("unidad", "Unidad", TipoColumna.TEXTO),
                ColumnaDTO.de("costoUnitario", "Costo Prom. ($)", TipoColumna.MONEDA),
                ColumnaDTO.de("estado", "Estado", TipoColumna.TEXTO)
        );

        LocalDate hoy = LocalDate.now();
        RangoDTO rangoHoy = new RangoDTO(hoy.atStartOfDay(), hoy.atTime(23, 59, 59), "Tiempo Real");
        PeriodoComparativo periodo = new PeriodoComparativo(PresetReporte.HOY, rangoHoy, rangoHoy);
        ReporteMetaDTO meta = ReporteMetaDTO.de(REPORTE_KEY, PILAR, NOMBRE, periodo, "day");

        return StockMinimoReporteDTO.de(
                meta,
                List.copyOf(kpis),
                TablaReporteDTO.de(colsAlertas, filasAlertas),
                TablaReporteDTO.de(colsCompras, filasCompras),
                TablaReporteDTO.de(colsGeneral, filasGeneral)
        );
    }

    private HojaExcelDTO hojaDesdeTabla(TablaReporteDTO tabla, String nombreHoja, String titulo, String nota) {
        List<String> claves = tabla.columnas().stream().map(ColumnaDTO::key).toList();
        List<String> encabezados = tabla.columnas().stream().map(ColumnaDTO::label).toList();
        List<TipoColumna> tipos = tabla.columnas().stream().map(ColumnaDTO::tipo).toList();

        return HojaExcelDTO.de(nombreHoja, titulo, tabla.filas().isEmpty() ? "Sin articulos en esta seccion" : nota,
                encabezados, tipos, excelService.aFilas(claves, tabla.filas()));
    }

    private double toDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(o.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private BigDecimal toBigDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal b) return b.setScale(2, RoundingMode.HALF_UP);
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        try {
            return new BigDecimal(o.toString()).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private double round(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}
