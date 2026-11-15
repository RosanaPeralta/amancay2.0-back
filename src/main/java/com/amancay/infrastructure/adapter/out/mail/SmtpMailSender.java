package com.amancay.infrastructure.adapter.out.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.amancay.application.port.out.MailContent;
import com.amancay.application.port.out.MailSenderPort;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

// Manda los mails por SMTP (spring.mail.*). Activo con amancay.mail.enabled=true; si no,
// queda LoggingMailSender. Un error de SMTP se propaga para que la cola reintente.
@Component
@ConditionalOnProperty(name = "amancay.mail.enabled", havingValue = "true")
public class SmtpMailSender implements MailSenderPort {

    // Va adjunto al mail (inline): un cliente de mail no puede cargar imagenes de localhost.
    private static final ClassPathResource LOGO = new ClassPathResource("mail/amancay_mark.png");

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

    // HTML con el texto plano como alternativa; sin HTML, un mail de texto comun.
    @Override
    public void send(String to, MailContent content) {
        if (!content.hasHtml()) {
            send(to, content.subject(), content.body());
            return;
        }
        MimeMessage message = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(content.subject());
            helper.setText(content.body(), content.htmlBody());
            if (content.htmlBody().contains("cid:" + MailContent.LOGO_CID)) {
                helper.addInline(MailContent.LOGO_CID, LOGO, "image/png");
            }
        } catch (MessagingException e) {
            throw new MailPreparationException("No se pudo armar el mail para " + to, e);
        }
        javaMailSender.send(message);
    }
}
