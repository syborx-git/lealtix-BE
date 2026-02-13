package com.lealtixservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantCustomerDTO {
    private Long id;

    @NotNull
    private Long tenantId;

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotBlank
    @Email
    @Size(max = 150)
    private String email;

    @Size(max = 10)
    private String gender;

    private LocalDate birthDate;

    @Size(max = 20)
    private String phone;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Nuevo: consentimiento y fecha
    private Boolean acceptedPromotions;
    private LocalDate acceptedAt;

    private Boolean active;
}
