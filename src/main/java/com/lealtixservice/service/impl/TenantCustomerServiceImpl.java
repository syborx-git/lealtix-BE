package com.lealtixservice.service.impl;

import com.lealtixservice.dto.BulkCustomerError;
import com.lealtixservice.dto.BulkCustomerUploadResponse;
import com.lealtixservice.dto.TenantCustomerDTO;
import com.lealtixservice.entity.Campaign;
import com.lealtixservice.entity.Coupon;
import com.lealtixservice.entity.Tenant;
import com.lealtixservice.entity.TenantCustomer;
import com.lealtixservice.event.CustomerCreatedEvent;
import com.lealtixservice.exception.EmailAlreadyRegisteredException;
import com.lealtixservice.repository.TenantCustomerRepository;
import com.lealtixservice.repository.TenantRepository;
import com.lealtixservice.service.CampaignService;
import com.lealtixservice.service.CouponService;
import com.lealtixservice.service.TenantCustomerService;
import com.lealtixservice.util.TenantCustomerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private CouponService couponService;

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

        // Validar que el email no esté registrado para este tenant, excepto si es el mismo cliente (update)
        if (customer.getTenant() != null && customer.getTenant().getId() != null) {
            Optional<TenantCustomer> existing = tenantCustomerRepository.findByEmailAndTenantId(
                customer.getEmail(),
                customer.getTenant().getId()
            );
            if (existing.isPresent()) {
                // Si es update, permitir si el id es el mismo
                if (customer.getId() == null || !existing.get().getId().equals(customer.getId())) {
                    throw new EmailAlreadyRegisteredException(
                        "El email " + customer.getEmail() + " ya está registrado para este negocio"
                    );
                }
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

        // Publicar evento para enviar email de bienvenida de forma asíncrona y post-commit
        // Esto evita bloquear la transacción de BD con llamadas externas (SendGrid)
        try {
            Tenant tenant = null;
            if (customer.getTenant() != null && customer.getTenant().getId() != null) {
                tenant = tenantRepository.findById(customer.getTenant().getId()).orElse(null);
            } else if (saved.getTenant() != null && saved.getTenant().getId() != null) {
                tenant = tenantRepository.findById(saved.getTenant().getId()).orElse(null);
            }

            // Construir y publicar evento con toda la información necesaria
            CustomerCreatedEvent event = CustomerCreatedEvent.builder()
                    .customer(saved)
                    .tenant(tenant)
                    .welcomeCoupon(welcomeCoupon)
                    .invitationBaseUrl(invitationBaseUrl)
                    .dashboardUrl(dashboardUrl)
                    .welcomeTemplateId(welcomeTemplateId)
                    .welcomeNoCouponTemplateId(welcomeNoCouponTemplateId)
                    .build();

            eventPublisher.publishEvent(event);
            log.info("CustomerCreatedEvent publicado para customer: {}", saved.getEmail());

        } catch (Exception ex) {
            log.error("Error publishing CustomerCreatedEvent for customer {}: {}", 
                    saved.getEmail(), ex.getMessage(), ex);
            // No fallar el guardado si falla la publicación del evento
        }

        return saved;
    }

    @Override
    @Transactional
    public TenantCustomer update(TenantCustomer customer) {
        Optional<TenantCustomer> existing = tenantCustomerRepository.findById(customer.getId());
        if (existing.isEmpty()) {
            return null;
        }
        TenantCustomer entity = existing.get();
        entity.setName(customer.getName());
        entity.setEmail(customer.getEmail());
        entity.setBirthDate(customer.getBirthDate());
        entity.setGender(customer.getGender());
        entity.setPhone(customer.getPhone());
        entity.setActive(true);
        entity.setUpdatedAt(LocalDateTime.now());
        return tenantCustomerRepository.save(entity);
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
        // Normalizar email: trim y lowercase
        String normalizedEmail = email != null ? email.trim() : "";
        log.debug("Searching customers by tenantId={}, email filter='{}', page={}, size={}", 
                tenantId, normalizedEmail, pageable.getPageNumber(), pageable.getPageSize());
        return tenantCustomerRepository.findByTenantIdAndEmailContainingIgnoreCaseAndActiveTrue(
                tenantId, normalizedEmail, pageable);
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