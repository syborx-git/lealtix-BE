package com.lealtixservice.service.impl;

import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.entity.Insumo;
import com.lealtixservice.entity.ProductAdditional;
import com.lealtixservice.entity.ProductRecipe;
import com.lealtixservice.entity.ProductSubReceta;
import com.lealtixservice.entity.RestockHistory;
import com.lealtixservice.entity.StockTransfer;
import com.lealtixservice.entity.Tenant;
import com.lealtixservice.entity.TenantMenuCategory;
import com.lealtixservice.entity.TenantMenuProduct;
import com.lealtixservice.exception.ResourceNotFoundException;
import com.lealtixservice.repository.InsumoRepository;
import com.lealtixservice.repository.ProductAdditionalRepository;
import com.lealtixservice.repository.ProductRecipeRepository;
import com.lealtixservice.repository.ProductSubRecetaRepository;
import com.lealtixservice.repository.RestockHistoryRepository;
import com.lealtixservice.repository.StockTransferRepository;
import com.lealtixservice.repository.TenantMenuCategoryRepository;
import com.lealtixservice.repository.TenantMenuProductRepository;
import com.lealtixservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final TenantMenuProductRepository productRepository;
    private final ProductRecipeRepository recipeRepository;
    private final ProductAdditionalRepository additionalRepository;
    private final ProductSubRecetaRepository subRecetaRepository;
    private final InsumoRepository insumoRepository;
    private final RestockHistoryRepository restockHistoryRepository;
    private final StockTransferRepository stockTransferRepository;
    private final TenantMenuCategoryRepository categoryRepository;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @Override
    public GenericResponse getInventoryByTenant(Long tenantId) {
        List<TenantMenuProduct> products = productRepository.findAllByTenantId(tenantId);
        List<Map<String, Object>> items = new ArrayList<>();
        for (TenantMenuProduct p : products) {
            boolean dish = isDish(p);
            double stock = dish ? stockDe(p) : safeStock(p);
            double min = dish ? stockMinDe(p) : safeMin(p);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", p.getId());
            item.put("name", p.getNombre());
            item.put("description", p.getDescripcion());
            item.put("categoryId", p.getCategory() != null ? p.getCategory().getId() : null);
            item.put("categoryName", p.getCategory() != null ? p.getCategory().getNombre() : null);
            item.put("categories", buildCategoryMaps(p.getCategories()));
            item.put("categoryIds", buildCategoryIds(p.getCategories()));
            item.put("price", p.getPrecio());
            item.put("imageUrl", p.getImgUrl());
            item.put("stock", stock);
            item.put("stockMinimo", min);
            item.put("unidad", p.getUnidad() != null ? p.getUnidad() : "pieza");
            item.put("esPlatillo", dish);
            item.put("esSubReceta", p.getEsSubReceta() != null && p.getEsSubReceta());
            item.put("insumos", buildInsumosList(p.getId()));
            item.put("adicionales", buildAdicionalesList(p.getId()));
            item.put("subRecetas", buildSubRecetasList(p.getId()));
            item.put("lowStock", min > 0 && stock <= min);
            item.put("outOfStock", stock <= 0);
            items.add(item);
        }
        return new GenericResponse(200, "Inventario obtenido", items);
    }

    /* ============ Insumos (catálogo compartido) ============ */

    @Override
    @Transactional(readOnly = true)
    public GenericResponse getInsumosByTenant(Long tenantId) {
        List<Insumo> insumos = insumoRepository.findByTenantIdAndIsActiveTrueOrderByNombreAsc(tenantId).stream()
                .filter(i -> !i.isEsBebida())
                .collect(java.util.stream.Collectors.toList());
        List<Map<String, Object>> items = new ArrayList<>();
        for (Insumo i : insumos) {
            items.add(insumoToMap(i));
        }
        return new GenericResponse(200, "Insumos obtenidos", items);
    }

    @Override
    @Transactional
    public GenericResponse createInsumo(Long tenantId, String nombre, String unidad, Double stock, Double stockMinimo, List<Long> categoryIds) {
        if (tenantId == null || nombre == null || nombre.isBlank()) {
            return new GenericResponse(400, "Tenant y nombre son requeridos", null);
        }
        Insumo insumo = Insumo.builder()
                .tenantId(tenantId)
                .nombre(nombre.trim())
                .unidad(unidad != null ? unidad : "pieza")
                .stock(stock != null ? stock : 0.0)
                .stockBodega(0.0)
                .stockCocina(stock != null ? stock : 0.0)
                .stockBarra(0.0)
                .stockMinimo(stockMinimo != null ? stockMinimo : 0.0)
                .isActive(true)
                .build();
        insumo.setCategories(resolveCategories(tenantId, categoryIds));
        insumoRepository.save(insumo);
        return new GenericResponse(200, "Insumo creado", insumoToMap(insumo));
    }

    @Override
    @Transactional
    public GenericResponse updateInsumo(Long insumoId, String nombre, String unidad, Double stock, Double stockMinimo, List<Long> categoryIds) {
        Insumo insumo = findInsumo(insumoId);
        if (nombre != null && !nombre.isBlank()) insumo.setNombre(nombre.trim());
        if (unidad != null && !unidad.isBlank()) insumo.setUnidad(unidad);
        if (stock != null) {
            aplicarStockDistribuido(insumo, Math.max(0, stock), false);
        }
        if (stockMinimo != null) insumo.setStockMinimo(Math.max(0, stockMinimo));
        if (categoryIds != null) {
            insumo.setCategories(resolveCategories(insumo.getTenantId(), categoryIds));
        }
        insumoRepository.save(insumo);
        syncAvailability(insumo.getTenantId());
        return new GenericResponse(200, "Insumo actualizado", insumoToMap(insumo));
    }

    @Override
    @Transactional
    public GenericResponse deleteInsumo(Long insumoId) {
        Insumo insumo = findInsumo(insumoId);

        // 1. Limpiar recetas donde este insumo esté presente
        entityManager.createNativeQuery("DELETE FROM product_recipe WHERE insumo_id = :id")
                .setParameter("id", insumoId)
                .executeUpdate();

        // 2. Limpiar adicionales donde este insumo esté presente
        entityManager.createNativeQuery("DELETE FROM product_additional WHERE insumo_id = :id")
                .setParameter("id", insumoId)
                .executeUpdate();

        // 3. Limpiar recetas de bebidas
        entityManager.createNativeQuery("DELETE FROM bebida_receta WHERE insumo_id = :id")
                .setParameter("id", insumoId)
                .executeUpdate();

        // 4. Limpiar categorías del insumo
        entityManager.createNativeQuery("DELETE FROM insumo_category WHERE insumo_id = :id")
                .setParameter("id", insumoId)
                .executeUpdate();

        // 5. Limpiar transferencias, restock, mermas, solicitudes y bebidas
        entityManager.createNativeQuery("DELETE FROM stock_transfer_history WHERE insumo_id = :id")
                .setParameter("id", insumoId)
                .executeUpdate();

        entityManager.createNativeQuery("DELETE FROM restock_history WHERE insumo_id = :id")
                .setParameter("id", insumoId)
                .executeUpdate();

        entityManager.createNativeQuery("DELETE FROM merma WHERE insumo_id = :id")
                .setParameter("id", insumoId)
                .executeUpdate();

        entityManager.createNativeQuery("DELETE FROM stock_request WHERE insumo_id = :id")
                .setParameter("id", insumoId)
                .executeUpdate();

        entityManager.createNativeQuery("DELETE FROM bebida WHERE insumo_id = :id")
                .setParameter("id", insumoId)
                .executeUpdate();

        // 5. Si tiene producto enlazado (bebida vendible)
        if (insumo.getProductoId() != null) {
            Long prodId = insumo.getProductoId();
            insumo.setProductoId(null);
            insumoRepository.saveAndFlush(insumo);
            try {
                entityManager.createNativeQuery("DELETE FROM client_order_item WHERE product_id = :prodId")
                        .setParameter("prodId", prodId).executeUpdate();
                entityManager.createNativeQuery("DELETE FROM product_recipe WHERE dish_product_id = :prodId")
                        .setParameter("prodId", prodId).executeUpdate();
                entityManager.createNativeQuery("DELETE FROM product_additional WHERE dish_product_id = :prodId")
                        .setParameter("prodId", prodId).executeUpdate();
                entityManager.createNativeQuery("DELETE FROM tenant_menu_product_category WHERE product_id = :prodId")
                        .setParameter("prodId", prodId).executeUpdate();
                productRepository.deleteById(prodId);
            } catch (Exception e) {
                log.warn("Error al borrar producto enlazado de bebida: {}", e.getMessage());
            }
        }

        insumoRepository.delete(insumo);
        return new GenericResponse(200, "Insumo eliminado exitosamente", null);
    }

    @Override
    @Transactional
    public GenericResponse restockInsumo(Long insumoId, Double cantidad, Double costoTotal) {
        if (cantidad == null || cantidad <= 0) {
            return new GenericResponse(400, "La cantidad debe ser mayor a 0", null);
        }
        Insumo insumo = findInsumo(insumoId);
        double current = insumo.getStock() != null ? insumo.getStock() : 0.0;
        insumo.setStock(current + cantidad);
        rebalancearDistribucion(insumo);
        insumoRepository.save(insumo);

        // Registrar el historial de restock con el costo total invertido (materia prima)
        double costo = (costoTotal != null && costoTotal > 0) ? costoTotal : 0.0;
        RestockHistory history = RestockHistory.builder()
                .tenantId(insumo.getTenantId())
                .insumoId(insumo.getId())
                .insumoNombre(insumo.getNombre())
                .cantidad(cantidad)
                .costoTotal(costo)
                .build();
        restockHistoryRepository.save(history);

        syncAvailability(insumo.getTenantId());

        return new GenericResponse(200, "Stock del insumo actualizado", insumo.getStock());
    }

    /* ============ Bodega (almacén central que distribuye a cocina/barra) ============ */

    @Override
    @Transactional(readOnly = true)
    public GenericResponse getBodegaByTenant(Long tenantId) {
        List<Insumo> insumos = insumoRepository.findByTenantIdAndIsActiveTrueOrderByNombreAsc(tenantId);
        List<Map<String, Object>> items = new ArrayList<>();
        for (Insumo i : insumos) {
            items.add(insumoToMap(i));
        }
        return new GenericResponse(200, "Bodega obtenida", items);
    }

    @Override
    @Transactional
    public GenericResponse createInsumoBodega(Long tenantId, String nombre, String unidad, Double cantidad, Double costoTotal, Double stockMinimo, List<Long> categoryIds) {
        if (tenantId == null || nombre == null || nombre.isBlank()) {
            return new GenericResponse(400, "Tenant y nombre son requeridos", null);
        }
        double qty = cantidad != null ? cantidad : 0.0;
        if (qty < 0) {
            return new GenericResponse(400, "La cantidad no puede ser negativa", null);
        }
        if (qty > 0 && (costoTotal == null || costoTotal <= 0)) {
            return new GenericResponse(400, "El costo de la carga es obligatorio al registrar un insumo con inventario", null);
        }
        Insumo insumo = Insumo.builder()
                .tenantId(tenantId)
                .nombre(nombre.trim())
                .unidad(unidad != null ? unidad : "pieza")
                .stock(0.0)
                .stockBodega(qty)
                .stockCocina(0.0)
                .stockBarra(0.0)
                .stockMinimo(stockMinimo != null ? stockMinimo : 0.0)
                .isActive(true)
                .build();
        insumo.setCategories(resolveCategories(tenantId, categoryIds));
        insumoRepository.save(insumo);

        if (qty > 0) {
            RestockHistory history = RestockHistory.builder()
                    .tenantId(tenantId)
                    .insumoId(insumo.getId())
                    .insumoNombre(insumo.getNombre())
                    .cantidad(qty)
                    .costoTotal(costoTotal)
                    .build();
            restockHistoryRepository.save(history);
        }

        return new GenericResponse(200, "Insumo registrado en bodega", insumoToMap(insumo));
    }

    @Override
    @Transactional
    public GenericResponse restockBodega(Long insumoId, Double cantidad, Double costoTotal) {
        if (cantidad == null || cantidad <= 0) {
            return new GenericResponse(400, "La cantidad debe ser mayor a 0", null);
        }
        if (costoTotal == null || costoTotal <= 0) {
            return new GenericResponse(400, "El costo de la carga es obligatorio en el restock", null);
        }
        Insumo insumo = findInsumo(insumoId);
        double bodega = insumo.getStockBodega() != null ? insumo.getStockBodega() : 0.0;
        insumo.setStockBodega(bodega + cantidad);
        insumoRepository.save(insumo);

        RestockHistory history = RestockHistory.builder()
                .tenantId(insumo.getTenantId())
                .insumoId(insumo.getId())
                .insumoNombre(insumo.getNombre())
                .cantidad(cantidad)
                .costoTotal(costoTotal)
                .build();
        restockHistoryRepository.save(history);

        return new GenericResponse(200, "Restock en bodega registrado", insumoToMap(insumo));
    }

    @Override
    @Transactional
    public GenericResponse moverBodega(Long insumoId, Double cantidad, String destino) {
        if (cantidad == null || cantidad <= 0) {
            return new GenericResponse(400, "La cantidad debe ser mayor a 0", null);
        }
        if (destino == null || (!destino.equals("cocina") && !destino.equals("barra"))) {
            return new GenericResponse(400, "El destino debe ser 'cocina' o 'barra'", null);
        }
        Insumo insumo = findInsumo(insumoId);
        double bodega = insumo.getStockBodega() != null ? insumo.getStockBodega() : 0.0;
        if (bodega < cantidad) {
            return new GenericResponse(400, "Stock insuficiente en bodega (" + redondear(bodega) + " disponible)", null);
        }
        insumo.setStockBodega(redondear(bodega - cantidad));

        double distribuido = insumo.getStock() != null ? insumo.getStock() : 0.0;
        if (destino.equals("cocina")) {
            double cocina = insumo.getStockCocina() != null ? insumo.getStockCocina() : 0.0;
            insumo.setStockCocina(redondear(cocina + cantidad));
        } else {
            double barra = insumo.getStockBarra() != null ? insumo.getStockBarra() : 0.0;
            insumo.setStockBarra(redondear(barra + cantidad));
        }
        insumo.setStock(redondear(distribuido + cantidad));
        insumoRepository.save(insumo);

        stockTransferRepository.save(StockTransfer.builder()
                .tenantId(insumo.getTenantId())
                .insumoId(insumo.getId())
                .insumoNombre(insumo.getNombre())
                .origen("bodega")
                .destino(destino)
                .cantidad(redondear(cantidad))
                .build());

        syncAvailability(insumo.getTenantId());
        return new GenericResponse(200, "Stock movido de bodega a " + destino, insumoToMap(insumo));
    }

    @Override
    @Transactional(readOnly = true)
    public GenericResponse getTransferenciasByTenant(Long tenantId) {
        List<StockTransfer> registros = stockTransferRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        List<Map<String, Object>> items = new ArrayList<>();
        for (StockTransfer t : registros) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("insumoId", t.getInsumoId());
            m.put("insumoNombre", t.getInsumoNombre());
            m.put("origen", t.getOrigen() != null ? t.getOrigen() : "bodega");
            m.put("destino", t.getDestino());
            m.put("cantidad", t.getCantidad() != null ? redondear(t.getCantidad()) : 0.0);
            m.put("createdAt", t.getCreatedAt() != null ? t.getCreatedAt().toString() : null);
            items.add(m);
        }
        return new GenericResponse(200, "Historial de transferencias obtenido", items);
    }

    /* ============ Bebidas (insumos marcados como bebida, vendibles en Comandix) ============ */

    @Override
    @Transactional(readOnly = true)
    public GenericResponse getBebidasByTenant(Long tenantId) {
        List<Insumo> bebidas = insumoRepository.findByTenantIdAndIsActiveTrueOrderByNombreAsc(tenantId).stream()
                .filter(Insumo::isEsBebida)
                .collect(java.util.stream.Collectors.toList());
        List<Map<String, Object>> items = new ArrayList<>();
        for (Insumo b : bebidas) {
            Map<String, Object> item = insumoToMap(b);
            // Bebidas sin categorías asignadas (ej. creadas antes de la multicategoría):
            // mostrar la categoría del producto de menú enlazado.
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cats = (List<Map<String, Object>>) item.get("categories");
            @SuppressWarnings("unchecked")
            List<Long> ids = (List<Long>) item.get("categoryIds");
            if ((cats == null || cats.isEmpty()) && b.getProductoId() != null) {
                productRepository.findById(b.getProductoId()).ifPresent(p -> {
                    if (p != null && p.getCategory() != null && p.getCategory().getId() != null
                            && p.getCategory().getNombre() != null) {
                        Map<String, Object> cm = new LinkedHashMap<>();
                        cm.put("id", p.getCategory().getId());
                        cm.put("name", p.getCategory().getNombre());
                        cats.add(cm);
                        ids.add(p.getCategory().getId());
                    }
                });
            }
            items.add(item);
        }
        return new GenericResponse(200, "Bebidas obtenidas", items);
    }

    @Override
    @Transactional
    public GenericResponse createBebida(Long tenantId, String nombre, String unidad, Double stock, Double stockMinimo, Double precioVenta, List<Long> categoryIds) {
        if (tenantId == null || nombre == null || nombre.isBlank()) {
            return new GenericResponse(400, "Tenant y nombre son requeridos", null);
        }
        Insumo insumo = Insumo.builder()
                .tenantId(tenantId)
                .nombre(nombre.trim())
                .unidad(unidad != null ? unidad : "pieza")
                .stock(stock != null ? stock : 0.0)
                .stockBodega(0.0)
                .stockCocina(0.0)
                .stockBarra(stock != null ? stock : 0.0)
                .stockMinimo(stockMinimo != null ? stockMinimo : 0.0)
                .esBebida(true)
                .precioVenta(precioVenta != null ? BigDecimal.valueOf(precioVenta) : BigDecimal.ZERO)
                .isActive(true)
                .build();

        List<TenantMenuCategory> cats = resolveCategories(tenantId, categoryIds);
        if (cats.isEmpty()) {
            // Sin categorías asignadas: la bebida pertenece a la categoría "Bebidas".
            TenantMenuCategory fb = obtenerOCrearCategoriaBebidas(tenantId);
            cats = new ArrayList<>();
            cats.add(fb);
        }
        insumo.setCategories(new ArrayList<>(cats));
        insumoRepository.save(insumo);

        // Crear el producto de menú enlazado y su receta de 1 unidad,
        // para que la bebida aparezca y se venda en el POS Comandix.
        TenantMenuCategory primaryCat = cats.get(0);
        List<TenantMenuCategory> productCats = new ArrayList<>(cats);
        TenantMenuProduct product = TenantMenuProduct.builder()
                .category(primaryCat)
                .categories(productCats)
                .precio(insumo.getPrecioVenta())
                .nombre(insumo.getNombre())
                .descripcion("Bebida")
                .unidad(insumo.getUnidad())
                .ventaIndividual(false)
                .isActive(true)
                .build();
        productRepository.save(product);

        ProductRecipe recipe = ProductRecipe.builder()
                .dish(product)
                .insumo(insumo)
                .cantidad(BigDecimal.ONE)
                .modificable(false)
                .build();
        recipeRepository.save(recipe);

        // Guardar el id del producto enlazado en la bebida.
        insumo.setProductoId(product.getId());
        insumoRepository.save(insumo);

        return new GenericResponse(200, "Bebida creada", insumoToMap(insumo));
    }

    @Override
    @Transactional
    public GenericResponse updateBebida(Long insumoId, String nombre, String unidad, Double stock, Double stockMinimo, Double precioVenta, List<Long> categoryIds) {
        Insumo insumo = findInsumo(insumoId);
        if (!insumo.isEsBebida()) {
            return new GenericResponse(400, "El insumo no es una bebida", null);
        }
        if (nombre != null && !nombre.isBlank()) insumo.setNombre(nombre.trim());
        if (unidad != null && !unidad.isBlank()) insumo.setUnidad(unidad);
        if (stock != null) {
            aplicarStockDistribuido(insumo, Math.max(0, stock), true);
        }
        if (stockMinimo != null) insumo.setStockMinimo(Math.max(0, stockMinimo));
        if (precioVenta != null) insumo.setPrecioVenta(BigDecimal.valueOf(Math.max(0, precioVenta)));
        insumoRepository.save(insumo);

        // Actualizar el producto de menú enlazado (nombre, precio, unidad) manteniendo el stock en el insumo.
        if (insumo.getProductoId() != null) {
            TenantMenuProduct product = findProduct(insumo.getProductoId());
            product.setNombre(insumo.getNombre());
            product.setPrecio(insumo.getPrecioVenta());
            product.setUnidad(insumo.getUnidad());
            if (categoryIds != null) {
                List<TenantMenuCategory> cats = resolveCategories(insumo.getTenantId(), categoryIds);
                if (cats.isEmpty() && product.getCategory() != null) {
                    // Sin categorías asignadas: conservar la categoría actual del producto enlazado
                    cats = new ArrayList<>();
                    cats.add(product.getCategory());
                }
                insumo.setCategories(new ArrayList<>(cats));
                if (!cats.isEmpty()) {
                    product.setCategory(cats.get(0));
                }
                product.setCategories(new ArrayList<>(cats));
            }
            productRepository.save(product);
        }
        syncAvailability(insumo.getTenantId());
        return new GenericResponse(200, "Bebida actualizada", insumoToMap(insumo));
    }

    @Override
    @Transactional
    public GenericResponse deleteBebida(Long insumoId) {
        Insumo insumo = findInsumo(insumoId);
        if (!insumo.isEsBebida()) {
            return new GenericResponse(400, "El insumo no es una bebida", null);
        }
        return deleteInsumo(insumoId);
    }

    private TenantMenuCategory obtenerOCrearCategoriaBebidas(Long tenantId) {
        return categoryRepository.findByTenantIdAndNombreIgnoreCase(tenantId, "Bebidas")
                .orElseGet(() -> {
                    Integer maxOrder = categoryRepository.findMaxDisplayOrderByTenantId(tenantId);
                    TenantMenuCategory cat = TenantMenuCategory.builder()
                            .tenant(Tenant.builder().id(tenantId).build())
                            .nombre("Bebidas")
                            .descripcion("Bebidas del menú")
                            .isActive(true)
                            .displayOrder((maxOrder != null ? maxOrder : 0) + 1)
                            .build();
                    return categoryRepository.save(cat);
                });
    }

    private TenantMenuCategory obtenerOCrearCategoriaPreparaciones(Long tenantId) {
        return categoryRepository.findByTenantIdAndNombreIgnoreCase(tenantId, "Preparaciones")
                .orElseGet(() -> {
                    Integer maxOrder = categoryRepository.findMaxDisplayOrderByTenantId(tenantId);
                    TenantMenuCategory cat = TenantMenuCategory.builder()
                            .tenant(Tenant.builder().id(tenantId).build())
                            .nombre("Preparaciones")
                            .descripcion("Sub-recetas (preparaciones intermedias)")
                            .isActive(false)
                            .displayOrder((maxOrder != null ? maxOrder : 0) + 1)
                            .build();
                    return categoryRepository.save(cat);
                });
    }

    /* ============ Stock directo de producto (sin receta) ============ */

    @Override
    @Transactional
    public GenericResponse updateProductStock(Long productId, Double stock, Double stockMinimo, String unidad) {
        TenantMenuProduct product = findProduct(productId);
        if (stock != null) product.setStock(Math.max(0, stock));
        if (stockMinimo != null) product.setStockMinimo(Math.max(0, stockMinimo));
        if (unidad != null && !unidad.isBlank()) product.setUnidad(unidad);
        productRepository.save(product);
        syncAvailability(productTenantId(product));
        return new GenericResponse(200, "Inventario actualizado", product.getId());
    }

    @Override
    @Transactional
    public GenericResponse restockProduct(Long productId, Double cantidad) {
        if (cantidad == null || cantidad <= 0) {
            return new GenericResponse(400, "La cantidad debe ser mayor a 0", null);
        }
        TenantMenuProduct product = findProduct(productId);
        if (isDish(product)) {
            return new GenericResponse(400, "Un platillo no se reabastece directamente; su stock se calcula de sus insumos", null);
        }
        double current = product.getStock() != null ? product.getStock() : 0.0;
        product.setStock(current + cantidad);
        productRepository.save(product);
        syncAvailability(productTenantId(product));
        return new GenericResponse(200, "Stock actualizado", product.getStock());
    }

    /* ============ Recetas (BOM) ============ */

    @Override
    public GenericResponse getRecipesByDish(Long dishId) {
        findProduct(dishId);
        List<ProductRecipe> recipes = recipeRepository.findByDishId(dishId);
        List<Map<String, Object>> items = new ArrayList<>();
        for (ProductRecipe r : recipes) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", r.getId());
            item.put("dishId", dishId);
            item.put("insumoId", r.getInsumo().getId());
            item.put("insumoName", r.getInsumo().getNombre());
            item.put("insumoUnit", r.getInsumo().getUnidad() != null ? r.getInsumo().getUnidad() : "pieza");
            item.put("cantidad", r.getCantidad());
            item.put("modificable", r.getModificable() != null && r.getModificable());
            items.add(item);
        }
        return new GenericResponse(200, "Recetas obtenidas", items);
    }

    @Override
    @Transactional
    public GenericResponse addRecipeIngredient(Long dishId, Long insumoId, Double cantidad, Boolean modificable) {
        if (insumoId == null || cantidad == null || cantidad <= 0) {
            return new GenericResponse(400, "Insumo y cantidad (mayor a 0) son requeridos", null);
        }
        TenantMenuProduct dish = findProduct(dishId);
        Insumo insumo = findInsumo(insumoId);
        boolean exists = recipeRepository.findByDishId(dishId).stream()
                .anyMatch(r -> r.getInsumo().getId().equals(insumoId));
        if (exists) {
            return new GenericResponse(400, "El insumo ya está en la receta", null);
        }
        ProductRecipe recipe = ProductRecipe.builder()
                .dish(dish)
                .insumo(insumo)
                .cantidad(BigDecimal.valueOf(cantidad))
                .modificable(modificable != null && modificable)
                .build();
        recipeRepository.save(recipe);
        syncAvailability(productTenantId(dish));
        return new GenericResponse(200, "Insumo agregado a la receta", recipe.getId());
    }

    @Override
    @Transactional
    public GenericResponse setRecipes(Long dishId, List<Map<String, Object>> lines) {
        TenantMenuProduct dish = findProduct(dishId);
        int lineas = guardarReceta(dish, lines);
        syncAvailability(productTenantId(dish));
        return new GenericResponse(200, "Receta actualizada con " + lineas + " insumo(s)", null);
    }

    @Override
    @Transactional
    public GenericResponse removeRecipeIngredient(Long recipeId) {
        ProductRecipe recipe = recipeRepository.findById(recipeId)
                .orElse(null);
        if (recipe == null) {
            return new GenericResponse(404, "Insumo de receta no encontrado", null);
        }
        Long tenantId = recipe.getDish() != null ? productTenantId(recipe.getDish()) : null;
        recipeRepository.deleteById(recipeId);
        syncAvailability(tenantId);
        return new GenericResponse(200, "Insumo eliminado de la receta", null);
    }

    @Override
    @Transactional
    public GenericResponse updateRecipeIngredient(Long recipeId, Double cantidad, Boolean modificable) {
        ProductRecipe recipe = recipeRepository.findById(recipeId)
                .orElse(null);
        if (recipe == null) {
            return new GenericResponse(404, "Insumo de receta no encontrado", null);
        }
        if (cantidad != null) {
            if (cantidad <= 0) {
                return new GenericResponse(400, "La cantidad debe ser mayor a 0", null);
            }
            recipe.setCantidad(BigDecimal.valueOf(cantidad));
        }
        if (modificable != null) {
            recipe.setModificable(modificable);
        }
        recipeRepository.save(recipe);
        syncAvailability(recipe.getDish() != null ? productTenantId(recipe.getDish()) : null);
        return new GenericResponse(200, "Insumo de receta actualizado", recipe.getId());
    }

    /* ============ Adicionales ============ */

    @Override
    public GenericResponse getAdditionalsByDish(Long dishId) {
        findProduct(dishId);
        List<ProductAdditional> additionals = additionalRepository.findByDishId(dishId);
        List<Map<String, Object>> items = new ArrayList<>();
        for (ProductAdditional a : additionals) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", a.getId());
            item.put("dishId", dishId);
            item.put("insumoId", a.getInsumo().getId());
            item.put("insumoName", a.getInsumo().getNombre());
            item.put("cantidad", a.getCantidad());
            item.put("unidad", a.getInsumo().getUnidad() != null ? a.getInsumo().getUnidad() : "pieza");
            item.put("precio", a.getPrecio() != null ? a.getPrecio() : BigDecimal.ZERO);
            items.add(item);
        }
        return new GenericResponse(200, "Adicionales obtenidos", items);
    }

    @Override
    @Transactional
    public GenericResponse addAdditional(Long dishId, Long insumoId, Double cantidad, Double precio) {
        if (insumoId == null || cantidad == null || cantidad <= 0) {
            return new GenericResponse(400, "Insumo y cantidad (mayor a 0) son requeridos", null);
        }
        TenantMenuProduct dish = findProduct(dishId);
        Insumo insumo = findInsumo(insumoId);
        boolean exists = additionalRepository.findByDishId(dishId).stream()
                .anyMatch(a -> a.getInsumo().getId().equals(insumoId));
        if (exists) {
            return new GenericResponse(400, "El adicional ya está permitido", null);
        }
        BigDecimal precioValue = precio != null ? BigDecimal.valueOf(precio) : BigDecimal.ZERO;
        ProductAdditional additional = ProductAdditional.builder()
                .dish(dish)
                .insumo(insumo)
                .cantidad(BigDecimal.valueOf(cantidad))
                .precio(precioValue)
                .build();
        additionalRepository.save(additional);
        return new GenericResponse(200, "Adicional permitido agregado", additional.getId());
    }

    @Override
    @Transactional
    public GenericResponse updateAdditional(Long additionalId, Double cantidad, Double precio) {
        ProductAdditional additional = additionalRepository.findById(additionalId)
                .orElse(null);
        if (additional == null) {
            return new GenericResponse(404, "Adicional no encontrado", null);
        }
        if (cantidad != null && cantidad > 0) {
            additional.setCantidad(BigDecimal.valueOf(cantidad));
        }
        if (precio != null && precio >= 0) {
            additional.setPrecio(BigDecimal.valueOf(precio));
        }
        additionalRepository.save(additional);
        return new GenericResponse(200, "Adicional actualizado", additional.getId());
    }

    @Override
    @Transactional
    public GenericResponse removeAdditional(Long additionalId) {
        if (!additionalRepository.existsById(additionalId)) {
            return new GenericResponse(404, "Adicional no encontrado", null);
        }
        additionalRepository.deleteById(additionalId);
        return new GenericResponse(200, "Adicional eliminado", null);
    }

    /* ============ Sub-recetas (preparaciones no vendidas individualmente) ============ */

    @Override
    @Transactional(readOnly = true)
    public GenericResponse getSubRecetasByTenant(Long tenantId) {
        List<TenantMenuProduct> subRecetas = productRepository.findSubRecetasByTenantId(tenantId);
        List<Map<String, Object>> items = new ArrayList<>();
        for (TenantMenuProduct s : subRecetas) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", s.getId());
            item.put("name", s.getNombre());
            item.put("descripcion", s.getDescripcion());
            item.put("categoria", s.getCategory() != null ? s.getCategory().getNombre() : null);
            item.put("categoryIds", buildCategoryIds(s.getCategories()));
            item.put("categories", buildCategoryMaps(s.getCategories()));
            item.put("unidad", s.getUnidad() != null ? s.getUnidad() : "pieza");
            item.put("insumos", buildInsumosList(s.getId()));
            items.add(item);
        }
        return new GenericResponse(200, "Sub-recetas obtenidas", items);
    }

    @Override
    @Transactional
    public GenericResponse createSubReceta(Long tenantId, String nombre, List<Map<String, Object>> lines, List<Long> categoryIds) {
        if (tenantId == null || nombre == null || nombre.isBlank()) {
            return new GenericResponse(400, "Tenant y nombre son requeridos", null);
        }
        List<TenantMenuCategory> cats = resolveCategories(tenantId, categoryIds);
        TenantMenuCategory primaryCat = cats.isEmpty() ? obtenerOCrearCategoriaPreparaciones(tenantId) : cats.get(0);
        TenantMenuProduct subReceta = TenantMenuProduct.builder()
                .category(primaryCat)
                .categories(new ArrayList<>(cats))
                .precio(BigDecimal.ZERO)
                .nombre(nombre.trim())
                .descripcion("Sub-receta (preparación)")
                .unidad("pieza")
                .ventaIndividual(false)
                .esSubReceta(true)
                .isActive(true)
                .build();
        productRepository.save(subReceta);

        int lineas = guardarReceta(subReceta, lines);
        syncAvailability(productTenantId(subReceta));
        return new GenericResponse(200, "Sub-receta creada con " + lineas + " insumo(s)", subReceta.getId());
    }

    @Override
    @Transactional
    public GenericResponse updateSubReceta(Long subRecetaId, String nombre, List<Map<String, Object>> lines, List<Long> categoryIds) {
        TenantMenuProduct subReceta = findSubReceta(subRecetaId);
        if (nombre != null && !nombre.isBlank()) subReceta.setNombre(nombre.trim());
        if (categoryIds != null) {
            List<TenantMenuCategory> cats = resolveCategories(productTenantId(subReceta), categoryIds);
            if (!cats.isEmpty()) {
                subReceta.setCategory(cats.get(0));
            }
            subReceta.setCategories(new ArrayList<>(cats));
        }
        productRepository.save(subReceta);
        int lineas = guardarReceta(subReceta, lines);
        syncAvailability(productTenantId(subReceta));
        return new GenericResponse(200, "Sub-receta actualizada con " + lineas + " insumo(s)", subReceta.getId());
    }

    @Override
    @Transactional
    public GenericResponse deleteSubReceta(Long subRecetaId) {
        TenantMenuProduct subReceta = findSubReceta(subRecetaId);
        Long tenantId = productTenantId(subReceta);
        List<ProductSubReceta> usos = subRecetaRepository.findAll().stream()
                .filter(s -> s.getSubReceta().getId().equals(subRecetaId))
                .collect(java.util.stream.Collectors.toList());
        subRecetaRepository.deleteAll(usos);
        recipeRepository.deleteByDishId(subRecetaId);
        productRepository.delete(subReceta);
        syncAvailability(tenantId);
        return new GenericResponse(200, "Sub-receta eliminada", null);
    }

    @Override
    @Transactional(readOnly = true)
    public GenericResponse getSubRecetasByDish(Long dishId) {
        findProduct(dishId);
        List<Map<String, Object>> items = new ArrayList<>();
        for (ProductSubReceta s : subRecetaRepository.findByDishId(dishId)) {
            TenantMenuProduct sr = s.getSubReceta();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", sr.getId());
            item.put("idAsignacion", s.getId());
            item.put("name", sr.getNombre());
            item.put("insumos", buildInsumosList(sr.getId()));
            items.add(item);
        }
        return new GenericResponse(200, "Sub-recetas del producto", items);
    }

    @Override
    @Transactional
    public GenericResponse assignSubReceta(Long dishId, Long subRecetaId) {
        TenantMenuProduct dish = findProduct(dishId);
        TenantMenuProduct subReceta = findSubReceta(subRecetaId);
        if (dish.getId().equals(subRecetaId)) {
            return new GenericResponse(400, "Un platillo no puede usar su misma sub-receta", null);
        }
        if (subRecetaRepository.existsByDishIdAndSubRecetaId(dishId, subRecetaId)) {
            return new GenericResponse(400, "La sub-receta ya está asignada a este producto", null);
        }
        subRecetaRepository.save(ProductSubReceta.builder()
                .dish(dish)
                .subReceta(subReceta)
                .build());
        syncAvailability(productTenantId(dish));
        return new GenericResponse(200,
                "Sub-receta '" + subReceta.getNombre() + "' asignada a '" + dish.getNombre() + "'", null);
    }

    @Override
    @Transactional
    public GenericResponse removeSubRecetaFromDish(Long dishId, Long subRecetaId) {
        TenantMenuProduct dish = findProduct(dishId);
        List<ProductSubReceta> usos = subRecetaRepository.findByDishId(dishId).stream()
                .filter(s -> s.getSubReceta().getId().equals(subRecetaId))
                .collect(java.util.stream.Collectors.toList());
        subRecetaRepository.deleteAll(usos);
        syncAvailability(productTenantId(dish));
        return new GenericResponse(200, "Sub-receta removida del producto", null);
    }

    /* ============ Descuento al confirmar comanda ============ */

    @Override
    @Transactional
    public GenericResponse deductForOrder(Long productId, Double cantidad, List<Long> excludedInsumoIds, List<Long> additionalInsumoIds) {
        TenantMenuProduct product = findProduct(productId);
        double units = cantidad != null ? cantidad : 1.0;
        List<Map<String, Object>> deducted = new ArrayList<>();

        List<ProductRecipe> recipes = effectiveRecipes(productId);
        if (recipes.isEmpty()) {
            deductProduct(product, units, deducted);
            syncAvailability(productTenantId(product));
            return new GenericResponse(200, "Stock descontado de " + product.getNombre(), deducted);
        }

        for (ProductRecipe r : recipes) {
            boolean excluded = r.getModificable() != null && r.getModificable()
                    && excludedInsumoIds != null && excludedInsumoIds.contains(r.getInsumo().getId());
            if (excluded) {
                continue;
            }
            double qty = r.getCantidad() != null ? r.getCantidad().doubleValue() : 0.0;
            if (qty <= 0) continue;
            deductInsumo(r.getInsumo(), qty * units, deducted);
        }

        if (additionalInsumoIds != null) {
            for (ProductAdditional a : additionalRepository.findByDishId(productId)) {
                if (additionalInsumoIds.contains(a.getInsumo().getId())) {
                    double qty = a.getCantidad() != null ? a.getCantidad().doubleValue() : 1.0;
                    deductInsumo(a.getInsumo(), qty * units, deducted);
                }
            }
        }

        syncAvailability(productTenantId(product));

        return new GenericResponse(200,
                "Stock descontado: " + deducted.size() + " insumo(s) de " + product.getNombre(), deducted);
    }

    /* ============ Restauración al cancelar comanda ============ */

    @Override
    @Transactional
    public GenericResponse restoreForOrder(Long productId, Double cantidad, List<Long> excludedInsumoIds, List<Long> additionalInsumoIds) {
        TenantMenuProduct product = findProduct(productId);
        double units = cantidad != null ? cantidad : 1.0;
        List<Map<String, Object>> restored = new ArrayList<>();

        List<ProductRecipe> recipes = effectiveRecipes(productId);
        if (recipes.isEmpty()) {
            restoreProduct(product, units, restored);
            syncAvailability(productTenantId(product));
            return new GenericResponse(200, "Stock restaurado de " + product.getNombre(), restored);
        }

        for (ProductRecipe r : recipes) {
            boolean excluded = r.getModificable() != null && r.getModificable()
                    && excludedInsumoIds != null && excludedInsumoIds.contains(r.getInsumo().getId());
            if (excluded) {
                continue;
            }
            double qty = r.getCantidad() != null ? r.getCantidad().doubleValue() : 0.0;
            if (qty <= 0) continue;
            restoreInsumo(r.getInsumo(), qty * units, restored);
        }

        if (additionalInsumoIds != null) {
            for (ProductAdditional a : additionalRepository.findByDishId(productId)) {
                if (additionalInsumoIds.contains(a.getInsumo().getId())) {
                    double qty = a.getCantidad() != null ? a.getCantidad().doubleValue() : 1.0;
                    restoreInsumo(a.getInsumo(), qty * units, restored);
                }
            }
        }

        syncAvailability(productTenantId(product));

        return new GenericResponse(200,
                "Stock restaurado: " + restored.size() + " insumo(s) de " + product.getNombre(), restored);
    }

    /* ============ Helpers ============ */

    private List<Map<String, Object>> buildInsumosList(Long dishId) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ProductRecipe r : recipeRepository.findByDishId(dishId)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", r.getId());
            item.put("insumoId", r.getInsumo().getId());
            item.put("insumoName", r.getInsumo().getNombre());
            item.put("unidad", r.getInsumo().getUnidad() != null ? r.getInsumo().getUnidad() : "pieza");
            item.put("cantidad", r.getCantidad());
            item.put("modificable", r.getModificable() != null && r.getModificable());
            item.put("stock", r.getInsumo().getStock() != null ? r.getInsumo().getStock() : 0.0);
            item.put("stockMinimo", r.getInsumo().getStockMinimo() != null ? r.getInsumo().getStockMinimo() : 0.0);
            list.add(item);
        }
        return list;
    }

    private List<Map<String, Object>> buildAdicionalesList(Long dishId) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ProductAdditional a : additionalRepository.findByDishId(dishId)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", a.getId());
            item.put("insumoId", a.getInsumo().getId());
            item.put("insumoName", a.getInsumo().getNombre());
            item.put("unidad", a.getInsumo().getUnidad() != null ? a.getInsumo().getUnidad() : "pieza");
            item.put("cantidad", a.getCantidad());
            item.put("precio", a.getPrecio() != null ? a.getPrecio() : BigDecimal.ZERO);
            item.put("stock", a.getInsumo().getStock() != null ? a.getInsumo().getStock() : 0.0);
            list.add(item);
        }
        return list;
    }

    private List<Map<String, Object>> buildSubRecetasList(Long dishId) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ProductSubReceta s : subRecetaRepository.findByDishId(dishId)) {
            TenantMenuProduct sr = s.getSubReceta();
            if (sr == null) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", sr.getId());
            item.put("idAsignacion", s.getId());
            item.put("name", sr.getNombre());
            list.add(item);
        }
        return list;
    }

    /** Receta efectiva de un producto: sus insumos + los insumos de las sub-recetas asignadas. */
    private List<ProductRecipe> effectiveRecipes(Long productId) {
        List<ProductRecipe> all = new ArrayList<>(recipeRepository.findByDishId(productId));
        for (ProductSubReceta s : subRecetaRepository.findByDishId(productId)) {
            TenantMenuProduct sr = s.getSubReceta();
            if (sr == null) continue;
            all.addAll(recipeRepository.findByDishId(sr.getId()));
        }
        return all;
    }

    /** Reemplaza la receta de un producto y devuelve el número de líneas guardadas. */
    private int guardarReceta(TenantMenuProduct dish, List<Map<String, Object>> lines) {
        recipeRepository.deleteByDishId(dish.getId());
        if (lines == null || lines.isEmpty()) return 0;
        int count = 0;
        for (Map<String, Object> line : lines) {
            Object rawId = line.get("insumoId");
            Object rawCant = line.get("cantidad");
            if (rawId == null || rawCant == null) continue;
            Long insumoId;
            Double cantidad;
            try {
                insumoId = Long.valueOf(rawId.toString());
                cantidad = Double.valueOf(rawCant.toString());
            } catch (NumberFormatException e) {
                continue;
            }
            if (cantidad <= 0) continue;
            Insumo insumo = findInsumo(insumoId);
            boolean modificable = line.get("modificable") != null && Boolean.parseBoolean(line.get("modificable").toString());
            recipeRepository.save(ProductRecipe.builder()
                    .dish(dish)
                    .insumo(insumo)
                    .cantidad(BigDecimal.valueOf(cantidad))
                    .modificable(modificable)
                    .build());
            count++;
        }
        return count;
    }

    /** Aplica el stock distribuido total de un insumo conservando, en lo posible, el split cocina/barra
     * (si no hay split previo, todo va al lado preferido: barra para bebidas, cocina para insumos). */
    private void aplicarStockDistribuido(Insumo insumo, double nuevoStock, boolean esBebida) {
        nuevoStock = Math.max(0, nuevoStock);
        double cocina = insumo.getStockCocina() != null ? insumo.getStockCocina() : 0.0;
        double barra = insumo.getStockBarra() != null ? insumo.getStockBarra() : 0.0;
        double total = redondear(cocina + barra);
        if (total > 0) {
            double ratio = nuevoStock / total;
            double nc = redondear(cocina * ratio);
            insumo.setStockCocina(nc);
            insumo.setStockBarra(redondear(Math.max(0, nuevoStock - nc)));
        } else if (esBebida) {
            insumo.setStockCocina(0.0);
            insumo.setStockBarra(nuevoStock);
        } else {
            insumo.setStockCocina(nuevoStock);
            insumo.setStockBarra(0.0);
        }
        insumo.setStock(nuevoStock);
    }

    /** Recalcula cocina/barra para que la suma coincida con el stock distribuido (invarianza POS). */
    private void rebalancearDistribucion(Insumo insumo) {
        double stock = insumo.getStock() != null ? insumo.getStock() : 0.0;
        double cocina = insumo.getStockCocina() != null ? insumo.getStockCocina() : 0.0;
        double barra = insumo.getStockBarra() != null ? insumo.getStockBarra() : 0.0;
        double total = redondear(cocina + barra);
        if (Math.abs(total - stock) < 0.01) {
            return;
        }
        if (stock > 0) {
            double ratio = stock / (total > 0 ? total : 1.0);
            double nc = redondear(cocina * ratio);
            insumo.setStockCocina(nc);
            insumo.setStockBarra(redondear(Math.max(0, stock - nc)));
        } else {
            insumo.setStockCocina(0.0);
            insumo.setStockBarra(0.0);
        }
    }

    /** Redondeo a 3 decimales, consistente con las deducciones del POS. */
    private double redondear(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    private Map<String, Object> insumoToMap(Insumo i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", i.getId());
        m.put("nombre", i.getNombre());
        m.put("unidad", i.getUnidad() != null ? i.getUnidad() : "pieza");
        m.put("stock", i.getStock() != null ? redondear(i.getStock()) : 0.0);
        m.put("stockBodega", i.getStockBodega() != null ? redondear(i.getStockBodega()) : 0.0);
        m.put("stockCocina", i.getStockCocina() != null ? redondear(i.getStockCocina()) : 0.0);
        m.put("stockBarra", i.getStockBarra() != null ? redondear(i.getStockBarra()) : 0.0);
        m.put("stockMinimo", i.getStockMinimo() != null ? i.getStockMinimo() : 0.0);
        m.put("esBebida", i.isEsBebida());
        m.put("precioVenta", i.getPrecioVenta() != null ? i.getPrecioVenta() : java.math.BigDecimal.ZERO);
        m.put("productoId", i.getProductoId());
        m.put("categories", buildCategoryMaps(i.getCategories()));
        m.put("categoryIds", buildCategoryIds(i.getCategories()));
        return m;
    }

    private List<Map<String, Object>> buildCategoryMaps(List<TenantMenuCategory> cats) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (cats != null) {
            for (TenantMenuCategory c : cats) {
                if (c == null || c.getId() == null) continue;
                Map<String, Object> cm = new LinkedHashMap<>();
                cm.put("id", c.getId());
                cm.put("name", c.getNombre() != null ? c.getNombre() : "");
                list.add(cm);
            }
        }
        return list;
    }

    private List<Long> buildCategoryIds(List<TenantMenuCategory> cats) {
        List<Long> ids = new ArrayList<>();
        if (cats != null) {
            for (TenantMenuCategory c : cats) {
                if (c != null && c.getId() != null) ids.add(c.getId());
            }
        }
        return ids;
    }

    /** Resuelve los IDs de categoría a entidades TenantMenuCategory validando que pertenezcan al tenant. */
    private List<TenantMenuCategory> resolveCategories(Long tenantId, List<Long> categoryIds) {
        List<TenantMenuCategory> cats = new ArrayList<>();
        if (categoryIds == null || tenantId == null) return cats;
        java.util.Set<Long> seen = new java.util.HashSet<>();
        for (Long cid : categoryIds) {
            if (cid == null || seen.contains(cid)) continue;
            categoryRepository.findById(cid).ifPresent(c -> {
                if (c.getTenant() != null && tenantId.equals(c.getTenant().getId())) {
                    cats.add(c);
                    seen.add(cid);
                }
            });
        }
        return cats;
    }

    private boolean isDish(TenantMenuProduct p) {
        return !recipeRepository.findByDishId(p.getId()).isEmpty();
    }

    @Override
    public boolean hasStock(Long productId, Double cantidad) {
        double qty = cantidad != null ? cantidad : 1.0;
        TenantMenuProduct product = productRepository.findById(productId).orElse(null);
        if (product == null) return false;
        List<ProductRecipe> recipes = effectiveRecipes(productId);
        if (recipes.isEmpty()) {
            return safeStock(product) >= qty;
        }
        for (ProductRecipe r : recipes) {
            double req = r.getCantidad() != null ? r.getCantidad().doubleValue() : 0.0;
            if (req <= 0) continue;
            double insumoStock = r.getInsumo().getStock() != null ? r.getInsumo().getStock() : 0.0;
            if (Math.floor(insumoStock / req) < qty) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isProductAvailable(TenantMenuProduct product) {
        if (product == null) return false;
        List<ProductRecipe> recipes = effectiveRecipes(product.getId());
        if (recipes.isEmpty()) {
            return safeStock(product) >= 1.0;
        }
        double min = Double.MAX_VALUE;
        for (ProductRecipe r : recipes) {
            double qty = r.getCantidad() != null ? r.getCantidad().doubleValue() : 0.0;
            if (qty <= 0) continue;
            double insumoStock = r.getInsumo().getStock() != null ? r.getInsumo().getStock() : 0.0;
            double availableUnits = Math.floor(insumoStock / qty);
            if (availableUnits < 1.0) return false;
            min = Math.min(min, availableUnits);
        }
        // Receta presente pero sin insumos efectivos -> no puede prepararse
        return min != Double.MAX_VALUE;
    }

    @Override
    @Transactional
    public int syncProductAvailabilityByTenant(Long tenantId) {
        if (tenantId == null) return 0;
        int changes = 0;
        List<TenantMenuProduct> products = productRepository.findAllByTenantId(tenantId);
        for (TenantMenuProduct p : products) {
            Boolean auto = p.getAutoAvailability();
            boolean autoManaged = auto == null || auto;
            if (!autoManaged) continue;
            boolean available = isProductAvailable(p);
            if (available != p.isActive()) {
                p.setActive(available);
                productRepository.save(p);
                changes++;
                log.info("[AutoDisponibilidad] Producto '{}' (id={}) {}",
                        p.getNombre(), p.getId(),
                        available ? "REACTIVADO (hay insumos)" : "DESACTIVADO (sin insumos suficientes)");
            }
        }
        return changes;
    }

    /** Sincroniza la disponibilidad de todo el tenant al que pertenece el producto/insumo. */
    private void syncAvailability(Long tenantId) {
        if (tenantId == null) return;
        int changes = syncProductAvailabilityByTenant(tenantId);
        if (changes > 0) {
            log.info("[AutoDisponibilidad] Tenant {}: {} producto(s) ajustados", tenantId, changes);
        }
    }

    private double safeStock(TenantMenuProduct p) {
        return p.getStock() != null ? p.getStock() : 0.0;
    }

    private double safeMin(TenantMenuProduct p) {
        return p.getStockMinimo() != null ? p.getStockMinimo() : 0.0;
    }

    /** stockDe: platillo -> min(floor(stockInsumo / cantidadReceta)); insumo de receta compartido */
    private double stockDe(TenantMenuProduct dish) {
        List<ProductRecipe> recipes = effectiveRecipes(dish.getId());
        if (recipes.isEmpty()) return 0.0;
        double min = Double.MAX_VALUE;
        for (ProductRecipe r : recipes) {
            double qty = r.getCantidad() != null ? r.getCantidad().doubleValue() : 0.0;
            if (qty <= 0) continue;
            double insumoStock = r.getInsumo().getStock() != null ? r.getInsumo().getStock() : 0.0;
            double available = Math.floor(insumoStock / qty);
            min = Math.min(min, available);
        }
        if (min == Double.MAX_VALUE) return 0.0;
        return Math.max(0, min);
    }

    private double stockMinDe(TenantMenuProduct dish) {
        List<ProductRecipe> recipes = effectiveRecipes(dish.getId());
        if (recipes.isEmpty()) return 0.0;
        double min = Double.MAX_VALUE;
        for (ProductRecipe r : recipes) {
            double qty = r.getCantidad() != null ? r.getCantidad().doubleValue() : 0.0;
            if (qty <= 0) continue;
            double insumoMin = r.getInsumo().getStockMinimo() != null ? r.getInsumo().getStockMinimo() : 0.0;
            double available = Math.floor(insumoMin / qty);
            min = Math.min(min, available);
        }
        if (min == Double.MAX_VALUE) return 0.0;
        return Math.max(0, min);
    }

    private void deductInsumo(Insumo insumo, double qty, List<Map<String, Object>> deducted) {
        double antes = insumo.getStock() != null ? insumo.getStock() : 0.0;
        double despues = Math.max(0, Math.round((antes - qty) * 1000) / 1000.0);
        insumo.setStock(despues);
        rebalancearDistribucion(insumo);
        insumoRepository.save(insumo);
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("nombre", insumo.getNombre());
        d.put("unidad", insumo.getUnidad() != null ? insumo.getUnidad() : "pieza");
        d.put("antes", antes);
        d.put("despues", despues);
        d.put("suficiente", antes >= qty);
        deducted.add(d);
    }

    private void deductProduct(TenantMenuProduct product, double qty, List<Map<String, Object>> deducted) {
        double antes = safeStock(product);
        double despues = Math.max(0, Math.round((antes - qty) * 1000) / 1000.0);
        product.setStock(despues);
        productRepository.save(product);
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("nombre", product.getNombre());
        d.put("unidad", product.getUnidad() != null ? product.getUnidad() : "pieza");
        d.put("antes", antes);
        d.put("despues", despues);
        d.put("suficiente", antes >= qty);
        deducted.add(d);
    }

    private void restoreInsumo(Insumo insumo, double qty, List<Map<String, Object>> restored) {
        double antes = insumo.getStock() != null ? insumo.getStock() : 0.0;
        double despues = Math.round((antes + qty) * 1000) / 1000.0;
        insumo.setStock(despues);
        rebalancearDistribucion(insumo);
        insumoRepository.save(insumo);
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("nombre", insumo.getNombre());
        d.put("unidad", insumo.getUnidad() != null ? insumo.getUnidad() : "pieza");
        d.put("antes", antes);
        d.put("despues", despues);
        restored.add(d);
    }

    private void restoreProduct(TenantMenuProduct product, double qty, List<Map<String, Object>> restored) {
        double antes = safeStock(product);
        double despues = Math.round((antes + qty) * 1000) / 1000.0;
        product.setStock(despues);
        productRepository.save(product);
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("nombre", product.getNombre());
        d.put("unidad", product.getUnidad() != null ? product.getUnidad() : "pieza");
        d.put("antes", antes);
        d.put("despues", despues);
        restored.add(d);
    }

    private TenantMenuProduct findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id=" + productId));
    }

    private TenantMenuProduct findSubReceta(Long subRecetaId) {
        TenantMenuProduct p = findProduct(subRecetaId);
        if (p.getEsSubReceta() == null || !p.getEsSubReceta()) {
            throw new ResourceNotFoundException("Sub-receta no encontrada con id=" + subRecetaId);
        }
        return p;
    }

    /** Tenant del producto a partir de su categoría principal (siempre tiene categoría). */
    private Long productTenantId(TenantMenuProduct product) {
        if (product == null || product.getCategory() == null || product.getCategory().getTenant() == null) {
            return null;
        }
        return product.getCategory().getTenant().getId();
    }

    private Insumo findInsumo(Long insumoId) {
        return insumoRepository.findById(insumoId)
                .orElseThrow(() -> new ResourceNotFoundException("Insumo no encontrado con id=" + insumoId));
    }
}

