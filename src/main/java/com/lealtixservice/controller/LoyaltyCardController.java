package com.lealtixservice.controller;

import com.lealtixservice.dto.*;
import com.lealtixservice.service.LoyaltyCardService;
import com.lealtixservice.service.LoyaltyCardBannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loyalty-card")
@RequiredArgsConstructor
@Slf4j
public class LoyaltyCardController {

    private final LoyaltyCardService loyaltyCardService;
    private final LoyaltyCardBannerService bannerService;

    @GetMapping("/config/{tenantId}")
    public ResponseEntity<LoyaltyCardConfigDto> getConfig(@PathVariable Long tenantId) {
        return ResponseEntity.ok(loyaltyCardService.getConfigByTenantId(tenantId));
    }

    @PutMapping("/config/{tenantId}")
    public ResponseEntity<LoyaltyCardConfigDto> updateConfig(
            @PathVariable Long tenantId,
            @RequestBody LoyaltyCardConfigDto dto) {
        return ResponseEntity.ok(loyaltyCardService.saveOrUpdateConfig(tenantId, dto));
    }

    @GetMapping("/customers/{tenantId}")
    public ResponseEntity<List<CustomerLoyaltyCardDto>> getCustomerCards(@PathVariable Long tenantId) {
        return ResponseEntity.ok(loyaltyCardService.getCustomerCardsByTenant(tenantId));
    }

    @GetMapping("/customers/{tenantId}/{customerId}")
    public ResponseEntity<CustomerLoyaltyCardDto> getCustomerCard(
            @PathVariable Long tenantId,
            @PathVariable Long customerId) {
        return ResponseEntity.ok(loyaltyCardService.getOrCreateCustomerCard(tenantId, customerId));
    }

    @PostMapping("/stamp/add")
    public ResponseEntity<CustomerLoyaltyCardDto> addStamps(@RequestBody LoyaltyStampRequest request) {
        return ResponseEntity.ok(loyaltyCardService.addStamps(request));
    }

    @PostMapping("/reward/redeem")
    public ResponseEntity<CustomerLoyaltyCardDto> redeemReward(@RequestBody LoyaltyRedeemRewardRequest request) {
        return ResponseEntity.ok(loyaltyCardService.redeemReward(request));
    }

    @GetMapping("/history/{tenantId}")
    public ResponseEntity<List<LoyaltyStampHistoryDto>> getHistoryByTenant(@PathVariable Long tenantId) {
        return ResponseEntity.ok(loyaltyCardService.getHistoryByTenant(tenantId));
    }

    @GetMapping("/history/card/{cardId}")
    public ResponseEntity<List<LoyaltyStampHistoryDto>> getHistoryByCard(@PathVariable Long cardId) {
        return ResponseEntity.ok(loyaltyCardService.getHistoryByCard(cardId));
    }

    @GetMapping("/google-wallet/pass-url/{tenantId}/{customerId}")
    public ResponseEntity<java.util.Map<String, String>> getGoogleWalletPassUrl(
            @PathVariable Long tenantId,
            @PathVariable Long customerId) {
        String saveUrl = loyaltyCardService.getGoogleWalletPassUrl(tenantId, customerId);
        return ResponseEntity.ok(java.util.Map.of("saveUrl", saveUrl != null ? saveUrl : ""));
    }

    @GetMapping(value = "/banner/{tenantId}/{customerId}.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getPassBanner(
            @PathVariable Long tenantId,
            @PathVariable Long customerId,
            @RequestParam(value = "stamps", required = false) Integer stampsParam) {
        byte[] imageBytes = bannerService.generateBannerPng(tenantId, customerId, stampsParam);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .body(imageBytes);
    }
}

