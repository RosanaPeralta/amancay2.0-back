package com.amancay.infrastructure.adapter.in.event;

// Separa el armado del texto de un mail (una clase por tipo de aviso) de cuando se decide
// mandarlo y de como se manda (MailSenderPort), que son responsabilidad del listener.
public record MailContent(String subject, String body) {
}
