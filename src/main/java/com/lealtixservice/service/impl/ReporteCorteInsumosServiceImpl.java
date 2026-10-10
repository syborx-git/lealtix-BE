package com.lealtixservice.service.impl;

import com.lealtixservice.dto.reportes.*;
import com.lealtixservice.entity.ClientOrderItem;
import com.lealtixservice.entity.Insumo;
import com.lealtixservice.entity.ProductRecipe;
import com.lealtixservice.entity.ProductSubReceta;
import com.lealtixservice.entity.TenantMenuProduct;
import com.lealtixservice.repository.*;
import com.lealtixservice.service.ReporteCorteInsumosService;
import com.lealtixservice.service.ReporteExcelService;
import com.lealtixservice.util.DateRangeResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteCorteInsumosServiceImpl implements ReporteCorteInsumosService {

    private static final String REPORTE_KEY = "2.5_corte_insumos";
    private static final String PILAR = "2. Menu e Inventario";
    private static final String NOMBRE = "Corte Diario de Insumos (Cocina / Barra)";

    private final ClientOrderItemRepository clientOrderItemRepository;
    private final ProductRecipeRepository productRecipeRepository;
    private final ProductSubRecetaRepository productSubRecetaRepository;
    private final InsumoRepository insumoRepository;
    private final RestockHistoryRepository restockHistoryRepository;
    private final DateRangeResolver dateRangeResolver;
    private final ReporteExcelService excelService;

    @Override
    public CorteInsumosDTO obtener(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to, String area) {
        PeriodoComparativo periodo = dateRangeResolver.resolver(preset, from, to);
        return construir(tenantId, periodo, normalizarArea(area));
    }

    @Override
    public byte[] exportar(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to, String area) {
        PeriodoComparativo periodo = dateRangeResolver.resolver(preset, from, to);
        String areaNorm = normalizarArea(area);
        CorteInsumosDTO datos = construir(tenantId, periodo, areaNorm);

        String subtitulo = "Periodo: " + periodo.actual().etiqueta()
                + " (" + periodo.actual().from().toLocalDate() + " a " + periodo.actual().to().toLocalDate() + ")"
                + " | Área: " + areaNorm;

        List<HojaExcelDTO> hojas = new ArrayList<>();

        // 1. Resumen de KPIs
        List<List<TipoColumna>> tiposPorFila = new ArrayList<>();
        for (KpiDTO k : datos.kpis()) {
            TipoColumna formatoValor = "numero".equals(k.formato()) ? TipoColumna.NUMERO : TipoColumna.MONEDA;
            tiposPorFila.add(List.of(TipoColumna.TEXTO, formatoValor, formatoValor, TipoColumna.PORCENTAJE, TipoColumna.TEXTO));
        }

        List<List<Object>> filasKpis = new ArrayList<>();
        for (KpiDTO k : datos.kpis()) {
            filasKpis.add(List.of(
                    k.label(),
                    k.actual(),
                    k.anterior(),
                    k.variacionPct() == null ? "Sin base" : k.variacionPct(),
                    etiquetaDireccion(k.direccion())
            ));
        }

        hojas.add(HojaExcelDTO.deTiposPorFila(
                "Resumen",
                NOMBRE + " - " + areaNorm,
                subtitulo,
                List.of("Indicador", "Periodo actual", "Periodo anterior", "Variación %", "Tendencia"),
                tiposPorFila,
                filasKpis
        ));

        // 2. Platillos / Productos Desplazados
        hojas.add(hojaDesdeTabla(datos.platillosDesplazados(), "Platillos Vendidos",
                "Desplazamiento de Platillos y Bebidas",
                "Volumen e ingresos de productos vendidos en el periodo (" + areaNorm + ")"));

        // 3. Insumos Consumidos
        hojas.add(hojaDesdeTabla(datos.insumosConsumidos(), "Insumos Consumidos",
                "Insumos Utilizados en Preparación",
                "Desglose de materia prima consumida por recetas y costo económico (" + areaNorm + ")"));

        // 4. Inventario Disponible
        hojas.add(hojaDesdeTabla(datos.stockRemanente(), "Stock Disponible",
                "Inventario Disponible al Cierre",
                "Stock remanente en almacén con estado de abastecimiento (" + areaNorm + ")"));

        return excelService.generar(excelService.nombreArchivo("corte_insumos_" + areaNorm.toLowerCase()), hojas);
    }

    private CorteInsumosDTO construir(Long tenantId, PeriodoComparativo periodo, String areaFiltro) {
        LocalDateTime actualFrom = periodo.actual().from();
        LocalDateTime actualToEx = dateRangeResolver.aExclusivo(periodo.actual().to());
        LocalDateTime anteriorFrom = periodo.anterior().from();
        LocalDateTime anteriorToEx = dateRangeResolver.aExclusivo(periodo.anterior().to());

        // Obtener ítems vendidos en ambos periodos
        List<ClientOrderItem> itemsActual = clientOrderItemRepository.findNonCancelledItemsInPeriod(tenantId, actualFrom, actualToEx);
        List<ClientOrderItem> itemsAnterior = clientOrderItemRepository.findNonCancelledItemsInPeriod(tenantId, anteriorFrom, anteriorToEx);

        // Catálogo de insumos y costos promedio
        List<Insumo> insumosTenant = insumoRepository.findByTenantIdAndIsActiveTrueOrderByNombreAsc(tenantId);
        Map<Long, Insumo> insumoMap = insumosTenant.stream().collect(Collectors.toMap(Insumo::getId, i -> i, (a, b) -> a));
        Map<Long, Double> costosPromedio = obtenerCostosPromedio(tenantId);

        // Identificar IDs de productos que son bebidas/barra
        Set<Long> barraProductIds = insumosTenant.stream()
                .filter(Insumo::isEsBebida)
                .map(Insumo::getProductoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Procesar periodo actual
        ProcessedAreaPeriod dataActual = procesarPeriodo(itemsActual, barraProductIds, insumoMap, costosPromedio, areaFiltro, insumosTenant);
        ProcessedAreaPeriod dataAnterior = procesarPeriodo(itemsAnterior, barraProductIds, insumoMap, costosPromedio, areaFiltro, insumosTenant);

        // KPIs comparativos
        List<KpiDTO> kpis = List.of(
                KpiDTO.de("platillos", "Platillos / Productos Vendidos", dataActual.totalUnidadesVendidas, dataAnterior.totalUnidadesVendidas, "numero"),
                KpiDTO.de("insumosVariedad", "Variedad Insumos Utilizados", (double) dataActual.variedadInsumosUsados, (double) dataAnterior.variedadInsumosUsados, "numero"),
                KpiDTO.de("costoInsumos", "Costo Total Insumos Consumidos", dataActual.costoTotalInsumos.doubleValue(), dataAnterior.costoTotalInsumos.doubleValue(), "moneda"),
                KpiDTO.de("insumosAlerta", "Insumos en Alerta / Agotados", (double) dataActual.insumosEnAlerta, (double) dataAnterior.insumosEnAlerta, "numero")
        );

        ReporteMetaDTO meta = ReporteMetaDTO.de(REPORTE_KEY, PILAR, NOMBRE, periodo, "day");

        return CorteInsumosDTO.de(
                meta,
                kpis,
                areaFiltro,
                dataActual.tablaPlatillos,
                dataActual.tablaInsumos,
                dataActual.tablaStock,
                dataActual.topPlatillosChart,
                dataActual.topInsumosChart
        );
    }

    private ProcessedAreaPeriod procesarPeriodo(
            List<ClientOrderItem> items,
            Set<Long> barraProductIds,
            Map<Long, Insumo> insumoMap,
            Map<Long, Double> costosPromedio,
            String areaFiltro,
            List<Insumo> insumosTenant
    ) {
        ProcessedAreaPeriod result = new ProcessedAreaPeriod();

        // 1. Filtrar y agrupar platillos vendidos
        Map<Long, PlatilloAcumulado> platillosMap = new LinkedHashMap<>();
        for (ClientOrderItem item : items) {
            TenantMenuProduct prod = item.getProduct();
            if (prod == null) continue;

            String areaProd = determinarAreaProducto(prod, barraProductIds);
            if (!cumpleArea(areaProd, areaFiltro)) {
                continue;
            }

            int qty = item.getCantidad() != null ? item.getCantidad() : 1;
            BigDecimal precio = item.getPrecioUnitario() != null ? item.getPrecioUnitario() : (prod.getPrecio() != null ? prod.getPrecio() : BigDecimal.ZERO);
            BigDecimal totalVenta = precio.multiply(BigDecimal.valueOf(qty));

            platillosMap.computeIfAbsent(prod.getId(), k -> new PlatilloAcumulado(
                    prod.getId(),
                    prod.getNombre(),
                    prod.getCategory() != null ? prod.getCategory().getNombre() : "General",
                    areaProd,
                    precio
            )).agregar(qty, totalVenta);

            result.totalUnidadesVendidas += qty;
        }

        // Cargar recetas y subrecetas de los platillos vendidos en lote (evita N+1)
        Set<Long> dishIds = platillosMap.keySet();
        List<ProductRecipe> recetasDirectas = dishIds.isEmpty() ? List.of() : productRecipeRepository.findByDishIdInWithInsumo(dishIds);
        List<ProductSubReceta> subRecetasAsignadas = dishIds.isEmpty() ? List.of() : productSubRecetaRepository.findByDishIdInWithSubReceta(dishIds);

        // Agrupar recetas por dishId
        Map<Long, List<ProductRecipe>> recetasPorPlatillo = recetasDirectas.stream()
                .collect(Collectors.groupingBy(r -> r.getDish().getId()));
        Map<Long, List<ProductSubReceta>> subRecetasPorPlatillo = subRecetasAsignadas.stream()
                .collect(Collectors.groupingBy(s -> s.getDish().getId()));

        // Para las subrecetas encontradas, cargar sus recetas de insumos base
        Set<Long> subRecetaIds = subRecetasAsignadas.stream()
                .map(s -> s.getSubReceta().getId())
                .collect(Collectors.toSet());
        List<ProductRecipe> recetasDeSubRecetas = subRecetaIds.isEmpty() ? List.of() : productRecipeRepository.findByDishIdInWithInsumo(subRecetaIds);
        Map<Long, List<ProductRecipe>> insumosPorSubReceta = recetasDeSubRecetas.stream()
                .collect(Collectors.groupingBy(r -> r.getDish().getId()));

        // 2. Calcular consumo de insumos derivados
        Map<Long, InsumoConsumidoAcumulado> insumosConsumidosMap = new HashMap<>();

        for (PlatilloAcumulado p : platillosMap.values()) {
            int qtyPlatillo = p.cantidad;

            // Recetas directas
            List<ProductRecipe> recs = recetasPorPlatillo.getOrDefault(p.id, List.of());
            for (ProductRecipe r : recs) {
                if (r.getInsumo() == null) continue;
                Insumo ins = r.getInsumo();
                double cantPorPlato = r.getCantidad() != null ? r.getCantidad().doubleValue() : 0.0;
                double cantTotal = cantPorPlato * qtyPlatillo;

                double costoUnit = costosPromedio.getOrDefault(ins.getId(), 0.0);
                insumosConsumidosMap.computeIfAbsent(ins.getId(), k -> new InsumoConsumidoAcumulado(
                        ins.getId(),
                        ins.getNombre(),
                        ins.getUnidad() != null ? ins.getUnidad() : "pz",
                        p.area,
                        costoUnit
                )).acumular(cantTotal);
            }

            // Subrecetas asignadas
            List<ProductSubReceta> subs = subRecetasPorPlatillo.getOrDefault(p.id, List.of());
            for (ProductSubReceta s : subs) {
                TenantMenuProduct subReceta = s.getSubReceta();
                if (subReceta == null) continue;

                double tamanoLote = subReceta.getTamanoLote() != null && subReceta.getTamanoLote() > 0 ? subReceta.getTamanoLote() : 1.0;
                double porcionPlatillo = s.getCantidad() != null && s.getCantidad() > 0 ? s.getCantidad() : 1.0;
                double ratioLote = porcionPlatillo / tamanoLote;

                // Desglosar insumos base de la subreceta
                List<ProductRecipe> lineasSub = insumosPorSubReceta.getOrDefault(subReceta.getId(), List.of());
                for (ProductRecipe line : lineasSub) {
                    if (line.getInsumo() == null) continue;
                    Insumo ins = line.getInsumo();
                    double cantEnLote = line.getCantidad() != null ? line.getCantidad().doubleValue() : 0.0;
                    double cantConsumida = cantEnLote * ratioLote * qtyPlatillo;

                    double costoUnit = costosPromedio.getOrDefault(ins.getId(), 0.0);
                    insumosConsumidosMap.computeIfAbsent(ins.getId(), k -> new InsumoConsumidoAcumulado(
                            ins.getId(),
                            ins.getNombre(),
                            ins.getUnidad() != null ? ins.getUnidad() : "pz",
                            p.area,
                            costoUnit
                    )).acumular(cantConsumida);
                }
            }
        }

        result.variedadInsumosUsados = insumosConsumidosMap.size();

        // 3. Tablas de Platillos Vendidos
        List<Map<String, Object>> filasPlatillos = new ArrayList<>();
        List<PlatilloAcumulado> listaOrdenadaPlatillos = platillosMap.values().stream()
                .sorted(Comparator.comparingInt((PlatilloAcumulado p) -> p.cantidad).reversed())
                .toList();

        for (PlatilloAcumulado p : listaOrdenadaPlatillos) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("platillo", p.nombre);
            fila.put("categoria", p.categoria);
            fila.put("area", p.area);
            fila.put("unidades", p.cantidad);
            fila.put("precioPromedio", p.precio);
            fila.put("totalVenta", p.totalVenta);
            filasPlatillos.add(fila);
        }

        result.tablaPlatillos = new TablaReporteDTO(
                List.of(
                        ColumnaDTO.de("platillo", "Platillo / Producto", TipoColumna.TEXTO),
                        ColumnaDTO.de("categoria", "Categoría", TipoColumna.TEXTO),
                        ColumnaDTO.de("area", "Área", TipoColumna.TEXTO),
                        ColumnaDTO.de("unidades", "Unidades Vendidas", TipoColumna.NUMERO),
                        ColumnaDTO.de("precioPromedio", "Precio Unit. ($)", TipoColumna.MONEDA),
                        ColumnaDTO.de("totalVenta", "Ventas Totales ($)", TipoColumna.MONEDA)
                ),
                filasPlatillos
        );

        // Gráfica Top 10 Platillos
        result.topPlatillosChart = listaOrdenadaPlatillos.stream().limit(10).map(p ->
                ChartPuntoDTO.deMoneda(p.nombre, (double) p.cantidad, p.totalVenta, "unidades", p.categoria, p.area)
        ).toList();

        // 4. Tablas de Insumos Consumidos
        List<Map<String, Object>> filasInsumos = new ArrayList<>();
        List<InsumoConsumidoAcumulado> listaInsumos = insumosConsumidosMap.values().stream()
                .sorted(Comparator.comparingDouble((InsumoConsumidoAcumulado i) -> i.costoTotal()).reversed())
                .toList();

        for (InsumoConsumidoAcumulado i : listaInsumos) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("insumo", i.nombre);
            fila.put("unidad", i.unidad);
            fila.put("area", i.area);
            fila.put("consumoTotal", redondear(i.consumoTotal));
            fila.put("costoUnitario", BigDecimal.valueOf(i.costoUnitario).setScale(2, RoundingMode.HALF_UP));
            BigDecimal costoTot = BigDecimal.valueOf(i.costoTotal()).setScale(2, RoundingMode.HALF_UP);
            fila.put("costoTotal", costoTot);
            filasInsumos.add(fila);

            result.costoTotalInsumos = result.costoTotalInsumos.add(costoTot);
        }

        result.tablaInsumos = new TablaReporteDTO(
                List.of(
                        ColumnaDTO.de("insumo", "Insumo / Materia Prima", TipoColumna.TEXTO),
                        ColumnaDTO.de("unidad", "Unidad", TipoColumna.TEXTO),
                        ColumnaDTO.de("area", "Área Destino", TipoColumna.TEXTO),
                        ColumnaDTO.de("consumoTotal", "Consumo en el Día", TipoColumna.NUMERO),
                        ColumnaDTO.de("costoUnitario", "Costo Unit. Prom. ($)", TipoColumna.MONEDA),
                        ColumnaDTO.de("costoTotal", "Costo Total Consumido ($)", TipoColumna.MONEDA)
                ),
                filasInsumos
        );

        // Gráfica Top 10 Insumos por impacto económico
        result.topInsumosChart = listaInsumos.stream().limit(10).map(i ->
                ChartPuntoDTO.deMoneda(i.nombre, redondear(i.consumoTotal), BigDecimal.valueOf(i.costoTotal()), i.unidad, i.area, i.area)
        ).toList();

        // 5. Tabla de Stock Disponible al Cierre
        List<Map<String, Object>> filasStock = new ArrayList<>();
        for (Insumo ins : insumosTenant) {
            boolean esBebida = ins.isEsBebida();
            String areaIns = esBebida ? "BARRA" : "COCINA";

            if (!cumpleArea(areaIns, areaFiltro)) {
                continue;
            }

            double stockArea = "BARRA".equals(areaFiltro) ? (ins.getStockBarra() != null ? ins.getStockBarra() : 0.0)
                    : ("COCINA".equals(areaFiltro) ? (ins.getStockCocina() != null ? ins.getStockCocina() : 0.0)
                    : (ins.getStock() != null ? ins.getStock() : 0.0));

            double stockMin = ins.getStockMinimo() != null ? ins.getStockMinimo() : 0.0;
            double stockBodega = ins.getStockBodega() != null ? ins.getStockBodega() : 0.0;

            String estado;
            if (stockArea <= 0.0001) {
                estado = "Agotado";
                result.insumosEnAlerta++;
            } else if (stockArea < stockMin) {
                estado = "Bajo";
                result.insumosEnAlerta++;
            } else {
                estado = "Óptimo";
            }

            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("insumo", ins.getNombre());
            fila.put("unidad", ins.getUnidad() != null ? ins.getUnidad() : "pz");
            fila.put("stockArea", redondear(stockArea));
            fila.put("stockBodega", redondear(stockBodega));
            fila.put("stockMinimo", redondear(stockMin));
            fila.put("estado", estado);
            filasStock.add(fila);
        }

        filasStock.sort(Comparator.comparing((Map<String, Object> m) -> {
            String est = (String) m.get("estado");
            if ("Agotado".equals(est)) return 1;
            if ("Bajo".equals(est)) return 2;
            return 3;
        }));

        result.tablaStock = new TablaReporteDTO(
                List.of(
                        ColumnaDTO.de("insumo", "Insumo", TipoColumna.TEXTO),
                        ColumnaDTO.de("unidad", "Unidad", TipoColumna.TEXTO),
                        ColumnaDTO.de("stockArea", "Stock en " + ("TODOS".equals(areaFiltro) ? "Cocina/Barra" : areaFiltro), TipoColumna.NUMERO),
                        ColumnaDTO.de("stockBodega", "Stock en Bodega", TipoColumna.NUMERO),
                        ColumnaDTO.de("stockMinimo", "Stock Mínimo", TipoColumna.NUMERO),
                        ColumnaDTO.de("estado", "Estado de Disponibilidad", TipoColumna.TEXTO)
                ),
                filasStock
        );

        return result;
    }

    private String normalizarArea(String area) {
        if (area == null || area.isBlank()) return "TODOS";
        String u = area.trim().toUpperCase();
        if ("COCINA".equals(u) || "BARRA".equals(u)) return u;
        return "TODOS";
    }

    private boolean cumpleArea(String areaElemento, String areaFiltro) {
        if ("TODOS".equals(areaFiltro)) return true;
        return areaFiltro.equalsIgnoreCase(areaElemento);
    }

    private String determinarAreaProducto(TenantMenuProduct prod, Set<Long> barraProductIds) {
        if (barraProductIds.contains(prod.getId())) return "BARRA";
        if (prod.getCategory() != null) {
            String catName = prod.getCategory().getNombre().toLowerCase();
            if (catName.contains("bebida") || catName.contains("coctel") || catName.contains("cocktail")
                    || catName.contains("cerveza") || catName.contains("trago") || catName.contains("licor")
                    || catName.contains("bar") || catName.contains("vino") || catName.contains("cafe")
                    || catName.contains("cafeteria")) {
                return "BARRA";
            }
        }
        return "COCINA";
    }

    private Map<Long, Double> obtenerCostosPromedio(Long tenantId) {
        Map<Long, Double> costos = new HashMap<>();
        List<Object[]> filas = restockHistoryRepository.sumCostoYCantidadPorInsumo(tenantId);
        if (filas != null) {
            for (Object[] fila : filas) {
                Long insumoId = ((Number) fila[0]).longValue();
                double costoTotal = ((Number) fila[1]).doubleValue();
                double cantidad = ((Number) fila[2]).doubleValue();
                costos.put(insumoId, cantidad > 0 ? costoTotal / cantidad : 0.0);
            }
        }
        return costos;
    }

    private HojaExcelDTO hojaDesdeTabla(TablaReporteDTO tabla, String nombreHoja, String titulo, String subtitulo) {
        List<String> encabezados = tabla.columnas().stream().map(ColumnaDTO::label).toList();
        List<TipoColumna> tipos = tabla.columnas().stream().map(ColumnaDTO::tipo).toList();

        List<List<Object>> filas = new ArrayList<>();
        for (Map<String, Object> filaMap : tabla.filas()) {
            List<Object> fila = new ArrayList<>();
            for (ColumnaDTO col : tabla.columnas()) {
                fila.add(filaMap.get(col.key()));
            }
            filas.add(fila);
        }

        return HojaExcelDTO.de(nombreHoja, titulo, subtitulo, encabezados, tipos, filas);
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

    private double redondear(double val) {
        return Math.round(val * 100.0) / 100.0;
    }

    // Helper classes
    private static class ProcessedAreaPeriod {
        int totalUnidadesVendidas = 0;
        int variedadInsumosUsados = 0;
        BigDecimal costoTotalInsumos = BigDecimal.ZERO;
        int insumosEnAlerta = 0;
        TablaReporteDTO tablaPlatillos;
        TablaReporteDTO tablaInsumos;
        TablaReporteDTO tablaStock;
        List<ChartPuntoDTO> topPlatillosChart = List.of();
        List<ChartPuntoDTO> topInsumosChart = List.of();
    }

    private static class PlatilloAcumulado {
        final Long id;
        final String nombre;
        final String categoria;
        final String area;
        final BigDecimal precio;
        int cantidad = 0;
        BigDecimal totalVenta = BigDecimal.ZERO;

        PlatilloAcumulado(Long id, String nombre, String categoria, String area, BigDecimal precio) {
            this.id = id;
            this.nombre = nombre;
            this.categoria = categoria;
            this.area = area;
            this.precio = precio;
        }

        void agregar(int qty, BigDecimal venta) {
            this.cantidad += qty;
            this.totalVenta = this.totalVenta.add(venta);
        }
    }

    private static class InsumoConsumidoAcumulado {
        final Long id;
        final String nombre;
        final String unidad;
        final String area;
        final double costoUnitario;
        double consumoTotal = 0.0;

        InsumoConsumidoAcumulado(Long id, String nombre, String unidad, String area, double costoUnitario) {
            this.id = id;
            this.nombre = nombre;
            this.unidad = unidad;
            this.area = area;
            this.costoUnitario = costoUnitario;
        }

        void acumular(double cant) {
            this.consumoTotal += cant;
        }

        double costoTotal() {
            return this.consumoTotal * this.costoUnitario;
        }
    }
}
