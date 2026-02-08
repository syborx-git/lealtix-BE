package com.lealtixservice.service;

import com.lealtixservice.dto.EmailAttachmentDTO;
import com.lealtixservice.dto.EmailDTO;
import com.lealtixservice.entity.Campaign;
import com.lealtixservice.entity.CampaignEmail;
import com.lealtixservice.entity.CampaignEmailPayload;
import com.lealtixservice.entity.Coupon;
import com.lealtixservice.entity.Tenant;
import com.lealtixservice.entity.TenantCustomer;
import com.lealtixservice.enums.CampaignEmailStatus;
import com.lealtixservice.repository.CampaignEmailPayloadRepository;
import com.lealtixservice.repository.CampaignEmailRepository;
import com.lealtixservice.repository.CampaignRepository;
import com.lealtixservice.repository.TenantCustomerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class CampaignEmailSender {

    @Autowired
    private CampaignEmailRepository campaignEmailRepository;

    @Autowired
    private CampaignEmailPayloadRepository campaignEmailPayloadRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private Emailservice emailService;

    @Autowired
    private TenantCustomerRepository tenantCustomerRepository;

    @Autowired
    private CouponService couponService;

    @Autowired
    private QrCodeService qrCodeService;

    @Autowired
    private EmailLogService emailLogService;

    @Value("${campaign.email.provider.name:sendgrid}")
    private String providerName;

    @Value("${campaign.email.max.attempts:3}")
    private int maxAttempts;

    @Value("${sendgrid.templates.generic-promotion:d-1d7c686b956f4d76a02d49b76a52db9a}")
    private String genericPromotionTemplateId;

    @Value("${lealtix.dashboard.url}")
    private String dashboardUrl;

    @Value("${invitation.base-url}")
    private String invitationBaseUrl;

    /**
     * Método público optimizado que recarga la entidad CampaignEmail con JOIN FETCH
     * de Campaign y PromotionReward dentro de la transacción, evitando LazyInitializationException.
     * 
     * Este es el método recomendado para ser llamado desde el scheduler u otros servicios.
     * 
     * @param campaignEmailId UUID del email a procesar
     * @throws EntityNotFoundException si no existe el CampaignEmail
     */
    @Transactional
    public void sendEmailById(UUID campaignEmailId) {
        log.debug("🔍 [CampaignEmailSender] Recargando CampaignEmail {} con Campaign fetch", campaignEmailId);
        
        CampaignEmail campaignEmail = campaignEmailRepository.findByIdWithCampaignFetch(campaignEmailId)
                .orElseThrow(() -> new EntityNotFoundException(
                    "CampaignEmail no encontrado con ID: " + campaignEmailId));
        
        // Delegar a la lógica de envío con la entidad ya inicializada
        sendEmailInternal(campaignEmail);
    }

    /**
     * Método legacy mantenido para compatibilidad.
     * ADVERTENCIA: Este método puede causar LazyInitializationException si la entidad
     * proviene de un contexto de persistencia diferente (detached).
     * 
     * Se recomienda usar sendEmailById(UUID) en su lugar.
     * 
     * @deprecated Usar {@link #sendEmailById(UUID)} para evitar LazyInitializationException
     */
    @Transactional
    @Deprecated
    public void sendEmail(CampaignEmail campaignEmail) {
        log.warn("⚠️ [CampaignEmailSender] Usando método legacy sendEmail(entity). " +
                "Considere usar sendEmailById(UUID) para evitar LazyInitializationException");
        sendEmailInternal(campaignEmail);
    }

    /**
     * Lógica interna de envío de email. Asume que la entidad CampaignEmail
     * y sus asociaciones lazy ya están inicializadas.
     */
    private void sendEmailInternal(CampaignEmail campaignEmail) {
        log.info("📨 [CampaignEmailSender] Procesando email: {} para campaña: {}",
                campaignEmail.getId(), campaignEmail.getCampaign().getId());

        Campaign campaign = campaignEmail.getCampaign();
        String recipientEmail = campaignEmail.getRecipientEmail();

        try {
            // 1. Buscar TenantCustomer por email y tenant (businessId)
            Optional<TenantCustomer> customerOpt = tenantCustomerRepository
                    .findByEmailAndTenantId(recipientEmail, campaign.getBusinessId());

            if (!customerOpt.isPresent()) {
                log.warn("❌ [CampaignEmailSender] No se encontró TenantCustomer para email {} y tenant {}. Marcando FAILED.", 
                        recipientEmail, campaign.getBusinessId());
                campaignEmail.setStatus(CampaignEmailStatus.FAILED);
                campaignEmail.setProviderErrorMessage("Customer not found for campaign");
                campaignEmail.setLastAttemptAt(LocalDateTime.now());
                campaignEmailRepository.save(campaignEmail);
                return;
            }

            TenantCustomer customer = customerOpt.get();
            Tenant tenant = customer.getTenant();

            // 2. OPTIMIZADO: Usar couponId pre-generado del orquestador (no generar aquí)
            Coupon coupon = null;
            if (campaignEmail.getCouponId() != null) {
                // El cupón ya fue pre-generado en el orquestador
                log.debug("[CampaignEmailSender] Cupón pre-generado con ID {} encontrado en CampaignEmail", 
                        campaignEmail.getCouponId());
                // Nota: no necesitamos cargar el Coupon desde BD si no vamos a usarlo más allá del couponId
            } else {
                log.warn("⚠️ [CampaignEmailSender] CampaignEmail {} no tiene couponId pre-generado. Generando bajo demanda.", 
                        campaignEmail.getId());
                // Fallback: generar cupón si no existe (debería ser raro)
                if (!couponService.hasActiveCouponForCampaign(customer.getId(), campaign.getId())) {
                    coupon = couponService.generateWelcomeCoupon(campaign, customer);
                    log.info("✓ Cupón {} generado bajo demanda para cliente {} en campaña {}", 
                            coupon.getCode(), customer.getEmail(), campaign.getId());
                }
            }

            // 3. Pre-cargar coupon si no existe para acceder al QR token
            if (coupon == null && campaignEmail.getCouponId() != null) {
                // Cargar el cupón por ID
                coupon = couponService.findByCustomerAndCampaign(customer.getId(), campaign.getId())
                        .orElse(null);
            }

            // 4. Generar QR base64 y adjuntos
            List<EmailAttachmentDTO> attachments = new ArrayList<>();
            if (coupon != null) {
                try {
                    String redeemUrl = dashboardUrl + "/redeem?code=" + coupon.getQrToken();
                    String qrBase64 = qrCodeService.generateQrCodeBase64(redeemUrl);
                    
                    EmailAttachmentDTO qrAttachment = EmailAttachmentDTO.builder()
                            .content(qrBase64)
                            .type("image/png")
                            .filename("coupon-qr.png")
                            .disposition("inline")
                            .contentId("couponQr")
                            .build();
                    attachments.add(qrAttachment);
                    log.debug("✓ QR code generado para cupón {}", coupon.getCode());
                } catch (IOException e) {
                    log.error("❌ Error generando QR para cupón: {}", e.getMessage());
                }
            }

            // 5. Preparar dynamicData
            Map<String, Object> dynamicData = new HashMap<>();
            dynamicData.put("tenantName", tenant != null ? tenant.getNombreNegocio() : "");
            dynamicData.put("logoUrl", tenant != null ? tenant.getLogoUrl() : "");
            dynamicData.put("promoImageUrl", campaign.getImageUrl() != null ? campaign.getImageUrl() : "");
            dynamicData.put("promoTitle", campaign.getTitle() != null ? campaign.getTitle() : "Promoción especial");
            dynamicData.put("promoDescription", campaign.getDescription() != null ? campaign.getDescription() : "Queremos consentirte con una promoción exclusiva.");
            
            String promoBenefit = "";
            if (campaign.getPromotionReward() != null && campaign.getPromotionReward().getDescription() != null) {
                promoBenefit = campaign.getPromotionReward().getDescription();
            }
            dynamicData.put("promoBenefit", promoBenefit);
            
            if (coupon != null) {
                dynamicData.put("couponCode", coupon.getCode());
            } else {
                dynamicData.put("couponCode", "");
            }
            
            String baseUrl = (invitationBaseUrl != null && !invitationBaseUrl.trim().isEmpty()) ? 
                    invitationBaseUrl.replaceAll("/+$", "") : "https://lealtix.com.mx";
            String landingUrl = baseUrl + "/landing-page/" + (tenant != null ? tenant.getSlug() : "");
            dynamicData.put("landingUrl", landingUrl);
            
            dynamicData.put("ctaText", campaign.getCallToAction() != null ? campaign.getCallToAction() : "Ver más detalles");

            // 6. Construir EmailDTO
            EmailDTO emailDTO = EmailDTO.builder()
                    .to(recipientEmail)
                    .subject(campaign.getTitle() != null ? campaign.getTitle() : "Promoción especial para ti")
                    .templateId(genericPromotionTemplateId)
                    .dynamicData(dynamicData)
                    .attachments(attachments.isEmpty() ? null : attachments)
                    .entityType("campaign_email")
                    .entityId(campaign.getId())
                    .build();

            log.debug("[CampaignEmailSender] 📧 EmailDTO construido para SendGrid - to: {}, templateId: {}, hasQR: {}, couponCode: {}", 
                    emailDTO.getTo(), emailDTO.getTemplateId(), !attachments.isEmpty(), 
                    dynamicData.get("couponCode"));

            // 7. Guardar payload de auditoría
            savePayload(campaignEmail, emailDTO.getSubject(), "<plantilla dinámica SendGrid>", genericPromotionTemplateId);
            
            // 8. ENVÍO POR SENDGRID 🚀
            log.info("🚀 [CampaignEmailSender] [SendGrid] Enviando email a: {} para campaña: {} con cupón: {}", 
                    recipientEmail, campaign.getId(), dynamicData.get("couponCode"));
            
            emailService.sendEmailWithTemplate(emailDTO);

            // 9. Actualizar estado a SENT
            campaignEmail.setStatus(CampaignEmailStatus.SENT);
            campaignEmail.setSentAt(LocalDateTime.now());
            campaignEmail.setProviderName(providerName);
            campaignEmail.setLastAttemptAt(LocalDateTime.now());
            campaignEmail.setAttemptCount(campaignEmail.getAttemptCount() + 1);
            campaignEmailRepository.save(campaignEmail);

            log.info("✅ [CampaignEmailSender] [SendGrid] Email enviado exitosamente: {} -> {} | Attempt: {}", 
                    campaignEmail.getId(), recipientEmail, campaignEmail.getAttemptCount());

        } catch (Exception e) {
            log.error("❌ [CampaignEmailSender] [SendGrid ERROR] Exception enviando email {} a {}: {}", 
                    campaignEmail.getId(), campaignEmail.getRecipientEmail(), e.getMessage(), e);
            handleSendError(campaignEmail, e);
        }

        updateCampaignMetrics(campaign.getId());
    }

    private String renderEmailContent(Campaign campaign, CampaignEmail campaignEmail) {
        log.debug("[CampaignEmailSender] Renderizando contenido para email: {}", campaignEmail.getId());

        StringBuilder content = new StringBuilder();
        content.append("<h1>").append(campaign.getTitle()).append("</h1>\n");
        if (campaign.getSubtitle() != null) {
            content.append("<h2>").append(campaign.getSubtitle()).append("</h2>\n");
        }
        if (campaign.getDescription() != null) {
            content.append("<p>").append(campaign.getDescription()).append("</p>\n");
        }
        if (campaign.getCallToAction() != null) {
            content.append("<p><strong>").append(campaign.getCallToAction()).append("</strong></p>\n");
        }

        return content.toString();
    }

    @Transactional
    private void savePayload(CampaignEmail campaignEmail, String subject, String body, String templateId) {
        log.debug("[CampaignEmailSender] Guardando payload para email: {}", campaignEmail.getId());

        CampaignEmailPayload payload = CampaignEmailPayload.builder()
                .campaignEmail(campaignEmail)
                .emailSubject(subject)
                .emailBodyHtml(body)
                .templateId(templateId)
                .attemptNumber(campaignEmail.getAttemptCount() + 1)
                .build();

        campaignEmailPayloadRepository.save(payload);
    }

    /**
     * Método heredado (deprecado) - no se usa en nuevo flujo con cupón+QR
     */
    private void simulateSendEmail(String recipient, String subject, String body) {
        log.info("📤 [CampaignEmailSender] Simulando envío a: {} | Asunto: {}", recipient, subject);
    }

    /**
     * Método heredado (deprecado) - reemplazado por sendEmail(...) que genera cupón y QR
     */
    private void sendEmailWithService(String recipientEmail, String subject, String templateId, Campaign campaign) throws IOException {
        log.info("📤 [CampaignEmailSender] Enviando email real a: {} usando template: {}", recipientEmail, templateId);

        Map<String, Object> dynamicData = prepareDynamicData(campaign);

        EmailDTO emailDTO = EmailDTO.builder()
                .to(recipientEmail)
                .subject(subject)
                .templateId(templateId)
                .dynamicData(dynamicData)
                .entityType("campaign_email")
                .entityId(campaign.getId())
                .build();

        log.debug("[CampaignEmailSender] Enviando EmailDTO: to={}, templateId={}", emailDTO.getTo(), emailDTO.getTemplateId());
        emailService.sendEmailWithTemplate(emailDTO);
    }

    /**
     * Método heredado (deprecado)
     */
    private Map<String, Object> prepareDynamicData(Campaign campaign) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", campaign.getTitle() != null ? campaign.getTitle() : "");
        data.put("subtitle", campaign.getSubtitle() != null ? campaign.getSubtitle() : "");
        data.put("description", campaign.getDescription() != null ? campaign.getDescription() : "");
        data.put("callToAction", campaign.getCallToAction() != null ? campaign.getCallToAction() : "");
        data.put("imageUrl", campaign.getImageUrl() != null ? campaign.getImageUrl() : "");
        return data;
    }

    @Transactional
    private void handleSendError(CampaignEmail campaignEmail, Exception e) {
        log.error("❌ [CampaignEmailSender] Error al enviar email {}: {}", campaignEmail.getId(), e.getMessage(), e);

        int attemptCount = campaignEmail.getAttemptCount() != null ? campaignEmail.getAttemptCount() : 0;
        attemptCount++;

        campaignEmail.setAttemptCount(attemptCount);
        campaignEmail.setLastAttemptAt(LocalDateTime.now());
        campaignEmail.setProviderErrorMessage(e.getMessage());
        campaignEmail.setProviderErrorCode("SEND_ERROR");

        if (attemptCount >= maxAttempts) {
            campaignEmail.setStatus(CampaignEmailStatus.FAILED);
            log.warn("⚠️ [CampaignEmailSender] Email {} alcanzó máximo de intentos. Marcado como FAILED", campaignEmail.getId());
        } else {
            // Programar reintentos con backoff exponencial
            long delaySeconds = (long) Math.pow(2, attemptCount) * 60; // 2min, 4min, 8min
            campaignEmail.setNextAttemptAt(LocalDateTime.now().plusSeconds(delaySeconds));
            log.info("⏰ [CampaignEmailSender] Email {} reintentará en {} segundos", campaignEmail.getId(), delaySeconds);
        }

        campaignEmailRepository.save(campaignEmail);
    }

    private void updateCampaignMetrics(Long campaignId) {
        log.debug("[CampaignEmailSender] Actualizando métricas de campaña: {}", campaignId);
        campaignRepository.updateTotalSent(campaignId);
        campaignRepository.updateTotalFailed(campaignId);
        campaignRepository.markAsFinished(campaignId);
    }
}
