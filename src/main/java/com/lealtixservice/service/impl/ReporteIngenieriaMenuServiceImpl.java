package com.lealtixservice.service.impl;

import com.lealtixservice.dto.reportes.*;
import com.lealtixservice.entity.ClientOrderItem;
import com.lealtixservice.entity.Insumo;
import com.lealtixservice.entity.ProductRecipe;
import com.lealtixservice.entity.ProductSubReceta;
import com.lealtixservice.entity.TenantMenuProduct;
import com.lealtixservice.repository.*;
import com.lealtixservice.service.ReporteExcelService;
import com.lealtixservice.service.ReporteIngenieriaMenuService;
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
public class ReporteIngenieriaMenuServiceImpl implements ReporteIngenieriaMenuService {

    private static final String REPORTE_KEY = "2.1_ingenieria_menu";
    private static final String PILAR = "2. Menu e Inventario";
    private static final String NOMBRE = "Ingeniería de Menú";

    private final ClientOrderItemRepository clientOrderItemRepository;
    private final ProductRecipeRepository productRecipeRepository;
    private final ProductSubRecetaRepository productSubRecetaRepository;
    private final InsumoRepository insumoRepository;
    private final RestockHistoryRepository restockHistoryRepository;
    private final DateRangeResolver dateRangeResolver;
    private final ReporteExcelService excelService;

    @Override
    public IngenieriaMenuDTO obtener(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to, String area) {
        PeriodoComparativo periodo = dateRangeResolver.resolver(preset, from, to);
        return construir(tenantId, periodo, normalizarArea(area));
    }

    @Override
    public byte[] exportar(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to, String area) {
        PeriodoComparativo periodo = dateRangeResolver.resolver(preset, from, to);
        String areaNorm = normalizarArea(area);
        IngenieriaMenuDTO datos = construir(tenantId, periodo, areaNorm);

        String subtitulo = "Periodo: " + periodo.actual().etiqueta()
                + " (" + periodo.actual().from().toLocalDate() + " a " + periodo.actual().to().toLocalDate() + ")"
                + " | Área: " + areaNorm
                + " | Margen Promedio: $" + redondear(datos.umbralMargen().doubleValue())
                + " | Umbral Popularidad: " + redondear(datos.umbralPopularidad()) + " u.";

        List<HojaExcelDTO> hojas = new ArrayList<>();

        // 1. Resumen de Cuadrantes
        List<List<Object>> filasCuadrantes = new ArrayList<>();
        for (CuadranteResumenDTO c : datos.resumenCuadrantes().values()) {
            filasCuadrantes.add(List.of(
                    c.etiqueta(),
                    c.cantidadPlatillos(),
                    c.unidadesVendidas(),
                    c.ingresosTotales(),
                    c.margenTotal(),
                    c.porcentajeVentas(),
                    c.estrategia()
            ));
        }

        hojas.add(HojaExcelDTO.de(
                "Resumen Cuadrantes",
                "Ingeniería de Menú - Matriz Kasavana & Smith",
                subtitulo,
                List.of("Cuadrante", "Platillos", "Unidades Vendidas", "Ventas Totales ($)", "Margen Total ($)", "% Mix Ventas", "Estrategia Recomendada"),
                List.of(TipoColumna.TEXTO, TipoColumna.NUMERO, TipoColumna.NUMERO, TipoColumna.MONEDA, TipoColumna.MONEDA, TipoColumna.PORCENTAJE, TipoColumna.TEXTO),
                filasCuadrantes
        ));

        // 2. Matriz Completa de Platillos
        hojas.add(hojaDesdeTabla(datos.matrizProductos(), "Matriz de Platillos",
                "Clasificación y Rentabilidad de Platillos",
                "Desglose de precio, costo de receta, margen unitario, volumen y clasificación estratégica"));

        return excelService.generar(excelService.nombreArchivo("ingenieria_menu_" + areaNorm.toLowerCase()), hojas);
    }

