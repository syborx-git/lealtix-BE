package com.lealtixservice.service;

import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.entity.TenantMenuProduct;

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

    /**
     * Bodega: lista TODOS los insumos del tenant (insumos y bebidas) con su
     * stock por ubicación (bodega, cocina, barra) y stock mínimo.
     */
    GenericResponse getBodegaByTenant(Long tenantId);

    /**
     * Bodega: registra un insumo directamente en bodega (alta con carga inicial).
     * El costo de la carga es obligatorio cuando la cantidad inicial es mayor a 0.
     */
    GenericResponse createInsumoBodega(Long tenantId, String nombre, String unidad, Double cantidad, Double costoTotal, Double stockMinimo, List<Long> categoryIds);

    /**
     * Bodega: restock (entrada) de un insumo hacia la bodega.
     * El costo total de la carga es obligatorio.
     */
    GenericResponse restockBodega(Long insumoId, Double cantidad, Double costoTotal);

    /**
     * Bodega: mueve stock desde bodega hacia cocina o barra.
     * Destino válido: "cocina" | "barra".
     */
    GenericResponse moverBodega(Long insumoId, Double cantidad, String destino);

    /**
     * Reporte: historial de transferencias bodega -> cocina/barra del tenant.
     */
    GenericResponse getTransferenciasByTenant(Long tenantId);

    GenericResponse createInsumo(Long tenantId, String nombre, String unidad, Double stock, Double stockMinimo, List<Long> categoryIds);

    GenericResponse updateInsumo(Long insumoId, String nombre, String unidad, Double stock, Double stockMinimo, List<Long> categoryIds);

    GenericResponse deleteInsumo(Long insumoId);

    GenericResponse restockInsumo(Long insumoId, Double cantidad, Double costoTotal);

    /**
     * Lista las bebidas (insumos con esBebida=true) de un tenant, incluyendo su precio de venta.
     */
    GenericResponse getBebidasByTenant(Long tenantId);

    /**
     * Crea una bebida: registra el insumo marcado como bebida (pieza o mililitros) con su stock
     * y crea el producto de menú enlazado (con receta de 1 unidad) para que se venda en Comandix.
     */
    GenericResponse createBebida(Long tenantId, String nombre, String unidad, Double stock, Double stockMinimo, Double precioVenta, List<Long> categoryIds);

    /**
     * Actualiza los datos de una bebida (insumo + producto de menú enlazado).
     */
    GenericResponse updateBebida(Long insumoId, String nombre, String unidad, Double stock, Double stockMinimo, Double precioVenta, List<Long> categoryIds);

    /**
     * Elimina una bebida (insumo y su producto de menú enlazado).
     */
    GenericResponse deleteBebida(Long insumoId);

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
     * Adicionales: permitir insumo adicional con un precio extra.
     */
    GenericResponse addAdditional(Long dishId, Long insumoId, Double cantidad, Double precio);

    /**
     * Adicionales: actualizar cantidad / precio de un adicional permitido.
     */
    GenericResponse updateAdditional(Long additionalId, Double cantidad, Double precio);

    /**
     * Adicionales: quitar adicional permitido.
     */
    GenericResponse removeAdditional(Long additionalId);

    /**
     * Sub-recetas: lista las preparaciones no vendidas individualmente (ej. salsas)
     * de un tenant, con sus insumos.
     */
    GenericResponse getSubRecetasByTenant(Long tenantId);

    /**
     * Sub-recetas: crea una preparación (producto esSubReceta=true) con sus insumos.
     */
    GenericResponse createSubReceta(Long tenantId, String nombre, List<Map<String, Object>> lines, List<Long> categoryIds);

    /**
     * Sub-recetas: actualiza nombre e insumos de una preparación.
     */
    GenericResponse updateSubReceta(Long subRecetaId, String nombre, List<Map<String, Object>> lines, List<Long> categoryIds);

    /**
     * Sub-recetas: elimina una preparación (y sus insumos).
     */
    GenericResponse deleteSubReceta(Long subRecetaId);

    /**
     * Sub-recetas: lista las sub-recetas asignadas a un platillo o bebida.
     */
    GenericResponse getSubRecetasByDish(Long dishId);

    /**
     * Sub-recetas: asigna una sub-receta a un platillo o bebida.
     */
    GenericResponse assignSubReceta(Long dishId, Long subRecetaId);

    /**
     * Sub-recetas: quita una sub-receta de un platillo o bebida.
     */
    GenericResponse removeSubRecetaFromDish(Long dishId, Long subRecetaId);

    /**
     * Descuenta stock de los insumos al confirmar una comanda.
     */
    GenericResponse deductForOrder(Long productId, Double cantidad, List<Long> excludedInsumoIds, List<Long> additionalInsumoIds);

    /**
     * Restaura el stock de los insumos al cancelar una comanda (inverso de deductForOrder).
     */
    GenericResponse restoreForOrder(Long productId, Double cantidad, List<Long> excludedInsumoIds, List<Long> additionalInsumoIds);

    /**
     * Verifica si hay stock suficiente de un producto (dinámico si es platillo con receta).
     */
    boolean hasStock(Long productId, Double cantidad);

    /**
     * Indica si el producto puede prepararse/venderse al menos 1 unidad hoy
     * (platillos: mínimo de floor(stockInsumo/cantidadReceta) de sus insumos;
     * productos sin receta: su stock directo). No depende de isActive.
     */
    boolean isProductAvailable(TenantMenuProduct product);

    /**
     * Recalcula la disponibilidad de todos los productos del tenant y sincroniza
     * su isActive automáticamente (solo productos con autoAvailability=true):
     * se desactivan si no pueden prepararse y se reactivan al abastecer.
     *
     * @return número de productos cuyo isActive cambió.
     */
    int syncProductAvailabilityByTenant(Long tenantId);
}
