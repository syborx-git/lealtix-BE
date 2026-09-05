package com.lealtixservice.service.impl;

import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.entity.Insumo;
import com.lealtixservice.entity.ProductAdditional;
import com.lealtixservice.entity.ProductRecipe;
import com.lealtixservice.entity.RestockHistory;
import com.lealtixservice.entity.Tenant;
import com.lealtixservice.entity.TenantMenuCategory;
import com.lealtixservice.entity.TenantMenuProduct;
import com.lealtixservice.exception.ResourceNotFoundException;
import com.lealtixservice.repository.InsumoRepository;
import com.lealtixservice.repository.ProductAdditionalRepository;
import com.lealtixservice.repository.ProductRecipeRepository;
import com.lealtixservice.repository.RestockHistoryRepository;
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
    private final InsumoRepository insumoRepository;
    private final RestockHistoryRepository restockHistoryRepository;
    private final TenantMenuCategoryRepository categoryRepository;

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
            item.put("insumos", buildInsumosList(p.getId()));
            item.put("adicionales", buildAdicionalesList(p.getId()));
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
        if (stock != null) insumo.setStock(Math.max(0, stock));
        if (stockMinimo != null) insumo.setStockMinimo(Math.max(0, stockMinimo));
        if (categoryIds != null) {
            insumo.setCategories(resolveCategories(insumo.getTenantId(), categoryIds));
        }
        insumoRepository.save(insumo);
        return new GenericResponse(200, "Insumo actualizado", insumoToMap(insumo));
    }

    @Override
    @Transactional
    public GenericResponse deleteInsumo(Long insumoId) {
        Insumo insumo = findInsumo(insumoId);
        boolean enUso = recipeRepository.findAll().stream()
                .anyMatch(r -> r.getInsumo().getId().equals(insumoId))
                || additionalRepository.findAll().stream()
                .anyMatch(a -> a.getInsumo().getId().equals(insumoId));
        if (enUso) {
            return new GenericResponse(400, "El insumo está en uso por algún platillo; quítalo de las recetas primero", null);
        }
        insumoRepository.delete(insumo);
        return new GenericResponse(200, "Insumo eliminado", null);
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

        return new GenericResponse(200, "Stock del insumo actualizado", insumo.getStock());
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
            items.add(insumoToMap(b));
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
                .stockMinimo(stockMinimo != null ? stockMinimo : 0.0)
                .esBebida(true)
                .precioVenta(precioVenta != null ? BigDecimal.valueOf(precioVenta) : BigDecimal.ZERO)
                .isActive(true)
                .build();

        List<TenantMenuCategory> cats = resolveCategories(tenantId, categoryIds);
        insumo.setCategories(new ArrayList<>(cats));
        insumoRepository.save(insumo);

        // Crear el producto de menú enlazado y su receta de 1 unidad,
        // para que la bebida aparezca y se venda en el POS Comandix.
        TenantMenuCategory primaryCat;
        List<TenantMenuCategory> productCats;
        if (cats.isEmpty()) {
            primaryCat = obtenerOCrearCategoriaBebidas(tenantId);
            productCats = new ArrayList<>();
            productCats.add(primaryCat);
        } else {
            primaryCat = cats.get(0);
            productCats = new ArrayList<>(cats);
        }
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
        if (stock != null) insumo.setStock(Math.max(0, stock));
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
        return new GenericResponse(200, "Bebida actualizada", insumoToMap(insumo));
    }

    @Override
    @Transactional
    public GenericResponse deleteBebida(Long insumoId) {
        Insumo insumo = findInsumo(insumoId);
        if (!insumo.isEsBebida()) {
            return new GenericResponse(400, "El insumo no es una bebida", null);
        }
        if (insumo.getProductoId() != null) {
            productRepository.findById(insumo.getProductoId()).ifPresent(product -> {
                recipeRepository.deleteByDishId(product.getId());
                productRepository.delete(product);
            });
        }
        insumoRepository.delete(insumo);
        return new GenericResponse(200, "Bebida eliminada", null);
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

    /* ============ Stock directo de producto (sin receta) ============ */

    @Override
    @Transactional
    public GenericResponse updateProductStock(Long productId, Double stock, Double stockMinimo, String unidad) {
        TenantMenuProduct product = findProduct(productId);
        if (stock != null) product.setStock(Math.max(0, stock));
        if (stockMinimo != null) product.setStockMinimo(Math.max(0, stockMinimo));
        if (unidad != null && !unidad.isBlank()) product.setUnidad(unidad);
        productRepository.save(product);
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
        return new GenericResponse(200, "Insumo agregado a la receta", recipe.getId());
    }

    @Override
    @Transactional
    public GenericResponse setRecipes(Long dishId, List<Map<String, Object>> lines) {
        TenantMenuProduct dish = findProduct(dishId);
        recipeRepository.deleteByDishId(dishId);
        if (lines == null || lines.isEmpty()) {
            return new GenericResponse(200, "Receta actualizada (sin insumos)", null);
        }
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
            ProductRecipe recipe = ProductRecipe.builder()
                    .dish(dish)
                    .insumo(insumo)
                    .cantidad(BigDecimal.valueOf(cantidad))
                    .modificable(modificable)
                    .build();
            recipeRepository.save(recipe);
        }
        return new GenericResponse(200, "Receta actualizada", null);
    }

    @Override
    @Transactional
    public GenericResponse removeRecipeIngredient(Long recipeId) {
        if (!recipeRepository.existsById(recipeId)) {
            return new GenericResponse(404, "Insumo de receta no encontrado", null);
        }
        recipeRepository.deleteById(recipeId);
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

    /* ============ Descuento al confirmar comanda ============ */

    @Override
    @Transactional
    public GenericResponse deductForOrder(Long productId, Double cantidad, List<Long> excludedInsumoIds, List<Long> additionalInsumoIds) {
        TenantMenuProduct product = findProduct(productId);
        double units = cantidad != null ? cantidad : 1.0;
        List<Map<String, Object>> deducted = new ArrayList<>();

        List<ProductRecipe> recipes = recipeRepository.findByDishId(productId);
        if (recipes.isEmpty()) {
            deductProduct(product, units, deducted);
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

        List<ProductRecipe> recipes = recipeRepository.findByDishId(productId);
        if (recipes.isEmpty()) {
            restoreProduct(product, units, restored);
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

    private Map<String, Object> insumoToMap(Insumo i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", i.getId());
        m.put("nombre", i.getNombre());
        m.put("unidad", i.getUnidad() != null ? i.getUnidad() : "pieza");
        m.put("stock", i.getStock() != null ? i.getStock() : 0.0);
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
        List<ProductRecipe> recipes = recipeRepository.findByDishId(productId);
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

    private double safeStock(TenantMenuProduct p) {
        return p.getStock() != null ? p.getStock() : 0.0;
    }

    private double safeMin(TenantMenuProduct p) {
        return p.getStockMinimo() != null ? p.getStockMinimo() : 0.0;
    }

    /** stockDe: platillo -> min(floor(stockInsumo / cantidadReceta)); insumo de receta compartido */
    private double stockDe(TenantMenuProduct dish) {
        List<ProductRecipe> recipes = recipeRepository.findByDishId(dish.getId());
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
        List<ProductRecipe> recipes = recipeRepository.findByDishId(dish.getId());
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

    private Insumo findInsumo(Long insumoId) {
        return insumoRepository.findById(insumoId)
                .orElseThrow(() -> new ResourceNotFoundException("Insumo no encontrado con id=" + insumoId));
    }
}
