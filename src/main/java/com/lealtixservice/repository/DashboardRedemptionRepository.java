package com.lealtixservice.repository;

import com.lealtixservice.entity.CouponRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Extensión del repositorio de CouponRedemption con queries para dashboard y reportes.
 */
@Repository
public interface DashboardRedemptionRepository extends JpaRepository<CouponRedemption, String> {

    /**
     * Resumen de ventas totales por cupones redimidos.
     * Retorna: [0]=totalSales, [1]=avgTicket, [2]=transactionCount
     */
    @Query(value = """
            SELECT COALESCE(SUM(cr.final_amount), 0.0) AS totalSales,
                   COALESCE(AVG(cr.final_amount), 0.0) AS avgTicket,
                   COUNT(cr) AS transactionCount
            FROM coupon_redemption cr
            WHERE cr.tenant_id = :tenantId
              AND cr.redeemed_at BETWEEN :from AND :to
            """, nativeQuery = true)
    List<Object[]> findSalesSummary(
            @Param("tenantId") Long tenantId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /**
     * Estadísticas de cupones creados vs redimidos por campaña.
     * Retorna: [0]=campaignId, [1]=campaignName, [2]=couponsCreated, [3]=couponsRedeemed, [4]=redemptionRatePct
     */
    @Query(value = """
            SELECT c.campaign_id AS campaignId,
                   ca.title AS campaignName,
                   COUNT(DISTINCT c.id) AS couponsCreated,
                   COUNT(DISTINCT cr.id) AS couponsRedeemed,
                   CASE WHEN COUNT(DISTINCT c.id) = 0 THEN 0.0
                        ELSE (CAST(COUNT(DISTINCT cr.id) AS numeric) / COUNT(DISTINCT c.id)) * 100
                   END AS redemptionRatePct
            FROM coupon c
            INNER JOIN campaign ca ON ca.id = c.campaign_id
            LEFT JOIN coupon_redemption cr ON cr.coupon_id = c.id
            WHERE ca.business_id = :tenantId
              AND (c.created_at BETWEEN :from AND :to OR cr.redeemed_at BETWEEN :from AND :to)
            GROUP BY c.campaign_id, ca.title
            ORDER BY couponsRedeemed DESC
            """, nativeQuery = true)
    List<Object[]> findCouponStatsByCampaign(
            @Param("tenantId") Long tenantId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /**
     * Rendimiento completo por campaña (tabla resumen dashboard).
     * Retorna: [0]=campaignId, [1]=campaignName, [2]=couponsIssued, [3]=redemptions,
     *          [4]=totalSales, [5]=avgTicket, [6]=redemptionRatePct
     */
    @Query(value = """
            SELECT ca.id AS campaignId,
                   ca.title AS campaignName,
                   COUNT(DISTINCT c.id) AS couponsIssued,
                   COUNT(cr.id) AS redemptions,
                   COALESCE(SUM(cr.final_amount), 0.0) AS totalSales,
                   COALESCE(AVG(cr.final_amount), 0.0) AS avgTicket,
                   CASE WHEN COUNT(DISTINCT c.id) = 0 THEN 0.0
                        ELSE (CAST(COUNT(cr.id) AS numeric) / COUNT(DISTINCT c.id)) * 100
                   END AS redemptionRatePct
            FROM campaign ca
            LEFT JOIN coupon c ON c.campaign_id = ca.id
            LEFT JOIN coupon_redemption cr ON cr.coupon_id = c.id
            WHERE ca.business_id = :tenantId
              AND (cr.redeemed_at BETWEEN :from AND :to OR ca.created_at BETWEEN :from AND :to)
            GROUP BY ca.id, ca.title
            ORDER BY totalSales DESC
            """, nativeQuery = true)
    List<Object[]> findCampaignPerformance(
            @Param("tenantId") Long tenantId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /**
     * Total de ventas generadas por cupones (simple).
     */
    @Query("SELECT COALESCE(SUM(cr.finalAmount), 0) FROM CouponRedemption cr " +
           "WHERE cr.tenantId = :tenantId " +
           "AND cr.redeemedAt BETWEEN :from AND :to")
    BigDecimal getTotalSalesByCoupons(
            @Param("tenantId") Long tenantId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /**
     * Ticket promedio de cupones redimidos.
     */
    @Query("SELECT COALESCE(AVG(cr.finalAmount), 0) FROM CouponRedemption cr " +
           "WHERE cr.tenantId = :tenantId " +
           "AND cr.redeemedAt BETWEEN :from AND :to")
    BigDecimal getAvgTicketByCoupons(
            @Param("tenantId") Long tenantId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /**
     * ROI por campaña usando costo implícito de descuento.
     * Retorna: [0]=campaignId, [1]=campaignName, [2]=redemptions, [3]=revenue, [4]=discountCost
     */
    @Query(value = """
            SELECT ca.id AS campaignId,
                   ca.title AS campaignName,
                   COUNT(cr.id) AS redemptions,
                   COALESCE(SUM(cr.final_amount), 0.0) AS revenue,
                   COALESCE(SUM(COALESCE(cr.discount_amount, 0.0)), 0.0) AS discountCost
            FROM campaign ca
            LEFT JOIN coupon_redemption cr
                   ON cr.campaign_id = ca.id
                  AND cr.redeemed_at BETWEEN :from AND :to
            WHERE ca.business_id = :tenantId
            GROUP BY ca.id, ca.title
            ORDER BY revenue DESC
            """, nativeQuery = true)
    List<Object[]> findCampaignRoiByDiscountCost(
            @Param("tenantId") Long tenantId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}