    private IngenieriaMenuDTO construir(Long tenantId, PeriodoComparativo periodo, String areaFiltro) {
        LocalDateTime actualFrom = periodo.actual().from();
        LocalDateTime actualToEx = dateRangeResolver.aExclusivo(periodo.actual().to());
        LocalDateTime anteriorFrom = periodo.anterior().from();
        LocalDateTime anteriorToEx = dateRangeResolver.aExclusivo(periodo.anterior().to());

        List<ClientOrderItem> itemsActual = clientOrderItemRepository.findNonCancelledItemsInPeriod(tenantId, actualFrom, actualToEx);
        List<ClientOrderItem> itemsAnterior = clientOrderItemRepository.findNonCancelledItemsInPeriod(tenantId, anteriorFrom, anteriorToEx);

        List<Insumo> insumosTenant = insumoRepository.findByTenantIdAndIsActiveTrueOrderByNombreAsc(tenantId);
        Map<Long, Double> costosPromedio = obtenerCostosPromedio(tenantId);

        Set<Long> barraProductIds = insumosTenant.stream()
                .filter(Insumo::isEsBebida)
                .map(Insumo::getProductoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Procesar periodo actual y anterior
        MatrizCalculada actual = calcularMatriz(itemsActual, barraProductIds, costosPromedio, areaFiltro);
        MatrizCalculada anterior = calcularMatriz(itemsAnterior, barraProductIds, costosPromedio, areaFiltro);

        // Construir KPIs
        List<KpiDTO> kpis = List.of(
                KpiDTO.de("totalPlatillos", "Platillos Analizados", (double) actual.productos.size(), (double) anterior.productos.size(), "numero"),
                KpiDTO.de("margenPromedio", "Margen Contribución Prom.", actual.margenPromedioPonderado.doubleValue(), anterior.margenPromedioPonderado.doubleValue(), "moneda"),
                KpiDTO.de("foodCostPct", "Food Cost Promedio (% Ventas)", actual.foodCostPromedioPct, anterior.foodCostPromedioPct, "porcentaje"),
                KpiDTO.de("estrellas", "Platillos Estrella (⭐)", (double) actual.conteoEstrellas, (double) anterior.conteoEstrellas, "numero"),
                KpiDTO.de("perros", "Platillos Perro (🐕)", (double) actual.conteoPerros, (double) anterior.conteoPerros, "numero")
        );

        ReporteMetaDTO meta = ReporteMetaDTO.de(REPORTE_KEY, PILAR, NOMBRE, periodo, "day");

        return IngenieriaMenuDTO.de(
                meta,
                kpis,
                actual.umbralPopularidad,
                actual.margenPromedioPonderado,
                actual.resumenCuadrantes,
                actual.tablaReporte,
                actual.puntosGrafica
        );
    }

    private MatrizCalculada calcularMatriz(
            List<ClientOrderItem> items,
            Set<Long> barraProductIds,
            Map<Long, Double> costosPromedio,
            String areaFiltro
    ) {
        MatrizCalculada resultado = new MatrizCalculada();

        // Agrupar ventas por producto
        Map<Long, ItemVentas> ventasPorProducto = new LinkedHashMap<>();
        for (ClientOrderItem item : items) {
            TenantMenuProduct prod = item.getProduct();
            if (prod == null) continue;

            String area = determinarAreaProducto(prod, barraProductIds);
            if (!cumpleArea(area, areaFiltro)) continue;

            int qty = item.getCantidad() != null ? item.getCantidad() : 1;
            BigDecimal precio = item.getPrecioUnitario() != null ? item.getPrecioUnitario() : (prod.getPrecio() != null ? prod.getPrecio() : BigDecimal.ZERO);

            ventasPorProducto.computeIfAbsent(prod.getId(), k -> new ItemVentas(prod, area))
                    .agregar(qty, precio);
        }

        if (ventasPorProducto.isEmpty()) {
            resultado.tablaReporte = new TablaReporteDTO(crearColumnasTabla(), List.of());
            resultado.resumenCuadrantes = crearCuadrantesVacios();
            return resultado;
        }

        Set<Long> dishIds = ventasPorProducto.keySet();
        List<ProductRecipe> recetasDirectas = productRecipeRepository.findByDishIdInWithInsumo(dishIds);
        List<ProductSubReceta> subRecetasAsignadas = productSubRecetaRepository.findByDishIdInWithSubReceta(dishIds);

        Map<Long, List<ProductRecipe>> recetasPorPlatillo = recetasDirectas.stream()
                .collect(Collectors.groupingBy(r -> r.getDish().getId()));
        Map<Long, List<ProductSubReceta>> subRecetasPorPlatillo = subRecetasAsignadas.stream()
                .collect(Collectors.groupingBy(s -> s.getDish().getId()));

        Set<Long> subRecetaIds = subRecetasAsignadas.stream()
                .map(s -> s.getSubReceta().getId())
                .collect(Collectors.toSet());
        List<ProductRecipe> recetasDeSubRecetas = subRecetaIds.isEmpty() ? List.of() : productRecipeRepository.findByDishIdInWithInsumo(subRecetaIds);
        Map<Long, List<ProductRecipe>> insumosPorSubReceta = recetasDeSubRecetas.stream()
                .collect(Collectors.groupingBy(r -> r.getDish().getId()));

        // Calcular costo de receta para cada producto
        long totalUnidadesVendidas = 0;
        BigDecimal totalIngresosGlobal = BigDecimal.ZERO;
        BigDecimal totalMargenGlobal = BigDecimal.ZERO;
        BigDecimal totalCostoGlobal = BigDecimal.ZERO;

        List<ProductoCalculado> productosCalculados = new ArrayList<>();

        for (ItemVentas v : ventasPorProducto.values()) {
            TenantMenuProduct prod = v.producto;
            int unidades = v.unidades;
            BigDecimal ingresos = v.totalIngresos;
            BigDecimal precioUnit = unidades > 0 ? ingresos.divide(BigDecimal.valueOf(unidades), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            // Costo directo
            double costoRecetaDouble = 0.0;
            List<ProductRecipe> directas = recetasPorPlatillo.getOrDefault(prod.getId(), List.of());
            for (ProductRecipe r : directas) {
                if (r.getInsumo() == null) continue;
                double cant = r.getCantidad() != null ? r.getCantidad().doubleValue() : 0.0;
                double cost = costosPromedio.getOrDefault(r.getInsumo().getId(), 0.0);
                costoRecetaDouble += (cant * cost);
            }

            // Costo subrecetas
            List<ProductSubReceta> subs = subRecetasPorPlatillo.getOrDefault(prod.getId(), List.of());
            for (ProductSubReceta s : subs) {
                TenantMenuProduct sub = s.getSubReceta();
                if (sub == null) continue;
                double tamanoLote = sub.getTamanoLote() != null && sub.getTamanoLote() > 0 ? sub.getTamanoLote() : 1.0;
                double porcion = s.getCantidad() != null && s.getCantidad() > 0 ? s.getCantidad() : 1.0;
                double ratio = porcion / tamanoLote;

                List<ProductRecipe> lineasSub = insumosPorSubReceta.getOrDefault(sub.getId(), List.of());
                for (ProductRecipe line : lineasSub) {
                    if (line.getInsumo() == null) continue;
                    double cant = line.getCantidad() != null ? line.getCantidad().doubleValue() : 0.0;
                    double cost = costosPromedio.getOrDefault(line.getInsumo().getId(), 0.0);
                    costoRecetaDouble += (cant * ratio * cost);
                }
            }

            BigDecimal costoReceta = BigDecimal.valueOf(costoRecetaDouble).setScale(2, RoundingMode.HALF_UP);
            BigDecimal margenUnit = precioUnit.subtract(costoReceta);
            BigDecimal margenTotal = margenUnit.multiply(BigDecimal.valueOf(unidades));
            BigDecimal costoTotal = costoReceta.multiply(BigDecimal.valueOf(unidades));
            double margenPct = precioUnit.compareTo(BigDecimal.ZERO) > 0 ? (margenUnit.doubleValue() / precioUnit.doubleValue()) * 100.0 : 0.0;

            totalUnidadesVendidas += unidades;
            totalIngresosGlobal = totalIngresosGlobal.add(ingresos);
            totalMargenGlobal = totalMargenGlobal.add(margenTotal);
            totalCostoGlobal = totalCostoGlobal.add(costoTotal);

            productosCalculados.add(new ProductoCalculado(
                    prod.getId(),
                    prod.getNombre(),
                    prod.getCategory() != null ? prod.getCategory().getNombre() : "General",
                    v.area,
                    precioUnit,
                    costoReceta,
                    margenUnit,
                    margenPct,
                    unidades,
                    ingresos,
                    margenTotal
            ));
        }

        int totalPlatillos = productosCalculados.size();
        resultado.productos = productosCalculados;

        // Umbrales Kasavana & Smith
        // Margen Promedio Ponderado
        BigDecimal margenPromedioPonderado = totalUnidadesVendidas > 0
                ? totalMargenGlobal.divide(BigDecimal.valueOf(totalUnidadesVendidas), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        resultado.margenPromedioPonderado = margenPromedioPonderado;

        // Umbral de popularidad: 70% del promedio de unidades vendidas por platillo
        double umbralPopularidad = totalPlatillos > 0 ? ((double) totalUnidadesVendidas / totalPlatillos) * 0.70 : 0.0;
        resultado.umbralPopularidad = redondear(umbralPopularidad);

        // Food Cost promedio (%)
        resultado.foodCostPromedioPct = totalIngresosGlobal.compareTo(BigDecimal.ZERO) > 0
                ? redondear((totalCostoGlobal.doubleValue() / totalIngresosGlobal.doubleValue()) * 100.0)
                : 0.0;

        // Clasificación en los 4 cuadrantes
        Map<String, CuadranteAcumulador> cuadrantesMap = new LinkedHashMap<>();
        cuadrantesMap.put("ESTRELLA", new CuadranteAcumulador("ESTRELLA", "Estrellas (⭐)", "pi-star", "#10b981",
                "Mantener rigurosamente receta y calidad. Proteger su posición visual privilegiada en el menú."));
        cuadrantesMap.put("CABALLO_BATALLA", new CuadranteAcumulador("CABALLO_BATALLA", "Caballos de Batalla (🐎)", "pi-bolt", "#6366f1",
                "Alta demanda pero bajo margen. Evaluar incremento moderado de precio o ajustar costos/porciones de insumos."));
        cuadrantesMap.put("ROMPECABEZAS", new CuadranteAcumulador("ROMPECABEZAS", "Rompecabezas (🧩)", "pi-question", "#f59e0b",
                "Alta rentabilidad con baja rotación. Promocionar activamente en comandas, fotografía atractiva y sugerencia de meseros."));
        cuadrantesMap.put("PERRO", new CuadranteAcumulador("PERRO", "Perros (🐕)", "pi-trash", "#ef4444",
                "Baja venta y baja ganancia. Considerar retirar del menú o rediseñar totalmente la preparación."));

        List<Map<String, Object>> filasMatriz = new ArrayList<>();
        List<PuntoMatrizDTO> puntos = new ArrayList<>();

        for (ProductoCalculado p : productosCalculados) {
            boolean altaPopularidad = p.unidades >= umbralPopularidad;
            boolean altoMargen = p.margenUnit.compareTo(margenPromedioPonderado) >= 0;

            String cuadrante;
            String estrategia;

            if (altaPopularidad && altoMargen) {
                cuadrante = "ESTRELLA";
                resultado.conteoEstrellas++;
            } else if (altaPopularidad && !altoMargen) {
                cuadrante = "CABALLO_BATALLA";
            } else if (!altaPopularidad && altoMargen) {
                cuadrante = "ROMPECABEZAS";
            } else {
                cuadrante = "PERRO";
                resultado.conteoPerros++;
            }

            CuadranteAcumulador acum = cuadrantesMap.get(cuadrante);
            acum.agregar(p.unidades, p.ingresosTotales, p.margenTotal);
            estrategia = acum.estrategia;

            double mixPct = totalUnidadesVendidas > 0 ? redondear(((double) p.unidades / totalUnidadesVendidas) * 100.0) : 0.0;

            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("producto", p.nombre);
            fila.put("categoria", p.categoria);
            fila.put("area", p.area);
            fila.put("precioVenta", p.precioUnit);
            fila.put("costoReceta", p.costoReceta);
            fila.put("margenUnitario", p.margenUnit);
            fila.put("margenPct", redondear(p.margenPct));
            fila.put("unidadesVendidas", p.unidades);
            fila.put("mixVentasPct", mixPct);
            fila.put("ingresosTotales", p.ingresosTotales);
            fila.put("margenTotal", p.margenTotal);
            fila.put("cuadrante", acum.etiqueta);
            fila.put("cuadranteKey", cuadrante);
            fila.put("estrategia", estrategia);
            filasMatriz.add(fila);

            puntos.add(new PuntoMatrizDTO(
                    p.id,
                    p.nombre,
                    p.categoria,
                    cuadrante,
                    (double) p.unidades,
                    p.margenUnit,
                    p.precioUnit,
                    p.costoReceta,
                    redondear(p.margenPct),
                    (long) p.unidades,
                    p.ingresosTotales,
                    p.margenTotal
            ));
        }

        filasMatriz.sort(Comparator.comparing((Map<String, Object> m) -> {
            String k = (String) m.get("cuadranteKey");
            return switch (k) {
                case "ESTRELLA" -> 1;
                case "CABALLO_BATALLA" -> 2;
                case "ROMPECABEZAS" -> 3;
                default -> 4;
            };
        }).thenComparing((Map<String, Object> m) -> ((BigDecimal) m.get("margenTotal")).negate()));

        resultado.tablaReporte = new TablaReporteDTO(crearColumnasTabla(), filasMatriz);
        resultado.puntosGrafica = puntos;

        // Construir mapa de resumen
        Map<String, CuadranteResumenDTO> resumen = new LinkedHashMap<>();
        for (Map.Entry<String, CuadranteAcumulador> e : cuadrantesMap.entrySet()) {
            CuadranteAcumulador a = e.getValue();
            double pctVentas = totalUnidadesVendidas > 0 ? redondear(((double) a.unidades / totalUnidadesVendidas) * 100.0) : 0.0;
            double pctMargen = totalMargenGlobal.compareTo(BigDecimal.ZERO) > 0 ? redondear((a.margenTotal.doubleValue() / totalMargenGlobal.doubleValue()) * 100.0) : 0.0;

            resumen.put(e.getKey(), new CuadranteResumenDTO(
                    a.clave,
                    a.etiqueta,
                    a.icono,
                    a.color,
                    a.cantidadPlatillos,
                    a.unidades,
                    a.ingresosTotales,
                    a.margenTotal,
                    pctVentas,
                    pctMargen,
                    a.estrategia
            ));
        }
        resultado.resumenCuadrantes = resumen;

        return resultado;
    }

    private List<ColumnaDTO> crearColumnasTabla() {
        return List.of(
                ColumnaDTO.de("producto", "Platillo / Bebida", TipoColumna.TEXTO),
                ColumnaDTO.de("categoria", "Categoría", TipoColumna.TEXTO),
                ColumnaDTO.de("area", "Área", TipoColumna.TEXTO),
                ColumnaDTO.de("precioVenta", "Precio ($)", TipoColumna.MONEDA),
                ColumnaDTO.de("costoReceta", "Costo Receta ($)", TipoColumna.MONEDA),
                ColumnaDTO.de("margenUnitario", "Margen ($)", TipoColumna.MONEDA),
                ColumnaDTO.de("margenPct", "Margen (%)", TipoColumna.PORCENTAJE),
                ColumnaDTO.de("unidadesVendidas", "Unidades Vendidas", TipoColumna.NUMERO),
                ColumnaDTO.de("mixVentasPct", "Mix Ventas (%)", TipoColumna.PORCENTAJE),
                ColumnaDTO.de("ingresosTotales", "Ingresos Totales ($)", TipoColumna.MONEDA),
                ColumnaDTO.de("margenTotal", "Margen Total ($)", TipoColumna.MONEDA),
                ColumnaDTO.de("cuadrante", "Cuadrante", TipoColumna.TEXTO),
                ColumnaDTO.de("estrategia", "Estrategia Recomendada", TipoColumna.TEXTO)
        );
    }

    private Map<String, CuadranteResumenDTO> crearCuadrantesVacios() {
        Map<String, CuadranteResumenDTO> m = new LinkedHashMap<>();
        m.put("ESTRELLA", new CuadranteResumenDTO("ESTRELLA", "Estrellas (⭐)", "pi-star", "#10b981", 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, 0.0, 0.0, "Mantener receta y calidad"));
        m.put("CABALLO_BATALLA", new CuadranteResumenDTO("CABALLO_BATALLA", "Caballos de Batalla (🐎)", "pi-bolt", "#6366f1", 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, 0.0, 0.0, "Ajustar precio o porciones"));
        m.put("ROMPECABEZAS", new CuadranteResumenDTO("ROMPECABEZAS", "Rompecabezas (🧩)", "pi-question", "#f59e0b", 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, 0.0, 0.0, "Impulsar promoción y combos"));
        m.put("PERRO", new CuadranteResumenDTO("PERRO", "Perros (🐕)", "pi-trash", "#ef4444", 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, 0.0, 0.0, "Evaluar retiro o rediseño"));
        return m;
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

    private double redondear(double val) {
        return Math.round(val * 100.0) / 100.0;
    }

    // Helper classes
    private static class MatrizCalculada {
        List<ProductoCalculado> productos = List.of();
        BigDecimal margenPromedioPonderado = BigDecimal.ZERO;
        double umbralPopularidad = 0.0;
        double foodCostPromedioPct = 0.0;
        int conteoEstrellas = 0;
        int conteoPerros = 0;
        Map<String, CuadranteResumenDTO> resumenCuadrantes = Map.of();
        TablaReporteDTO tablaReporte;
        List<PuntoMatrizDTO> puntosGrafica = List.of();
    }

    private static class ItemVentas {
        final TenantMenuProduct producto;
        final String area;
        int unidades = 0;
        BigDecimal totalIngresos = BigDecimal.ZERO;

        ItemVentas(TenantMenuProduct producto, String area) {
            this.producto = producto;
            this.area = area;
        }

        void agregar(int qty, BigDecimal precio) {
            this.unidades += qty;
            this.totalIngresos = this.totalIngresos.add(precio.multiply(BigDecimal.valueOf(qty)));
        }
    }

    private static class ProductoCalculado {
        final Long id;
        final String nombre;
        final String categoria;
        final String area;
        final BigDecimal precioUnit;
        final BigDecimal costoReceta;
        final BigDecimal margenUnit;
        final double margenPct;
        final int unidades;
        final BigDecimal ingresosTotales;
        final BigDecimal margenTotal;

        ProductoCalculado(Long id, String nombre, String categoria, String area, BigDecimal precioUnit,
                          BigDecimal costoReceta, BigDecimal margenUnit, double margenPct, int unidades,
                          BigDecimal ingresosTotales, BigDecimal margenTotal) {
            this.id = id;
            this.nombre = nombre;
            this.categoria = categoria;
            this.area = area;
            this.precioUnit = precioUnit;
            this.costoReceta = costoReceta;
            this.margenUnit = margenUnit;
            this.margenPct = margenPct;
            this.unidades = unidades;
            this.ingresosTotales = ingresosTotales;
            this.margenTotal = margenTotal;
        }
    }

    private static class CuadranteAcumulador {
        final String clave;
        final String etiqueta;
        final String icono;
        final String color;
        final String estrategia;
        int cantidadPlatillos = 0;
        long unidades = 0;
        BigDecimal ingresosTotales = BigDecimal.ZERO;
        BigDecimal margenTotal = BigDecimal.ZERO;

        CuadranteAcumulador(String clave, String etiqueta, String icono, String color, String estrategia) {
            this.clave = clave;
            this.etiqueta = etiqueta;
            this.icono = icono;
            this.color = color;
            this.estrategia = estrategia;
        }

        void agregar(long cant, BigDecimal ingresos, BigDecimal margen) {
            this.cantidadPlatillos++;
            this.unidades += cant;
            this.ingresosTotales = this.ingresosTotales.add(ingresos);
            this.margenTotal = this.margenTotal.add(margen);
        }
    }
}
