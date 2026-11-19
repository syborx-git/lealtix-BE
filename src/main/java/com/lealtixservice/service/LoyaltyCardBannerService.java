package com.lealtixservice.service;

import com.lealtixservice.entity.CustomerLoyaltyCard;
import com.lealtixservice.entity.TenantLoyaltyCardConfig;

public interface LoyaltyCardBannerService {

    byte[] generateBannerPng(Long tenantId, Long customerId, Integer overrideStamps);

    byte[] renderBanner(CustomerLoyaltyCard card, TenantLoyaltyCardConfig config, Integer overrideStamps);
}
