package com.lealtixservice.service;

import com.lealtixservice.dto.GenericResponse;

import java.util.List;
import java.util.Map;

public interface InventoryService {

    /**
     * Lista el inventario de un tenant: productos con stock dinámico, su receta (insumos) y adicionales.
     */
    GenericResponse getInventoryByTenant(Long tenantId);

    /**
     * Lista el catálogo compartido de insumos de un tenant (no son productos).
     */
    GenericResponse getInsumosByTenant(Long tenantId);

    GenericResponse createInsumo(Long tenantId, String nombre, String unidad, Double stock, Double stockMinimo);

    GenericResponse updateInsumo(Long insumoId, String nombre, String unidad, Double stock, Double stockMinimo);

    GenericResponse deleteInsumo(Long insumoId);

    GenericResponse restockInsumo(Long insumoId, Double cantidad);

    /**
     * Actualiza el stock propio de un producto sin receta (venta directa).
     */
    GenericResponse updateProductStock(Long productId, Double stock, Double stockMinimo, String unidad);

    /**
     * Restock directo de un producto sin receta.
     */
    GenericResponse restockProduct(Long productId, Double cantidad);

    /**
     * Recetas (BOM): obtener insumos de un platillo.
     */
    GenericResponse getRecipesByDish(Long dishId);

    /**
     * Recetas: agregar insumo a un platillo.
     */
    GenericResponse addRecipeIngredient(Long dishId, Long insumoId, Double cantidad, Boolean modificable);

    /**
     * Recetas: reemplaza la receta completa de un platillo.
     */
    GenericResponse setRecipes(Long dishId, List<Map<String, Object>> lines);

    /**
     * Recetas: quitar insumo de un platillo.
     */
    GenericResponse removeRecipeIngredient(Long recipeId);

    /**
     * Recetas: actualizar cantidad/modificable de un insumo de la receta.
     */
    GenericResponse updateRecipeIngredient(Long recipeId, Double cantidad, Boolean modificable);

    /**
     * Adicionales: obtener insumos adicionales permitidos de un platillo.
     */
    GenericResponse getAdditionalsByDish(Long dishId);

    /**
     * Adicionales: permitir insumo adicional.
     */
    GenericResponse addAdditional(Long dishId, Long insumoId, Double cantidad);

    /**
     * Adicionales: quitar adicional permitido.
     */
    GenericResponse removeAdditional(Long additionalId);

    /**
     * Descuenta stock de los insumos al confirmar una comanda.
     */
    GenericResponse deductForOrder(Long productId, Double cantidad, List<Long> excludedInsumoIds, List<Long> additionalInsumoIds);

    /**
     * Verifica si hay stock suficiente de un producto (dinámico si es platillo con receta).
     */
    boolean hasStock(Long productId, Double cantidad);
}
