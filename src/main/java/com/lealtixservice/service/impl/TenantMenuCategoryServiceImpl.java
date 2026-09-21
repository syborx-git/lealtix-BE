package com.lealtixservice.service.impl;

import com.lealtixservice.dto.CategoryDTO;
import com.lealtixservice.dto.ReorderCategoryRequest;
import com.lealtixservice.dto.TenantMenuCategoryDTO;
import com.lealtixservice.dto.TenantMenuProductDTO;
import com.lealtixservice.entity.Tenant;
import com.lealtixservice.entity.TenantMenuCategory;
import com.lealtixservice.entity.TenantMenuProduct;
import com.lealtixservice.repository.TenantMenuCategoryRepository;
import com.lealtixservice.repository.TenantMenuProductRepository;
import com.lealtixservice.repository.TenantRepository;
import com.lealtixservice.service.TenantMenuCategoryService;
import com.lealtixservice.util.TextNormalizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service
public class TenantMenuCategoryServiceImpl implements TenantMenuCategoryService {

    @Autowired
    private TenantMenuCategoryRepository categoryRepository;

    @Autowired
    private TenantMenuProductRepository productRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @Override
    public TenantMenuCategory save(TenantMenuCategory category) {
        return categoryRepository.save(category);
    }

    @Override
    public Optional<TenantMenuCategory> findById(Long id) {
        return categoryRepository.findById(id);
    }

    @Override
    public List<TenantMenuCategory> findAll() {
        return categoryRepository.findAll();
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        if (id == null) return;

        // 1. Desvincular insumos de esta categoría
        entityManager.createNativeQuery("DELETE FROM insumo_category WHERE category_id = :id")
                .setParameter("id", id)
                .executeUpdate();

        // 2. Desvincular productos de la tabla many-to-many con esta categoría
        entityManager.createNativeQuery("DELETE FROM tenant_menu_product_category WHERE category_id = :id")
                .setParameter("id", id)
                .executeUpdate();

        // 3. Reasignar o limpiar productos cuya categoría principal sea esta
        List<TenantMenuProduct> products = productRepository.findByCategoryId(id);
        for (TenantMenuProduct p : products) {
            List<?> otherCats = entityManager.createNativeQuery(
                    "SELECT category_id FROM tenant_menu_product_category WHERE product_id = :prodId LIMIT 1")
                    .setParameter("prodId", p.getId())
                    .getResultList();
            if (!otherCats.isEmpty()) {
                Long newCatId = ((Number) otherCats.get(0)).longValue();
                entityManager.createNativeQuery("UPDATE tenant_menu_product SET category_id = :newCatId WHERE id = :prodId")
                        .setParameter("newCatId", newCatId)
                        .setParameter("prodId", p.getId())
                        .executeUpdate();
            } else {
                entityManager.createNativeQuery("DELETE FROM product_recipe WHERE dish_product_id = :prodId")
                        .setParameter("prodId", p.getId()).executeUpdate();
                entityManager.createNativeQuery("DELETE FROM product_additional WHERE dish_product_id = :prodId")
                        .setParameter("prodId", p.getId()).executeUpdate();
                entityManager.createNativeQuery("DELETE FROM product_sub_receta WHERE dish_product_id = :prodId OR sub_receta_id = :prodId")
                        .setParameter("prodId", p.getId()).executeUpdate();
                entityManager.createNativeQuery("DELETE FROM product_cross_selling WHERE product_id = :prodId OR suggested_product_id = :prodId")
                        .setParameter("prodId", p.getId()).executeUpdate();
                entityManager.createNativeQuery("DELETE FROM client_order_item WHERE product_id = :prodId")
                        .setParameter("prodId", p.getId()).executeUpdate();
                entityManager.createNativeQuery("UPDATE insumo SET producto_id = NULL WHERE producto_id = :prodId")
                        .setParameter("prodId", p.getId()).executeUpdate();
                entityManager.createNativeQuery("DELETE FROM tenant_menu_product WHERE id = :prodId")
                        .setParameter("prodId", p.getId()).executeUpdate();
            }
        }

        // 4. Eliminar la categoría
        categoryRepository.deleteById(id);
    }

