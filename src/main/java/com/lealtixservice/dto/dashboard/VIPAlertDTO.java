package com.lealtixservice.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para información de cliente VIP activo en el dashboard de cocina.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VIPAlertDTO {
    private Boolean active;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private BigDecimal lifetimeValue;
    private String note;
}
