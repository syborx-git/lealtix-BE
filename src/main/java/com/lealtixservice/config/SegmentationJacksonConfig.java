package com.lealtixservice.config;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.lealtixservice.enums.SegmentationType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

/**
 * Configuración de serialización/deserialización JSON para SegmentationType.
 * 
 * Permite que el enum SegmentationType se maneje correctamente en:
 * - JSON requests desde el frontend
 * - JSON responses hacia el frontend
 * 
 * Ejemplo:
 * Frontend envía: { "segmentation": "male" }
 * Backend deserializa a: SegmentationType.MALE
 */
@Configuration
public class SegmentationJacksonConfig {

    /**
     * Deserializador personalizado para SegmentationType.
     * Convierte strings como "male" al enum MALE.
     */
    public static class SegmentationTypeDeserializer extends StdDeserializer<SegmentationType> {
        
        public SegmentationTypeDeserializer() {
            super(SegmentationType.class);
        }

        @Override
        public SegmentationType deserialize(com.fasterxml.jackson.core.JsonParser p, 
                                           com.fasterxml.jackson.databind.DeserializationContext ctxt) throws IOException {
            String value = p.getValueAsString();
            if (value == null || value.isBlank()) {
                return SegmentationType.ALL; // Default
            }
            return SegmentationType.fromValue(value);
        }
    }

    /**
     * Serializador personalizado para SegmentationType.
     * Convierte el enum MALE a string "male" (value) en JSON response.
     */
    public static class SegmentationTypeSerializer extends StdSerializer<SegmentationType> {
        
        public SegmentationTypeSerializer() {
            super(SegmentationType.class);
        }

        @Override
        public void serialize(SegmentationType value, com.fasterxml.jackson.core.JsonGenerator gen, 
                             com.fasterxml.jackson.databind.SerializerProvider provider) throws IOException {
            if (value == null) {
                gen.writeNull();
            } else {
                gen.writeString(value.getValue());
            }
        }
    }

    /**
     * Registra los serializers/deserializers en el ObjectMapper de Spring.
     * Esto permite que @RequestBody y @ResponseBody funcionen automáticamente.
     */
    @Bean
    public SimpleModule segmentationTypeModule() {
        SimpleModule module = new SimpleModule("SegmentationType");
        
        module.addDeserializer(SegmentationType.class, new SegmentationTypeDeserializer());
        module.addSerializer(SegmentationType.class, new SegmentationTypeSerializer());
        
        return module;
    }
}
