package com.lealtixservice.enums;

public enum CampaignStatus {
    DRAFT,
    READY,
    SENDING,
    ACTIVE,
    INACTIVE,
    SCHEDULED;

    public String getValue() {
        return name();
    }
}

