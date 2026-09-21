package com.lealtixservice.service.impl;

import com.lealtixservice.dto.TenantCustomerDTO;
import com.lealtixservice.entity.Allergy;
import com.lealtixservice.entity.ProductRecipe;
import com.lealtixservice.entity.ProductSubReceta;
import com.lealtixservice.entity.TenantCustomer;
import com.lealtixservice.entity.TenantMenuProduct;
import com.lealtixservice.repository.AllergyRepository;
import com.lealtixservice.repository.InsumoRepository;
import com.lealtixservice.repository.ProductRecipeRepository;
import com.lealtixservice.repository.ProductSubRecetaRepository;
import com.lealtixservice.repository.TenantCustomerRepository;
import com.lealtixservice.service.AllergyService;
import com.lealtixservice.util.TextNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class AllergyServiceImpl implements AllergyService {

    @Autowired
    private AllergyRepository allergyRepository;

    @Autowired
    private TenantCustomerRepository tenantCustomerRepository;

    @Autowired
    private InsumoRepository insumoRepository;

    @Autowired
    private ProductRecipeRepository recipeRepository;

    @Autowired
    private ProductSubRecetaRepository subRecetaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Allergy> findAll() {
        return allergyRepository.findAll();
    }

    @Override
    public List<String> parseNames(TenantCustomerDTO dto) {
        if (dto == null) {
            return new ArrayList<>();
        }
        List<String> raw;
        // Si llega texto libre, prevalecen los términos parseados del texto
        if (dto.getAllergyText() != null && !dto.getAllergyText().isBlank()) {
            raw = splitAllergyText(dto.getAllergyText());
        } else if (dto.getAllergies() != null) {
            raw = new ArrayList<>(dto.getAllergies());
        } else {
            raw = new ArrayList<>();
        }
        if (raw.isEmpty()) {
            return new ArrayList<>();
        }
        // Normalizar + dedupe (mantener orden de aparición)
        Set<String> normalized = new LinkedHashSet<>();
        for (String r : raw) {
            if (r == null) continue;
            String n = TextNormalizer.normalize(r);
            if (!n.isEmpty()) normalized.add(n);
        }
        return new ArrayList<>(normalized);
    }

    private List<String> splitAllergyText(String text) {
        if (text == null || text.isBlank()) {
            return new ArrayList<>();
        }
        String aux = text
                .replaceAll("(?i)\\by\\b", ",")
                .replaceAll("(?i)\\be\\b", ",")
                .replaceAll("[,;]", ",");
        List<String> out = new ArrayList<>();
        for (String s : aux.split(",")) {
            if (s != null && !s.isBlank()) out.add(s.trim());
        }
        return out;
    }

    @Override
    @Transactional
    public List<Allergy> resolveAllergies(List<String> names) {
        Map<Long, Allergy> byId = new LinkedHashMap<>();
        if (names == null || names.isEmpty()) {
            return new ArrayList<>();
        }
        for (String name : names) {
            if (name == null || name.isBlank()) continue;
            String normalized = TextNormalizer.normalize(name);
            if (normalized.isEmpty()) continue;
            Allergy allergy = allergyRepository.findByNameIgnoreCase(normalized).orElseGet(() -> {
                Allergy newAllergy = Allergy.builder()
                        .name(normalized)
                        .createdAt(LocalDateTime.now())
                        .build();
                log.info("Creando nueva alergia: {}", normalized);
                return allergyRepository.save(newAllergy);
            });
            byId.putIfAbsent(allergy.getId(), allergy);
        }
        return new ArrayList<>(byId.values());
    }

    @Override
    @Transactional
    public void syncCustomerAllergies(Long customerId, List<String> names) {
        TenantCustomer customer = tenantCustomerRepository.findById(customerId).orElse(null);
        if (customer == null) {
            log.warn("syncCustomerAllergies: cliente {} no encontrado", customerId);
            return;
        }
        List<Allergy> resolved = resolveAllergies(names);
        customer.setAllergies(new ArrayList<>(resolved));
        customer.setUpdatedAt(LocalDateTime.now());
        tenantCustomerRepository.save(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> checkCustomerAllergies(Long customerId, Long productId,
                                                            List<Long> excludedIds, List<Long> additionalIds) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (customerId == null || productId == null) return result;

        TenantCustomer customer = tenantCustomerRepository.findById(customerId).orElse(null);
        if (customer == null || customer.getAllergies() == null || customer.getAllergies().isEmpty()) return result;

        Set<String> allergies = new HashSet<>();
        for (Allergy a : customer.getAllergies()) {
            String norm = TextNormalizer.normalize(a.getName());
            if (!norm.isEmpty()) allergies.add(norm);
        }
        if (allergies.isEmpty()) return result;

        Set<Long> excluded = excludedIds != null ? new HashSet<>(excludedIds) : new HashSet<>();
        // insumoId -> nombre (LinkedHashMap para preservar orden y dedupe por insumo)
        Map<Long, String> insumos = new LinkedHashMap<>();

        // Receta propia del platillo
        for (ProductRecipe r : recipeRepository.findByDishId(productId)) {
            if (r.getInsumo() != null) {
                insumos.putIfAbsent(r.getInsumo().getId(), r.getInsumo().getNombre());
            }
        }
        // Sub-recetas asignadas
        for (ProductSubReceta s : subRecetaRepository.findByDishId(productId)) {
            TenantMenuProduct sr = s.getSubReceta();
            if (sr == null) continue;
            for (ProductRecipe r : recipeRepository.findByDishId(sr.getId())) {
                if (r.getInsumo() != null) {
                    insumos.putIfAbsent(r.getInsumo().getId(), r.getInsumo().getNombre());
                }
            }
        }
        // Adicionales elegidos por el mesero
        if (additionalIds != null) {
            for (Long addId : additionalIds) {
                if (addId == null) continue;
                insumoRepository.findById(addId).ifPresent(i ->
                        insumos.putIfAbsent(i.getId(), i.getNombre()));
            }
        }

        for (Map.Entry<Long, String> e : insumos.entrySet()) {
            if (excluded.contains(e.getKey())) continue;
            String norm = TextNormalizer.normalize(e.getValue());
            if (norm.isEmpty()) continue;
            if (allergies.contains(norm)) {
                Map<String, Object> m = new HashMap<>();
                m.put("insumoId", e.getKey());
                m.put("insumoName", e.getValue());
                result.add(m);
            }
        }
        return result;
    }
}