package com.amancay.infrastructure.adapter.in.web;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.amancay.infrastructure.config.MercadoPagoProperties;

// Valida el header x-signature de los webhooks de Mercado Pago: HMAC-SHA256 con la clave
// secreta de la aplicacion sobre "id:{data.id};request-id:{x-request-id};ts:{ts};".
// Sin clave configurada acepta todo (desarrollo): igual es seguro porque el pago se vuelve
// a leer de la API de Mercado Pago antes de aplicarlo.
@Component
public class MercadoPagoSignatureValidator {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoSignatureValidator.class);

    private final String secret;

    public MercadoPagoSignatureValidator(MercadoPagoProperties properties) {
        this.secret = properties.webhookSecret();
        if (!properties.hasWebhookSecret()) {
            log.warn("MP_WEBHOOK_SECRET is not set: Mercado Pago webhook signatures are NOT validated");
        }
    }

    public boolean isValid(String xSignature, String xRequestId, String dataId) {
        if (secret == null || secret.isBlank()) {
            return true;
        }
        if (xSignature == null || xSignature.isBlank()) {
            return false;
        }

        String ts = null;
        String v1 = null;
        for (String part : xSignature.split(",")) {
            String[] keyValue = part.trim().split("=", 2);
            if (keyValue.length != 2) {
                continue;
            }
            if (keyValue[0].equals("ts")) {
                ts = keyValue[1].trim();
            } else if (keyValue[0].equals("v1")) {
                v1 = keyValue[1].trim();
            }
        }
        if (ts == null || v1 == null) {
            return false;
        }

        // Segun la documentacion, las partes que no vienen en la notificacion se omiten.
        StringBuilder manifest = new StringBuilder();
        if (dataId != null && !dataId.isBlank()) {
            manifest.append("id:").append(dataId.toLowerCase(Locale.ROOT)).append(';');
        }
        if (xRequestId != null && !xRequestId.isBlank()) {
            manifest.append("request-id:").append(xRequestId).append(';');
        }
        manifest.append("ts:").append(ts).append(';');

        byte[] expected = hmacHex(secret, manifest.toString()).getBytes(StandardCharsets.UTF_8);
        byte[] received = v1.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, received);
    }

    private static String hmacHex(String key, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HmacSHA256 is not available", exception);
        }
    }
}
