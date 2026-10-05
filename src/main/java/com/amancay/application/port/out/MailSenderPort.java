package com.amancay.application.port.out;

public interface MailSenderPort {
    void send(String to, String subject, String body);
}
