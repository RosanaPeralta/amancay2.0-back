package com.amancay.infrastructure.adapter.out.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.amancay.application.port.out.MailSenderPort;

@Component
@ConditionalOnProperty(name = "amancay.mail.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingMailSender implements MailSenderPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingMailSender.class);

    @Override
    public void send(String to, String subject, String body) {
        log.info("Mail a {} | asunto: {} | cuerpo: {}", to, subject, body);
    }
}
