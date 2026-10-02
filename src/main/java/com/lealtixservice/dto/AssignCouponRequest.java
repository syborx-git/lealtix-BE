package com.lealtixservice.dto;

import com.lealtixservice.enums.RewardType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para la solicitud de asignación de cupones a clientes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignCouponRequest {

    @NotNull(message = "El customerId es obligatorio")
    private Long customerId;

    // Si se pasa campaignId se usa una campaña existente del negocio
    private Long campaignId;

    // O bien, si se crea un cupón personalizado directo
    private String title;
    private RewardType rewardType;
    private BigDecimal discountValue;
    private String description;
    private Integer daysValid;
}
