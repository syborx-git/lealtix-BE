package com.lealtixservice.service.impl;

import com.lealtixservice.dto.CrossSellingDTO;
import com.lealtixservice.dto.ProductCrossSellingRequest;
import com.lealtixservice.dto.ProductCrossSellingResponse;
import com.lealtixservice.entity.ProductCrossSelling;
import com.lealtixservice.entity.Tenant;
import com.lealtixservice.entity.TenantMenuProduct;
import com.lealtixservice.repository.ProductCrossSellingRepository;
import com.lealtixservice.repository.TenantMenuProductRepository;
import com.lealtixservice.repository.TenantRepository;
import com.lealtixservice.service.ProductCrossSellingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductCrossSellingServiceImpl implements ProductCrossSellingService {

    private static final String DEFAULT_IMAGE_PLACEHOLDER = "/assets/product-placeholder.png";
    
    /**
     * Límite máximo de sugerencias de cross-selling por producto.
     * Basado en mejores prácticas de UX (3-5 sugerencias óptimas).
     */
    private static final int MAX_SUGGESTIONS_PER_PRODUCT = 3;

    @Autowired
    private ProductCrossSellingRepository crossSellingRepository;

    @Autowired
    private TenantMenuProductRepository productRepository;
    
    @Autowired
    private TenantRepository tenantRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CrossSellingDTO> getSuggestionsByProduct(Long productId, Long tenantId) {
        // Validar entrada
        if (productId == null || tenantId == null) {
            throw new IllegalArgumentException("productId y tenantId son requeridos");
        }

        // Validar que el producto existe y pertenece al tenant
        TenantMenuProduct product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

        if (!product.getCategory().getTenant().getId().equals(tenantId)) {
            throw new IllegalArgumentException("El producto no pertenece al tenant especificado");
        }

        // Obtener sugerencias activas
        List<ProductCrossSelling> suggestions = crossSellingRepository
            .findActiveSuggestionsByProductAndTenant(productId, tenantId);

        // Mapear a DTOs
        return suggestions.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public ProductCrossSellingResponse createCrossSelling(ProductCrossSellingRequest request) {
        // Validaciones
        validateRequest(request);
        
        // Verificar que no sea el mismo producto
        if (request.getProductId().equals(request.getSuggestedProductId())) {
            throw new IllegalArgumentException("Un producto no puede sugerirse a sí mismo");
        }
        
        // Verificar si ya existe
        if (crossSellingRepository.existsByProductIdAndSuggestedProductIdAndTenantId(
                request.getProductId(), request.getSuggestedProductId(), request.getTenantId())) {
            throw new IllegalArgumentException("Esta configuración de cross-selling ya existe");
        }
        
        // Verificar límite máximo de sugerencias por producto
        List<ProductCrossSelling> existingSuggestions = crossSellingRepository
            .findByProductIdAndTenantId(request.getProductId(), request.getTenantId());
        
        if (existingSuggestions.size() >= MAX_SUGGESTIONS_PER_PRODUCT) {
            throw new IllegalArgumentException(
                String.format("El producto ya tiene el máximo de %d sugerencias permitidas. " +
                             "Por favor, elimina una sugerencia existente antes de agregar una nueva.", 
                             MAX_SUGGESTIONS_PER_PRODUCT)
            );
        }
        
        // Obtener entidades
        TenantMenuProduct product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new IllegalArgumentException("Producto principal no encontrado"));
            
        TenantMenuProduct suggestedProduct = productRepository.findById(request.getSuggestedProductId())
            .orElseThrow(() -> new IllegalArgumentException("Producto sugerido no encontrado"));
            
        Tenant tenant = tenantRepository.findById(request.getTenantId())
            .orElseThrow(() -> new IllegalArgumentException("Tenant no encontrado"));
        
        // Validar que ambos productos pertenecen al tenant
        if (!product.getCategory().getTenant().getId().equals(request.getTenantId()) ||
            !suggestedProduct.getCategory().getTenant().getId().equals(request.getTenantId())) {
            throw new IllegalArgumentException("Los productos deben pertenecer al tenant especificado");
        }
        
        // Crear entidad
        ProductCrossSelling crossSelling = ProductCrossSelling.builder()
            .product(product)
            .suggestedProduct(suggestedProduct)
            .tenant(tenant)
            .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 1)
            .isActive(request.getIsActive() != null ? request.getIsActive() : true)
            .build();
        
        // Guardar
        ProductCrossSelling saved = crossSellingRepository.save(crossSelling);
        
        return mapToResponse(saved);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ProductCrossSellingResponse> getAllByTenant(Long tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId es requerido");
        }
        
        List<ProductCrossSelling> configurations = crossSellingRepository.findAllByTenantId(tenantId);
        
        return configurations.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ProductCrossSellingResponse> getByProduct(Long productId, Long tenantId) {
        if (productId == null || tenantId == null) {
            throw new IllegalArgumentException("productId y tenantId son requeridos");
        }
        
        List<ProductCrossSelling> configurations = crossSellingRepository
            .findByProductIdAndTenantId(productId, tenantId);
        
        return configurations.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public ProductCrossSellingResponse updateCrossSelling(Long id, ProductCrossSellingRequest request) {
        if (id == null) {
            throw new IllegalArgumentException("ID es requerido");
        }
        
        validateRequest(request);
        
        // Obtener configuración existente
        ProductCrossSelling existing = crossSellingRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Configuración no encontrada"));
        
        // Validar que pertenece al tenant
        if (!existing.getTenant().getId().equals(request.getTenantId())) {
            throw new IllegalArgumentException("La configuración no pertenece al tenant especificado");
        }
        
        // Verificar que no sea el mismo producto
        if (request.getProductId().equals(request.getSuggestedProductId())) {
            throw new IllegalArgumentException("Un producto no puede sugerirse a sí mismo");
        }
        
        // Si cambiaron los productos, verificar duplicados
        if (!existing.getProduct().getId().equals(request.getProductId()) ||
            !existing.getSuggestedProduct().getId().equals(request.getSuggestedProductId())) {
            
            if (crossSellingRepository.existsByProductIdAndSuggestedProductIdAndTenantId(
                    request.getProductId(), request.getSuggestedProductId(), request.getTenantId())) {
                throw new IllegalArgumentException("Ya existe una configuración con estos productos");
            }
        }
        
        // Actualizar productos si cambiaron
        if (!existing.getProduct().getId().equals(request.getProductId())) {
            TenantMenuProduct newProduct = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Producto principal no encontrado"));
            existing.setProduct(newProduct);
        }
        
        if (!existing.getSuggestedProduct().getId().equals(request.getSuggestedProductId())) {
            TenantMenuProduct newSuggested = productRepository.findById(request.getSuggestedProductId())
                .orElseThrow(() -> new IllegalArgumentException("Producto sugerido no encontrado"));
            existing.setSuggestedProduct(newSuggested);
        }
        
        // Actualizar otros campos
        if (request.getDisplayOrder() != null) {
            existing.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getIsActive() != null) {
            existing.setIsActive(request.getIsActive());
        }
        
        ProductCrossSelling updated = crossSellingRepository.save(existing);
        
        return mapToResponse(updated);
    }
    
    @Override
    @Transactional
    public void deleteCrossSelling(Long id, Long tenantId) {
        if (id == null || tenantId == null) {
            throw new IllegalArgumentException("ID y tenantId son requeridos");
        }
        
        ProductCrossSelling existing = crossSellingRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Configuración no encontrada"));
        
        // Validar que pertenece al tenant
        if (!existing.getTenant().getId().equals(tenantId)) {
            throw new IllegalArgumentException("La configuración no pertenece al tenant especificado");
        }
        
        crossSellingRepository.delete(existing);
    }
    
    @Override
    @Transactional
    public ProductCrossSellingResponse toggleActive(Long id, Long tenantId, Boolean isActive) {
        if (id == null || tenantId == null || isActive == null) {
            throw new IllegalArgumentException("ID, tenantId e isActive son requeridos");
        }
        
        ProductCrossSelling existing = crossSellingRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Configuración no encontrada"));
        
        // Validar que pertenece al tenant
        if (!existing.getTenant().getId().equals(tenantId)) {
            throw new IllegalArgumentException("La configuración no pertenece al tenant especificado");
        }
        
        existing.setIsActive(isActive);
        ProductCrossSelling updated = crossSellingRepository.save(existing);
        
        return mapToResponse(updated);
    }

    /**
     * Mapea una entidad ProductCrossSelling a un DTO para sugerencias.
     * Usa placeholder si no hay imagen disponible.
     */
    private CrossSellingDTO mapToDTO(ProductCrossSelling crossSelling) {
        TenantMenuProduct suggestedProduct = crossSelling.getSuggestedProduct();
        
        return CrossSellingDTO.builder()
            .id(suggestedProduct.getId())
            .name(suggestedProduct.getNombre())
            .price(suggestedProduct.getPrecio())
            .imageUrl(getImageUrl(suggestedProduct.getImgUrl()))
            .description(suggestedProduct.getDescripcion())
            .categoryId(suggestedProduct.getCategory().getId())
            .categoryName(suggestedProduct.getCategory().getNombre())
            .build();
    }
    
    /**
     * Mapea una entidad ProductCrossSelling a un DTO de respuesta completo.
     */
    private ProductCrossSellingResponse mapToResponse(ProductCrossSelling crossSelling) {
        return ProductCrossSellingResponse.builder()
            .id(crossSelling.getId())
            .productId(crossSelling.getProduct().getId())
            .productName(crossSelling.getProduct().getNombre())
            .suggestedProductId(crossSelling.getSuggestedProduct().getId())
            .suggestedProductName(crossSelling.getSuggestedProduct().getNombre())
            .tenantId(crossSelling.getTenant().getId())
            .displayOrder(crossSelling.getDisplayOrder())
            .isActive(crossSelling.getIsActive())
            .createdAt(crossSelling.getCreatedAt())
            .updatedAt(crossSelling.getUpdatedAt())
            .build();
    }

    /**
     * Retorna la URL de imagen o un placeholder si no está disponible.
     */
    private String getImageUrl(String imgUrl) {
        return (imgUrl != null && !imgUrl.trim().isEmpty()) 
            ? imgUrl 
            : DEFAULT_IMAGE_PLACEHOLDER;
    }
    
    /**
     * Valida los campos básicos del request.
     */
    private void validateRequest(ProductCrossSellingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request no puede ser nulo");
        }
        if (request.getProductId() == null) {
            throw new IllegalArgumentException("productId es requerido");
        }
        if (request.getSuggestedProductId() == null) {
            throw new IllegalArgumentException("suggestedProductId es requerido");
        }
        if (request.getTenantId() == null) {
            throw new IllegalArgumentException("tenantId es requerido");
        }
    }
}