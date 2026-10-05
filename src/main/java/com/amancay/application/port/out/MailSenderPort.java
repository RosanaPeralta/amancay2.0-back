package com.amancay.application.port.out;

// Mecanismo de envio, sin opinion sobre el contenido: quien construye el asunto/cuerpo
// (ej: un listener de dominio) no sabe si esto termina en un log o en un mail real.
public interface MailSenderPort {
    void send(String to, String subject, String body);
}
