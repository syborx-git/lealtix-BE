package com.lealtixservice.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para cliente VIP del dashboard mesero.
 * Contiene información del cliente con mayor LTV (Life Time Value).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VipClientDTO {
    
    /**
     * ID del cliente
     */
    private Long id;
    
    /**
     * Nombre del cliente
     */
    private String name;
    
    /**
     * Lifetime Value - valor total generado por el cliente
     */
    private BigDecimal ltv;
    
    /**
     * Fecha de la última visita
     */
    private LocalDateTime lastVisitDate;
    
    /**
     * Teléfono de contacto
     */
    private String phone;
    
    /**
     * Email del cliente
     */
    private String email;
    
    /**
     * Cantidad total de visitas
     */
    private Long visitCount;
    
    /**
     * Ticket promedio del cliente
     */
    private BigDecimal averageTicket;
}
