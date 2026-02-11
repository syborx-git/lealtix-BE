package com.lealtixservice.service.impl;

import com.lealtixservice.dto.BulkCustomerError;
import com.lealtixservice.dto.BulkCustomerUploadResponse;
import com.lealtixservice.dto.EmailAttachmentDTO;
import com.lealtixservice.dto.EmailDTO;
import com.lealtixservice.dto.TenantCustomerDTO;
import com.lealtixservice.entity.Campaign;
import com.lealtixservice.entity.Coupon;
import com.lealtixservice.entity.Tenant;
import com.lealtixservice.entity.TenantCustomer;
import com.lealtixservice.exception.EmailAlreadyRegisteredException;
import com.lealtixservice.repository.TenantCustomerRepository;
import com.lealtixservice.repository.TenantRepository;
import com.lealtixservice.service.CampaignService;
import com.lealtixservice.service.CouponService;
import com.lealtixservice.service.Emailservice;
import com.lealtixservice.service.QrCodeService;
import com.lealtixservice.service.TenantCustomerService;
import com.lealtixservice.util.TenantCustomerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TenantCustomerServiceImpl implements TenantCustomerService {

    @Autowired
    private TenantCustomerRepository tenantCustomerRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private Emailservice emailService;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private CouponService couponService;

    @Autowired
    private QrCodeService qrCodeService;

    @Value("${sendgrid.templates.welcome-customer}")
    private String welcomeTemplateId;

    @Value("${sendgrid.templates.welcome-customer-no-coupon}")
    private String welcomeNoCouponTemplateId;

    @Value("${lealtix.dashboard.url}")
    private String dashboardUrl;

    @Value("${invitation.base-url}")
    private String invitationBaseUrl;

    @Override
    @Transactional
    public TenantCustomer save(TenantCustomer customer) {

        // Validar que el email no esté registrado para este tenant
        if (customer.getTenant() != null && customer.getTenant().getId() != null) {
            boolean emailExists = tenantCustomerRepository.existsByEmailAndTenantId(
                customer.getEmail(),
                customer.getTenant().getId()
            );

            if (emailExists) {
                throw new EmailAlreadyRegisteredException(
                    "El email " + customer.getEmail() + " ya está registrado para este negocio"
                );
            }
        }

        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());
        TenantCustomer saved = tenantCustomerRepository.save(customer);

        // FASE 5: Validar si existe campaña de bienvenida activa y generar cupón
        Coupon welcomeCoupon = null;
        if (saved.getTenant() != null && saved.getTenant().getId() != null) {
            try {
                log.info("Verificando campaña de bienvenida para tenant {}", saved.getTenant().getId());
                boolean hasActiveWelcomeCampaign = campaignService.hasActiveWelcomeCampaign(
                        saved.getTenant().getId());

                if (hasActiveWelcomeCampaign) {
                    log.info("El tenant {} tiene campaña de bienvenida activa, generando cupón para customer {}",
                            saved.getTenant().getId(), saved.getId());

                    // Obtener la campaña activa completa (con template y reward precargados)
                    log.debug("Llamando a getActiveWelcomeCampaignEntity para tenant {}", saved.getTenant().getId());
                    Campaign campaign = campaignService.getActiveWelcomeCampaignEntity(saved.getTenant().getId());

                    if (campaign != null) {
                        // Forzar la inicialización de las relaciones lazy antes de debuggear
                        if (campaign.getTemplate() != null) {
                            campaign.getTemplate().getName(); // Fuerza la carga
                        }
                        if (campaign.getPromotionReward() != null) {
                            campaign.getPromotionReward().getRewardType(); // Fuerza la carga
                        }

                        log.debug("Campaña obtenida: id={}, title={}, template={}, promotionReward={}",
                            campaign.getId(),
                            campaign.getTitle(),
                            campaign.getTemplate() != null ? campaign.getTemplate().getId() : "null",
                            campaign.getPromotionReward() != null ? campaign.getPromotionReward().getId() : "null");

                        welcomeCoupon = couponService.generateWelcomeCoupon(campaign, saved);
                        log.info("Cupón de bienvenida {} generado exitosamente para customer {}",
                                welcomeCoupon.getCode(), saved.getId());
                    } else {
                        log.warn("No se pudo obtener la entidad de campaña de bienvenida para tenant {}",
                                saved.getTenant().getId());
                    }
                } else {
                    log.info("El tenant {} no tiene campaña de bienvenida activa, se omite generación de cupón",
                            saved.getTenant().getId());
                }
            } catch (Exception ex) {
                log.error("Error al generar cupón de bienvenida para customer {}: {}",
                        saved.getId(), ex.getMessage(), ex);
                // No fallar el guardado del cliente si falla la generación del cupón
            }
        } else {
            log.debug("saved.getTenant() es null o no tiene ID, se omite validación de campaña de bienvenida");
        }

        // Enviar correo de bienvenida usando SendGrid si el guardado fue exitoso
        try {
            Tenant tenant = null;
            if (customer.getTenant() != null && customer.getTenant().getId() != null) {
                tenant = tenantRepository.findById(customer.getTenant().getId()).orElse(null);
            } else if (saved.getTenant() != null && saved.getTenant().getId() != null) {
                tenant = tenantRepository.findById(saved.getTenant().getId()).orElse(null);
            }

            Map<String, Object> dynamicData = new HashMap<>();
            if (tenant != null) {
                dynamicData.put("tenantName", tenant.getNombreNegocio());
                dynamicData.put("logoUrl", tenant.getLogoUrl());
            } else {
                dynamicData.put("tenantName", "");
                dynamicData.put("logoUrl", "");
            }

            dynamicData.put("customerName", saved.getName());

            // Si se generó cupón, usar datos reales y generar QR
            List<EmailAttachmentDTO> attachments = new ArrayList<>();
            String templateToUse;

            if (welcomeCoupon != null) {
                // Usar template con cupón
                templateToUse = welcomeTemplateId;
                dynamicData.put("discount", welcomeCoupon.getCampaign().getPromotionReward().getDescription());
                dynamicData.put("couponCode", welcomeCoupon.getCode());
                dynamicData.put("promoImageUrl",  welcomeCoupon.getCampaign().getImageUrl());

                // Generar QR code para el cupón
                try {
                    String redeemUrl = dashboardUrl + "/redeem?code=" + welcomeCoupon.getQrToken();
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
                templateToUse = welcomeNoCouponTemplateId;
                log.info("No hay cupón de bienvenida, usando template sin cupón para customer {}", saved.getId());
            }

            // Construir landing URL basada en la propiedad invitation.base-url del environment
            String slug = tenant.getSlug();
            String baseUrl;
            if (invitationBaseUrl != null && !invitationBaseUrl.trim().isEmpty()) {
                baseUrl = invitationBaseUrl.replaceAll("/+$", "");
            } else {
                // Si no hay base URL definida (por ejemplo en dev local), usar el comportamiento anterior
                baseUrl = "https://lealtix.com.mx";
            }
            String landingUrl = baseUrl + "/landing-page/" + slug;
            dynamicData.put("landingUrl", landingUrl);

            EmailDTO emailDTO = EmailDTO.builder()
                    .to(saved.getEmail())
                    .subject("Bienvenido a " + (tenant != null ? tenant.getNombreNegocio() : "nuestro servicio"))
                    .templateId(templateToUse)
                    .dynamicData(dynamicData)
                    .attachments(attachments.isEmpty() ? null : attachments)
                    .build();

            log.info("Intentando enviar email de bienvenida a: {}, template: {}, tenant: {}",
                    saved.getEmail(), templateToUse, tenant != null ? tenant.getNombreNegocio() : "null");
            log.debug("EmailDTO construido - to: {}, subject: {}, templateId: {}, hasAttachments: {}",
                    emailDTO.getTo(), emailDTO.getSubject(), emailDTO.getTemplateId(),
                    emailDTO.getAttachments() != null && !emailDTO.getAttachments().isEmpty());
            log.debug("DynamicData keys: {}", dynamicData != null ? dynamicData.keySet() : "null");

            emailService.sendEmailWithTemplate(emailDTO);
            log.info("✅ Email de bienvenida enviado exitosamente a: {}", saved.getEmail());
        } catch (IOException ex) {
            log.error("Error sending welcome email to customer: {}", saved.getEmail(), ex);
        } catch (Exception ex) {
            log.error("Unexpected error sending welcome email", ex);
        }

        return saved;
    }

    @Override
    public Optional<TenantCustomer> findById(Long id) {
        return tenantCustomerRepository.findById(id);
    }

    @Override
    public List<TenantCustomer> findAll() {
        return tenantCustomerRepository.findAll();
    }

    @Override
    public void deleteById(Long id) {
        tenantCustomerRepository.deleteById(id);
    }

    @Override
    public List<TenantCustomer> findByTenantId(Long tenantId) {
        return tenantCustomerRepository.findByTenantId(tenantId);
    }

    @Override
    public Page<TenantCustomer> findByTenantIdPaginated(Long tenantId, Pageable pageable) {
        return tenantCustomerRepository.findByTenantIdAndActiveTrue(tenantId, pageable);
    }

    @Override
    public Page<TenantCustomer> findByTenantIdAndEmailPaginated(Long tenantId, String email, Pageable pageable) {
        return tenantCustomerRepository.findByTenantIdAndEmailContainingIgnoreCaseAndActiveTrue(tenantId, email, pageable);
    }

    @Override
    @Transactional
    public void softDeleteById(Long id) {
        Optional<TenantCustomer> customer = tenantCustomerRepository.findById(id);
        if (customer.isPresent()) {
            TenantCustomer entity = customer.get();
            entity.setActive(false);
            entity.setUpdatedAt(LocalDateTime.now());
            tenantCustomerRepository.save(entity);
            log.info("Customer {} soft deleted (marked as inactive)", id);
        }
    }

    @Override
    @Transactional
    public BulkCustomerUploadResponse bulkUpload(Long tenantId, List<TenantCustomerDTO> customers) {
        List<BulkCustomerError> errores = new ArrayList<>();
        int exitosos = 0;
        int fallidos = 0;

        // Validar que el tenant exista
        if (!tenantRepository.existsById(tenantId)) {
            BulkCustomerError error = BulkCustomerError.builder()
                    .indice(-1)
                    .mensaje("El tenant con ID " + tenantId + " no existe")
                    .build();
            errores.add(error);
            return BulkCustomerUploadResponse.builder()
                    .exitosos(0)
                    .fallidos(customers != null ? customers.size() : 0)
                    .errores(errores)
                    .build();
        }

        if (customers == null || customers.isEmpty()) {
            return BulkCustomerUploadResponse.builder()
                    .exitosos(0)
                    .fallidos(0)
                    .errores(new ArrayList<>())
                    .build();
        }

        for (int i = 0; i < customers.size(); i++) {
            TenantCustomerDTO customerDTO = customers.get(i);
            try {
                // Validar datos básicos
                if (customerDTO.getName() == null || customerDTO.getName().trim().isEmpty()) {
                    throw new IllegalArgumentException("El nombre del cliente es requerido");
                }
                if (customerDTO.getEmail() == null || customerDTO.getEmail().trim().isEmpty()) {
                    throw new IllegalArgumentException("El email del cliente es requerido");
                }

                // Establecer tenantId si no está especificado
                if (customerDTO.getTenantId() == null) {
                    customerDTO.setTenantId(tenantId);
                } else if (!customerDTO.getTenantId().equals(tenantId)) {
                    throw new IllegalArgumentException("El tenantId del cliente no coincide con el parámetro");
                }

                // Convertir a entidad y guardar
                TenantCustomer entity = TenantCustomerMapper.toEntity(customerDTO);
                if (entity.getTenant() == null) {
                    entity.setTenant(Tenant.builder().id(tenantId).build());
                }

                // Guardar cliente
                entity.setActive(true);
                entity.setUpdatedAt(LocalDateTime.now());
                entity.setAcceptedAt(LocalDate.now());
                entity.setAcceptedPromotions(true);
                this.save(entity);
                exitosos++;
                log.info("Cliente {} cargado exitosamente en bulk upload, índice: {}", customerDTO.getEmail(), i);

            } catch (EmailAlreadyRegisteredException e) {
                fallidos++;
                BulkCustomerError error = BulkCustomerError.builder()
                        .indice(i)
                        .mensaje(e.getMessage())
                        .build();
                errores.add(error);
                log.warn("Error en bulk upload índice {}: {}", i, e.getMessage());
            } catch (Exception e) {
                fallidos++;
                String errorMsg = e.getMessage() != null ? e.getMessage() : "Error desconocido al procesar el cliente";
                BulkCustomerError error = BulkCustomerError.builder()
                        .indice(i)
                        .mensaje(errorMsg)
                        .build();
                errores.add(error);
                log.error("Error en bulk upload índice {}: {}", i, errorMsg);
            }
        }

        return BulkCustomerUploadResponse.builder()
                .exitosos(exitosos)
                .fallidos(fallidos)
                .errores(errores)
                .build();
    }
}
