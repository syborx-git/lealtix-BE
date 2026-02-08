package com.lealtixservice.dto;

import lombok.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Alternate DTO (enum-based) for clients that use SegmentationType directly.
 * Filename ends with DTO, class named CreateCampaignDTO to avoid collision with CreateCampaignDto.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCampaignDTO {
    private Long templateId;

    @NotNull(message = "Business ID es requerido")
    private Long businessId;

    private String title;
    private String subtitle;
    private String description;
    private String imageUrl;

    private LocalDate startDate;
    private LocalDate endDate;

    private String callToAction;

    private List<String> channels; // lista de canales

    private List<String> segmentation; // lista de valores de segmentación (strings)

    private Boolean isAutomatic;

    /**
     * Retorna una lista no-nula de segmentation. Si no viene, devuelve lista con "all".
     */
    public List<String> getSegmentationOrDefault() {
        if (segmentation == null || segmentation.isEmpty()) {
            return Collections.singletonList("all");
        }
        // Trim entries and remove blanks
        List<String> cleaned = new ArrayList<>();
        for (String s : segmentation) {
            if (s != null) {
                String t = s.trim();
                if (!t.isEmpty()) cleaned.add(t);
            }
        }
        return cleaned.isEmpty() ? Collections.singletonList("all") : cleaned;
    }

    /**
     * Retorna una lista no-nula de canales. Si no viene, devuelve lista vacía.
     */
    public List<String> getChannelsOrEmpty() {
        if (channels == null || channels.isEmpty()) return Collections.emptyList();
        List<String> cleaned = new ArrayList<>();
        for (String s : channels) {
            if (s != null) {
                String t = s.trim();
                if (!t.isEmpty()) cleaned.add(t);
            }
        }
        return cleaned;
    }

    /**
     * Normaliza segmentation y channels in-place (útil antes de pasar al mapper/service).
     */
    public void normalize() {
        this.segmentation = getSegmentationOrDefault();
        this.channels = getChannelsOrEmpty();
        if (this.isAutomatic == null) this.isAutomatic = Boolean.FALSE;
    }
}