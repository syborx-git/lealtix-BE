package com.lealtixservice.repository;

import com.lealtixservice.entity.Campaign;
import com.lealtixservice.enums.CampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    List<Campaign> findByBusinessId(Long businessId);
    List<Campaign> findByBusinessIdAndStatus(Long businessId, CampaignStatus status);
    List<Campaign> findByStatus(CampaignStatus status);
    List<Campaign> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate start, LocalDate end);

    // Métodos para borradores
    List<Campaign> findByBusinessIdAndIsDraftOrderByUpdatedAtDesc(Long businessId, boolean isDraft);
    List<Campaign> findByBusinessIdAndIsDraftFalseOrderByCreatedAtDesc(Long businessId);

    // Precargar template para operaciones donde necesitamos acceder a la relación (evita lazy-loading adicional)
    @Query("select c from Campaign c left join fetch c.template where c.id = :id")
    Optional<Campaign> findByIdWithTemplate(@Param("id") Long id);

    // Nuevo: existencia de campaña activa de bienvenida (template.category = :category AND template.name = :name)
    @Query("select case when count(c) > 0 then true else false end from Campaign c join c.template t " +
           "where c.businessId = :businessId and c.status = :status " +
           "and (c.endDate is null or c.endDate >= CURRENT_DATE) " +
           "and t.category = :category and t.name = :name")
    boolean existsActiveWelcomeCampaignForTenant(@Param("businessId") Long businessId,
                                                @Param("status") CampaignStatus status,
                                                @Param("category") String category,
                                                @Param("name") String name);

    // Nuevo: obtener la primera campaña de bienvenida activa (con template y promotionReward precargados)
    @Query("select distinct c from Campaign c " +
           "left join fetch c.template t " +
           "left join fetch c.promotionReward pr " +
           "where c.businessId = :businessId and c.status = :status " +
           "and (c.endDate is null or c.endDate >= CURRENT_DATE) " +
           "and t.category = :category and t.name = :name")
    List<Campaign> findActiveWelcomeCampaignsForTenant(@Param("businessId") Long businessId,
                                                       @Param("status") CampaignStatus status,
                                                       @Param("category") String category,
                                                       @Param("name") String name);

    // Métricas de envío de emails
    @Modifying
    @Transactional
    @Query(value = "UPDATE campaign SET total_sent = " +
            "(SELECT COUNT(*) FROM campaign_email WHERE campaign_id = :campaignId AND status = 'SENT') " +
            "WHERE id = :campaignId", nativeQuery = true)
    void updateTotalSent(@Param("campaignId") Long campaignId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE campaign SET total_failed = " +
            "(SELECT COUNT(*) FROM campaign_email WHERE campaign_id = :campaignId AND status = 'FAILED') " +
            "WHERE id = :campaignId", nativeQuery = true)
    void updateTotalFailed(@Param("campaignId") Long campaignId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE campaign SET finished_at = CURRENT_TIMESTAMP " +
            "WHERE id = :campaignId AND " +
            "(SELECT COUNT(*) FROM campaign_email WHERE campaign_id = :campaignId AND status = 'PENDING') = 0", 
            nativeQuery = true)
    void markAsFinished(@Param("campaignId") Long campaignId);
}
