package com.lealtixservice.util;

import com.lealtixservice.enums.SegmentationType;
import org.springframework.stereotype.Component;

/**
 * Utilidad para conversión y validación de tipos de segmentación.
 * Facilita la integración con DTOs y requests/responses del API.
 */
@Component
public class SegmentationConverter {

    /**
     * Convierte un string a SegmentationType.
     * Usado en la deserialización de JSON desde el frontend.
     * 
     * @param value Valor string del frontend
     * @return SegmentationType correspondiente, o ALL si es nulo
     */
    public static SegmentationType convertToSegmentationType(String value) {
        if (value == null || value.isBlank()) {
            return SegmentationType.ALL;
        }
        try {
            return SegmentationType.fromValue(value);
        } catch (IllegalArgumentException e) {
            // Log y fallback a ALL si el valor es inválido
            throw new IllegalArgumentException("Tipo de segmentación inválido: " + value, e);
        }
    }

    /**
     * Valida si un valor string es un tipo de segmentación válido.
     * 
     * @param value Valor a validar
     * @return true si es válido, false en caso contrario
     */
    public static boolean isValidSegmentationType(String value) {
        if (value == null || value.isBlank()) {
            return true; // null se considera válido (se convierte a ALL)
        }
        try {
            SegmentationType.fromValue(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Obtiene el nombre mostrable de un tipo de segmentación.
     * Útil para respuestas del API que quieren mostrar el nombre amigable.
     * 
     * @param value Valor string de la segmentación
     * @return Nombre mostrable del tipo
     */
    public static String getDisplayName(String value) {
        SegmentationType type = SegmentationType.fromValue(value);
        return type.getDisplayName();
    }
}
