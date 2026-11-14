package com.amancay.application.port.out;

// Un mail listo para mandar. htmlBody es opcional: si esta, se manda HTML con body como
// alternativa en texto plano para los clientes que no muestran HTML.
public record MailContent(String subject, String body, String htmlBody) {

    // El HTML referencia el logo como "cid:" + LOGO_CID; el adaptador SMTP lo adjunta inline.
    public static final String LOGO_CID = "amancay-logo";

    public MailContent(String subject, String body) {
        this(subject, body, null);
    }

    public boolean hasHtml() {
        return htmlBody != null && !htmlBody.isBlank();
    }
}
