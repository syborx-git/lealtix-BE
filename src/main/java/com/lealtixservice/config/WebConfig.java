package com.lealtixservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 * Configuración web para personalizar el manejo de parámetros HTTP.
 * Incluye soporte mejorado para parsing de fechas ISO 8601 con zona horaria.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        // Registrar un conversor personalizado que acepta múltiples formatos ISO 8601
        registry.addConverter(new StringToLocalDateTimeConverter());
    }

    /**
     * Conversor personalizado para parsear fechas en múltiples formatos ISO 8601
     * y convertirlas a LocalDateTime.
     * 
     * Formatos soportados:
     * - 2026-01-01T06:00:00
     * - 2026-01-01T06:00:00.000
     * - 2026-01-01T06:00:00Z
     * - 2026-01-01T06:00:00.000Z
     * - 2026-01-01T06:00:00+00:00
     * - 2026-01-01T06:00:00-05:00
     */
    public static class StringToLocalDateTimeConverter implements Converter<String, LocalDateTime> {
        
        private static final DateTimeFormatter FLEXIBLE_FORMATTER = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .append(DateTimeFormatter.ISO_LOCAL_DATE)
                .appendLiteral('T')
                .append(DateTimeFormatter.ISO_LOCAL_TIME)
                .optionalStart()
                .appendOffsetId()  // Acepta Z, +00:00, -05:00, etc.
                .optionalEnd()
                .parseDefaulting(ChronoField.OFFSET_SECONDS, 0)  // Default a UTC si no se especifica
                .toFormatter();

        @Override
        public LocalDateTime convert(@NonNull String source) {
            // Intentar parsear con zona horaria primero
            if (source.contains("Z") || source.contains("+") || source.matches(".*-\\d{2}:\\d{2}$")) {
                try {
                    ZonedDateTime zonedDateTime = ZonedDateTime.parse(source, FLEXIBLE_FORMATTER);
                    return zonedDateTime.toLocalDateTime();
                } catch (Exception e) {
                    // Si falla, intentar como LocalDateTime directo
                }
            }
            
            // Parsear como LocalDateTime sin zona horaria
            return LocalDateTime.parse(source, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }
}
