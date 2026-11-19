package com.lealtixservice.service.impl;

import com.lealtixservice.entity.CustomerLoyaltyCard;
import com.lealtixservice.entity.Tenant;
import com.lealtixservice.entity.TenantCustomer;
import com.lealtixservice.entity.TenantLoyaltyCardConfig;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoyaltyCardBannerServiceTest {

    @Test
    void testRenderBannerPng() throws IOException {
        LoyaltyCardBannerServiceImpl service = new LoyaltyCardBannerServiceImpl(null, null);

        TenantLoyaltyCardConfig config = TenantLoyaltyCardConfig.builder()
                .cardTitle("Petra Club Gourmet")
                .cardSubtitle("Tarjeta de Sellos y Beneficios Exclusivos")
                .bgColorHex("#1E1B4B")
                .stampIcon("STAR")
                .totalStamps(8)
                .rewardTitle("20% de descuento en tu siguiente compra")
                .build();

        Tenant tenant = Tenant.builder().id(1L).nombreNegocio("Restaurante Petra").build();
        TenantCustomer customer = TenantCustomer.builder().id(1L).name("Jorge Martínez").build();
        CustomerLoyaltyCard card = CustomerLoyaltyCard.builder()
                .id(1L)
                .tenant(tenant)
                .customer(customer)
                .currentStamps(6)
                .build();

        byte[] pngBytes = service.renderBanner(card, config, 6);

        assertNotNull(pngBytes, "PNG bytes should not be null");
        assertTrue(pngBytes.length > 1000, "PNG bytes should contain valid image data");

        // Save sample to disk
        File outDir = new File("scratch");
        if (!outDir.exists()) outDir.mkdirs();
        File outFile = new File(outDir, "test_wallet_hero_6_stamps.png");
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            fos.write(pngBytes);
        }
        System.out.println(">>> Sample hero banner written to: " + outFile.getAbsolutePath() + " (size: " + pngBytes.length + " bytes)");
    }
}
