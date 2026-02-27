package com.lealtixservice.dto;

import com.lealtixservice.enums.RewardType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta tras redimir un cupón desde el ChatBot.
 * Contiene toda la información necesaria para que el frontend
 * aplique los descuentos correctamente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatBotCouponRedemptionResponse {

    // Estado de la redención
    private boolean success;
    private String message;

    // Información del cupón
    private String couponCode;
    private Long couponId;
    private String campaignTitle;
    private LocalDateTime redeemedAt;

    // Tipo de descuento
    private RewardType discountType; // PERCENT_DISCOUNT, FIXED_AMOUNT, BUY_X_GET_Y, etc.
    private String discountDescription; // Descripción amigable del descuento

    // Cálculos de descuento
    private BigDecimal originalAmount; // Total original de la orden
    private BigDecimal discountAmount; // Monto del descuento aplicado
    private BigDecimal finalAmount;    // Total final después del descuento
    private BigDecimal discountValue;  // Valor del descuento (% o monto fijo)

    // Para descuentos porcentuales
    private BigDecimal discountPercentage; // Ejemplo: 20 (para 20%)

    // Para descuentos de monto fijo
    private BigDecimal fixedDiscountAmount; // Ejemplo: 100 (para $100 de descuento)

    // Para 2x1 (BUY_X_GET_Y)
    private TwoForOneDetails twoForOneDetails;

    // Productos afectados (útil para resaltar en el frontend)
    private List<AffectedProduct> affectedProducts;

    /**
     * DTO para detalles de 2x1
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TwoForOneDetails {
        private Long productId;
        private String productName;
        private Integer buyQuantity;
        private Integer freeQuantity;
        private BigDecimal unitPrice;
        private BigDecimal totalSavings; // Monto ahorrado
        private String message; // Ejemplo: "¡Compra 2 y lleva 1 gratis!"
    }

    /**
     * DTO para productos afectados por el descuento
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AffectedProduct {
        private Long productId;
        private String productName;
        private Integer quantity;
        private BigDecimal originalPrice;
        private BigDecimal discountedPrice;
        private BigDecimal savings;
    }

    /**
     * Factory method para redención exitosa con descuento porcentual
     */
    public static ChatBotCouponRedemptionResponse successWithPercentage(
            String couponCode, Long couponId, String campaignTitle,
            BigDecimal originalAmount, BigDecimal discountPercentage,
            BigDecimal discountAmount, BigDecimal finalAmount) {

        String description = String.format("Descuento del %.0f%% aplicado", discountPercentage);

        return ChatBotCouponRedemptionResponse.builder()
                .success(true)
                .message("Cupón aplicado exitosamente")
                .couponCode(couponCode)
                .couponId(couponId)
                .campaignTitle(campaignTitle)
                .redeemedAt(LocalDateTime.now())
                .discountType(RewardType.PERCENT_DISCOUNT)
                .discountDescription(description)
                .originalAmount(originalAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .discountValue(discountPercentage)
                .discountPercentage(discountPercentage)
                .build();
    }

    /**
     * Factory method para redención exitosa con descuento de monto fijo
     */
    public static ChatBotCouponRedemptionResponse successWithFixedAmount(
            String couponCode, Long couponId, String campaignTitle,
            BigDecimal originalAmount, BigDecimal fixedAmount,
            BigDecimal discountAmount, BigDecimal finalAmount) {

        String description = String.format("Descuento de $%.2f aplicado", fixedAmount);

        return ChatBotCouponRedemptionResponse.builder()
                .success(true)
                .message("Cupón aplicado exitosamente")
                .couponCode(couponCode)
                .couponId(couponId)
                .campaignTitle(campaignTitle)
                .redeemedAt(LocalDateTime.now())
                .discountType(RewardType.FIXED_AMOUNT)
                .discountDescription(description)
                .originalAmount(originalAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .discountValue(fixedAmount)
                .fixedDiscountAmount(fixedAmount)
                .build();
    }

    /**
     * Factory method para redención exitosa con 2x1
     */
    public static ChatBotCouponRedemptionResponse successWithTwoForOne(
            String couponCode, Long couponId, String campaignTitle,
            BigDecimal originalAmount, BigDecimal discountAmount, BigDecimal finalAmount,
            TwoForOneDetails twoForOneDetails) {

        return ChatBotCouponRedemptionResponse.builder()
                .success(true)
                .message("Cupón aplicado exitosamente")
                .couponCode(couponCode)
                .couponId(couponId)
                .campaignTitle(campaignTitle)
                .redeemedAt(LocalDateTime.now())
                .discountType(RewardType.BUY_X_GET_Y)
                .discountDescription(twoForOneDetails.getMessage())
                .originalAmount(originalAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .twoForOneDetails(twoForOneDetails)
                .build();
    }

    /**
     * Factory method para redención fallida
     */
    public static ChatBotCouponRedemptionResponse failure(String message) {
        return ChatBotCouponRedemptionResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