    @Override
    public List<TenantMenuProductDTO> getByTenantId(Long tenantId) {
        List<TenantMenuCategory> categoryList = categoryRepository.findByTenantId(tenantId);
        if (categoryList == null || categoryList.isEmpty()) {
            return null;
        }
        return categoryList.stream()
                .flatMap(category -> {
                    List<TenantMenuProduct> products = productRepository.findByCategoryId(category.getId());
                    return products.stream().map(product -> TenantMenuProductDTO.builder()
                            .id(product.getId())
                            .categoryId(category.getId())
                            .name(product.getNombre())
                            .description(product.getDescripcion())
                            .price(product.getPrecio() != null ? product.getPrecio() : BigDecimal.ZERO)
                            .imageUrl(product.getImgUrl())
                            .tenantId(category.getTenant().getId())
                            .categoryName(category.getNombre())
                            .categoryDescription(category.getDescripcion())
                            .build());
                })
                .collect(toList());
    }

    @Override
    public TenantMenuCategoryDTO createCategoryProduct(TenantMenuCategoryDTO categoryDTO) {
        if (categoryDTO == null || categoryDTO.getTenantId() == null) {
            throw new IllegalArgumentException("El objeto category o tenantId no puede ser nulo");
        }

        // Buscar si ya existe la categoría
        Optional<TenantMenuCategory> existingCategoryOpt = categoryRepository.findByTenantId(categoryDTO.getTenantId())
                .stream()
                .filter(cat -> cat.getNombre().equalsIgnoreCase(categoryDTO.getName()))
                .findFirst();

        TenantMenuCategory categoryEntity;
        if (existingCategoryOpt.isPresent()) {
            categoryEntity = existingCategoryOpt.get();
            categoryEntity.setUpdatedAt(LocalDateTime.now());
            categoryEntity.setNombre(categoryDTO.getName());
            categoryEntity.setActive(categoryDTO.isActive());
            categoryEntity.setDescripcion(categoryDTO.getDescription());
        } else {
            // Obtener el máximo displayOrder actual y sumar 1 para la nueva categoría
            Integer maxDisplayOrder = categoryRepository.findMaxDisplayOrderByTenantId(categoryDTO.getTenantId());
            Integer newDisplayOrder = (maxDisplayOrder != null ? maxDisplayOrder : 0) + 1;

            categoryEntity = TenantMenuCategory.builder()
                    .nombre(categoryDTO.getName())
                    .descripcion(categoryDTO.getDescription())
                    .isActive(categoryDTO.isActive())
                    .tenant(Tenant.builder().id(categoryDTO.getTenantId()).build())
                    .displayOrder(newDisplayOrder)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        }
        categoryEntity = categoryRepository.save(categoryEntity);

        // Si hay productos en el DTO, crear y asociar
        if (categoryDTO.getProductsDTO() != null && !categoryDTO.getProductsDTO().isEmpty()) {
            for (TenantMenuProductDTO productDTO : categoryDTO.getProductsDTO()) {
                TenantMenuProduct productEntity = TenantMenuProduct.builder()
                        .nombre(productDTO.getName())
                        .descripcion(productDTO.getDescription())
                        .precio(productDTO.getPrice() != null ? productDTO.getPrice() : BigDecimal.ZERO)
                        .imgUrl(productDTO.getImageUrl())
                        .category(categoryEntity)
                        .isActive(true)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                productRepository.save(productEntity);
            }
        }

        // Mapear productos a DTO
        List<TenantMenuProduct> products = productRepository.findByCategoryId(categoryEntity.getId());
        TenantMenuCategory finalCategoryEntity = categoryEntity;
        List<TenantMenuProductDTO> productsDTO = products.stream()
                .map(product -> TenantMenuProductDTO.builder()
                        .id(product.getId())
                        .name(product.getNombre())
                        .description(product.getDescripcion())
                        .price(product.getPrecio() != null ? product.getPrecio() : BigDecimal.ZERO)
                        .imageUrl(product.getImgUrl())
                        .tenantId(finalCategoryEntity.getTenant().getId())
                        .build())
                .collect(toList());

        return TenantMenuCategoryDTO.builder()
                .id(categoryEntity.getId())
                .tenantId(categoryEntity.getTenant().getId())
                .name(categoryEntity.getNombre())
                .productsDTO(productsDTO)
                .build();
    }

