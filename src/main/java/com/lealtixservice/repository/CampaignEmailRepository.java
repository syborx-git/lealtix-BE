package com.lealtixservice.repository;

import com.lealtixservice.entity.CampaignEmail;
import com.lealtixservice.enums.CampaignEmailStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignEmailRepository extends JpaRepository<CampaignEmail, UUID> {
    
    List<CampaignEmail> findByCampaignId(Long campaignId);
    
    List<CampaignEmail> findByCampaignIdAndStatus(Long campaignId, CampaignEmailStatus status);
    
    long countByCampaignId(Long campaignId);
    
    long countByCampaignIdAndStatus(Long campaignId, CampaignEmailStatus status);
    
    Page<CampaignEmail> findByStatus(CampaignEmailStatus status, Pageable pageable);
    
    @Query("SELECT ce FROM CampaignEmail ce WHERE ce.status = :status AND " +
           "(ce.nextAttemptAt IS NULL OR ce.nextAttemptAt <= CURRENT_TIMESTAMP) " +
           "ORDER BY ce.createdAt ASC")
    Page<CampaignEmail> findPendingEmails(@Param("status") CampaignEmailStatus status, Pageable pageable);
    
    /**
     * Busca un CampaignEmail por ID con JOIN FETCH de Campaign y PromotionReward.
     * Esto previene LazyInitializationException al cargar las asociaciones lazy dentro de la transacción.
     */
    @Query("SELECT ce FROM CampaignEmail ce " +
           "JOIN FETCH ce.campaign c " +
           "LEFT JOIN FETCH c.promotionReward " +
           "WHERE ce.id = :id")
    Optional<CampaignEmail> findByIdWithCampaignFetch(@Param("id") UUID id);
    
    @Query("SELECT COUNT(ce) FROM CampaignEmail ce WHERE ce.campaign.id = :campaignId AND ce.status = :status")
    long countByStatusForCampaign(@Param("campaignId") Long campaignId, @Param("status") CampaignEmailStatus status);
    
    long countByStatus(CampaignEmailStatus status);
}
