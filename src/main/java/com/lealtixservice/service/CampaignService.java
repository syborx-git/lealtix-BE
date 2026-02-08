package com.lealtixservice.service;

import com.lealtixservice.dto.*;
import com.lealtixservice.entity.Campaign;
import java.util.List;

public interface CampaignService {
    CampaignResponse create(CreateCampaignRequest request);
    CampaignResponse update(Long id, UpdateCampaignRequest request);
    CampaignResponse findById(Long id);
    List<CampaignResponse> findByBusinessId(Long businessId);
    void delete(Long id);

    // Métodos para borradores
    CampaignResponse createDraft(CreateCampaignDraftDto dto);
    CampaignResponse updateDraft(Long id, CreateCampaignDraftDto dto);
    CampaignResponse publishDraft(Long id);
    CampaignResponse create(CreateCampaignDTO dto);
    List<CampaignResponse> getDraftsByBusiness(Long businessId);
    List<CampaignResponse> getActiveCampaigns(Long businessId);

    // Reglas de negocio
    CampaignResponse activateCampaign(Long campaignId);
    CampaignResponse createWelcomeCampaignForTenant(Long tenantId);
    PromotionRewardResponse configureReward(Long campaignId, ConfigureRewardRequest request);

    // Nuevo: Validar si un tenant tiene una campaña de bienvenida activa
    boolean hasActiveWelcomeCampaign(Long tenantId);

    // Nuevo: Obtener la campaña de bienvenida activa (entidad completa para generar cupones)
    Campaign getActiveWelcomeCampaignEntity(Long tenantId);

    // Nuevo: Validar campañas de un negocio y retornar lista de faltantes
    List<CampaignValidationResult> validateCampaignsForBusiness(Long businessId);
}
