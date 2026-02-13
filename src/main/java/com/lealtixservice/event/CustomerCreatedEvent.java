package com.lealtixservice.event;

import com.lealtixservice.entity.Coupon;
import com.lealtixservice.entity.Tenant;
import com.lealtixservice.entity.TenantCustomer;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Evento de dominio que se publica cuando un nuevo cliente es creado exitosamente.
 * Contiene toda la información necesaria para enviar el email de bienvenida post-commit.
 */
@Getter
@Builder
@ToString
public class CustomerCreatedEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Cliente que fue creado
     */
    private final TenantCustomer customer;
    
    /**
     * Tenant al que pertenece el cliente
     */
    private final Tenant tenant;
    
    /**
     * Cupón de bienvenida generado (puede ser null si no hay campaña activa)
     */
    private final Coupon welcomeCoupon;
    
    /**
     * URL base para invitaciones (configurada en properties)
     */
    private final String invitationBaseUrl;
    
    /**
     * URL del dashboard (configurada en properties)
     */
    private final String dashboardUrl;
    
    /**
     * Template ID para email con cupón
     */
    private final String welcomeTemplateId;
    
    /**
     * Template ID para email sin cupón
     */
    private final String welcomeNoCouponTemplateId;
}
