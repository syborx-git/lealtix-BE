package com.lealtixservice.repository;

import com.lealtixservice.entity.Coupon;
import com.lealtixservice.enums.CouponStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    /**
     * Buscar cupón por código (sin optimización, puede causar N+1)
     */
    Optional<Coupon> findByCode(String code);

    /**
     * Buscar cupón por código con EAGER FETCH para validación rápida
     * Incluye: campaign, customer, tenant, promotionReward
     */
    @Query("SELECT c FROM Coupon c " +
           "JOIN FETCH c.campaign camp " +
           "JOIN FETCH c.customer cust " +
           "JOIN FETCH cust.tenant t " +
           "LEFT JOIN FETCH camp.promotionReward pr " +
           "WHERE c.code = :code")
    Optional<Coupon> findByCodeWithRelations(@Param("code") String code);

    /**
     * Buscar cupón por QR token (sin optimización, puede causar N+1)
     */
    Optional<Coupon> findByQrToken(String qrToken);

    /**
     * Buscar cupón por QR token con EAGER FETCH para validación rápida
     * Incluye: campaign, customer, tenant, promotionReward
     */
    @Query("SELECT c FROM Coupon c " +
           "JOIN FETCH c.campaign camp " +
           "JOIN FETCH c.customer cust " +
           "JOIN FETCH cust.tenant t " +
           "LEFT JOIN FETCH camp.promotionReward pr " +
           "WHERE c.qrToken = :qrToken")
    Optional<Coupon> findByQrTokenWithRelations(@Param("qrToken") String qrToken);

    /**
     * Verificar si existe un cupón con el código
     */
    boolean existsByCode(String code);

    /**
     * Verificar si existe un cupón con el QR token
     */
    boolean existsByQrToken(String qrToken);

    /**
     * Listar cupones de un cliente
     */
    List<Coupon> findByCustomerId(Long customerId);

    /**
     * Listar cupones activos de un cliente
     */
    List<Coupon> findByCustomerIdAndStatus(Long customerId, CouponStatus status);

    /**
     * Listar cupones de una campaña
     */
    List<Coupon> findByCampaignId(Long campaignId);

    /**
     * Contar cupones por campaña y estado
     */
    long countByCampaignIdAndStatus(Long campaignId, CouponStatus status);

    /**
     * Cupones expirados que aún están en estado ACTIVE
     */
    @Query("SELECT c FROM Coupon c WHERE c.status = 'ACTIVE' AND c.expiresAt < :now")
    List<Coupon> findExpiredActiveCoupons(@Param("now") LocalDateTime now);

    /**
     * Cupones activos de un cliente para una campaña específica
     */
    @Query("SELECT c FROM Coupon c WHERE c.customer.id = :customerId " +
           "AND c.campaign.id = :campaignId AND c.status = 'ACTIVE'")
    List<Coupon> findActiveByCustomerAndCampaign(
            @Param("customerId") Long customerId,
            @Param("campaignId") Long campaignId
    );

    /**
     * Verificar si un cliente ya tiene cupón activo para una campaña
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
           "FROM Coupon c WHERE c.customer.id = :customerId " +
           "AND c.campaign.id = :campaignId AND c.status = 'ACTIVE'")
    boolean hasActiveCouponForCampaign(
            @Param("customerId") Long customerId,
            @Param("campaignId") Long campaignId
    );
}

