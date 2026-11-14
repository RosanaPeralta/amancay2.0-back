package com.amancay.infrastructure.adapter.out.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.amancay.application.port.out.MailSenderPort;

// Manda los mails por SMTP (spring.mail.*). Activo con amancay.mail.enabled=true; si no,
// queda LoggingMailSender. Un error de SMTP se propaga para que la cola reintente.
@Component
@ConditionalOnProperty(name = "amancay.mail.enabled", havingValue = "true")
public class SmtpMailSender implements MailSenderPort {

    private final JavaMailSender javaMailSender;
    private final String from;

    public SmtpMailSender(JavaMailSender javaMailSender, @Value("${amancay.mail.from}") String from) {
        this.javaMailSender = javaMailSender;
        this.from = from;
    }

    @Override
    public void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        javaMailSender.send(message);
    }
}
