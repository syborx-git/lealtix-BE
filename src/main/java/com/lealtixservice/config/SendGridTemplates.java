package com.lealtixservice.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class SendGridTemplates {

    @Value("${sendgrid.templates.pre-registro}")
    private String preRegistroTemplate;

    @Value("${sendgrid.templates.welcome}")
    private String welcomeTemplate;

    @Value("${sendgrid.templates.factura}")
    private String facturaTemplate;

    public String getPreRegistroTemplate() {
        return preRegistroTemplate != null ? preRegistroTemplate.trim() : null;
    }

    public String getWelcomeTemplate() {
        return welcomeTemplate != null ? welcomeTemplate.trim() : null;
    }

    public String getFacturaTemplate() {
        return facturaTemplate != null ? facturaTemplate.trim() : null;
    }

}

