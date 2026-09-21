package com.lealtixservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombre;

    @Email(message = "El email debe ser válido")
    private String email;

    @Size(min = 6, message = "La contraseña debe tener mínimo 6 caracteres")
    private String contrasena;

    private String rol;

    @DecimalMin(value = "0.0", message = "El sueldo mensual no puede ser negativo")
    private Double sueldoMensual;
}