    @Override
    public List<CategoryDTO> getCategoriesByTenantId(Long tenantId) {
        List<TenantMenuCategory> categoriesEntity = categoryRepository.findByTenantIdOrderByDisplayOrderAsc(tenantId);
        if (categoriesEntity == null || categoriesEntity.isEmpty()) {
            return null;
        }
        return categoriesEntity.stream()
                .map(cat -> CategoryDTO.builder()
                        .categoryId(cat.getId())
                        .categoryName(cat.getNombre())
                        .categoryDescription(cat.getDescripcion())
                        .isActive(cat.isActive())
                        .tenantId(cat.getTenant().getId())
                        .displayOrder(cat.getDisplayOrder())
                        .build())
                .collect(toList());
    }

    @Override
    @Transactional
    public void reorderCategories(Long tenantId, List<ReorderCategoryRequest> reorderRequests) {
        if (reorderRequests == null || reorderRequests.isEmpty()) {
            throw new IllegalArgumentException("La lista de reordenamiento no puede estar vacía");
        }

        // Obtener todas las categorías del tenant ordenadas
        List<TenantMenuCategory> categories = categoryRepository
                .findByTenantIdOrderByDisplayOrderAsc(tenantId);

        // Crear un mapa para acceso rápido por ID
        Map<Long, TenantMenuCategory> categoryMap = categories.stream()
                .collect(Collectors.toMap(TenantMenuCategory::getId, Function.identity()));

        // Actualizar el displayOrder de cada categoría según la nueva posición
        for (ReorderCategoryRequest request : reorderRequests) {
            TenantMenuCategory category = categoryMap.get(request.getId());
            if (category != null && category.getTenant().getId().equals(tenantId)) {
                category.setDisplayOrder(request.getDisplayOrder());
                category.setUpdatedAt(LocalDateTime.now());
            }
        }

        // Guardar todos los cambios
        categoryRepository.saveAll(categories);
    }

    @Override
    @Transactional
    public TenantMenuCategory findOrCreateByNameNormalized(Long tenantId, String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de categoría no puede estar vacío");
        }

        if (tenantId == null) {
            throw new IllegalArgumentException("El tenantId no puede ser nulo");
        }

        String normalizedInput = TextNormalizer.normalize(categoryName);

        // Primero intentar búsqueda exacta case-insensitive
        Optional<TenantMenuCategory> exactMatch = categoryRepository
                .findByTenantIdAndNombreIgnoreCase(tenantId, categoryName.trim());

        if (exactMatch.isPresent()) {
            return exactMatch.get();
        }

        // Si no hay coincidencia exacta, buscar por normalización en todas las categorías del tenant
        List<TenantMenuCategory> allCategories = categoryRepository.findByTenantId(tenantId);

        Optional<TenantMenuCategory> normalizedMatch = allCategories.stream()
                .filter(cat -> TextNormalizer.normalize(cat.getNombre()).equals(normalizedInput))
                .findFirst();

        if (normalizedMatch.isPresent()) {
            return normalizedMatch.get();
        }

        // Si no existe, crear nueva categoría
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el tenant con ID: " + tenantId));

        Integer maxDisplayOrder = categoryRepository.findMaxDisplayOrderByTenantId(tenantId);
        Integer newDisplayOrder = (maxDisplayOrder != null ? maxDisplayOrder : 0) + 1;

        TenantMenuCategory newCategory = TenantMenuCategory.builder()
                .tenant(tenant)
                .nombre(categoryName.trim())
                .isActive(true)
                .displayOrder(newDisplayOrder)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return categoryRepository.save(newCategory);
    }
}
