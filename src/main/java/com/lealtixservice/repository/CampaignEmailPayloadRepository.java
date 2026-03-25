package com.lealtixservice.repository;

import com.lealtixservice.entity.CampaignEmailPayload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CampaignEmailPayloadRepository extends JpaRepository<CampaignEmailPayload, UUID> {
    
    List<CampaignEmailPayload> findByCampaignEmailId(UUID campaignEmailId);
    
    long countByCampaignEmailId(UUID campaignEmailId);
}
