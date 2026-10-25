package com.lealtixservice.util;

import com.lealtixservice.entity.Insumo;
import com.lealtixservice.entity.TenantMenuCategory;
import com.lealtixservice.entity.TenantMenuProduct;
import com.lealtixservice.repository.InsumoRepository;
import com.lealtixservice.repository.TenantMenuProductRepository;
import lombok.extern.slf4j.Slf4j;

import java.text.Normalizer;
import java.util.*;

/**
 * Utilidad centralizada para clasificar de forma exhaustiva y confiable
 * qué productos e insumos pertenecen al área de BARRA / BEBIDAS vs COCINA.
 */
@Slf4j
public final class BeverageClassificationUtil {

    private BeverageClassificationUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static final Set<String> BEVERAGE_CATEGORY_KEYWORDS = Set.of(
            "cerveza", "cervezas", "beer", "beers",
            "bebida", "bebidas", "beverage", "beverages", "drink", "drinks",
            "barra", "bar",
            "coctel", "cocteles", "cocteleria", "cocktail", "cocktails", "mixologia",
            "destilado", "destilados", "botella", "botellas",
            "licor", "licores", "liquor",
            "vino", "vinos", "wine", "wines",
            "trago", "tragos", "shot", "shots",
            "cafe", "cafes", "cafeteria", "coffee",
            "infusion", "infusiones", "te", "tes", "tea",
            "refresco", "refrescos", "soda", "sodas",
            "agua", "aguas", "water",
            "jugo", "jugos", "juice",
            "smoothie", "smoothies", "malteada", "malteadas"
    );

    private static final List<String> BEVERAGE_NAME_KEYWORDS = List.of(
            "cerveza", "heineken", "corona", "victoria", "modelo", "bohemia", "stella",
            "michelob", "amstel", "ultra", "mojito", "margarita", "carajillo", "piña colada",
            "pina colada", "azulito", "ruso negro", "alma negra", "coca-cola", "coca cola",
            "sprite", "fanta", "refresco", "tequila", "whisky", "vodka", "mezcal", "brandy",
            "perrier", "pellegrino", "peñafiel", "penafiel", "ciel", "agua embotellada",
            "agua mineral", "agua natural", "agua fresca", "limonada", "naranjada", "ron ",
            "ron 7", "ron blanco", "ron añejo"
    );

    public static String cleanText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        String trimmed = text.trim().toLowerCase(Locale.ROOT);
        return Normalizer.normalize(trimmed, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    public static boolean isBeverageCategoryName(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            return false;
        }
        String clean = cleanText(categoryName);
        for (String kw : BEVERAGE_CATEGORY_KEYWORDS) {
            String cleanKw = cleanText(kw);
            if (clean.contains(cleanKw)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBeverageProductName(String productName) {
        if (productName == null || productName.trim().isEmpty()) {
            return false;
        }
        String clean = cleanText(productName);
        for (String kw : BEVERAGE_NAME_KEYWORDS) {
            String cleanKw = cleanText(kw);
            if (clean.contains(cleanKw)) {
                return true;
            }
        }
        if (clean.equals("ron") || clean.endsWith(" ron") || clean.startsWith("ron ")) {
            return true;
        }
        return false;
    }

    public static boolean isBeverageProduct(TenantMenuProduct product) {
        if (product == null) {
            return false;
        }
        // 1. Revisar categoría principal
        if (product.getCategory() != null && isBeverageCategoryName(product.getCategory().getNombre())) {
            return true;
        }
        // 2. Revisar multicategorías
        if (product.getCategories() != null) {
            for (TenantMenuCategory cat : product.getCategories()) {
                if (cat != null && isBeverageCategoryName(cat.getNombre())) {
                    return true;
                }
            }
        }
        // 3. Revisar nombre del producto
        return isBeverageProductName(product.getNombre());
    }

    /**
     * Resuelve el conjunto exhaustivo de IDs de productos que corresponden a Barra / Bebidas para un tenant.
     */
    public static Set<Long> getBeverageProductIds(Long tenantId, TenantMenuProductRepository productRepo, InsumoRepository insumoRepo) {
        if (tenantId == null) {
            return Collections.emptySet();
        }
        Set<Long> ids = new HashSet<>();
        try {
            // 1. Productos del tenant clasificados por su categoría o nombre
            if (productRepo != null) {
                List<TenantMenuProduct> products = productRepo.findAllByTenantId(tenantId);
                Map<String, Long> productByName = new HashMap<>();
                for (TenantMenuProduct p : products) {
                    if (p.getId() != null) {
                        if (isBeverageProduct(p)) {
                            ids.add(p.getId());
                        }
                        if (p.getNombre() != null) {
                            productByName.put(cleanText(p.getNombre()), p.getId());
                        }
                    }
                }

                // 2. Insumos marcados como esBebida (por productoId o por coincidencia de nombre)
                if (insumoRepo != null) {
                    List<Insumo> insumos = insumoRepo.findByTenantIdAndIsActiveTrueOrderByNombreAsc(tenantId);
                    for (Insumo ins : insumos) {
                        if (ins.isEsBebida()) {
                            if (ins.getProductoId() != null) {
                                ids.add(ins.getProductoId());
                            }
                            if (ins.getNombre() != null) {
                                String cleanInsName = cleanText(ins.getNombre());
                                Long pid = productByName.get(cleanInsName);
                                if (pid != null) {
                                    ids.add(pid);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error resolviendo catálogo de bebidas para tenant {}: {}", tenantId, e.getMessage());
        }
        return ids;
    }
}
