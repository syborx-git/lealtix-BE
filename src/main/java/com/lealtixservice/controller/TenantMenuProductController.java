package com.lealtixservice.controller;

import com.lealtixservice.dto.BulkProductRequest;
import com.lealtixservice.dto.BulkProductResponse;
import com.lealtixservice.dto.CrossSellingDTO;
import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.dto.GenericResponseProd;
import com.lealtixservice.dto.TenantMenuProductDTO;
import com.lealtixservice.entity.TenantMenuProduct;
import com.lealtixservice.service.ProductCrossSellingService;
import com.lealtixservice.service.TenantMenuProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api" +
        "/tenant-menu-products")
@Tag(name = "TenantMenuProduct", description = "Operaciones sobre productos del menú del tenant")
public class TenantMenuProductController {

    private final TenantMenuProductService productService;
    private final ProductCrossSellingService crossSellingService;

    @Autowired
    public TenantMenuProductController(
            TenantMenuProductService productService,
            ProductCrossSellingService crossSellingService) {
        this.productService = productService;
        this.crossSellingService = crossSellingService;
    }

    @Operation(summary = "Obtener todos los productos")
    @GetMapping
    public ResponseEntity<List<TenantMenuProduct>> getAll() {
        return ResponseEntity.ok(productService.findAll());
    }

    @Operation(summary = "Obtener un producto por tenantId")
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<GenericResponseProd> getProductsByTenantId(@PathVariable Long tenantId) {
        try {
            List<TenantMenuProductDTO>  products = productService.getProductsByTenantId(tenantId);
            if (products != null && !products.isEmpty()) {
                return ResponseEntity.ok(new GenericResponseProd(200, "Productos obtenidos exitosamente", products, products.size()));
            } else {
                return ResponseEntity.ok(new GenericResponseProd(400, "No se pudo obtener  Productos", null, 0));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(new GenericResponseProd(500, "Error interno del servidor", null, 0));
        }
    }

    @Operation(summary = "Crear un nuevo producto")
    @PostMapping
    public ResponseEntity<GenericResponse> create(@RequestBody TenantMenuProductDTO product) {
        try{
            TenantMenuProductDTO resp = productService.create(product);
            if(resp != null){
                return ResponseEntity.ok(new GenericResponse(201, "Producto creado exitosamente", resp));
            }else{
                return ResponseEntity.ok(new GenericResponse(400, "No se pudo crear la Producto", null));
            }
        }catch (Exception e){
            return ResponseEntity.ok(new GenericResponse(500, "Error interno del servidor: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Crear múltiples productos de forma masiva",
               description = "Permite crear varios productos en una sola operación. " +
                           "Utiliza búsqueda normalizada de categorías (case-insensitive, tolerante a plurales/acentos). " +
                           "Retorna un resumen con productos creados exitosamente y errores si los hay.")
    @PostMapping("/bulk")
    public ResponseEntity<GenericResponse> createBulk(@RequestBody BulkProductRequest bulkRequest) {
        try {
            BulkProductResponse response = productService.createBulk(bulkRequest);

            if (response.getSuccessCount() > 0) {
                String message = String.format(
                    "Proceso completado: %d productos creados exitosamente de %d solicitados",
                    response.getSuccessCount(),
                    response.getTotalProcessed()
                );

                // Si hubo errores parciales, código 207 (Multi-Status), sino 201 (Created)
                int statusCode = response.getFailureCount() > 0 ? 207 : 201;

                return ResponseEntity.status(HttpStatus.OK)
                        .body(new GenericResponse(statusCode, message, response));
            } else {
                return ResponseEntity.ok(
                    new GenericResponse(400, "No se pudo crear ningún producto", response)
                );
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(
                new GenericResponse(400, "Solicitud inválida: " + e.getMessage(), null)
            );
        } catch (Exception e) {
            return ResponseEntity.ok(
                new GenericResponse(500, "Error interno del servidor: " + e.getMessage(), null)
            );
        }
    }

    @Operation(
        summary = "Obtener sugerencias de productos complementarios (Cross-Selling)",
        description = "Retorna una lista de productos sugeridos para venta cruzada al seleccionar un producto. " +
                      "Diseñado para aumentar el ticket promedio y la recompra intencional. " +
                      "Las sugerencias se filtran por tenant para garantizar aislamiento SaaS."
    )
    @GetMapping("/{id}/suggestions")
    public ResponseEntity<GenericResponse> getCrossSellingSuggestions(
            @Parameter(description = "ID del producto principal") 
            @PathVariable Long id,
            @Parameter(description = "ID del tenant") 
            @RequestParam Long tenantId) {
        try {
            List<CrossSellingDTO> suggestions = crossSellingService.getSuggestionsByProduct(id, tenantId);
            
            if (suggestions != null && !suggestions.isEmpty()) {
                return ResponseEntity.ok(
                    new GenericResponse(200, "Sugerencias obtenidas exitosamente", suggestions)
                );
            } else {
                return ResponseEntity.ok(
                    new GenericResponse(200, "No hay sugerencias disponibles para este producto", List.of())
                );
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(
                new GenericResponse(400, "Error: " + e.getMessage(), null)
            );
        } catch (Exception e) {
            return ResponseEntity.ok(
                new GenericResponse(500, "Error interno del servidor: " + e.getMessage(), null)
            );
        }
    }

    @Operation(summary = "Eliminar un producto por ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

