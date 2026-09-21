package com.lealtixservice.controller;

import com.lealtixservice.dto.CatalogCategoryDTO;
import com.lealtixservice.dto.CatalogProductDTO;
import com.lealtixservice.dto.CategoryDTO;
import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.dto.ReorderCategoriesRequest;
import com.lealtixservice.dto.TenantMenuCategoryDTO;
import com.lealtixservice.dto.TenantMenuProductDTO;
import com.lealtixservice.entity.TenantMenuCategory;
import com.lealtixservice.entity.TenantMenuProduct;
import com.lealtixservice.repository.TenantMenuCategoryRepository;
import com.lealtixservice.repository.TenantMenuProductRepository;
import com.lealtixservice.service.TenantMenuCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/tenant-menu-categories")
@Tag(name = "TenantMenuCategory", description = "Operaciones sobre categorías del menú del tenant")
public class TenantMenuCategoryController {

    private final TenantMenuCategoryService categoryService;

    @Autowired
    private TenantMenuProductRepository productRepository;

    @Autowired
    private TenantMenuCategoryRepository categoryRepository;

    @Autowired
    public TenantMenuCategoryController(TenantMenuCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "Obtener todas las categorías")
    @GetMapping
    public ResponseEntity<List<TenantMenuCategory>> getAll() {
        return ResponseEntity.ok(categoryService.findAll());
    }

    @Operation(summary = "Obtener una categoría por ID")
    @GetMapping("/{id}")
    public ResponseEntity<TenantMenuCategory> getById(@PathVariable Long id) {
        return categoryService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Obtener una categoría por tenantId")
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<GenericResponse> getByTenantId(@PathVariable Long tenantId) {
        try{
             List<TenantMenuProductDTO> menuCategoryList = categoryService.getByTenantId(tenantId);
             if(menuCategoryList != null && !menuCategoryList.isEmpty()){
                 return ResponseEntity.ok(new GenericResponse(200, "SUCCESS", menuCategoryList));
             }else{
                    return ResponseEntity.ok(new GenericResponse(404, "No se encontró la categoría para el tenantId proporcionado", null));
             }
        }catch (Exception e){
            return ResponseEntity.ok(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Obtener diferentes  categorías por tenantId")
    @GetMapping("/categories/{tenantId}")
    public ResponseEntity<GenericResponse> getCategoriesByTenantId(@PathVariable Long tenantId) {
        try{
            List<CategoryDTO> distinctCategories = categoryService.getCategoriesByTenantId(tenantId);
            if(distinctCategories != null && !distinctCategories.isEmpty()){
                return ResponseEntity.ok(new GenericResponse(200, "SUCCESS", distinctCategories));
            }else{
                return ResponseEntity.ok(new GenericResponse(404, "No se encontró la categoría para el tenantId proporcionado", null));
            }
        }catch (Exception e){
            return ResponseEntity.ok(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Obtener catálogo: categorías con productos por tenantId (migrado desde /api/catalog)")
    @GetMapping(value = "/catalog/categories-with-products", params = "tenantId")
    public ResponseEntity<GenericResponse> getCategoriesWithProductsByTenant(@RequestParam Long tenantId) {
        try {
            List<CatalogCategoryDTO> result = new ArrayList<>();
            List<TenantMenuProductDTO> flat = productRepository.findByCategoryTenantId(tenantId).stream()
                    .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                    .collect(Collectors.toList());
            Map<Long, List<TenantMenuProductDTO>> grouped = flat.stream()
                    .collect(Collectors.groupingBy(TenantMenuProductDTO::getCategoryId, LinkedHashMap::new, Collectors.toList()));

            for (Map.Entry<Long, List<TenantMenuProductDTO>> e : grouped.entrySet()) {
                Long catId = e.getKey();
                List<TenantMenuProductDTO> items = e.getValue();
                String catName = items.isEmpty() ? null : items.get(0).getCategoryName();
                List<CatalogProductDTO> products = items.stream()
                        .map(p -> CatalogProductDTO.builder()
                                .id(p.getId())
                                .name(p.getName())
                                .price(p.getPrice())
                                .build())
                        .collect(Collectors.toList());
                result.add(CatalogCategoryDTO.builder()
                        .id(catId)
                        .name(catName)
                        .products(products)
                        .build());
            }

            return ResponseEntity.ok(new GenericResponse(200, "SUCCESS", result));
        } catch (Exception ex) {
            log.error("Error al obtener catálogo por tenantId {}: {}", tenantId, ex.getMessage(), ex);
            return ResponseEntity.ok(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Obtener catálogo completo: categorías con productos (sin tenantId)")
    @GetMapping("/catalog/categories-with-products")
    public ResponseEntity<GenericResponse> getCategoriesWithProducts() {
        try {
            List<CatalogCategoryDTO> result = new ArrayList<>();
            List<TenantMenuCategory> categories = categoryRepository.findAll();
            for (TenantMenuCategory cat : categories) {
                List<TenantMenuProduct> productsEntity = productRepository.findByCategoryId(cat.getId()).stream()
                    .filter(p -> p.isActive())
                    .collect(Collectors.toList());
                List<CatalogProductDTO> products = productsEntity.stream()
                        .map(p -> CatalogProductDTO.builder()
                                .id(p.getId())
                                .name(p.getNombre())
                                 .price(p.getPrecio())
                                .build())
                        .collect(Collectors.toList());
                result.add(CatalogCategoryDTO.builder()
                        .id(cat.getId())
                        .name(cat.getNombre())
                        .products(products)
                        .build());
            }

            return ResponseEntity.ok(new GenericResponse(200, "SUCCESS", result));
        } catch (Exception ex) {
            log.error("Error al obtener catálogo completo: {}", ex.getMessage(), ex);
            return ResponseEntity.ok(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Crear una nueva categoría")
    @PostMapping
    public ResponseEntity<GenericResponse> createCategoryProduct(@RequestBody TenantMenuCategoryDTO category) {
        try{
            TenantMenuCategoryDTO resp = categoryService.createCategoryProduct(category);
            if(resp != null){
                return ResponseEntity.ok(new GenericResponse(201, "Categoría creada exitosamente", resp));
            }else{
                return ResponseEntity.ok(new GenericResponse(400, "No se pudo crear la categoría", null));
            }
        }catch (Exception e){
            log.error("Error al crear la categoría: ", e);
            return ResponseEntity.ok(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Eliminar una categoría por ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reordenar categorías mediante drag and drop")
    @PutMapping("/tenant/{tenantId}/reorder")
    public ResponseEntity<GenericResponse> reorderCategories(
            @PathVariable Long tenantId,
            @RequestBody ReorderCategoriesRequest request) {
        try {
            if (request.getCategories() == null || request.getCategories().isEmpty()) {
                return ResponseEntity.ok(new GenericResponse(400, "La lista de categorías no puede estar vacía", null));
            }

            categoryService.reorderCategories(tenantId, request.getCategories());
            return ResponseEntity.ok(new GenericResponse(200, "Categorías reordenadas exitosamente", null));
        } catch (IllegalArgumentException e) {
            log.error("Error de validación al reordenar categorías: ", e);
            return ResponseEntity.ok(new GenericResponse(400, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error al reordenar categorías: ", e);
            return ResponseEntity.ok(new GenericResponse(500, "Error interno del servidor", null));
        }
    }
}

