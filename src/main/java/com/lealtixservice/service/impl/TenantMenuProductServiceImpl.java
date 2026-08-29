package com.lealtixservice.service.impl;

import com.lealtixservice.dto.BulkProductRequest;
import com.lealtixservice.dto.BulkProductResponse;
import com.lealtixservice.dto.TenantMenuCategoryDTO;
import com.lealtixservice.dto.TenantMenuProductDTO;
import com.lealtixservice.entity.ProductRecipe;
import com.lealtixservice.entity.Tenant;
import com.lealtixservice.entity.TenantMenuCategory;
import com.lealtixservice.entity.TenantMenuProduct;
import com.lealtixservice.repository.ProductRecipeRepository;
import com.lealtixservice.repository.TenantMenuProductRepository;
import com.lealtixservice.repository.TenantRepository;
import com.lealtixservice.service.TenantMenuCategoryService;
import com.lealtixservice.service.TenantMenuProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TenantMenuProductServiceImpl implements TenantMenuProductService {

    @Autowired
    private  TenantMenuProductRepository productRepository;

    @Autowired
    private TenantMenuCategoryService categoryService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ProductRecipeRepository recipeRepository;

    @Override
    public TenantMenuProduct save(TenantMenuProduct product) {
        return productRepository.save(product);
    }

    @Override
    public Optional<TenantMenuProduct> findById(Long id) {
        return productRepository.findById(id);
    }

    @Override
    public List<TenantMenuProduct> findAll() {
        return productRepository.findAll();
    }

    @Override
    public void deleteById(Long id) {
        productRepository.deleteById(id);
    }

    @Override
    @Transactional
    public TenantMenuProductDTO create(TenantMenuProductDTO product) {
        if (product == null || product.getTenantId() == null) {
            throw new IllegalArgumentException("El producto y el tenantId no pueden ser nulos");
        }

        TenantMenuCategory category = resolveCategory(product);
        TenantMenuProduct productEntity = buildProductEntity(product, category);

        TenantMenuProduct savedProduct = productRepository.save(productEntity);
        product.setId(savedProduct.getId());

        return product;
    }

    /**
     * Resuelve la categoría del producto, creándola si no existe.
     * Utiliza normalización para búsqueda case-insensitive.
     */
    private TenantMenuCategory resolveCategory(TenantMenuProductDTO product) {
        // Si se proporciona un ID de categoría válido, intentar usarlo
        if (product.getCategoryId() != null && product.getCategoryId() > 0) {
            Optional<TenantMenuCategory> existingCategory = categoryService.findById(product.getCategoryId());
            if (existingCategory.isPresent()) {
                return existingCategory.get();
            }
        }

        // Si hay nombre de categoría, buscar/crear con normalización
        if (product.getCategoryName() != null && !product.getCategoryName().trim().isEmpty()) {
            return categoryService.findOrCreateByNameNormalized(
                product.getTenantId(),
                product.getCategoryName()
            );
        }

        throw new IllegalArgumentException("Se debe proporcionar categoryId o categoryName");
    }

    /**
     * Construye la entidad de producto a partir del DTO.
     */
    private TenantMenuProduct buildProductEntity(TenantMenuProductDTO dto, TenantMenuCategory category) {
        TenantMenuProduct productEntity;

        // Si existe ID, actualizar producto existente
        if (dto.getId() != null) {
            productEntity = productRepository.findById(dto.getId())
                    .orElse(new TenantMenuProduct());
        } else {
            productEntity = new TenantMenuProduct();
        }

        productEntity.setCategory(category);

        if (dto.getName() != null && !dto.getName().isEmpty()) {
            productEntity.setNombre(dto.getName());
        }

        if (dto.getDescription() != null && !dto.getDescription().isEmpty()) {
            productEntity.setDescripcion(dto.getDescription());
        }

        if (dto.getPrice() != null) {
            productEntity.setPrecio(dto.getPrice());
        }

        productEntity.setImgUrl(dto.getImageUrl());

        if (dto.getIsActive() != null) {
            productEntity.setActive(dto.getIsActive());
        }

        return productEntity;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantMenuProductDTO> getProductsByTenantId(Long tenantId) {
        List<TenantMenuProductDTO> products = productRepository.findByCategoryTenantId(tenantId);
        if (products == null) return List.of();

        // Enriquecer con stock disponible (dinámico para platillos con receta)
        Map<Long, TenantMenuProduct> entities = new HashMap<>();
        for (TenantMenuProduct p : productRepository.findAllByTenantId(tenantId)) {
            entities.put(p.getId(), p);
        }
        for (TenantMenuProductDTO dto : products) {
            TenantMenuProduct entity = entities.get(dto.getId());
            if (entity == null) continue;
            List<ProductRecipe> recipes = recipeRepository.findByDishId(entity.getId());
            double stock;
            if (!recipes.isEmpty()) {
                double min = Double.MAX_VALUE;
                for (ProductRecipe r : recipes) {
                    double qty = r.getCantidad() != null ? r.getCantidad().doubleValue() : 0.0;
                    if (qty <= 0) continue;
                    double insumoStock = r.getInsumo().getStock() != null ? r.getInsumo().getStock() : 0.0;
                    min = Math.min(min, Math.floor(insumoStock / qty));
                }
                stock = min == Double.MAX_VALUE ? 0.0 : Math.max(0.0, min);
            } else {
                stock = entity.getStock() != null ? entity.getStock() : 0.0;
            }
            dto.setStock(stock);
            dto.setStockMinimo(entity.getStockMinimo() != null ? entity.getStockMinimo() : 0.0);
            dto.setUnidad(entity.getUnidad() != null ? entity.getUnidad() : "pieza");
        }

        products.sort((p1, p2) -> {
            Integer order1 = p1.getCategoryDisplayOrder() != null ? p1.getCategoryDisplayOrder() : Integer.MAX_VALUE;
            Integer order2 = p2.getCategoryDisplayOrder() != null ? p2.getCategoryDisplayOrder() : Integer.MAX_VALUE;
            int orderComparison = order1.compareTo(order2);
            if (orderComparison != 0) {
                return orderComparison;
            }
            String c1 = p1.getCategoryName() != null ? p1.getCategoryName() : "";
            String c2 = p2.getCategoryName() != null ? p2.getCategoryName() : "";
            return c1.compareToIgnoreCase(c2);
        });
        return products;
    }

    @Override
    @Transactional
    public BulkProductResponse createBulk(BulkProductRequest bulkRequest) {
        if (bulkRequest == null || bulkRequest.getProducts() == null || bulkRequest.getProducts().isEmpty()) {
            throw new IllegalArgumentException("La solicitud bulk debe contener al menos un producto");
        }

        if (bulkRequest.getTenantId() == null) {
            throw new IllegalArgumentException("El tenantId no puede ser nulo");
        }

        // Validar que el tenant existe
        tenantRepository.findById(bulkRequest.getTenantId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "No se encontró el tenant con ID: " + bulkRequest.getTenantId()));

        List<TenantMenuProductDTO> createdProducts = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        // Procesar cada producto
        for (int i = 0; i < bulkRequest.getProducts().size(); i++) {
            TenantMenuProductDTO productDTO = bulkRequest.getProducts().get(i);

            try {
                // Asegurar que el producto tenga el tenantId correcto
                productDTO.setTenantId(bulkRequest.getTenantId());

                // Validar datos mínimos
                validateProductDTO(productDTO);

                // Resolver categoría con normalización
                TenantMenuCategory category = resolveCategory(productDTO);

                // Construir y guardar producto
                TenantMenuProduct productEntity = buildProductEntity(productDTO, category);
                TenantMenuProduct savedProduct = productRepository.save(productEntity);

                // Actualizar DTO con ID generado
                productDTO.setId(savedProduct.getId());
                productDTO.setCategoryId(category.getId());

                createdProducts.add(productDTO);
                successCount++;

            } catch (Exception e) {
                failureCount++;
                String errorMsg = String.format("Error en producto #%d (%s): %s",
                    i + 1,
                    productDTO.getName() != null ? productDTO.getName() : "sin nombre",
                    e.getMessage());
                errors.add(errorMsg);
            }
        }

        return BulkProductResponse.builder()
                .totalProcessed(bulkRequest.getProducts().size())
                .successCount(successCount)
                .failureCount(failureCount)
                .createdProducts(createdProducts)
                .errors(errors.isEmpty() ? null : errors)
                .build();
    }

    /**
     * Valida que el DTO de producto contenga los datos mínimos requeridos.
     */
    private void validateProductDTO(TenantMenuProductDTO dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del producto es obligatorio");
        }

        if (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio debe ser un valor válido mayor o igual a cero");
        }

        if ((dto.getCategoryId() == null || dto.getCategoryId() <= 0) &&
            (dto.getCategoryName() == null || dto.getCategoryName().trim().isEmpty())) {
            throw new IllegalArgumentException("Se debe proporcionar categoryId o categoryName");
        }
    }
}
