package com.lealtixservice.service;

import com.lealtixservice.entity.CampaignEmail;
import com.lealtixservice.enums.CampaignEmailStatus;
import com.lealtixservice.repository.CampaignEmailRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class CampaignEmailScheduler {

    @Autowired
    private CampaignEmailRepository campaignEmailRepository;

    @Autowired
    private CampaignEmailSender campaignEmailSender;

    @Value("${campaign.email.batch.size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${campaign.email.scheduler.interval:30000}")
    public void processPendingEmails() {
        log.info("🔄 [CampaignEmailScheduler] Iniciando procesamiento de emails pendientes...");

        try {
            Pageable pageable = PageRequest.of(0, batchSize);
            Page<CampaignEmail> pendingEmails = campaignEmailRepository.findPendingEmails(CampaignEmailStatus.PENDING, pageable);

            if (pendingEmails.isEmpty()) {
                log.debug("✅ [CampaignEmailScheduler] No hay emails pendientes para procesar");
                return;
            }

            log.info("📧 [CampaignEmailScheduler] Procesando {} emails pendientes (lote de {})",
                    pendingEmails.getContent().size(), batchSize);

            pendingEmails.getContent().forEach(email -> {
                try {
                    processSingleEmail(email);
                } catch (Exception e) {
                    log.error("❌ [CampaignEmailScheduler] Error procesando email {}: {}",
                            email.getId(), e.getMessage(), e);
                }
            });

            log.info("✅ [CampaignEmailScheduler] Lote procesado. {} emails restantes",
                    campaignEmailRepository.countByStatus(CampaignEmailStatus.PENDING));

        } catch (Exception e) {
            log.error("❌ [CampaignEmailScheduler] Error en job de procesamiento: {}", e.getMessage(), e);
        }
    }

    private void processSingleEmail(CampaignEmail email) {
        log.debug("[CampaignEmailScheduler] Procesando email: {}", email.getId());
        // Optimización: pasar solo el ID para que el sender recargue la entidad
        // con JOIN FETCH dentro de su propia transacción, evitando LazyInitializationException
        campaignEmailSender.sendEmailById(email.getId());
    }
}
