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

    private static final List<String> BEVERAGE_CATEGORY_KEYWORDS = List.of(
            "cerveza", "cervezas", "beer", "beers",
            "bebida", "bebidas", "beverage", "beverages", "drink", "drinks",
            "barra", "bar",
            "coctel", "cocteles", "cocteleria", "cocktail", "cocktails", "mixologia",
            "destilado", "destilados", "botella", "botellas",
            "licor", "licores", "liquor",
            "vino", "vinos", "wine", "wines",
            "trago", "tragos", "shot", "shots", "chupito", "chupitos",
            "cafe", "cafes", "coffee", "cafeteria",
            "infusion", "infusiones", "te", "tes", "tea",
            "refresco", "refrescos", "soda", "sodas",
            "agua", "aguas", "water",
            "jugo", "jugos", "juice",
            "smoothie", "smoothies", "malteada", "malteadas", "frappe", "frappes",
            "cava", "cavas", "cantina", "cantinas", "copeo",
            "mixers", "mixer", "digestivo", "digestivos", "aperitivo", "aperitivos",
            "sin alcohol", "con alcohol", "bebestibles"
    );

    private static final List<String> BEVERAGE_NAME_KEYWORDS = List.of(
            // Cervezas y preparados
            "cerveza", "heineken", "corona", "cerveza victoria", "victoria de barril", "modelo especial",
            "cerveza modelo", "negra modelo", "cerveza bohemia", "stella artois",
            "michelob", "michelob ultra", "amstel ultra", "pacifico", "cerveza indio", "tecate", "carta blanca",
            "dos equis", "xx", "cerveza leon", "montejo", "caguama", "caguamon",
            "michelada", "chelada", "ojo rojo", "cubana", "clamato preparado", "clamato",
            // Cocteles clásicos y populares
            "mojito", "margarita", "carajillo", "piña colada", "pina colada", "azulito",
            "ruso negro", "ruso blanco", "alma negra", "paloma", "charro negro", "vampiro", "cantarito",
            "piñada", "pinada", "conga", "sangria", "clericot", "aperol", "spritz",
            "gin tonic", "gin & tonic", "martini", "cosmopolitan", "daikiri", "daiquiri",
            "submarino", "perla negra", "baby mango", "cucaracha", "torito",
            // Destilados y marcas habituales
            "tequila", "whisky", "whiskey", "vodka", "mezcal", "brandy", "cognac", "ginebra", "gin",
            "don julio", "herradura", "maestro dobel", "dobel", "gran centenario", "tequila centenario",
            "7 leguas", "siete leguas", "jose cuervo", "cuervo tradicional", "cuervo especial", "1800",
            "tequila cazadores", "cazadores reposado", "tequila espolon",
            "buchanans", "buchanan", "red label", "black label", "chivas", "jack daniel", "jim beam",
            "macallan", "glenfiddich", "j&b", "ballantines", "johnnie walker",
            "bacardi", "havana club", "havana", "captain morgan", "matusalem", "appleton", "zacapa", "flor de caña", "flor de cana",
            "absolut", "smirnoff", "grey goose", "belvedere", "stolichnaya",
            "400 conejos", "ojo de tigre", "alipus", "montelobos", "mezcal amores", "creyente",
            "torres 10", "torres 5", "fundador", "don pedro", "terry", "hennessy", "martell",
            "baileys", "kahlua", "frangelico", "jagermeister", "licor 43", "anis dulce", "anis seco", "campari",
            "amaretto", "sambuca", "midori", "hpnotiq",
            // Vinos
            "cabernet", "merlot", "malbec", "chardonnay", "sauvignon", "tempranillo",
            "syrah", "pinot", "rioja", "ribera", "lambrusco", "prosecco", "champagne", "champana", "champaña",
            // Cafetería
            "latte", "capuchino", "cappuccino", "espresso", "expreso", "americano", "moka", "mocha",
            "frappe", "matcha latte", "chai latte",
            // Refrescos, aguas y energizantes
            "coca-cola", "coca cola", "sprite", "fanta", "refresco", "perrier", "pellegrino",
            "peñafiel", "penafiel", "ciel", "agua embotellada", "agua mineral", "agua natural",
            "agua fresca", "limonada", "naranjada", "horchata", "jamaica", "tamarindo",
            "topo chico", "tehuacan", "squirt", "mundet", "sidral", "delaware", "boing", "jarrito",
            "red bull", "monster", "boost", "electrolit",
            // Ron
            "ron"
    );

    private static java.util.regex.Pattern buildWordOrPhrasePattern(String phrase) {
        String cleanP = cleanText(phrase);
        String patternStr = "(^|[^a-z0-9])" + java.util.regex.Pattern.quote(cleanP) + "($|[^a-z0-9])";
        return java.util.regex.Pattern.compile(patternStr, java.util.regex.Pattern.CASE_INSENSITIVE);
    }

    private static final List<java.util.regex.Pattern> BEVERAGE_CATEGORY_PATTERNS = BEVERAGE_CATEGORY_KEYWORDS.stream()
            .map(BeverageClassificationUtil::buildWordOrPhrasePattern)
            .toList();

    private static final List<java.util.regex.Pattern> BEVERAGE_NAME_PATTERNS = BEVERAGE_NAME_KEYWORDS.stream()
            .map(BeverageClassificationUtil::buildWordOrPhrasePattern)
            .toList();

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
        for (java.util.regex.Pattern pattern : BEVERAGE_CATEGORY_PATTERNS) {
            if (pattern.matcher(clean).find()) {
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
        for (java.util.regex.Pattern pattern : BEVERAGE_NAME_PATTERNS) {
            if (pattern.matcher(clean).find()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBeverageProduct(TenantMenuProduct product) {
        if (product == null) {
            return false;
        }
        // 0. Si el producto define explícitamente su área de distribución/preparación, manda esta asignación
        if (product.getArea() != null && !product.getArea().trim().isEmpty()) {
            return "BARRA".equalsIgnoreCase(product.getArea().trim());
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
                Map<Long, TenantMenuProduct> productById = new HashMap<>();
                for (TenantMenuProduct p : products) {
                    if (p.getId() != null) {
                        productById.put(p.getId(), p);
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
                                TenantMenuProduct linked = productById.get(ins.getProductoId());
                                if (linked == null || !"COCINA".equalsIgnoreCase(linked.getArea())) {
                                    ids.add(ins.getProductoId());
                                }
                            }
                            if (ins.getNombre() != null) {
                                String cleanInsName = cleanText(ins.getNombre());
                                Long pid = productByName.get(cleanInsName);
                                if (pid != null) {
                                    TenantMenuProduct linked = productById.get(pid);
                                    if (linked == null || !"COCINA".equalsIgnoreCase(linked.getArea())) {
                                        ids.add(pid);
                                    }
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
