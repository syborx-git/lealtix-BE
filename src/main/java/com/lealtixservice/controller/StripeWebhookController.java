package com.lealtixservice.controller;

import com.lealtixservice.dto.PagoDto;
import com.lealtixservice.service.StripeWebhookService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Tag(name = "Stripe Webhook", description = "Recibe y procesa eventos enviados por Stripe")
@RestController
@RequestMapping("/api/stripe")
public class StripeWebhookController {

    @Value("${stripe.webhook.secret:}")
    private String endpointSecret;

    @Autowired
    private StripeWebhookService stripeWebhookService;

    @Operation(summary = "Recibe eventos webhook de Stripe", description = "Verifica la firma y procesa eventos principales")
    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> handleStripeWebhook(
            HttpServletRequest request,
            @RequestHeader(name = "Stripe-Signature", required = false) String sigHeader) {

        String payload;
        boolean resp = true;
        try {
            payload = getRawBody(request);
        } catch (IOException e) {
            log.error("❌ Error al leer el cuerpo de la petición: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error leyendo payload");
        }

        if (sigHeader == null || sigHeader.isBlank()) {
            log.warn("⚠️ Falta Stripe-Signature header");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Falta Stripe-Signature");
        }

        if (endpointSecret == null || endpointSecret.isBlank()) {
            log.error("❌ stripe.webhook.secret no está configurado");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook secret no configurado");
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            log.error("❌ Firma inválida: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Firma inválida");
        } catch (Exception e) {
            log.error("❌ Error construyendo el evento de Stripe: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payload inválido");
        }

        log.info("✅ Evento Stripe validado correctamente: type={} id={}", event.getType(), event.getId());
        PagoDto pagoDto;
        try {
            switch (event.getType()) {
                case "checkout.session.completed":
                    pagoDto = stripeWebhookService.handleCheckoutSessionCompleted(event);
                    break;
                case "payment_intent.succeeded":
                    stripeWebhookService.handlePaymentIntentSucceeded(event);
                    break;
                case "payment_intent.payment_failed":
                    stripeWebhookService.handlePaymentIntentFailed(event);
                    resp = false;
                    break;
                case "charge.failed":
                    stripeWebhookService.handleChargeFailed(event);
                    resp = false;
                    break;
                default:
                    log.info("ℹ️ Evento no manejado: {}", event.getType());
                    break;
            }
        } catch (RuntimeException e) {
            log.error("❌ Error procesando webhook de Stripe (type={}): {}", event.getType(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error procesando webhook: " + e.getMessage());
        }

        if(resp){
            return ResponseEntity.ok("Evento procesado correctamente");
        }else{
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error procesando el evento");
        }


    }

    private String getRawBody(HttpServletRequest request) throws IOException {
        if (request instanceof ContentCachingRequestWrapper wrapper) {
            byte[] cached = wrapper.getContentAsByteArray();
            if (cached != null && cached.length > 0) {
                return new String(cached, StandardCharsets.UTF_8);
            }
        }
        try (InputStream is = request.getInputStream()) {
            byte[] bytes = is.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }
}
