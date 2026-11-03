package com.amancay.infrastructure.adapter.in.web;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.amancay.application.port.in.SyncProviderPaymentUseCase;
import com.amancay.domain.exception.InsufficientStockException;

/**
 * Webhook de Mercado Pago (evento "Pagos"). Publico: lo llama Mercado Pago, no el front.
 * Responde 200 a todo lo que no hay que reintentar; si falla la API de Mercado Pago, el
 * error llega al ApiExceptionHandler y Mercado Pago reintenta mas tarde.
 */
@RestController
@RequestMapping("/api/webhooks/mercadopago")
public class MercadoPagoWebhookController {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookController.class);

    private final SyncProviderPaymentUseCase syncProviderPaymentUseCase;
    private final MercadoPagoSignatureValidator signatureValidator;

    public MercadoPagoWebhookController(SyncProviderPaymentUseCase syncProviderPaymentUseCase,
            MercadoPagoSignatureValidator signatureValidator) {
        this.syncProviderPaymentUseCase = syncProviderPaymentUseCase;
        this.signatureValidator = signatureValidator;
    }

    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "data.id", required = false) String dataId,
            @RequestHeader(name = "x-signature", required = false) String signature,
            @RequestHeader(name = "x-request-id", required = false) String requestId,
            @RequestBody(required = false) Map<String, Object> body) {

        String eventType = type != null ? type : bodyString(body, "type");
        String paymentId = dataId != null ? dataId : bodyDataId(body);

        // Solo nos interesan los pagos. Las notificaciones IPN viejas (?topic=...&id=...) se
        // ignoran: ver MERCADOPAGO.md (source_news=webhooks).
        if (!"payment".equals(eventType) || paymentId == null) {
            log.debug("Ignoring Mercado Pago notification type={} id={}", eventType, paymentId);
            return ResponseEntity.ok().build();
        }

        if (!signatureValidator.isValid(signature, requestId, dataId)) {
            log.warn("Rejected Mercado Pago notification with an invalid signature (payment {})", paymentId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Mercado Pago notification for payment {}", paymentId);
        try {
            syncProviderPaymentUseCase.syncFromProvider(paymentId);
        } catch (InsufficientStockException exception) {
            // Reintentar no lo arregla: el pago queda PENDIENTE y lo ve el admin.
            log.error("Mercado Pago payment {} approved but there is not enough stock", paymentId, exception);
        }
        return ResponseEntity.ok().build();
    }

    private static String bodyString(Map<String, Object> body, String key) {
        Object value = body == null ? null : body.get(key);
        return value == null ? null : value.toString();
    }

    private static String bodyDataId(Map<String, Object> body) {
        Object data = body == null ? null : body.get("data");
        if (data instanceof Map<?, ?> dataMap && dataMap.get("id") != null) {
            return dataMap.get("id").toString();
        }
        return null;
    }
}
