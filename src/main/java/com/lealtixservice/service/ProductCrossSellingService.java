package com.lealtixservice.service;

import com.lealtixservice.dto.CrossSellingDTO;
import com.lealtixservice.dto.ProductCrossSellingRequest;
import com.lealtixservice.dto.ProductCrossSellingResponse;
import java.util.List;

public interface ProductCrossSellingService {
    
    /**
     * Obtiene las sugerencias de productos complementarios para un producto específico.
     * Usado por el frontend en la comanda o chatbot.
     * 
     * @param productId ID del producto principal
     * @param tenantId ID del tenant para garantizar aislamiento SaaS
     * @return Lista de productos sugeridos
     */
    List<CrossSellingDTO> getSuggestionsByProduct(Long productId, Long tenantId);
    
    /**
     * Crea una nueva configuración de cross-selling.
     * 
     * @param request Datos de la configuración
     * @return Configuración creada
     */
    ProductCrossSellingResponse createCrossSelling(ProductCrossSellingRequest request);
    
    /**
     * Obtiene todas las configuraciones de cross-selling de un tenant.
     * 
     * @param tenantId ID del tenant
     * @return Lista de configuraciones
     */
    List<ProductCrossSellingResponse> getAllByTenant(Long tenantId);
    
    /**
     * Obtiene todas las configuraciones de un producto específico.
     * 
     * @param productId ID del producto
     * @param tenantId ID del tenant
     * @return Lista de configuraciones
     */
    List<ProductCrossSellingResponse> getByProduct(Long productId, Long tenantId);
    
    /**
     * Actualiza una configuración existente.
     * 
     * @param id ID de la configuración
     * @param request Nuevos datos
     * @return Configuración actualizada
     */
    ProductCrossSellingResponse updateCrossSelling(Long id, ProductCrossSellingRequest request);
    
    /**
     * Elimina una configuración de cross-selling.
     * 
     * @param id ID de la configuración
     * @param tenantId ID del tenant (seguridad)
     */
    void deleteCrossSelling(Long id, Long tenantId);
    
    /**
     * Activa o desactiva una configuración.
     * 
     * @param id ID de la configuración
     * @param tenantId ID del tenant (seguridad)
     * @param isActive Nuevo estado
     * @return Configuración actualizada
     */
    ProductCrossSellingResponse toggleActive(Long id, Long tenantId, Boolean isActive);
}
