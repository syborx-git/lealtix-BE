package com.lealtixservice.service;

import com.lealtixservice.dto.*;

import java.util.List;

public interface LoyaltyCardService {

    LoyaltyCardConfigDto getConfigByTenantId(Long tenantId);

    LoyaltyCardConfigDto saveOrUpdateConfig(Long tenantId, LoyaltyCardConfigDto dto);

    List<CustomerLoyaltyCardDto> getCustomerCardsByTenant(Long tenantId);

    CustomerLoyaltyCardDto getOrCreateCustomerCard(Long tenantId, Long customerId);

    CustomerLoyaltyCardDto addStamps(LoyaltyStampRequest request);

    CustomerLoyaltyCardDto redeemReward(LoyaltyRedeemRewardRequest request);

    List<LoyaltyStampHistoryDto> getHistoryByTenant(Long tenantId);

    List<LoyaltyStampHistoryDto> getHistoryByCard(Long cardId);

    String getGoogleWalletPassUrl(Long tenantId, Long customerId);
}

