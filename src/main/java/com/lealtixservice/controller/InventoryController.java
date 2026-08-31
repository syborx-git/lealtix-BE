package com.lealtixservice.controller;

import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inventory", description = "Gestión de inventario: insumos, recetas (BOM) y stock")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @Operation(summary = "Obtener inventario por tenant (productos con stock dinámico y su receta)")
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<GenericResponse> getByTenant(@PathVariable Long tenantId) {
        return ResponseEntity.ok(inventoryService.getInventoryByTenant(tenantId));
    }

    /* ============ Insumos (catálogo compartido, no son productos) ============ */

    @Operation(summary = "Obtener insumos de un tenant")
    @GetMapping("/insumos/tenant/{tenantId}")
    public ResponseEntity<GenericResponse> getInsumos(@PathVariable Long tenantId) {
        return ResponseEntity.ok(inventoryService.getInsumosByTenant(tenantId));
    }

    @Operation(summary = "Crear insumo")
    @PostMapping("/insumos")
    public ResponseEntity<GenericResponse> createInsumo(@RequestBody Map<String, Object> body) {
        Long tenantId = toLong(body.get("tenantId"));
        String nombre = body.get("nombre") != null ? body.get("nombre").toString() : null;
        String unidad = body.get("unidad") != null ? body.get("unidad").toString() : null;
        Double stock = toDouble(body.get("stock"));
        Double stockMinimo = toDouble(body.get("stockMinimo"));
        return ResponseEntity.ok(inventoryService.createInsumo(tenantId, nombre, unidad, stock, stockMinimo));
    }

    @Operation(summary = "Actualizar insumo")
    @PutMapping("/insumos/{insumoId}")
    public ResponseEntity<GenericResponse> updateInsumo(
            @PathVariable Long insumoId,
            @RequestBody Map<String, Object> body) {
        String nombre = body.get("nombre") != null ? body.get("nombre").toString() : null;
        String unidad = body.get("unidad") != null ? body.get("unidad").toString() : null;
        Double stock = toDouble(body.get("stock"));
        Double stockMinimo = toDouble(body.get("stockMinimo"));
        return ResponseEntity.ok(inventoryService.updateInsumo(insumoId, nombre, unidad, stock, stockMinimo));
    }

    @Operation(summary = "Eliminar insumo")
    @DeleteMapping("/insumos/{insumoId}")
    public ResponseEntity<GenericResponse> deleteInsumo(@PathVariable Long insumoId) {
        return ResponseEntity.ok(inventoryService.deleteInsumo(insumoId));
    }

    @Operation(summary = "Restock de insumo")
    @PostMapping("/insumos/{insumoId}/restock")
    public ResponseEntity<GenericResponse> restockInsumo(
            @PathVariable Long insumoId,
            @RequestBody Map<String, Object> body) {
        Double cantidad = toDouble(body.get("cantidad"));
        return ResponseEntity.ok(inventoryService.restockInsumo(insumoId, cantidad));
    }

    /* ============ Stock directo de producto sin receta ============ */

    @Operation(summary = "Actualizar stock, stock mínimo y unidad de un producto sin receta")
    @PutMapping("/product/{productId}")
    public ResponseEntity<GenericResponse> updateStock(
            @PathVariable Long productId,
            @RequestBody Map<String, Object> body) {
        Double stock = toDouble(body.get("stock"));
        Double stockMinimo = toDouble(body.get("stockMinimo"));
        String unidad = body.get("unidad") != null ? body.get("unidad").toString() : null;
        return ResponseEntity.ok(inventoryService.updateProductStock(productId, stock, stockMinimo, unidad));
    }

    @Operation(summary = "Restock: sumar cantidad al stock de un producto sin receta")
    @PostMapping("/product/{productId}/restock")
    public ResponseEntity<GenericResponse> restock(
            @PathVariable Long productId,
            @RequestBody Map<String, Object> body) {
        Double cantidad = toDouble(body.get("cantidad"));
        return ResponseEntity.ok(inventoryService.restockProduct(productId, cantidad));
    }

    /* ============ Recetas (BOM) ============ */

    @Operation(summary = "Obtener insumos de un platillo")
    @GetMapping("/dish/{dishId}/recipes")
    public ResponseEntity<GenericResponse> getRecipes(@PathVariable Long dishId) {
        return ResponseEntity.ok(inventoryService.getRecipesByDish(dishId));
    }

    @Operation(summary = "Agregar insumo a la receta de un platillo")
    @PostMapping("/dish/{dishId}/recipes")
    public ResponseEntity<GenericResponse> addRecipe(
            @PathVariable Long dishId,
            @RequestBody Map<String, Object> body) {
        Long insumoId = toLong(body.get("insumoId"));
        Double cantidad = toDouble(body.get("cantidad"));
        Boolean modificable = body.get("modificable") != null ? Boolean.valueOf(body.get("modificable").toString()) : null;
        return ResponseEntity.ok(inventoryService.addRecipeIngredient(dishId, insumoId, cantidad, modificable));
    }

    @Operation(summary = "Reemplazar la receta completa de un platillo")
    @PutMapping("/dish/{dishId}/recipes")
    public ResponseEntity<GenericResponse> setRecipes(
            @PathVariable Long dishId,
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lines = (List<Map<String, Object>>) body.getOrDefault("lines", new java.util.ArrayList<>());
        return ResponseEntity.ok(inventoryService.setRecipes(dishId, lines));
    }

    @Operation(summary = "Quitar insumo de una receta")
    @DeleteMapping("/recipes/{recipeId}")
    public ResponseEntity<GenericResponse> removeRecipe(@PathVariable Long recipeId) {
        return ResponseEntity.ok(inventoryService.removeRecipeIngredient(recipeId));
    }

    @Operation(summary = "Actualizar cantidad/modificable de un insumo de receta")
    @PutMapping("/recipes/{recipeId}")
    public ResponseEntity<GenericResponse> updateRecipe(
            @PathVariable Long recipeId,
            @RequestBody Map<String, Object> body) {
        Double cantidad = toDouble(body.get("cantidad"));
        Boolean modificable = body.get("modificable") != null ? Boolean.valueOf(body.get("modificable").toString()) : null;
        return ResponseEntity.ok(inventoryService.updateRecipeIngredient(recipeId, cantidad, modificable));
    }

    /* ============ Adicionales ============ */

    @Operation(summary = "Obtener adicionales permitidos de un platillo")
    @GetMapping("/dish/{dishId}/additionals")
    public ResponseEntity<GenericResponse> getAdditionals(@PathVariable Long dishId) {
        return ResponseEntity.ok(inventoryService.getAdditionalsByDish(dishId));
    }

    @Operation(summary = "Permitir adicional a un platillo")
    @PostMapping("/dish/{dishId}/additionals")
    public ResponseEntity<GenericResponse> addAdditional(
            @PathVariable Long dishId,
            @RequestBody Map<String, Object> body) {
        Long insumoId = toLong(body.get("insumoId"));
        Double cantidad = toDouble(body.get("cantidad"));
        Double precio = toDouble(body.get("precio"));
        return ResponseEntity.ok(inventoryService.addAdditional(dishId, insumoId, cantidad, precio));
    }

    @Operation(summary = "Actualizar cantidad/precio de un adicional permitido")
    @PutMapping("/additionals/{additionalId}")
    public ResponseEntity<GenericResponse> updateAdditional(
            @PathVariable Long additionalId,
            @RequestBody Map<String, Object> body) {
        Double cantidad = toDouble(body.get("cantidad"));
        Double precio = toDouble(body.get("precio"));
        return ResponseEntity.ok(inventoryService.updateAdditional(additionalId, cantidad, precio));
    }

    @Operation(summary = "Quitar adicional permitido")
    @DeleteMapping("/additionals/{additionalId}")
    public ResponseEntity<GenericResponse> removeAdditional(@PathVariable Long additionalId) {
        return ResponseEntity.ok(inventoryService.removeAdditional(additionalId));
    }

    /* ============ Descuento de comanda ============ */

    @Operation(summary = "Descontar stock de insumos al confirmar una comanda")
    @PostMapping("/deduct")
    public ResponseEntity<GenericResponse> deduct(@RequestBody Map<String, Object> body) {
        Long productId = toLong(body.get("productId"));
        Double cantidad = toDouble(body.get("cantidad"));
        List<Long> excluded = toLongList(body.get("excludedInsumoIds"));
        List<Long> additionals = toLongList(body.get("additionalInsumoIds"));
        return ResponseEntity.ok(inventoryService.deductForOrder(productId, cantidad, excluded, additionals));
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Long> toLongList(Object value) {
        if (value == null) return null;
        List<Long> result = new java.util.ArrayList<>();
        if (value instanceof List) {
            for (Object o : (List<Object>) value) {
                Long l = toLong(o);
                if (l != null) result.add(l);
            }
        }
        return result;
    }

    private Double toDouble(Object value) {
        if (value == null) return null;
        try {
            return Double.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
