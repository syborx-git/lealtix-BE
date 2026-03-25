package com.lealtixservice.service;

import com.lealtixservice.dto.EmailAttachmentDTO;
import com.lealtixservice.dto.EmailDTO;
import com.lealtixservice.entity.*;
import com.lealtixservice.enums.CampaignEmailStatus;
import com.lealtixservice.enums.CampaignStatus;
import com.lealtixservice.enums.SegmentationType;
import com.lealtixservice.repository.CampaignEmailRepository;
import com.lealtixservice.repository.CampaignRepository;
import com.lealtixservice.repository.TenantCustomerRepository;
import com.lealtixservice.repository.TenantRepository;
import com.lealtixservice.specification.TenantCustomerSpecifications;
import com.lealtixservice.util.SegmentationConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class CampaignEmailOrchestrator {

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CampaignEmailRepository campaignEmailRepository;

    @Autowired
    private TenantCustomerRepository tenantCustomerRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private CouponService couponService;

    @Autowired
    private Emailservice emailService;

    @Autowired
    private QrCodeService qrCodeService;

    @Autowired
    private EmailLogService emailLogService;

    @Value("${sendgrid.templates.generic-promotion:d-1d7c686b956f4d76a02d49b76a52db9a}")
    private String genericPromotionTemplateId;

    @Value("${lealtix.dashboard.url}")
    private String dashboardUrl;

    @Value("${invitation.base-url}")
    private String invitationBaseUrl;

    @Value("${campaign.email.batch.size:50}")
    private int batchSize;

    @Value("${campaign.email.max.attempts:3}")
    private int maxAttempts;

    // Constantes
    private static final String TEMPLATE_NAME_GENERIC_PROMOTION = "generic-promotion";
    private static final String EMAIL_SUBJECT_DEFAULT = "Promoción especial para ti";
    private static final String PROMO_TITLE_DEFAULT = "Promoción especial";
    private static final String PROMO_DESCRIPTION_DEFAULT = "Queremos consentirte con una promoción exclusiva.";
    private static final String PROMO_CONDITIONS_SUFFIX = ". No acumulable con otras promociones.";
    private static final String CTA_TEXT_DEFAULT = "Ver más detalles";
    private static final String DEFAULT_BASE_URL = "https://lealtix.com.mx";
    private static final String ENTITY_TYPE_CAMPAIGN_EMAIL = "campaign_email";
    private static final String QR_FILENAME = "coupon-qr.png";
    private static final String QR_CONTENT_ID = "couponQr";

    @Transactional
    public void startCampaign(Long campaignId) {
        log.info("🚀 [CampaignEmailOrchestrator] Iniciando campaña: {}", campaignId);

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaña no encontrada: " + campaignId));

        // Allow campaigns in READY or ACTIVE state to be started. DRAFT or other states are invalid.
        if (campaign.getStatus() != CampaignStatus.ACTIVE && campaign.getStatus() != CampaignStatus.READY) {
            log.warn("⚠️ [CampaignEmailOrchestrator] Campaña {} no está en estado READY/ACTIVE. Estado actual: {}",
                    campaignId, campaign.getStatus());
            throw new IllegalStateException("La campaña debe estar en estado READY o ACTIVE para iniciar el envío");
        }

        campaign.setStatus(CampaignStatus.SENDING);
        campaign.setPublishedAt(LocalDateTime.now());
        campaignRepository.save(campaign);
        log.info("✅ [CampaignEmailOrchestrator] Campaña {} cambiada a estado SENDING", campaignId);

        // Generar registros CampaignEmail para la audiencia
        generateCampaignEmails(campaign);
    }

    @Transactional
    private void generateCampaignEmails(Campaign campaign) {
        log.info("📧 [CampaignEmailOrchestrator] Generando emails para campaña: {}", campaign.getId());

        // Obtener tipo de segmentación de la campaña (la entidad almacena un String)
        SegmentationType segmentation;
        try {
            segmentation = SegmentationConverter.convertToSegmentationType(campaign.getSegmentation());
        } catch (IllegalArgumentException e) {
            log.warn("Segmentación inválida '{}' para campaña {}. Usando ALL por defecto", campaign.getSegmentation(), campaign.getId());
            segmentation = SegmentationType.ALL;
        }

        // Construir especificación base: siempre filtrar por tenant y consentimiento
        Specification<TenantCustomer> spec = Specification
                .where(TenantCustomerSpecifications.byTenantId(campaign.getBusinessId()))
                .and(TenantCustomerSpecifications.acceptedPromotions());

        // Aplicar filtros de segmentación
        spec = applySegmentationFilter(spec, segmentation);

        // Obtener información del tenant para los emails
        Tenant tenant = getTenantOrThrow(campaign.getBusinessId(), campaign.getId());

        // Procesar clientes por lotes (paging) para evitar cargar toda la audiencia en memoria
        processCampaignEmailsInBatches(campaign, tenant, spec, segmentation);
    }

    /**
     * Obtiene el tenant o lanza excepción si no existe.
     */
    private Tenant getTenantOrThrow(Long tenantId, Long campaignId) {
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null) {
            log.error("❌ Tenant no encontrado para campaña {}. No se pueden enviar emails.", campaignId);
            throw new IllegalStateException("Tenant no encontrado: " + tenantId);
        }
        return tenant;
    }

    /**
     * Aplica los filtros de segmentación según el tipo.
     */
    private Specification<TenantCustomer> applySegmentationFilter(Specification<TenantCustomer> spec, 
                                                                   SegmentationType segmentation) {
        switch (segmentation) {
            case ALL:
                log.debug("Segmentación: ALL - Se incluyen todos los clientes");
                break;
            case MALE:
                spec = spec.and(TenantCustomerSpecifications.byGender("male"));
                log.debug("Segmentación: MALE");
                break;
            case FEMALE:
                spec = spec.and(TenantCustomerSpecifications.byGender("female"));
                log.debug("Segmentación: FEMALE");
                break;
            case UPCOMING_BIRTHDAY_7D:
                spec = spec.and(TenantCustomerSpecifications.birthdayInNextDays(7));
                log.debug("Segmentación: UPCOMING_BIRTHDAY_7D");
                break;
            case ACTIVE_30D:
                spec = spec.and(TenantCustomerSpecifications.activeWithinDays(30));
                log.debug("Segmentación: ACTIVE_30D");
                break;
            case NEW_30D:
                spec = spec.and(TenantCustomerSpecifications.createdWithinDays(30));
                log.debug("Segmentación: NEW_30D");
                break;
            case HIGH_LTV:
                log.warn("Segmentación HIGH_LTV no implementada. Campo 'ltv' no existe en TenantCustomer");
                break;
            case NO_PURCHASE_60D:
                log.warn("Segmentación NO_PURCHASE_60D no implementada. Campo 'lastPurchaseAt' no existe en TenantCustomer");
                break;
            case VIP:
                log.warn("Segmentación VIP no implementada. Campo 'isVip' no existe en TenantCustomer");
                break;
            default:
                log.error("Tipo de segmentación desconocido: {}", segmentation);
                break;
        }
        return spec;
    }

    /**
     * Procesa los emails de campaña en lotes para evitar carga masiva en memoria.
     * PRE-GENERA cupones y QR para ligereza del worker.
     */
    private void processCampaignEmailsInBatches(Campaign campaign, Tenant tenant, 
                                                 Specification<TenantCustomer> spec,
                                                 SegmentationType segmentation) {
        int pageNumber = 0;
        int totalCreated = 0;

        Page<TenantCustomer> pageResult;
        do {
            Pageable pageable = PageRequest.of(pageNumber, batchSize);
            pageResult = tenantCustomerRepository.findAll(spec, pageable);

            // Crear CampaignEmail para cada cliente del lote
            List<CampaignEmail> emailBatch = new ArrayList<>(pageResult.getContent().size());
            for (TenantCustomer customer : pageResult.getContent()) {
                // Skip customers without valid email
                if (customer.getEmail() == null || customer.getEmail().trim().isEmpty()) {
                    log.warn("⚠️ [CampaignEmailOrchestrator] Cliente {} sin email válido. Se omite.", customer.getId());
                    continue;
                }

                try {
                    Coupon coupon = null;
                    if (!couponService.hasActiveCouponForCampaign(customer.getId(), campaign.getId())) {
                        coupon = couponService.generateWelcomeCoupon(campaign, customer);
                        log.debug("✓ Cupón {} pre-generado para cliente {} durante orquestación", 
                                coupon.getCode(), customer.getEmail());
                    }

                    // PRE-GENERAR QR (aunque no se almacene, asegura que funciona)
                    if (coupon != null) {
                        try {
                            String redeemUrl = dashboardUrl + "/redeem?code=" + coupon.getQrToken();
                            String qrBase64 = qrCodeService.generateQrCodeBase64(redeemUrl);
                            log.debug("✓ QR pre-generado (base64 length: {}) para cupón {}", 
                                    qrBase64.length(), coupon.getCode());
                        } catch (IOException e) {
                            log.warn("⚠️ Error pre-generando QR para cupón {}: {}", coupon.getCode(), e.getMessage());
                            // Continuar sin QR — el worker manejará reintento
                        }
                    }

                    // Crear registro CampaignEmail con couponId pre-generado
                    CampaignEmail campaignEmail = createCampaignEmailRecord(campaign, customer, 
                            coupon != null ? coupon.getId() : null);
                    emailBatch.add(campaignEmail);

                } catch (Exception e) {
                    log.error("❌ Error pre-generando cupón para cliente {}: {}", customer.getEmail(), e.getMessage(), e);
                    // Crear email sin couponId — worker manejará gracefully
                    CampaignEmail campaignEmail = createCampaignEmailRecord(campaign, customer, null);
                    emailBatch.add(campaignEmail);
                }
            }

            // Guardar lote en BD
            if (!emailBatch.isEmpty()) {
                // Log before saving batch
                List<String> sampleRecipients = emailBatch.stream()
                        .map(CampaignEmail::getRecipientEmail)
                        .filter(e -> e != null && !e.isEmpty())
                        .limit(5)
                        .toList();

                log.info("[CampaignEmailOrchestrator] Guardando lote {} con {} CampaignEmail. Ejemplos: {}",
                        pageNumber + 1, emailBatch.size(), sampleRecipients);

                try {
                    campaignEmailRepository.saveAll(emailBatch);
                    // Force flush to DB to detect schema/constraint issues immediately
                    campaignEmailRepository.flush();

                    totalCreated += emailBatch.size();
                    log.debug("✓ Lote {} procesado: {} CampaignEmail creados con cupones pre-generados",
                            pageNumber + 1, emailBatch.size());

                    // Verify persisted count for this campaign so far
                    long persistedCount = campaignEmailRepository.countByCampaignId(campaign.getId());
                    log.info("[CampaignEmailOrchestrator] Verificación: {} CampaignEmail persisten en BD para campaign {} hasta ahora.",
                            persistedCount, campaign.getId());
                } catch (Exception e) {
                    // If batch save fails, try saving individually to avoid losing entire batch
                    log.error("❌ Error guardando batch {}: {}. Intentando guardar individualmente...",
                            pageNumber + 1, e.getMessage(), e);
                    int savedCount = 0;
                    for (CampaignEmail ce : emailBatch) {
                        try {
                            campaignEmailRepository.save(ce);
                            campaignEmailRepository.flush();
                            savedCount++;
                        } catch (Exception ex) {
                            log.error("❌ No se pudo persistir CampaignEmail para {}: {}",
                                    ce.getRecipientEmail(), ex.getMessage(), ex);
                        }
                    }
                    totalCreated += savedCount;
                    long persistedCount = campaignEmailRepository.countByCampaignId(campaign.getId());
                    log.debug("✓ Lote {} procesado parcialmente: {} CampaignEmail creados ({} fallidos). Persistidos hasta ahora: {}",
                            pageNumber + 1, savedCount, emailBatch.size() - savedCount, persistedCount);
                }
            }

            pageNumber++;
        } while (!pageResult.isLast());

        // Actualizar contadores en la campaña (envíos se realizarán posteriormente por scheduler)
        updateCampaignCounters(campaign, 0, 0);

        log.info("✅ [CampaignEmailOrchestrator] Campaña {} completada: {} emails creados con cupones pre-generados, {} enviados, {} fallidos (segmentación: {})",
                campaign.getId(), totalCreated, 0, 0, segmentation.getDisplayName());
    }

    /**
     * Crea un registro CampaignEmail con valores por defecto.
     * Ahora incluye couponId para que el worker no necesite generarlo.
     */
    private CampaignEmail createCampaignEmailRecord(Campaign campaign, TenantCustomer customer, Long couponId) {
        return CampaignEmail.builder()
                .campaign(campaign)
                .recipientEmail(customer.getEmail())
                .recipientName(customer.getName())
                .status(CampaignEmailStatus.PENDING)
                .attemptCount(0)
                .maxAttempts(maxAttempts)
                .scheduledAt(LocalDateTime.now())
                .couponId(couponId)
                .build();
    }

    /**
     * Actualiza el estado del CampaignEmail según el resultado del envío.
     */
    private void updateCampaignEmailStatus(Campaign campaign, TenantCustomer customer, Tenant tenant,
                                          CampaignEmail campaignEmail, int totalEmailsSent, int totalEmailsFailed) {
        try {
            boolean emailSent = processAndSendCampaignEmail(campaign, customer, tenant, campaignEmail);
            if (emailSent) {
                campaignEmail.setStatus(CampaignEmailStatus.SENT);
                campaignEmail.setSentAt(LocalDateTime.now());
            } else {
                campaignEmail.setStatus(CampaignEmailStatus.FAILED);
            }
        } catch (Exception ex) {
            log.error("Error procesando email para cliente {}: {}", customer.getEmail(), ex.getMessage(), ex);
            campaignEmail.setStatus(CampaignEmailStatus.FAILED);
            campaignEmail.setProviderErrorMessage(ex.getMessage());
        }
    }

    /**
     * Actualiza los contadores de la campaña.
     */
    private void updateCampaignCounters(Campaign campaign, int totalSent, int totalFailed) {
        campaign.setTotalSent(totalSent);
        campaign.setTotalFailed(totalFailed);
        campaignRepository.save(campaign);
    }

    /**
     * Procesa y envía un email de campaña a un cliente específico.
     * Genera cupón, QR code, prepara template y envía email.
     * 
     * @param campaign Campaña activa
     * @param customer Cliente destinatario
     * @param tenant Tenant/negocio
     * @param campaignEmail Registro CampaignEmail para tracking
     * @return true si el email se envió exitosamente
     */
    private boolean processAndSendCampaignEmail(Campaign campaign, TenantCustomer customer, 
                                                 Tenant tenant, CampaignEmail campaignEmail) {
        try {
            // 1. Verificar que el cliente no tenga cupón activo para esta campaña
            if (couponService.hasActiveCouponForCampaign(customer.getId(), campaign.getId())) {
                log.debug("Cliente {} ya tiene cupón activo para campaña {}. Omitiendo envío.", 
                         customer.getEmail(), campaign.getId());
                return false;
            }

            // 2. Generar cupón para el cliente vinculado a la campaña
            Coupon coupon = couponService.generateWelcomeCoupon(campaign, customer);
            log.info("✓ Cupón {} generado para cliente {} en campaña {}", 
                    coupon.getCode(), customer.getEmail(), campaign.getId());

            // 3. Preparar y enviar email
            sendCampaignEmail(campaign, customer, tenant, coupon, campaignEmail);
            
            log.info("✅ Email de campaña {} enviado exitosamente a {}", campaign.getId(), customer.getEmail());
            return true;

        } catch (IOException ex) {
            log.error("❌ Error enviando email de campaña {} a {}: {}", 
                     campaign.getId(), customer.getEmail(), ex.getMessage());
            logEmailSent(campaignEmail, customer, TEMPLATE_NAME_GENERIC_PROMOTION, "failed", ex.getMessage());
            return false;
        } catch (Exception ex) {
            log.error("❌ Error inesperado procesando email de campaña {} para {}: {}", 
                     campaign.getId(), customer.getEmail(), ex.getMessage(), ex);
            logEmailSent(campaignEmail, customer, TEMPLATE_NAME_GENERIC_PROMOTION, "failed", ex.getMessage());
            return false;
        }
    }

    /**
     * Prepara y envía el email de campaña con cupón.
     */
    private void sendCampaignEmail(Campaign campaign, TenantCustomer customer, Tenant tenant, 
                                   Coupon coupon, CampaignEmail campaignEmail) throws IOException {
        Map<String, Object> dynamicData = prepareDynamicDataForEmail(campaign, customer, tenant, coupon);
        List<EmailAttachmentDTO> attachments = generateQrCodeAttachment(coupon);

        EmailDTO emailDTO = EmailDTO.builder()
                .to(customer.getEmail())
                .subject(getEmailSubject(campaign))
                .templateId(genericPromotionTemplateId)
                .dynamicData(dynamicData)
                .attachments(attachments.isEmpty() ? null : attachments)
                .build();

        log.debug("Enviando email de campaña {} a {}", campaign.getId(), customer.getEmail());
        emailService.sendEmailWithTemplate(emailDTO);
        
        logEmailSent(campaignEmail, customer, TEMPLATE_NAME_GENERIC_PROMOTION, "sent", null);
    }

    /**
     * Obtiene el subject del email (usa título de campaña o default).
     */
    private String getEmailSubject(Campaign campaign) {
        return campaign.getTitle() != null ? campaign.getTitle() : EMAIL_SUBJECT_DEFAULT;
    }

    /**
     * Prepara los datos dinámicos para la plantilla de SendGrid.
     * Mapea los campos del JSON requerido por la plantilla generic-promotion.
     */
    private Map<String, Object> prepareDynamicDataForEmail(Campaign campaign, TenantCustomer customer, 
                                                            Tenant tenant, Coupon coupon) {
        Map<String, Object> dynamicData = new HashMap<>();

        // Datos del tenant/negocio
        dynamicData.put("tenantName", getValueOrDefault(tenant.getNombreNegocio(), ""));
        dynamicData.put("logoUrl", getValueOrDefault(tenant.getLogoUrl(), ""));

        // Datos de la promoción/campaña
        dynamicData.put("promoImageUrl", getValueOrDefault(campaign.getImageUrl(), ""));
        dynamicData.put("promoTitle", getValueOrDefault(campaign.getTitle(), PROMO_TITLE_DEFAULT));
        dynamicData.put("promoDescription", getValueOrDefault(campaign.getDescription(), PROMO_DESCRIPTION_DEFAULT));
        dynamicData.put("promoBenefit", getPromoBenefit(campaign));
        dynamicData.put("couponCode", coupon.getCode());
        dynamicData.put("promoConditions", buildPromoConditions(campaign));
        dynamicData.put("landingUrl", buildLandingUrl(tenant));
        dynamicData.put("ctaText", getValueOrDefault(campaign.getCallToAction(), CTA_TEXT_DEFAULT));

        return dynamicData;
    }

    /**
     * Retorna value si no es null, sino default.
     */
    private String getValueOrDefault(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * Obtiene el beneficio de la promoción desde el reward.
     */
    private String getPromoBenefit(Campaign campaign) {
        if (campaign.getPromotionReward() != null && campaign.getPromotionReward().getDescription() != null) {
            return campaign.getPromotionReward().getDescription();
        }
        return "";
    }

    /**
     * Genera las condiciones de la promoción (fechas de validez).
     */
    private String buildPromoConditions(Campaign campaign) {
        if (campaign.getEndDate() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd 'de' MMMM");
            String endDateStr = campaign.getEndDate().format(formatter);
            return "Válido hasta el " + endDateStr + PROMO_CONDITIONS_SUFFIX;
        }
        return "No acumulable con otras promociones.";
    }

    /**
     * Construye la URL de la landing page del negocio.
     */
    private String buildLandingUrl(Tenant tenant) {
        String slug = tenant.getSlug();
        String baseUrl = (invitationBaseUrl != null && !invitationBaseUrl.trim().isEmpty()) ? 
                         invitationBaseUrl.replaceAll("/+$", "") : DEFAULT_BASE_URL;
        return baseUrl + "/landing-page/" + slug;
    }

    /**
     * Genera el QR code para el cupón y lo adjunta como attachment inline.
     */
    private List<EmailAttachmentDTO> generateQrCodeAttachment(Coupon coupon) {
        List<EmailAttachmentDTO> attachments = new ArrayList<>();
        
        try {
            String redeemUrl = dashboardUrl + "/redeem?code=" + coupon.getQrToken();
            String qrBase64 = qrCodeService.generateQrCodeBase64(redeemUrl);

            EmailAttachmentDTO qrAttachment = EmailAttachmentDTO.builder()
                    .content(qrBase64)
                    .type("image/png")
                    .filename(QR_FILENAME)
                    .disposition("inline")
                    .contentId(QR_CONTENT_ID)
                    .build();

            attachments.add(qrAttachment);
            log.debug("✓ QR code generado y adjuntado para cupón {}", coupon.getCode());

        } catch (IOException qrEx) {
            log.error("❌ Error generando QR code para cupón {}: {}", 
                     coupon.getCode(), qrEx.getMessage());
        }
        
        return attachments;
    }

    /**
     * Registra el log del envío de email en la tabla email_log.
     */
    private void logEmailSent(CampaignEmail campaignEmail, TenantCustomer customer, 
                              String templateName, String status, String errorMessage) {
        try {
            EmailLog emailLog = EmailLog.builder()
                    .entityType(ENTITY_TYPE_CAMPAIGN_EMAIL)
                    .entityId(campaignEmail.getId() != null ? 
                             Long.valueOf(campaignEmail.getId().toString().hashCode()) : 0L)
                    .email(customer.getEmail())
                    .templateName(templateName)
                    .sendgridMessageId(campaignEmail.getProviderMessageId())
                    .status(status)
                    .errorMessage(errorMessage)
                    .createdAt(LocalDateTime.now())
                    .build();

            emailLogService.save(emailLog);
            log.debug("✓ EmailLog guardado para {}", customer.getEmail());

        } catch (Exception ex) {
            log.error("Error guardando EmailLog para {}: {}", customer.getEmail(), ex.getMessage());
        }
    }
}
