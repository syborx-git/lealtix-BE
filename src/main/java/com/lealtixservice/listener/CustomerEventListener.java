package com.lealtixservice.listener;

import com.lealtixservice.dto.EmailAttachmentDTO;
import com.lealtixservice.dto.EmailDTO;
import com.lealtixservice.entity.Coupon;
import com.lealtixservice.entity.Tenant;
import com.lealtixservice.entity.TenantCustomer;
import com.lealtixservice.event.CustomerCreatedEvent;
import com.lealtixservice.service.Emailservice;
import com.lealtixservice.service.QrCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Listener de eventos de dominio relacionados con clientes.
 * Maneja el envío de emails de forma asíncrona y post-commit para evitar:
 * - Bloqueo de transacciones de BD durante llamadas externas
 * - Envío de emails si la transacción falla y hace rollback
 */
@Component
@Slf4j
public class CustomerEventListener {

    @Autowired
    private Emailservice emailService;

    @Autowired
    private QrCodeService qrCodeService;

    /**
     * Maneja el evento de creación de cliente.
     * Se ejecuta DESPUÉS de que la transacción se haya confirmado exitosamente.
     * El envío es asíncrono para no bloquear el hilo principal.
     *
     * @param event evento con datos del cliente creado
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCustomerCreated(CustomerCreatedEvent event) {
        log.info("Processing CustomerCreatedEvent for customer: {}", event.getCustomer().getEmail());
        
        try {
            sendWelcomeEmail(event);
        } catch (Exception e) {
            log.error("Error processing CustomerCreatedEvent for customer {}: {}",
                    event.getCustomer().getEmail(), e.getMessage(), e);
            // No propagamos la excepción porque ya estamos post-commit
            // El cliente ya está guardado, solo logueamos el error del email
        }
    }

    /**
     * Envía el email de bienvenida al cliente.
     * Reutiliza la lógica original extraída del método save().
     *
     * @param event evento con toda la información necesaria
     */
    private void sendWelcomeEmail(CustomerCreatedEvent event) {
        TenantCustomer customer = event.getCustomer();
        Tenant tenant = event.getTenant();
        Coupon welcomeCoupon = event.getWelcomeCoupon();

        try {
            Map<String, Object> dynamicData = new HashMap<>();
            
            // Datos del tenant
            if (tenant != null) {
                dynamicData.put("tenantName", tenant.getNombreNegocio());
                dynamicData.put("logoUrl", tenant.getLogoUrl());
            } else {
                dynamicData.put("tenantName", "");
                dynamicData.put("logoUrl", "");
            }

            dynamicData.put("customerName", customer.getName());

            // Preparar attachments y template según si hay cupón o no
            List<EmailAttachmentDTO> attachments = new ArrayList<>();
            String templateToUse;

            if (welcomeCoupon != null) {
                // Usar template con cupón
                templateToUse = event.getWelcomeTemplateId();
                dynamicData.put("discount", welcomeCoupon.getCampaign().getPromotionReward().getDescription());
                dynamicData.put("couponCode", welcomeCoupon.getCode());
                dynamicData.put("promoImageUrl", welcomeCoupon.getCampaign().getImageUrl());

                // Generar QR code para el cupón
                try {
                    String redeemUrl = event.getDashboardUrl() + "/redeem?code=" + welcomeCoupon.getQrToken();
                    String qrBase64 = qrCodeService.generateQrCodeBase64(redeemUrl);

                    // Crear attachment inline para el QR
                    EmailAttachmentDTO qrAttachment = EmailAttachmentDTO.builder()
                            .content(qrBase64)
                            .type("image/png")
                            .filename("coupon-qr.png")
                            .disposition("inline")
                            .contentId("couponQr")
                            .build();

                    attachments.add(qrAttachment);
                    dynamicData.put("hasQr", true);
                    log.info("QR code generado y adjuntado para cupón {}", welcomeCoupon.getCode());

                } catch (IOException qrEx) {
                    log.error("Error generando QR code para cupón {}: {}",
                            welcomeCoupon.getCode(), qrEx.getMessage());
                    dynamicData.put("hasQr", false);
                }
            } else {
                // Usar template sin cupón (solo bienvenida simple)
                templateToUse = event.getWelcomeNoCouponTemplateId();
                log.info("No hay cupón de bienvenida, usando template sin cupón para customer {}", 
                        customer.getId());
            }

            // Construir landing URL
            String slug = tenant != null ? tenant.getSlug() : "";
            String baseUrl = event.getInvitationBaseUrl() != null && !event.getInvitationBaseUrl().trim().isEmpty()
                    ? event.getInvitationBaseUrl().replaceAll("/+$", "")
                    : "https://lealtix.com.mx";
            String landingUrl = baseUrl + "/landing-page/" + slug;
            dynamicData.put("landingUrl", landingUrl);

            // Construir y enviar email
            EmailDTO emailDTO = EmailDTO.builder()
                    .to(customer.getEmail())
                    .subject("Bienvenido a " + (tenant != null ? tenant.getNombreNegocio() : "nuestro servicio"))
                    .templateId(templateToUse)
                    .dynamicData(dynamicData)
                    .attachments(attachments.isEmpty() ? null : attachments)
                    .build();

            log.info("Intentando enviar email de bienvenida a: {}, template: {}, tenant: {}",
                    customer.getEmail(), templateToUse, tenant != null ? tenant.getNombreNegocio() : "null");
            log.debug("EmailDTO construido - to: {}, subject: {}, templateId: {}, hasAttachments: {}",
                    emailDTO.getTo(), emailDTO.getSubject(), emailDTO.getTemplateId(),
                    emailDTO.getAttachments() != null && !emailDTO.getAttachments().isEmpty());
            log.debug("DynamicData keys: {}", dynamicData.keySet());

            emailService.sendEmailWithTemplate(emailDTO);
            log.info("✅ Email de bienvenida enviado exitosamente a: {}", customer.getEmail());
            
        } catch (IOException ex) {
            log.error("Error sending welcome email to customer: {}", customer.getEmail(), ex);
            throw new RuntimeException("Failed to send welcome email", ex);
        } catch (Exception ex) {
            log.error("Unexpected error sending welcome email", ex);
            throw ex;
        }
    }
}
