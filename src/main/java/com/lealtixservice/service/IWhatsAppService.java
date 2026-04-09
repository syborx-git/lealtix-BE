package com.lealtixservice.service;

import com.lealtixservice.dto.WhatsAppMessageRequest;
import com.lealtixservice.dto.WhatsAppMessageResponse;

public interface IWhatsAppService {

    WhatsAppMessageResponse sendTemplateMessage(WhatsAppMessageRequest request);
}
