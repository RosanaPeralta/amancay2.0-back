package com.amancay.application.port.out;

public interface MailSenderPort {
    void send(String to, String subject, String body);

    // Los adaptadores que saben mandar HTML lo sobrescriben; el resto manda el texto plano.
    default void send(String to, MailContent content) {
        send(to, content.subject(), content.body());
    }
}
