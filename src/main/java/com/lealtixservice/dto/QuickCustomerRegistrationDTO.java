package com.lealtixservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO para registro rápido de cliente desde el ChatBot.
 * Incluye todos los campos requeridos: nombre, email, teléfono opcional, género y fecha de cumpleaños.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuickCustomerRegistrationDTO {

    @NotNull(message = "tenantId es requerido")
    private Long tenantId;

    @NotBlank(message = "name es requerido")
    private String name;

    @NotBlank(message = "email es requerido")
    @Email(message = "email debe ser válido")
    private String email;

    private String phone;  // Opcional

    private String gender;  // Opcional pero recomendado

    private LocalDate birthDate;  // Fecha de cumpleaños (opcional)

    @Builder.Default
    private Boolean acceptedPromotions = true;
}
