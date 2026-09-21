package com.lealtixservice.service;

import com.lealtixservice.dto.TenantCustomerDTO;
import com.lealtixservice.entity.Allergy;

import java.util.List;
import java.util.Map;

public interface AllergyService {

    /** Lista todas las alergias registradas (catálogo global). */
    List<Allergy> findAll();

    /** Parsea las alergias del DTO (texto libre o lista) y las normaliza (minúsculas, sin acentos, singular). */
    List<String> parseNames(TenantCustomerDTO dto);

    /** Resuelve nombres a entidades persistidas: crea las que no existan y devuelve las existentes (dedupe). */
    List<Allergy> resolveAllergies(List<String> names);

    /** Reemplaza las alergias del cliente por las indicadas (crea las nuevas automáticamente). */
    void syncCustomerAllergies(Long customerId, List<String> names);

    /**
     * Verifica si algún insumo del platillo (receta + sub-recetas + adicionales elegidos,
     * menos los excluidos) coincide con las alergias del cliente.
     * Devuelve lista de {insumoId, insumoName}.
     */
    List<Map<String, Object>> checkCustomerAllergies(Long customerId, Long productId,
                                                     List<Long> excludedIds, List<Long> additionalIds);
}