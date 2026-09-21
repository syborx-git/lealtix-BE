package com.lealtixservice.dto;

import jakarta.validation.constraints.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombre;

    @NotBlank(message = "El email es requerido")
    @Email(message = "El email debe ser válido")
    private String email;

    @NotBlank(message = "La contraseña es requerida")
    @Size(min = 6, message = "La contraseña debe tener mínimo 6 caracteres")
    private String contrasena;

    @NotBlank(message = "El rol es requerido")
    private String rol;

    @NotNull(message = "El tenantId es requerido")
    private Long tenantId;

    @NotNull(message = "El sueldo mensual es requerido")
    @DecimalMin(value = "0.0", message = "El sueldo mensual no puede ser negativo")
    private Double sueldoMensual;
}
