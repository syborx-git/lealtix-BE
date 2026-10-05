package com.lealtixservice.dto;

import com.lealtixservice.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para registrar pago de una orden
 * Solo registra el método, la propina y la referencia, sin procesar transacciones en línea
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordPaymentRequest {

    @NotNull(message = "Método de pago es requerido")
    private PaymentMethod method;  // CASH, CARD, TRANSFER, MIXED

    private String reference;  // Referencia del comprobante (opcional para CASH, obligatoria para otros)

    @NotNull(message = "Email del usuario que registra el pago es requerido")
    private String userEmail;  // Email del usuario (mesero/cajero) que registra el pago

    /**
     * Propina opcional. Es informacion adicional que se cobra encima de la cuenta,
     * por lo que NO forma parte del total de la comanda: el reporte de corte la
     * reporta aparte para no alterar los ingresos ni el ticket promedio.
     */
    @DecimalMin(value = "0.0", message = "La propina no puede ser negativa")
    private BigDecimal propina;

    /**
     * Monto total de la cuenta a cobrar (opcional, si se ajusta en caja).
     */
    @DecimalMin(value = "0.0", message = "El monto de la cuenta no puede ser negativo")
    private BigDecimal monto;

    /**
     * Código de cupón opcional a aplicar durante el cobro/cierre de la comanda
     */
    private String couponCode;

    // Validaciones:
    // - Si method es CASH: reference es opcional
    // - Si method es CARD, TRANSFER, MIXED: reference es obligatorio
    // - userEmail es obligatorio para auditoría
    // - propina es opcional, pero si viene debe ser >= 0
    // - couponCode es opcional
}
