package com.lealtixservice.service.impl;

import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.entity.Insumo;
import com.lealtixservice.entity.ProductAdditional;
import com.lealtixservice.entity.ProductRecipe;
import com.lealtixservice.entity.TenantMenuProduct;
import com.lealtixservice.exception.ResourceNotFoundException;
import com.lealtixservice.repository.InsumoRepository;
import com.lealtixservice.repository.ProductAdditionalRepository;
import com.lealtixservice.repository.ProductRecipeRepository;
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
    public GenericResponse getInsumosByTenant(Long tenantId) {
        List<Insumo> insumos = insumoRepository.findByTenantIdAndIsActiveTrueOrderByNombreAsc(tenantId);
        List<Map<String, Object>> items = new ArrayList<>();
        for (Insumo i : insumos) {
            items.add(insumoToMap(i));
        }
        return new GenericResponse(200, "Insumos obtenidos", items);
    }

    @Override
    @Transactional
    public GenericResponse createInsumo(Long tenantId, String nombre, String unidad, Double stock, Double stockMinimo) {
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
        insumoRepository.save(insumo);
        return new GenericResponse(200, "Insumo creado", insumoToMap(insumo));
    }

    @Override
    @Transactional
    public GenericResponse updateInsumo(Long insumoId, String nombre, String unidad, Double stock, Double stockMinimo) {
        Insumo insumo = findInsumo(insumoId);
        if (nombre != null && !nombre.isBlank()) insumo.setNombre(nombre.trim());
        if (unidad != null && !unidad.isBlank()) insumo.setUnidad(unidad);
        if (stock != null) insumo.setStock(Math.max(0, stock));
        if (stockMinimo != null) insumo.setStockMinimo(Math.max(0, stockMinimo));
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
    public GenericResponse restockInsumo(Long insumoId, Double cantidad) {
        if (cantidad == null || cantidad <= 0) {
            return new GenericResponse(400, "La cantidad debe ser mayor a 0", null);
        }
        Insumo insumo = findInsumo(insumoId);
        double current = insumo.getStock() != null ? insumo.getStock() : 0.0;
        insumo.setStock(current + cantidad);
        insumoRepository.save(insumo);
        return new GenericResponse(200, "Stock del insumo actualizado", insumo.getStock());
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
            items.add(item);
        }
        return new GenericResponse(200, "Adicionales obtenidos", items);
    }

    @Override
    @Transactional
    public GenericResponse addAdditional(Long dishId, Long insumoId, Double cantidad) {
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
        ProductAdditional additional = ProductAdditional.builder()
                .dish(dish)
                .insumo(insumo)
                .cantidad(BigDecimal.valueOf(cantidad))
                .build();
        additionalRepository.save(additional);
        return new GenericResponse(200, "Adicional permitido agregado", additional.getId());
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
        return m;
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

    private TenantMenuProduct findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id=" + productId));
    }

    private Insumo findInsumo(Long insumoId) {
        return insumoRepository.findById(insumoId)
                .orElseThrow(() -> new ResourceNotFoundException("Insumo no encontrado con id=" + insumoId));
    }
}
