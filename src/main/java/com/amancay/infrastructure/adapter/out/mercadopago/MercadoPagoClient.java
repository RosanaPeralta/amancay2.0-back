package com.amancay.infrastructure.adapter.out.mercadopago;

import java.math.BigDecimal;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.amancay.application.port.out.PayableOrderPort.PayableOrder;
import com.amancay.application.port.out.PaymentProviderPort;
import com.amancay.infrastructure.config.MercadoPagoProperties;

// Cliente HTTP de la API de Mercado Pago, sin SDK. Las respuestas se leen como Map para
// no depender de todos los campos que devuelve la API.
@Component
public class MercadoPagoClient implements MercadoPagoCheckout, PaymentProviderPort {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON_OBJECT =
            new ParameterizedTypeReference<>() {
            };

    private final MercadoPagoProperties properties;
    private final RestClient restClient;

    public MercadoPagoClient(MercadoPagoProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().baseUrl(properties.apiBaseUrl()).build();
    }

    @Override
    public String createCheckout(PayableOrder order) {
        String orderId = order.id().toString();
        String returnUrl = properties.returnUrl();

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", orderId);
        item.put("title", "Orden Amancay " + shortId(order.id()));
        item.put("quantity", 1);
        item.put("currency_id", "ARS");
        item.put("unit_price", order.total());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", List.of(item));
        // Con esto se vuelve a encontrar la orden desde el pago (webhook o verificacion).
        body.put("external_reference", orderId);
        // Mercado Pago no redirige a localhost/127.0.0.1: en ese caso no se mandan back_urls y
        // el front abre el checkout en otra pestaña y espera el pago (ver MERCADOPAGO.md).
        if (!isLocal(returnUrl)) {
            body.put("back_urls", Map.of("success", returnUrl, "failure", returnUrl, "pending", returnUrl));
            if (returnUrl.startsWith("https://")) {
                body.put("auto_return", "approved");
            }
        }
        if (properties.hasNotificationUrl()) {
            body.put("notification_url", properties.notificationUrl());
        }

        Map<String, Object> response = restClient.post()
                .uri("/checkout/preferences")
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JSON_OBJECT);

        String initPoint = response == null ? null : asString(response.get("init_point"));
        if (initPoint == null) {
            throw new IllegalStateException("Mercado Pago did not return a checkout URL");
        }
        return initPoint;
    }

    @Override
    public Optional<ProviderPayment> findPayment(String providerPaymentId) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri("/v1/payments/{id}", providerPaymentId)
                    .header(HttpHeaders.AUTHORIZATION, bearer())
                    .retrieve()
                    .body(JSON_OBJECT);
            return Optional.ofNullable(response).map(MercadoPagoClient::toProviderPayment);
        } catch (HttpClientErrorException.NotFound exception) {
            // Ej: la "notificacion de prueba" del panel de Mercado Pago usa un id inventado.
            return Optional.empty();
        }
    }

    @Override
    public List<ProviderPayment> findPaymentsByExternalReference(String externalReference) {
        Map<String, Object> response = restClient.get()
                .uri(builder -> builder.path("/v1/payments/search")
                        .queryParam("external_reference", externalReference)
                        .queryParam("sort", "date_created")
                        .queryParam("criteria", "desc")
                        .build())
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .retrieve()
                .body(JSON_OBJECT);

        if (response == null || !(response.get("results") instanceof List<?> results)) {
            return List.of();
        }
        return results.stream()
                .filter(Map.class::isInstance)
                .map(result -> toProviderPayment(asJsonObject(result)))
                .toList();
    }

    private String bearer() {
        if (!properties.configured()) {
            throw new IllegalStateException("Mercado Pago is not configured (missing MP_ACCESS_TOKEN)");
        }
        return "Bearer " + properties.accessToken();
    }

    private static ProviderPayment toProviderPayment(Map<String, Object> json) {
        Object amount = json.get("transaction_amount");
        return new ProviderPayment(
                asString(json.get("id")),
                asString(json.get("status")),
                amount == null ? null : new BigDecimal(amount.toString()),
                asString(json.get("external_reference")));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asJsonObject(Object value) {
        return (Map<String, Object>) value;
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private static boolean isLocal(String url) {
        String host = URI.create(url).getHost();
        return host == null || host.equals("localhost") || host.equals("127.0.0.1");
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }
}
