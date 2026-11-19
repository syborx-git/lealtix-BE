package com.lealtixservice.service.impl;

import com.lealtixservice.dto.*;
import com.lealtixservice.entity.*;
import com.lealtixservice.repository.*;
import com.lealtixservice.service.LoyaltyCardService;
import com.lealtixservice.service.GoogleWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoyaltyCardServiceImpl implements LoyaltyCardService {

    private final TenantLoyaltyCardConfigRepository configRepository;
    private final CustomerLoyaltyCardRepository cardRepository;
    private final LoyaltyStampHistoryRepository historyRepository;
    private final TenantRepository tenantRepository;
    private final TenantCustomerRepository customerRepository;
    private final TenantMenuProductRepository productRepository;
    private final ClientOrderRepository orderRepository;
    private final AppUserRepository userRepository;
    private final GoogleWalletService googleWalletService;

    @Override
    @Transactional(readOnly = true)
    public LoyaltyCardConfigDto getConfigByTenantId(Long tenantId) {
        TenantLoyaltyCardConfig config = configRepository.findByTenantId(tenantId)
                .orElseGet(() -> createDefaultConfig(tenantId));

        return toConfigDto(config);
    }

    @Override
    @Transactional
    public LoyaltyCardConfigDto saveOrUpdateConfig(Long tenantId, LoyaltyCardConfigDto dto) {
        TenantLoyaltyCardConfig config = configRepository.findByTenantId(tenantId)
                .orElseGet(() -> {
                    Tenant tenant = tenantRepository.findById(tenantId)
                            .orElseThrow(() -> new IllegalArgumentException("Tenant no encontrado con ID: " + tenantId));
                    return TenantLoyaltyCardConfig.builder().tenant(tenant).build();
                });

        if (dto.getCardTitle() != null && !dto.getCardTitle().isBlank()) {
            config.setCardTitle(dto.getCardTitle().trim());
        }
        if (dto.getCardSubtitle() != null) {
            config.setCardSubtitle(dto.getCardSubtitle().trim());
        }
        if (dto.getBgColorHex() != null && !dto.getBgColorHex().isBlank()) {
            config.setBgColorHex(dto.getBgColorHex().trim());
        }
        if (dto.getTextColorHex() != null && !dto.getTextColorHex().isBlank()) {
            config.setTextColorHex(dto.getTextColorHex().trim());
        }
        if (dto.getAccentColorHex() != null && !dto.getAccentColorHex().isBlank()) {
            config.setAccentColorHex(dto.getAccentColorHex().trim());
        }
        if (dto.getStampIcon() != null && !dto.getStampIcon().isBlank()) {
            config.setStampIcon(dto.getStampIcon().trim());
        }
        if (dto.getTotalStamps() != null && dto.getTotalStamps() > 0) {
            config.setTotalStamps(dto.getTotalStamps());
        }
        if (dto.getRewardMode() != null && !dto.getRewardMode().isBlank()) {
            config.setRewardMode(dto.getRewardMode().trim());
        }
        if (dto.getRewardType() != null && !dto.getRewardType().isBlank()) {
            config.setRewardType(dto.getRewardType().trim());
        }
        if (dto.getRewardValue() != null) {
            config.setRewardValue(dto.getRewardValue());
        }
        if (dto.getRewardTitle() != null && !dto.getRewardTitle().isBlank()) {
            config.setRewardTitle(dto.getRewardTitle().trim());
        }
        if (dto.getRewardDescription() != null) {
            config.setRewardDescription(dto.getRewardDescription().trim());
        }
        if (dto.getTermsAndConditions() != null) {
            config.setTermsAndConditions(dto.getTermsAndConditions().trim());
        }
        if (dto.getActive() != null) {
            config.setActive(dto.getActive());
        }

        if (dto.getRewardProductId() != null) {
            TenantMenuProduct product = productRepository.findById(dto.getRewardProductId()).orElse(null);
            config.setRewardProduct(product);
        } else {
            config.setRewardProduct(null);
        }

        config = configRepository.save(config);
        log.info("Configuración de tarjeta de lealtad actualizada para tenant ID {}", tenantId);

        // Propagar cambios a tarjetas de clientes existentes y sincronizar en Google Wallet
        try {
            List<CustomerLoyaltyCard> cards = cardRepository.findAllByTenantIdWithCustomer(tenantId);
            for (CustomerLoyaltyCard card : cards) {
                // Invalidar URLs previas de guardado para forzar regeneración con nueva configuración
                card.setGoogleWalletSaveUrl(null);
                cardRepository.save(card);

                // Si ya fue agregada en el Google Wallet del cliente, actualizar pase en tiempo real vía REST API
                if (card.getGoogleWalletObjectId() != null && !card.getGoogleWalletObjectId().isBlank() && googleWalletService.isConfigured()) {
                    googleWalletService.updatePassStamps(card, config);
                }
            }
        } catch (Exception e) {
            log.warn("Aviso al propagar configuración a Google Wallet: {}", e.getMessage());
        }

        return toConfigDto(config);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerLoyaltyCardDto> getCustomerCardsByTenant(Long tenantId) {
        TenantLoyaltyCardConfig config = configRepository.findByTenantId(tenantId)
                .orElseGet(() -> createDefaultConfig(tenantId));
        int totalStampsRequired = config.getTotalStamps() != null ? config.getTotalStamps() : 8;

        List<CustomerLoyaltyCard> cards = cardRepository.findAllByTenantIdWithCustomer(tenantId);
        return cards.stream()
                .map(card -> toCardDto(card, totalStampsRequired))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CustomerLoyaltyCardDto getOrCreateCustomerCard(Long tenantId, Long customerId) {
        TenantLoyaltyCardConfig config = configRepository.findByTenantId(tenantId)
                .orElseGet(() -> createDefaultConfig(tenantId));
        int totalStampsRequired = config.getTotalStamps() != null ? config.getTotalStamps() : 8;

        CustomerLoyaltyCard card = cardRepository.findByTenantIdAndCustomerId(tenantId, customerId)
                .orElseGet(() -> {
                    Tenant tenant = tenantRepository.findById(tenantId)
                            .orElseThrow(() -> new IllegalArgumentException("Tenant no encontrado: " + tenantId));
                    TenantCustomer customer = customerRepository.findById(customerId)
                            .orElseThrow(() -> new IllegalArgumentException("Customer no encontrado: " + customerId));
                    CustomerLoyaltyCard newCard = CustomerLoyaltyCard.builder()
                            .tenant(tenant)
                            .customer(customer)
                            .currentStamps(0)
                            .completedCardsCount(0)
                            .totalStampsAccumulated(0)
                            .hasPendingReward(false)
                            .build();
                    return cardRepository.save(newCard);
                });

        if (card.getGoogleWalletSaveUrl() == null && googleWalletService.isConfigured()) {
            try {
                googleWalletService.generateSavePassUrl(card, config);
            } catch (Exception e) {
                log.warn("Could not generate Google Wallet pass for customer {}: {}", customerId, e.getMessage());
            }
        }

        return toCardDto(card, totalStampsRequired);
    }

    @Override
    @Transactional
    public CustomerLoyaltyCardDto addStamps(LoyaltyStampRequest request) {
        Long tenantId = request.getTenantId();
        Long customerId = request.getCustomerId();
        int stampsToAdd = (request.getStampsToAdd() != null && request.getStampsToAdd() > 0) ? request.getStampsToAdd() : 1;

        TenantLoyaltyCardConfig config = configRepository.findByTenantId(tenantId)
                .orElseGet(() -> createDefaultConfig(tenantId));
        int totalStamps = config.getTotalStamps() != null ? config.getTotalStamps() : 8;

        CustomerLoyaltyCard card = cardRepository.findByTenantIdAndCustomerId(tenantId, customerId)
                .orElseGet(() -> {
                    Tenant tenant = tenantRepository.findById(tenantId)
                            .orElseThrow(() -> new IllegalArgumentException("Tenant no encontrado: " + tenantId));
                    TenantCustomer customer = customerRepository.findById(customerId)
                            .orElseThrow(() -> new IllegalArgumentException("Customer no encontrado: " + customerId));
                    return CustomerLoyaltyCard.builder()
                            .tenant(tenant)
                            .customer(customer)
                            .currentStamps(0)
                            .completedCardsCount(0)
                            .totalStampsAccumulated(0)
                            .hasPendingReward(false)
                            .build();
                });

        int before = card.getCurrentStamps() != null ? card.getCurrentStamps() : 0;
        int after = before + stampsToAdd;

        card.setCurrentStamps(after);
        card.setTotalStampsAccumulated((card.getTotalStampsAccumulated() != null ? card.getTotalStampsAccumulated() : 0) + stampsToAdd);
        card.setLastStampAt(LocalDateTime.now());

        if (after >= totalStamps) {
            card.setHasPendingReward(true);
            card.setPendingRewardDescription(config.getRewardTitle());
        }

        card = cardRepository.save(card);

        // Registrar en historial
        ClientOrder order = null;
        if (request.getOrderId() != null) {
            order = orderRepository.findById(request.getOrderId()).orElse(null);
        }
        AppUser user = null;
        if (request.getUserId() != null) {
            user = userRepository.findById(request.getUserId()).orElse(null);
        }

        LoyaltyStampHistory history = LoyaltyStampHistory.builder()
                .card(card)
                .tenant(card.getTenant())
                .customer(card.getCustomer())
                .order(order)
                .registeredByUser(user)
                .actionType("ADD_STAMP")
                .stampsChange(stampsToAdd)
                .stampsBefore(before)
                .stampsAfter(after)
                .notes(request.getNotes() != null ? request.getNotes() : "Sello añadido en visita")
                .build();
        historyRepository.save(history);

        // Notificación y actualización en tiempo real en Google Wallet
        if (googleWalletService.isConfigured()) {
            try {
                googleWalletService.updatePassStamps(card, config);
            } catch (Exception e) {
                log.debug("Google Wallet update skipped/failed: {}", e.getMessage());
            }
        }

        log.info("Sello añadido: Cliente ID {} en Tenant ID {} ahora tiene {}/{} sellos", customerId, tenantId, after, totalStamps);
        return toCardDto(card, totalStamps);
    }

    @Override
    @Transactional
    public CustomerLoyaltyCardDto redeemReward(LoyaltyRedeemRewardRequest request) {
        Long tenantId = request.getTenantId();
        Long customerId = request.getCustomerId();

        TenantLoyaltyCardConfig config = configRepository.findByTenantId(tenantId)
                .orElseGet(() -> createDefaultConfig(tenantId));
        int totalStamps = config.getTotalStamps() != null ? config.getTotalStamps() : 8;

        CustomerLoyaltyCard card = cardRepository.findByTenantIdAndCustomerId(tenantId, customerId)
                .orElseThrow(() -> new IllegalStateException("El cliente no tiene tarjeta de lealtad registrada"));

        int current = card.getCurrentStamps() != null ? card.getCurrentStamps() : 0;
        if (current < totalStamps && !Boolean.TRUE.equals(card.getHasPendingReward())) {
            throw new IllegalStateException("El cliente aún no alcanza los sellos requeridos para canjear la recompensa");
        }

        int before = current;
        int after = Math.max(0, current - totalStamps);

        card.setCurrentStamps(after);
        card.setCompletedCardsCount((card.getCompletedCardsCount() != null ? card.getCompletedCardsCount() : 0) + 1);
        card.setHasPendingReward(after >= totalStamps);
        card.setPendingRewardDescription(after >= totalStamps ? config.getRewardTitle() : null);
        card.setLastRedemptionAt(LocalDateTime.now());

        card = cardRepository.save(card);

        // Historial
        ClientOrder order = null;
        if (request.getOrderId() != null) {
            order = orderRepository.findById(request.getOrderId()).orElse(null);
        }
        AppUser user = null;
        if (request.getUserId() != null) {
            user = userRepository.findById(request.getUserId()).orElse(null);
        }

        LoyaltyStampHistory history = LoyaltyStampHistory.builder()
                .card(card)
                .tenant(card.getTenant())
                .customer(card.getCustomer())
                .order(order)
                .registeredByUser(user)
                .actionType("REDEEM_REWARD")
                .stampsChange(-totalStamps)
                .stampsBefore(before)
                .stampsAfter(after)
                .rewardRedeemed(config.getRewardTitle())
                .notes(request.getNotes() != null ? request.getNotes() : "Recompensa canjeada con éxito")
                .build();
        historyRepository.save(history);

        // Notificación en Google Wallet
        if (googleWalletService.isConfigured()) {
            try {
                googleWalletService.updatePassStamps(card, config);
            } catch (Exception e) {
                log.debug("Google Wallet update skipped/failed: {}", e.getMessage());
            }
        }

        log.info("Recompensa canjeada: Cliente ID {} en Tenant ID {}. Tarjetas completadas: {}", customerId, tenantId, card.getCompletedCardsCount());
        return toCardDto(card, totalStamps);
    }

    @Override
    @Transactional
    public String getGoogleWalletPassUrl(Long tenantId, Long customerId) {
        TenantLoyaltyCardConfig config = configRepository.findByTenantId(tenantId)
                .orElseGet(() -> createDefaultConfig(tenantId));

        CustomerLoyaltyCard card = cardRepository.findByTenantIdAndCustomerId(tenantId, customerId)
                .orElseGet(() -> {
                    Tenant tenant = tenantRepository.findById(tenantId)
                            .orElseThrow(() -> new IllegalArgumentException("Tenant no encontrado: " + tenantId));
                    TenantCustomer customer = customerRepository.findById(customerId)
                            .orElseThrow(() -> new IllegalArgumentException("Customer no encontrado: " + customerId));
                    CustomerLoyaltyCard newCard = CustomerLoyaltyCard.builder()
                            .tenant(tenant)
                            .customer(customer)
                            .currentStamps(0)
                            .completedCardsCount(0)
                            .totalStampsAccumulated(0)
                            .hasPendingReward(false)
                            .build();
                    return cardRepository.save(newCard);
                });

        if (googleWalletService.isConfigured()) {
            return googleWalletService.generateSavePassUrl(card, config);
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoyaltyStampHistoryDto> getHistoryByTenant(Long tenantId) {
        List<LoyaltyStampHistory> list = historyRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        return list.stream().map(this::toHistoryDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoyaltyStampHistoryDto> getHistoryByCard(Long cardId) {
        List<LoyaltyStampHistory> list = historyRepository.findByCardIdOrderByCreatedAtDesc(cardId);
        return list.stream().map(this::toHistoryDto).collect(Collectors.toList());
    }

    private TenantLoyaltyCardConfig createDefaultConfig(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant no encontrado: " + tenantId));

        TenantLoyaltyCardConfig config = TenantLoyaltyCardConfig.builder()
                .tenant(tenant)
                .cardTitle("Tarjeta de Fidelización")
                .cardSubtitle("Programa de Lealtad y Recompensas")
                .bgColorHex("#4338CA")
                .textColorHex("#FFFFFF")
                .accentColorHex("#F59E0B")
                .stampIcon("HAMBURGER")
                .totalStamps(8)
                .rewardMode("FULL_CARD")
                .rewardType("FREE_PRODUCT")
                .rewardTitle("Cortesía de la Casa")
                .rewardDescription("Completa tus sellos y gana una cortesía exclusiva.")
                .active(true)
                .build();

        return configRepository.save(config);
    }

    private LoyaltyCardConfigDto toConfigDto(TenantLoyaltyCardConfig c) {
        return LoyaltyCardConfigDto.builder()
                .id(c.getId())
                .tenantId(c.getTenant().getId())
                .cardTitle(c.getCardTitle())
                .cardSubtitle(c.getCardSubtitle())
                .bgColorHex(c.getBgColorHex())
                .textColorHex(c.getTextColorHex())
                .accentColorHex(c.getAccentColorHex())
                .stampIcon(c.getStampIcon())
                .totalStamps(c.getTotalStamps())
                .rewardMode(c.getRewardMode())
                .rewardType(c.getRewardType())
                .rewardValue(c.getRewardValue())
                .rewardProductId(c.getRewardProduct() != null ? c.getRewardProduct().getId() : null)
                .rewardProductName(c.getRewardProduct() != null ? c.getRewardProduct().getNombre() : null)
                .rewardTitle(c.getRewardTitle())
                .rewardDescription(c.getRewardDescription())
                .termsAndConditions(c.getTermsAndConditions())
                .active(c.getActive())
                .tenantName(c.getTenant().getNombreNegocio())
                .tenantLogoUrl(c.getTenant().getLogoUrl())
                .build();
    }

    private CustomerLoyaltyCardDto toCardDto(CustomerLoyaltyCard c, int totalStampsRequired) {
        return CustomerLoyaltyCardDto.builder()
                .id(c.getId())
                .tenantId(c.getTenant().getId())
                .customerId(c.getCustomer().getId())
                .customerName(c.getCustomer().getName())
                .customerEmail(c.getCustomer().getEmail())
                .customerPhone(c.getCustomer().getPhone())
                .currentStamps(c.getCurrentStamps())
                .totalStampsRequired(totalStampsRequired)
                .completedCardsCount(c.getCompletedCardsCount())
                .totalStampsAccumulated(c.getTotalStampsAccumulated())
                .hasPendingReward(c.getHasPendingReward())
                .pendingRewardDescription(c.getPendingRewardDescription())
                .googleWalletObjectId(c.getGoogleWalletObjectId())
                .googleWalletSaveUrl(c.getGoogleWalletSaveUrl())
                .lastStampAt(c.getLastStampAt())
                .lastRedemptionAt(c.getLastRedemptionAt())
                .build();
    }

    private LoyaltyStampHistoryDto toHistoryDto(LoyaltyStampHistory h) {
        return LoyaltyStampHistoryDto.builder()
                .id(h.getId())
                .cardId(h.getCard().getId())
                .customerId(h.getCustomer().getId())
                .customerName(h.getCustomer().getName())
                .actionType(h.getActionType())
                .stampsChange(h.getStampsChange())
                .stampsBefore(h.getStampsBefore())
                .stampsAfter(h.getStampsAfter())
                .rewardRedeemed(h.getRewardRedeemed())
                .notes(h.getNotes())
                .registeredByName(h.getRegisteredByUser() != null ? h.getRegisteredByUser().getFullName() : "Sistema")
                .createdAt(h.getCreatedAt())
                .build();
    }
}
