package com.lealtixservice.dto;

import com.lealtixservice.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para registrar pago de una orden
 * Solo registra el método y referencia, sin procesar transacciones en línea
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

    // Validaciones:
    // - Si method es CASH: reference es opcional
    // - Si method es CARD, TRANSFER, MIXED: reference es obligatorio
    // - userEmail es obligatorio para auditoría
}
