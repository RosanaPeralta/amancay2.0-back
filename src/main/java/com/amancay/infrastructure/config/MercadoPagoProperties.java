package com.amancay.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Configuracion de Mercado Pago (ver MERCADOPAGO.md). Sin accessToken la app arranca
// igual; solo falla el pago con MERCADO_PAGO.
@ConfigurationProperties(prefix = "mercadopago")
public record MercadoPagoProperties(
        String accessToken,
        String webhookSecret,
        String notificationUrl,
        String frontendUrl,
        String returnPath,
        String apiBaseUrl) {

    public MercadoPagoProperties {
        apiBaseUrl = isBlank(apiBaseUrl) ? "https://api.mercadopago.com" : apiBaseUrl;
        frontendUrl = isBlank(frontendUrl) ? "http://localhost:5173" : stripTrailingSlash(frontendUrl);
        returnPath = isBlank(returnPath) ? "/checkout/resultado" : returnPath;
    }

    public boolean configured() {
        return !isBlank(accessToken);
    }

    public boolean hasWebhookSecret() {
        return !isBlank(webhookSecret);
    }

    public boolean hasNotificationUrl() {
        return !isBlank(notificationUrl);
    }

    public String returnUrl() {
        return frontendUrl + returnPath;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
