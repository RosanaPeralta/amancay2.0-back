package com.amancay.infrastructure.adapter.out.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.amancay.application.port.out.MailSenderPort;

/**
 * Placeholder: solo loguea, no manda mails de verdad. Se activa por defecto (o con
 * {@code amancay.mail.enabled=false}) mientras no exista un envio real (SMTP/SendGrid). El
 * dia que llegue esa implementacion, activarla con {@code amancay.mail.enabled=true}
 * deshabilita este bean sin tocar a quien lo llama (mismo mecanismo que
 * {@link com.amancay.infrastructure.adapter.out.purchase.AlwaysAllowPurchaseVerifier}).
 */
@Component
@ConditionalOnProperty(name = "amancay.mail.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingMailSender implements MailSenderPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingMailSender.class);

    @Override
    public void send(String to, String subject, String body) {
        log.info("Mail a {} | asunto: {} | cuerpo: {}", to, subject, body);
    }
}
