package com.lealtixservice.service.impl;

import com.lealtixservice.dto.*;
import com.lealtixservice.entity.Campaign;
import com.lealtixservice.entity.CampaignTemplate;
import com.lealtixservice.entity.Coupon;
import com.lealtixservice.entity.PromotionReward;
import com.lealtixservice.enums.CampaignStatus;
import com.lealtixservice.enums.RewardType;
import com.lealtixservice.exception.BusinessRuleException;
import com.lealtixservice.exception.ResourceNotFoundException;
import com.lealtixservice.mapper.CampaignMapper;
import com.lealtixservice.repository.CampaignRepository;
import com.lealtixservice.repository.CampaignTemplateRepository;
import com.lealtixservice.repository.CouponRepository;
import com.lealtixservice.repository.PromotionRewardRepository;
import com.lealtixservice.service.CampaignService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CampaignServiceImpl implements CampaignService {

    private static final Logger log = LoggerFactory.getLogger(CampaignServiceImpl.class);

    private final CampaignRepository campaignRepository;
    private final CampaignTemplateRepository templateRepository;
    private final PromotionRewardRepository promotionRewardRepository;
    private final CouponRepository couponRepository;

    @Override
    @Transactional
    public CampaignResponse create(CreateCampaignRequest request) {
        validateDates(request.getStartDate(), request.getEndDate());
        CampaignTemplate template = null;
        if (request.getTemplateId() != null) {
            template = templateRepository.findById(request.getTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("CampaignTemplate no encontrado id=" + request.getTemplateId()));
        }
        Campaign entity = CampaignMapper.toEntity(request, template);
        Campaign saved = campaignRepository.save(entity);
        return CampaignMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CampaignResponse update(Long id, UpdateCampaignRequest request) {
        long t0 = System.currentTimeMillis();

        Campaign campaign = campaignRepository.findByIdWithTemplate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign no encontrada id=" + id));
        long t1 = System.currentTimeMillis();

        // Loguear campos clave para ver qué se ha cargado.
        try {
            Long templateId = campaign.getTemplate() != null ? campaign.getTemplate().getId() : null;
            log.debug("Campaign loaded: id={}, businessId={}, status={}, isDraft={}, templateId={}, createdAt={}, updatedAt={}",
                    campaign.getId(), campaign.getBusinessId(), campaign.getStatus(), campaign.getIsDraft(), templateId,
                    campaign.getCreatedAt(), campaign.getUpdatedAt());
        } catch (Exception e) {
            log.debug("Error al leer propiedades de campaign en debug: {}", e.getMessage());
        }

        if (request.getStartDate() != null || request.getEndDate() != null) {
            LocalDate start = request.getStartDate() != null ? request.getStartDate() : campaign.getStartDate();
            LocalDate end = request.getEndDate() != null ? request.getEndDate() : campaign.getEndDate();
            validateDates(start, end);
        }
        long t2 = System.currentTimeMillis();

        CampaignMapper.updateEntityFromRequest(request, campaign);
        long t3 = System.currentTimeMillis();

        Campaign saved = campaignRepository.save(campaign);
        long t4 = System.currentTimeMillis();

        // REGLA: Si promoType = NONE, eliminar reward y coupons asociados
        if (request.getPromoType() != null && "NONE".equals(request.getPromoType())) {
            log.info("promoType = NONE detectado para campaña {}. Eliminando reward y coupons asociados.", saved.getId());

            // 2. Eliminar el PromotionReward si existe
            Long savedCampaignId = saved.getId();
            promotionRewardRepository.findByCampaignId(savedCampaignId).ifPresent(reward -> {
                log.info("Eliminando PromotionReward {} de campaña {}", reward.getId(), savedCampaignId);
                promotionRewardRepository.delete(reward);
            });
            saved.setPromotionReward(null); // limpiar referencia en entity
            saved = campaignRepository.save(campaign);

            log.info("Reward y coupons eliminados para campaña {}. Status actual: {}", saved.getId(), saved.getStatus());
        }
        // Si el request trae configuración de reward (y NO es NONE), delegar a configureReward para validar y persistir
        else if (request.getReward() != null) {
            log.debug("Request incluye reward - aplicando configureReward para campaignId={}", saved.getId());
            try {
                Long savedId = saved.getId();
                this.configureReward(savedId, request.getReward());
                // Recargar campaign para asegurar que promotionReward esté presente en el entity manejado
                saved = campaignRepository.findById(savedId)
                        .orElseThrow(() -> new ResourceNotFoundException("Campaign no encontrada id=" + savedId));
            } catch (Exception e) {
                log.error("Error al configurar reward durante update de campaign {}: {}", saved.getId(), e.getMessage(), e);
                throw e; // propagar para manejo superior
            }
        }

        return CampaignMapper.toResponse(saved);
    }

    @Override
    public CampaignResponse findById(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign no encontrada id=" + id));
        return CampaignMapper.toResponse(campaign);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CampaignResponse> findByBusinessId(Long businessId) {
        return campaignRepository.findByBusinessId(businessId).stream()
                .map(CampaignMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign no encontrada id=" + id));
        campaignRepository.delete(campaign);
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new IllegalArgumentException("endDate debe ser posterior a startDate");
        }
    }

    @Override
    @Transactional
    public CampaignResponse createDraft(CreateCampaignDraftDto dto) {
        CampaignTemplate template = null;
        if (dto.getTemplateId() != null) {
            template = templateRepository.findById(dto.getTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("CampaignTemplate no encontrado id=" + dto.getTemplateId()));
        }

        Campaign campaign = Campaign.builder()
                .template(template)
                .businessId(dto.getBusinessId())
                .title(dto.getTitle())
                .subtitle(dto.getSubtitle())
                .description(dto.getDescription())
                .imageUrl(dto.getImageUrl())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .status(CampaignStatus.DRAFT)
                .callToAction(dto.getCallToAction())
                .channels(dto.getChannels() != null ? String.join(",", dto.getChannels()) : null)
                .segmentation(dto.getSegmentation() != null ? String.join(",", dto.getSegmentation()) : null)
                .isAutomatic(dto.getIsAutomatic() != null ? dto.getIsAutomatic() : false)
                .isDraft(true)
                .build();

        // NOTA: Los campos promoType y promoValue fueron movidos a PromotionReward
        // Para configurar el reward, usar el nuevo endpoint de PromotionReward

        Campaign saved = campaignRepository.save(campaign);
        return CampaignMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CampaignResponse updateDraft(Long id, CreateCampaignDraftDto dto) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign no encontrada id=" + id));

        if (!campaign.getIsDraft()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La campaña no es un borrador");
        }

        // Validar fechas si se proporcionan
        if (dto.getStartDate() != null && dto.getEndDate() != null) {
            validateDates(dto.getStartDate(), dto.getEndDate());
        }

        // Actualizar campos
        campaign.setTitle(dto.getTitle());
        campaign.setSubtitle(dto.getSubtitle());
        campaign.setDescription(dto.getDescription());
        campaign.setImageUrl(dto.getImageUrl());
        // NOTA: promoType y promoValue removidos - usar PromotionReward
        campaign.setStartDate(dto.getStartDate());
        campaign.setEndDate(dto.getEndDate());
        campaign.setCallToAction(dto.getCallToAction());
        campaign.setChannels(dto.getChannels() != null ? String.join(",", dto.getChannels()) : null);
        campaign.setSegmentation(dto.getSegmentation() != null ? String.join(",", dto.getSegmentation()) : null);
        campaign.setIsAutomatic(dto.getIsAutomatic() != null ? dto.getIsAutomatic() : false);

        Campaign saved = campaignRepository.save(campaign);
        return CampaignMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CampaignResponse publishDraft(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign no encontrada id=" + id));

        if (!campaign.getIsDraft()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La campaña no es un borrador");
        }

        // Validaciones para publicación
        List<String> errors = new ArrayList<>();

        if (campaign.getTitle() == null || campaign.getTitle().length() < 3) {
            errors.add("El título debe tener al menos 3 caracteres");
        }

        if (campaign.getDescription() == null || campaign.getDescription().length() < 10) {
            errors.add("La descripción debe tener al menos 10 caracteres");
        }

        if (campaign.getStartDate() == null || campaign.getStartDate().isBefore(LocalDate.now())) {
            errors.add("La fecha de inicio debe ser presente o futura");
        }

        if (campaign.getEndDate() == null ||
            (campaign.getStartDate() != null && campaign.getEndDate().isBefore(campaign.getStartDate()))) {
            errors.add("La fecha de fin debe ser posterior a la fecha de inicio");
        }

        if (campaign.getChannels() == null || campaign.getChannels().trim().isEmpty()) {
            errors.add("Debe especificar al menos un canal");
        }

        if (!errors.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Errores de validación: " + String.join(", ", errors));
        }

        // Publicar campaña
        campaign.setIsDraft(false);
        campaign.setStatus(CampaignStatus.ACTIVE);
        campaign.setPublishedAt(LocalDateTime.now());

        Campaign saved = campaignRepository.save(campaign);
        return CampaignMapper.toResponse(saved);
    }

    @Override
    public List<CampaignResponse> getDraftsByBusiness(Long businessId) {
        return campaignRepository.findByBusinessIdAndIsDraftOrderByUpdatedAtDesc(businessId, true)
                .stream()
                .map(CampaignMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CampaignResponse> getActiveCampaigns(Long businessId) {
        return campaignRepository.findByBusinessIdAndIsDraftFalseOrderByCreatedAtDesc(businessId)
                .stream()
                .map(CampaignMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CampaignResponse create(CreateCampaignDTO dto) {
        // Validar fechas
        validateDates(dto.getStartDate(), dto.getEndDate());

        CampaignTemplate template = null;
        if (dto.getTemplateId() != null) {
            template = templateRepository.findById(dto.getTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("CampaignTemplate no encontrado id=" + dto.getTemplateId()));
        }

        Campaign campaign = Campaign.builder()
                .template(template)
                .businessId(dto.getBusinessId())
                .title(dto.getTitle())
                .subtitle(dto.getSubtitle())
                .description(dto.getDescription())
                .imageUrl(dto.getImageUrl())
                // NOTA: promoType y promoValue removidos - usar PromotionReward
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .status(CampaignStatus.ACTIVE)
                .callToAction(dto.getCallToAction())
                .channels(dto.getChannels() != null ? String.join(",", dto.getChannels()) : null)
                .segmentation(dto.getSegmentation() != null ? String.join(",", dto.getSegmentation()) : null)
                .isAutomatic(dto.getIsAutomatic() != null ? dto.getIsAutomatic() : false)
                .isDraft(false)
                .publishedAt(LocalDateTime.now())
                .build();

        Campaign saved = campaignRepository.save(campaign);
        return CampaignMapper.toResponse(saved);
    }

    /**
     * REGLA DE NEGOCIO 1: Activación explícita de campaña (FASE 4)
     *
     * Una campaña solo puede ser activada si:
     * - Existe
     * - Tiene status DRAFT (no se permite reactivar campañas ya activas)
     * - Tiene un PromotionReward configurado
     * - El PromotionReward está completo según su tipo (validación estricta)
     *
     * Esta fase separa CONFIGURAR reward (fase 3) de ACTIVAR campaña (fase 4).
     *
     * @param campaignId ID de la campaña a activar
     * @return CampaignResponse con la campaña activada
     * @throws ResourceNotFoundException si la campaña no existe
     * @throws BusinessRuleException si la campaña no cumple las validaciones requeridas
     */
    @Override
    @Transactional
    public CampaignResponse activateCampaign(Long campaignId) {
        log.info("Intentando activar campaña con ID: {}", campaignId);

        // 1. Validar que la campaña exista
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la campaña con ID: " + campaignId));

        // 2. Validar que el status sea DRAFT (no permitir reactivación)
        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            log.warn("Intento de activar campaña {} con status {}", campaignId, campaign.getStatus());
            throw new BusinessRuleException(
                    "Solo se pueden activar campañas en estado DRAFT. " +
                    "La campaña actual está en estado: " + campaign.getStatus());
        }

        // 3. Validar que tenga un PromotionReward asociado
        PromotionReward reward = promotionRewardRepository.findByCampaignId(campaignId)
                .orElseThrow(() -> new BusinessRuleException(
                        "No se puede activar la campaña sin un PromotionReward configurado. " +
                        "Por favor, configure una recompensa antes de activar la campaña."));

        // 4. Validar que el reward esté completo según su tipo
        validateRewardCompleteness(reward);

        // 5. Activar la campaña
        campaign.setStatus(CampaignStatus.ACTIVE);
        campaign.setPublishedAt(LocalDateTime.now());

        Campaign saved = campaignRepository.save(campaign);
        log.info("Campaña {} activada exitosamente en FASE 4", campaignId);

        return CampaignMapper.toResponse(saved);
    }

    /**
     * Valida que un PromotionReward esté completo y listo para ser usado.
     * Se asegura de que todos los campos requeridos según el tipo estén presentes.
     *
     * @param reward El PromotionReward a validar
     * @throws BusinessRuleException si el reward no está completo
     */
    private void validateRewardCompleteness(PromotionReward reward) {
        RewardType type = reward.getRewardType();

        if (type == null) {
            throw new BusinessRuleException("El reward no tiene un tipo definido");
        }

        switch (type) {
            case PERCENT_DISCOUNT:
                if (reward.getNumericValue() == null || reward.getNumericValue().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessRuleException(
                            "El reward tipo PERCENT_DISCOUNT debe tener un 'numericValue' mayor a 0");
                }
                if (reward.getNumericValue().compareTo(new BigDecimal("100")) > 0) {
                    throw new BusinessRuleException(
                            "El porcentaje de descuento no puede ser mayor a 100");
                }
                break;

            case FIXED_AMOUNT:
                if (reward.getNumericValue() == null || reward.getNumericValue().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessRuleException(
                            "El reward tipo FIXED_AMOUNT debe tener un 'numericValue' mayor a 0");
                }
                break;

            case FREE_PRODUCT:
                if (reward.getProductId() == null) {
                    throw new BusinessRuleException(
                            "El reward tipo FREE_PRODUCT debe tener un 'productId' configurado");
                }
                break;

            case BUY_X_GET_Y:
                if (reward.getBuyQuantity() == null || reward.getBuyQuantity() <= 0) {
                    throw new BusinessRuleException(
                            "El reward tipo BUY_X_GET_Y debe tener un 'buyQuantity' mayor a 0");
                }
                if (reward.getFreeQuantity() == null || reward.getFreeQuantity() <= 0) {
                    throw new BusinessRuleException(
                            "El reward tipo BUY_X_GET_Y debe tener un 'freeQuantity' mayor a 0");
                }
                break;

            case CUSTOM:
                // Para CUSTOM no hay validaciones estrictas obligatorias
                log.info("Validando reward tipo CUSTOM para campaña {}", reward.getCampaign().getId());
                break;

            default:
                throw new BusinessRuleException("Tipo de reward no soportado: " + type);
        }

        log.info("Reward tipo {} validado exitosamente para campaña {}", type, reward.getCampaign().getId());
    }

    /**
     * REGLA DE NEGOCIO 2: Creación automática de campaña de bienvenida
     *
     * Al crear un tenant, se genera automáticamente una campaña de bienvenida en estado DRAFT.
     * Esta campaña usa el template de categoría "BIENVENIDA" y está marcada como automática.
     * No se crea PromotionReward ni Coupons en este momento.
     *
     * @param tenantId ID del tenant (businessId) para el cual crear la campaña
     * @return CampaignResponse con la campaña de bienvenida creada
     * @throws ResourceNotFoundException si no se encuentra un template de bienvenida
     */
    @Override
    @Transactional
    public CampaignResponse createWelcomeCampaignForTenant(Long tenantId) {
        log.info("Creando campaña de bienvenida automática para tenant: {}", tenantId);

        // Buscar el template de bienvenida
        List<CampaignTemplate> welcomeTemplates = templateRepository.findByCategory("BIENVENIDA");
        if (welcomeTemplates.isEmpty()) {
            log.warn("No se encontró template de categoría BIENVENIDA");
            throw new ResourceNotFoundException(
                    "No se encontró un template de categoría BIENVENIDA. " +
                    "Por favor, cree un template de bienvenida antes de crear tenants.");
        }

        // Usar el primer template activo de bienvenida
        CampaignTemplate welcomeTemplate = welcomeTemplates.stream()
                .filter(t -> t.getIsActive() != null && t.getIsActive())
                .findFirst()
                .orElse(welcomeTemplates.get(0));

        // Crear la campaña de bienvenida en estado DRAFT
        Campaign welcomeCampaign = Campaign.builder()
                .template(welcomeTemplate)
                .businessId(tenantId)
                .title(welcomeTemplate.getDefaultTitle() != null ?
                        welcomeTemplate.getDefaultTitle() : "¡Bienvenido a nuestro programa de fidelización!")
                .subtitle(welcomeTemplate.getDefaultSubtitle())
                .description(welcomeTemplate.getDefaultDescription())
                .imageUrl(welcomeTemplate.getDefaultImageUrl())
                .status(CampaignStatus.DRAFT)
                .isAutomatic(true)
                .isDraft(true)
                .build();

        Campaign saved = campaignRepository.save(welcomeCampaign);
        log.info("Campaña de bienvenida creada exitosamente para tenant {} con ID: {}",
                tenantId, saved.getId());

        return CampaignMapper.toResponse(saved);
    }

    /**
     * REGLA DE NEGOCIO 3: Configuración de Reward
     *
     * Permite a un tenant configurar o actualizar el reward de una campaña.
     * La campaña NO se activa en este paso, solo se configura el reward.
     *
     * @param campaignId ID de la campaña
     * @param request Datos del reward a configurar
     * @return PromotionRewardResponse con el reward configurado
     * @throws ResourceNotFoundException si la campaña no existe
     * @throws BusinessRuleException si las validaciones de negocio fallan
     */
    @Override
    @Transactional
    public PromotionRewardResponse configureReward(Long campaignId, ConfigureRewardRequest request) {
        log.info("Configurando reward para campaña ID: {} con tipo: {}", campaignId, request.getRewardType());

        // 1. Validar que la campaña exista
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la campaña con ID: " + campaignId));

        // 2. Validar parámetros según el tipo de reward
        validateRewardParameters(request);

        // 3. Buscar si ya existe un reward para esta campaña
        PromotionReward existingReward = promotionRewardRepository.findByCampaignId(campaignId)
                .orElse(null);

        PromotionReward reward;
        if (existingReward != null) {
            // Actualizar reward existente
            log.info("Actualizando reward existente ID: {} para campaña {}", existingReward.getId(), campaignId);
            reward = updateRewardFromRequest(existingReward, request);
        } else {
            // Crear nuevo reward
            log.info("Creando nuevo reward para campaña {}", campaignId);
            reward = createRewardFromRequest(campaign, request);
        }

        // 4. Guardar el reward (NO modificar el status de la campaña)
        PromotionReward savedReward = promotionRewardRepository.save(reward);
        log.info("Reward {} configurado exitosamente para campaña {}",
                savedReward.getId(), campaignId);

        return mapToRewardResponse(savedReward);
    }

    /**
     * Valida los parámetros del reward según su tipo.
     * Lanza BusinessRuleException si falta algún parámetro requerido.
     */
    private void validateRewardParameters(ConfigureRewardRequest request) {
        RewardType type = request.getRewardType();

        switch (type) {
            case PERCENT_DISCOUNT:
                if (request.getNumericValue() == null || request.getNumericValue().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessRuleException(
                            "Para reward tipo PERCENT_DISCOUNT, el campo 'numericValue' es obligatorio y debe ser mayor a 0");
                }
                if (request.getNumericValue().compareTo(new BigDecimal("100")) > 0) {
                    throw new BusinessRuleException(
                            "El porcentaje de descuento no puede ser mayor a 100");
                }
                break;

            case FIXED_AMOUNT:
                if (request.getNumericValue() == null || request.getNumericValue().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessRuleException(
                            "Para reward tipo FIXED_AMOUNT, el campo 'numericValue' es obligatorio y debe ser mayor a 0");
                }
                break;

            case FREE_PRODUCT:
                if (request.getProductId() == null) {
                    throw new BusinessRuleException(
                            "Para reward tipo FREE_PRODUCT, el campo 'productId' es obligatorio");
                }
                break;

            case BUY_X_GET_Y:
                if (request.getBuyQuantity() == null || request.getBuyQuantity() <= 0) {
                    throw new BusinessRuleException(
                            "Para reward tipo BUY_X_GET_Y, el campo 'buyQuantity' es obligatorio y debe ser mayor a 0");
                }
                if (request.getFreeQuantity() == null || request.getFreeQuantity() <= 0) {
                    throw new BusinessRuleException(
                            "Para reward tipo BUY_X_GET_Y, el campo 'freeQuantity' es obligatorio y debe ser mayor a 0");
                }
                break;

            case CUSTOM:
                // Para CUSTOM no hay validaciones estrictas
                log.info("Reward tipo CUSTOM configurado con customConfig: {}", request.getCustomConfig());
                break;

            default:
                throw new BusinessRuleException("Tipo de reward no soportado: " + type);
        }
    }

    /**
     * Crea un nuevo PromotionReward a partir del request.
     */
    private PromotionReward createRewardFromRequest(Campaign campaign, ConfigureRewardRequest request) {
        return PromotionReward.builder()
                .campaign(campaign)
                .rewardType(request.getRewardType())
                .numericValue(request.getNumericValue())
                .productId(request.getProductId())
                .buyQuantity(request.getBuyQuantity())
                .freeQuantity(request.getFreeQuantity())
                .customConfig(request.getCustomConfig())
                .description(request.getDescription())
                .minPurchaseAmount(request.getMinPurchaseAmount())
                .usageLimit(request.getUsageLimit())
                .usageCount(0)
                .build();
    }

    /**
     * Actualiza un PromotionReward existente con los datos del request.
     */
    private PromotionReward updateRewardFromRequest(PromotionReward existing, ConfigureRewardRequest request) {
        existing.setRewardType(request.getRewardType());
        existing.setNumericValue(request.getNumericValue());
        existing.setProductId(request.getProductId());
        existing.setBuyQuantity(request.getBuyQuantity());
        existing.setFreeQuantity(request.getFreeQuantity());
        existing.setCustomConfig(request.getCustomConfig());
        existing.setDescription(request.getDescription());
        existing.setMinPurchaseAmount(request.getMinPurchaseAmount());
        existing.setUsageLimit(request.getUsageLimit());
        // NO modificar usageCount al actualizar
        return existing;
    }

    /**
     * Mapea un PromotionReward a PromotionRewardResponse.
     */
    private PromotionRewardResponse mapToRewardResponse(PromotionReward reward) {
        return PromotionRewardResponse.builder()
                .id(reward.getId())
                .campaignId(reward.getCampaign().getId())
                .rewardType(reward.getRewardType())
                .numericValue(reward.getNumericValue())
                .productId(reward.getProductId())
                .buyQuantity(reward.getBuyQuantity())
                .freeQuantity(reward.getFreeQuantity())
                .customConfig(reward.getCustomConfig())
                .description(reward.getDescription())
                .minPurchaseAmount(reward.getMinPurchaseAmount())
                .usageLimit(reward.getUsageLimit())
                .usageCount(reward.getUsageCount())
                .createdAt(reward.getCreatedAt())
                .updatedAt(reward.getUpdatedAt())
                .build();
    }

    @Override
    public boolean hasActiveWelcomeCampaign(Long tenantId) {
        log.info("Verificando si el tenant {} tiene una campaña de bienvenida activa", tenantId);
        if (tenantId == null) {
            return false;
        }
        
        // Reglas: template.category = 'General', template.name = 'Bienvenida', status = ACTIVE, endDate null o >= today
        log.debug("INTENTO 1: Buscando por template exacto (category='General', name='Bienvenida')");
        boolean exists = campaignRepository.existsActiveWelcomeCampaignForTenant(
                tenantId, CampaignStatus.ACTIVE, "General", "Bienvenida");
        
        log.info("INTENTO 1 Resultado: {} | Buscando campaña de bienvenida para tenant {}: {}", exists ? "ENCONTRADA" : "NO ENCONTRADA", tenantId, exists);
        
        if (exists) {
            log.info("Campaña de bienvenida encontrada en INTENTO 1 para tenant {}", tenantId);
            return true;
        }
        
        // Fallback: Buscar solo por nombre "Bienvenida" sin criterios de template tan restrictivos
        log.debug("INTENTO 2 (fallback): Buscando campaña con nombre similar a 'bienvenida' (case-insensitive)");
        List<Campaign> fallbackCampaigns = campaignRepository.findAll()
                .stream()
                .filter(c -> c.getBusinessId() != null && c.getBusinessId().equals(tenantId))
                .filter(c -> c.getStatus() == CampaignStatus.ACTIVE)
                .filter(c -> c.getEndDate() == null || c.getEndDate().compareTo(LocalDate.now()) >= 0)
                .filter(c -> c.getTitle() != null && c.getTitle().toLowerCase().contains("bienvenid"))
                .collect(Collectors.toList());
        
        if (!fallbackCampaigns.isEmpty()) {
            log.info("INTENTO 2: Campaña de bienvenida encontrada (fallback) para tenant {}: {}", tenantId, fallbackCampaigns.get(0).getTitle());
            return true;
        }
        
        log.warn("INTENTO 2: No se encontró campaña de bienvenida incluso con fallback para tenant {}", tenantId);
        return false;
    }

    @Override
    public Campaign getActiveWelcomeCampaignEntity(Long tenantId) {
        log.info("Obteniendo entidad de campaña de bienvenida activa para tenant {}", tenantId);
        if (tenantId == null) {
            log.warn("tenantId es null, retornando null");
            return null;
        }

        // Buscar campañas activas de bienvenida (con template y promotionReward precargados)
        log.debug("INTENTO 1: Ejecutando query findActiveWelcomeCampaignsForTenant con parámetros: tenantId={}, status=ACTIVE, category=General, name=Bienvenida", tenantId);
        List<Campaign> campaigns = campaignRepository.findActiveWelcomeCampaignsForTenant(
                tenantId, CampaignStatus.ACTIVE, "General", "Bienvenida");

        log.debug("INTENTO 1: Query ejecutado. Número de campañas encontradas: {}", campaigns.size());

        if (campaigns.isEmpty()) {
            log.warn("INTENTO 1: No se encontró campaña de bienvenida con criterios estrictos para tenant {}, intentando fallback...", tenantId);
            
            // Fallback: Buscar solo por nombre "bienvenida" sin criterios de template tan restrictivos
            log.debug("INTENTO 2 (fallback): Buscando por nombre contiene 'bienvenida'");
            campaigns = campaignRepository.findAll()
                    .stream()
                    .filter(c -> c.getBusinessId() != null && c.getBusinessId().equals(tenantId))
                    .filter(c -> c.getStatus() == CampaignStatus.ACTIVE)
                    .filter(c -> c.getEndDate() == null || c.getEndDate().compareTo(LocalDate.now()) >= 0)
                    .filter(c -> c.getTitle() != null && c.getTitle().toLowerCase().contains("bienvenid"))
                    .collect(Collectors.toList());
            
            log.debug("INTENTO 2: Campañas encontradas con fallback: {}", campaigns.size());
            
            if (campaigns.isEmpty()) {
                log.warn("INTENTO 2: No se encontró campaña de bienvenida activa incluso con fallback para tenant {}", tenantId);
                return null;
            }
        }

        Campaign campaign = campaigns.get(0);
        log.info("Campaña de bienvenida {} encontrada para tenant {}: {}", campaign.getId(), tenantId, campaign.getTitle());

        // Forzar la inicialización de las relaciones lazy (para evitar LazyInitializationException)
        try {
            if (campaign.getTemplate() != null) {
                campaign.getTemplate().getName(); // Forzar carga del template
            }
            if (campaign.getPromotionReward() != null) {
                campaign.getPromotionReward().getRewardType(); // Forzar carga del promotionReward
            }

            log.debug("Verificando carga de relaciones - template: {}, promotionReward: {}",
                campaign.getTemplate() != null ? campaign.getTemplate().getId() : "null",
                campaign.getPromotionReward() != null ? campaign.getPromotionReward().getId() : "null");
        } catch (Exception e) {
            log.error("Error al verificar relaciones de la campaña: {}", e.getMessage());
        }

        return campaign;
    }

    /**
     * Valida todas las campañas de un negocio y retorna el estado de completitud
     * para mostrar alertas visuales en el UI.
     *
     * @param businessId ID del tenant/negocio
     * @return Lista de resultados de validación por cada campaña
     */
    @Override
    public List<CampaignValidationResult> validateCampaignsForBusiness(Long businessId) {
        log.info("Validando campañas para businessId: {}", businessId);

        if (businessId == null) {
            return new ArrayList<>();
        }

        return campaignRepository.findByBusinessId(businessId).stream()
                .map(campaign -> {
                    List<String> missing = validateCampaignCompleteness(campaign);
                    String severity = missing.isEmpty() ? "OK" : "ACTION_REQUIRED";
                    return CampaignValidationResult.builder()
                            .campaignId(campaign.getId())
                            .configComplete(missing.isEmpty())
                            .missingItems(missing)
                            .severity(severity)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Valida la completitud de una campaña según su tipo.
     *
     * REGLAS DE VALIDACIÓN:
     * 1. Campaña MANUAL (template = null):
     *    - No requiere: template, description, reward
     *    - Requiere: título, fechas, canales
     *
     * 2. Campaña de BIENVENIDA (template.name = "Bienvenida"):
     *    - Requiere: reward obligatorio
     *    - Si no tiene reward → cambiar status a DRAFT
     *
     * 3. Campaña AUTOMÁTICA (isAutomatic = true):
     *    - Requiere: reward, call to action, imagen
     *
     * 4. Otras campañas con TEMPLATE:
     *    - Requiere: template, título, description, fechas, canales
     *
     * @param campaign Campaña a validar
     * @return Lista de mensajes con errores de validación (lista vacía = válida)
     */
    private List<String> validateCampaignCompleteness(Campaign campaign) {
        List<String> errors = new ArrayList<>();

        if (campaign == null) {
            errors.add("Campaign no encontrada");
            return errors;
        }

        boolean isManualCampaign = campaign.getTemplate() == null;
        boolean isWelcomeCampaign = isWelcomeCampaignType(campaign);
        boolean isAutomaticCampaign = Boolean.TRUE.equals(campaign.getIsAutomatic());

        log.debug("Validando campaña {} - Manual: {}, Bienvenida: {}, Automática: {}",
                campaign.getId(), isManualCampaign, isWelcomeCampaign, isAutomaticCampaign);

        // Si es campaña manual, validaciones mínimas
        if (isManualCampaign) {
            validateManualCampaign(campaign, errors);
        } else {
            // Campaña con template requiere validaciones completas
            validateTemplateCampaign(campaign, errors, isWelcomeCampaign, isAutomaticCampaign);
        }

        // Validaciones transversales (aplican a todas las campañas)
        validateCampaignDates(campaign, errors);

        log.debug("Validación de campaña {}: {} errores encontrados", campaign.getId(), errors.size());
        return errors;
    }

    /**
     * Valida una campaña MANUAL (sin template).
     * Requisitos mínimos: título, fechas y canales.
     */
    private void validateManualCampaign(Campaign campaign, List<String> errors) {
        log.debug("Validando campaña manual {}", campaign.getId());

        // Validar título
        if (!isValidString(campaign.getTitle(), 3)) {
            errors.add("Título insuficiente (mínimo 3 caracteres)");
        }

        // Validar canales
        if (!isValidString(campaign.getChannels(), 1)) {
            errors.add("Canales no configurados");
        }
    }

    /**
     * Valida una campaña con TEMPLATE.
     * Requisitos: template, título, descripción, canales.
     * Requisito especial para BIENVENIDA: reward obligatorio.
     */
    private void validateTemplateCampaign(Campaign campaign, List<String> errors,
                                          boolean isWelcomeCampaign, boolean isAutomaticCampaign) {
        log.debug("Validando campaña con template {}", campaign.getId());

        // Validar estructura del template
        validateTemplateStructure(campaign, errors);

        // Validar título
        if (!isValidString(campaign.getTitle(), 3)) {
            errors.add("Título insuficiente (mínimo 3 caracteres)");
        }

        // Validar descripción (requerida para campañas con template)
        if (!isValidString(campaign.getDescription(), 10)) {
            errors.add("Descripción insuficiente (mínimo 10 caracteres)");
        }

        // Validar canales
        if (!isValidString(campaign.getChannels(), 1)) {
            errors.add("Canales no configurados");
        }

        // REGLA ESPECIAL: Campaña de BIENVENIDA REQUIERE reward
        if (isWelcomeCampaign) {
            validateWelcomeCampaignReward(campaign, errors);
        }

        // Validar reglas de campañas automáticas
        if (isAutomaticCampaign) {
            validateAutomaticCampaign(campaign, errors);
        }
    }

    /**
     * Valida que el template esté correctamente configurado.
     */
    private void validateTemplateStructure(Campaign campaign, List<String> errors) {
        CampaignTemplate template = campaign.getTemplate();

        if (template == null) {
            errors.add("Template no configurado");
            return;
        }

        if (!isValidString(template.getCategory(), 1)) {
            errors.add("Template: categoría no definida");
        }

        if (!isValidString(template.getName(), 1)) {
            errors.add("Template: nombre no definido");
        }
    }

    /**
     * Valida que una campaña de BIENVENIDA tenga reward configurado.
     * IMPORTANTE: Si no tiene reward, cambia el status a DRAFT.
     */
    private void validateWelcomeCampaignReward(Campaign campaign, List<String> errors) {
        log.debug("Validando reward para campaña de bienvenida {}", campaign.getId());

        boolean hasReward = promotionRewardRepository.findByCampaignId(campaign.getId()).isPresent();

        if (!hasReward) {
            errors.add("Reward obligatorio para campaña de bienvenida");

            // REGLA: Si no tiene reward, cambiar status a DRAFT
            if (campaign.getStatus() != CampaignStatus.DRAFT) {
                log.warn("Campaña de bienvenida {} sin reward. Cambiando status de {} a DRAFT",
                        campaign.getId(), campaign.getStatus());
                campaign.setStatus(CampaignStatus.DRAFT);
                campaignRepository.save(campaign);
            }
        } else {
            // Validar que el reward sea válido
            promotionRewardRepository.findByCampaignId(campaign.getId()).ifPresent(reward -> {
                try {
                    validateRewardCompleteness(reward);
                } catch (BusinessRuleException bre) {
                    errors.add("Reward inválido: " + bre.getMessage());
                }
            });
        }
    }

    /**
     * Valida requisitos específicos de campañas automáticas.
     */
    private void validateAutomaticCampaign(Campaign campaign, List<String> errors) {
        log.debug("Validando requisitos de campaña automática {}", campaign.getId());

        if (!isValidString(campaign.getCallToAction(), 1)) {
            errors.add("Call to Action requerido para campañas automáticas");
        }

        if (!isValidString(campaign.getImageUrl(), 1)) {
            errors.add("Imagen requerida para campañas automáticas");
        }
    }

    /**
     * Valida que las fechas de la campaña sean coherentes.
     * Requisitos: fecha de inicio debe existir, fecha de fin debe ser posterior.
     */
    private void validateCampaignDates(Campaign campaign, List<String> errors) {
        LocalDate startDate = campaign.getStartDate();
        LocalDate endDate = campaign.getEndDate();

        // Validar fecha de inicio
        if (startDate == null) {
            errors.add("Fecha de inicio no configurada");
            return;
        }

        // Validar fecha de fin si existe
        if (endDate != null) {
            if (endDate.isBefore(startDate)) {
                errors.add("Fecha de fin debe ser posterior a fecha de inicio");
            }

            if (endDate.isBefore(LocalDate.now())) {
                errors.add("Fecha de fin ya expiró");
            }
        }
    }

    /**
     * Verifica si una cadena es válida (no null, no vacía, longitud mínima).
     *
     * @param value Valor a validar
     * @param minLength Longitud mínima requerida
     * @return true si es válida, false en caso contrario
     */
    private boolean isValidString(String value, int minLength) {
        return value != null && value.trim().length() >= minLength;
    }

    /**
     * Verifica si una campaña es de tipo BIENVENIDA.
     *
     * @param campaign Campaña a verificar
     * @return true si es de bienvenida, false en caso contrario
     */
    private boolean isWelcomeCampaignType(Campaign campaign) {
        if (campaign.getTemplate() == null) {
            return false;
        }

        String templateName = campaign.getTemplate().getName();
        return templateName != null && "Bienvenida".equals(templateName.trim());
    }
}
