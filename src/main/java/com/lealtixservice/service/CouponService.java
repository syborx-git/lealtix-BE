package com.lealtixservice.service;

import com.lealtixservice.dto.CouponResponseDTO;
import com.lealtixservice.entity.Campaign;
import com.lealtixservice.entity.Coupon;
import com.lealtixservice.entity.TenantCustomer;

import java.util.List;
import java.util.Optional;

public interface CouponService {

    /**
     * Genera un cupón de bienvenida para un cliente en una campaña específica.
     * El cupón incluye código QR y se configura con estado ACTIVE.
     *
     * @param campaign Campaña de bienvenida
     * @param customer Cliente que recibe el cupón
     * @return Cupón generado con código QR
     */
    Coupon generateWelcomeCoupon(Campaign campaign, TenantCustomer customer);

    /**
     * Busca un cupón por su código
     */
    Optional<Coupon> findByCode(String code);

    /**
     * Busca un cupón activo para un cliente en una campaña específica
     */
    Optional<Coupon> findByCustomerAndCampaign(Long customerId, Long campaignId);

    /**
     * Lista cupones de un cliente
     */
    List<Coupon> findByCustomerId(Long customerId);

    /**
     * Canjea un cupón por su código
     */
    Coupon redeemCoupon(String code, String metadata);

    /**
     * Verifica si un cliente ya tiene un cupón activo para una campaña
     */
    boolean hasActiveCouponForCampaign(Long customerId, Long campaignId);

    /**
     * Convierte un Coupon a CouponResponseDTO
     */
    CouponResponseDTO toDTO(Coupon coupon);

    /**
     * Convierte una lista de Coupon a lista de CouponResponseDTO
     */
    List<CouponResponseDTO> toDTOList(List<Coupon> coupons);
}

