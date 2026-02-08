package com.lealtixservice.controller;

import com.lealtixservice.entity.Campaign;
import com.lealtixservice.entity.CampaignEmail;
import com.lealtixservice.enums.CampaignEmailStatus;
import com.lealtixservice.enums.CampaignStatus;
import com.lealtixservice.repository.CampaignEmailRepository;
import com.lealtixservice.repository.CampaignRepository;
import com.lealtixservice.service.CampaignEmailOrchestrator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador de ejemplo para integración del sistema de envío de emails por campaña.
 * 
 * Demuestra:
 * 1. Iniciar envío de campaña (startCampaignSending)
 * 2. Obtener estado en tiempo real (getCampaignEmailStatus)
 * 3. Listar errores (getCampaignEmailErrors)
 * 4. Reintentar email fallido (retryFailedEmail)
 */
@RestController
@RequestMapping("/api/campaigns")
@Slf4j
public class CampaignEmailController {

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CampaignEmailRepository campaignEmailRepository;

    @Autowired
    private CampaignEmailOrchestrator orchestrator;

    @PostMapping("/{id}/send")
    public ResponseEntity<?> startCampaignSending(@PathVariable Long id) {
        log.info("📧 [CampaignEmailController] Solicitando inicio de envío para campaña: {}", id);

        try {
            Campaign campaign = campaignRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Campaña no encontrada: " + id));

            orchestrator.startCampaign(id);

            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Campaña iniciada. Los emails se enviarán automáticamente.",
                    "campaignId", id,
                    "currentStatus", CampaignStatus.SENDING
            ));

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ [CampaignEmailController] Argumento inválido: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "message", e.getMessage()
            ));
        } catch (IllegalStateException e) {
            log.warn("⚠️ [CampaignEmailController] Estado inválido: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("❌ [CampaignEmailController] Error inesperado: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "ERROR",
                    "message", "Error interno: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/campaigns/{id}/email-status
     * 
     * Obtiene el estado actual del envío de emails de una campaña.
     * 
     * Respuesta (200):
     * {
     *   "campaignId": 1,
     *   "status": "SENDING",
     *   "totalEmails": 150,
     *   "pending": 50,
     *   "sent": 95,
     *   "failed": 5,
     *   "progress": "93.33%",
     *   "finishedAt": null
     * }
     */
    @GetMapping("/{id}/email-status")
    public ResponseEntity<?> getCampaignEmailStatus(@PathVariable Long id) {
        log.debug("[CampaignEmailController] Consultando estado de emails para campaña: {}", id);

        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Campaña no encontrada: " + id));

        long total = campaignEmailRepository.countByCampaignId(id);
        long pending = campaignEmailRepository.countByCampaignIdAndStatus(id, CampaignEmailStatus.PENDING);
        long sent = campaign.getTotalSent() != null ? campaign.getTotalSent() : 0;
        long failed = campaign.getTotalFailed() != null ? campaign.getTotalFailed() : 0;

        double progress = total > 0 ? ((sent + failed) * 100.0 / total) : 0;

        Map<String, Object> response = new HashMap<>();
        response.put("campaignId", id);
        response.put("status", campaign.getStatus());
        response.put("totalEmails", total);
        response.put("pending", pending);
        response.put("sent", sent);
        response.put("failed", failed);
        response.put("progress", String.format("%.2f%%", progress));
        response.put("finishedAt", campaign.getFinishedAt());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/campaigns/{id}/email-errors
     * 
     * Obtiene la lista de emails que fallaron durante el envío.
     * 
     * Respuesta (200):
     * {
     *   "campaignId": 1,
     *   "failedCount": 5,
     *   "errors": [
     *     {
     *       "id": "550e8400-e29b-41d4-a716-446655440000",
     *       "recipient": "cliente@example.com",
     *       "attempts": 3,
     *       "error": "Connection timeout",
     *       "errorCode": "SEND_ERROR",
     *       "lastAttempt": "2026-02-05T14:30:00"
     *     }
     *   ]
     * }
     */
    @GetMapping("/{id}/email-errors")
    public ResponseEntity<?> getCampaignEmailErrors(@PathVariable Long id) {
        log.debug("[CampaignEmailController] Consultando errores de emails para campaña: {}", id);

        List<CampaignEmail> failedEmails = campaignEmailRepository
                .findByCampaignIdAndStatus(id, CampaignEmailStatus.FAILED);

        List<Map<String, Object>> errors = failedEmails.stream()
                .map(email -> {
                    Map<String, Object> errorMap = new HashMap<>();
                    errorMap.put("id", email.getId());
                    errorMap.put("recipient", email.getRecipientEmail());
                    errorMap.put("attempts", email.getAttemptCount());
                    errorMap.put("error", email.getProviderErrorMessage());
                    errorMap.put("errorCode", email.getProviderErrorCode());
                    errorMap.put("lastAttempt", email.getLastAttemptAt());
                    return errorMap;
                })
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("campaignId", id);
        response.put("failedCount", failedEmails.size());
        response.put("errors", errors);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/campaigns/{campaignId}/emails
     * 
     * Obtiene la lista paginada de todos los emails de una campaña.
     * 
     * Respuesta (200):
     * {
     *   "campaignId": 1,
     *   "total": 150,
     *   "emails": [
     *     {
     *       "id": "550e8400-e29b-41d4-a716-446655440000",
     *       "recipient": "cliente1@example.com",
     *       "status": "SENT",
     *       "attempts": 1,
     *       "sentAt": "2026-02-05T14:00:00"
     *     }
     *   ]
     * }
     */
    @GetMapping("/{campaignId}/emails")
    public ResponseEntity<?> getCampaignEmails(@PathVariable Long campaignId) {
        log.debug("[CampaignEmailController] Listando emails de campaña: {}", campaignId);

        List<CampaignEmail> emails = campaignEmailRepository.findByCampaignId(campaignId);

        List<Map<String, Object>> emailList = emails.stream()
                .map(email -> {
                    Map<String, Object> emailMap = new HashMap<>();
                    emailMap.put("id", email.getId());
                    emailMap.put("recipient", email.getRecipientEmail());
                    emailMap.put("status", email.getStatus());
                    emailMap.put("attempts", email.getAttemptCount());
                    emailMap.put("sentAt", email.getSentAt());
                    emailMap.put("failedAt", email.getBouncedAt());
                    return emailMap;
                })
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("campaignId", campaignId);
        response.put("total", emails.size());
        response.put("emails", emailList);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/campaigns/{campaignId}/emails/{emailId}/retry
     * 
     * Reinicia un email fallido para intentar envío nuevamente.
     * 
     * Respuesta (200):
     * {
     *   "status": "SUCCESS",
     *   "message": "Email reestablecido para reintentar",
     *   "emailId": "550e8400-e29b-41d4-a716-446655440000"
     * }
     */
    @PostMapping("/{campaignId}/emails/{emailId}/retry")
    public ResponseEntity<?> retryFailedEmail(@PathVariable Long campaignId, @PathVariable UUID emailId) {
        log.info("🔄 [CampaignEmailController] Reintentando email: {} de campaña: {}", emailId, campaignId);

        try {
            CampaignEmail email = campaignEmailRepository.findById(emailId)
                    .orElseThrow(() -> new IllegalArgumentException("Email no encontrado: " + emailId));

            if (email.getCampaign().getId() != campaignId) {
                throw new IllegalArgumentException("El email no pertenece a la campaña especificada");
            }

            // Resetear para reintentar
            email.setStatus(CampaignEmailStatus.PENDING);
            email.setNextAttemptAt(null); // Procesar inmediatamente
            email.setAttemptCount(0);
            campaignEmailRepository.save(email);

            log.info("✅ [CampaignEmailController] Email {} reestablecido para reintentar", emailId);

            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Email reestablecido para reintentar",
                    "emailId", emailId
            ));

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ [CampaignEmailController] Error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("❌ [CampaignEmailController] Error al reintentar: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "ERROR",
                    "message", "Error interno: " + e.getMessage()
            ));
        }
    }
}
