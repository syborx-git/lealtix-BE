package com.lealtixservice.enums;

/**
 * Tipos de segmentación de audiencias para campañas de email.
 * Cada valor corresponde a un criterio de filtrado específico de clientes.
 * 
 * Diseño extensible: permite agregar nuevos tipos fácilmente.
 */
public enum SegmentationType {
    // Descripción: Todos los clientes del tenant que aceptaron promociones
    ALL("all", "Todos los clientes"),
    
    // Descripción: Clientes con gender = 'male'
    MALE("male", "Hombres"),
    
    // Descripción: Clientes con gender = 'female'
    FEMALE("female", "Mujeres"),
    
    // Descripción: Clientes con cumpleaños en los próximos 7 días (mes/día actual)
    UPCOMING_BIRTHDAY_7D("upcoming_birthday_7d", "Próximo cumpleaños (7 días)"),
    
    // Descripción: Clientes activos en los últimos 30 días (por cambios recientes)
    ACTIVE_30D("active_30d", "Activos últimos 30 días"),
    
    // Descripción: Clientes registrados en los últimos 30 días (created_at >= now - 30d)
    NEW_30D("new_30d", "Usuarios nuevos (últimos 30 días)"),
    
    // Descripción: Clientes con LTV (Lifetime Value) alto
    HIGH_LTV("high_ltv", "Alto valor (LTV alto)"),
    
    // Descripción: Clientes sin compras en los últimos 60 días
    NO_PURCHASE_60D("no_purchase_60d", "Sin compras 60 días"),
    
    // Descripción: Clientes con estatus VIP
    VIP("vip", "Clientes VIP");

    private final String value;
    private final String displayName;

    SegmentationType(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Convierte un string a SegmentationType.
     * 
     * @param value Valor string enviado desde el frontend
     * @return SegmentationType correspondiente
     * @throws IllegalArgumentException si el valor no es válido
     */
    public static SegmentationType fromValue(String value) {
        if (value == null) {
            return ALL; // Por defecto, todos los clientes
        }
        
        for (SegmentationType segmentation : values()) {
            if (segmentation.value.equalsIgnoreCase(value)) {
                return segmentation;
            }
        }
        throw new IllegalArgumentException("Tipo de segmentación desconocido: " + value);
    }
}
