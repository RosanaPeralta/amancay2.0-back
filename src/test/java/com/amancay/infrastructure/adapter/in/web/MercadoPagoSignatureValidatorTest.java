package com.amancay.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.amancay.infrastructure.config.MercadoPagoProperties;

class MercadoPagoSignatureValidatorTest {

    private static final String SECRET = "test-secret";
    // HMAC-SHA256("test-secret", "id:123456;request-id:req-1;ts:1700000000;"), calculado aparte.
    private static final String V1 = "e9a39bbdd481339f443a07c1c3d7f4fd07f3bb23ec03ae625222043493c25d3f";

    private final MercadoPagoSignatureValidator validator = validatorWithSecret(SECRET);

    @Test
    void acceptsASignatureBuiltWithTheSecret() {
        assertThat(validator.isValid("ts=1700000000,v1=" + V1, "req-1", "123456")).isTrue();
    }

    @Test
    void rejectsASignatureForAnotherPayment() {
        assertThat(validator.isValid("ts=1700000000,v1=" + V1, "req-1", "999999")).isFalse();
    }

    @Test
    void rejectsAMissingOrMalformedHeader() {
        assertThat(validator.isValid(null, "req-1", "123456")).isFalse();
        assertThat(validator.isValid("garbage", "req-1", "123456")).isFalse();
        assertThat(validator.isValid("ts=1700000000", "req-1", "123456")).isFalse();
    }

    @Test
    void withoutASecretEveryNotificationIsAccepted() {
        assertThat(validatorWithSecret("").isValid(null, null, "123456")).isTrue();
    }

    private static MercadoPagoSignatureValidator validatorWithSecret(String secret) {
        return new MercadoPagoSignatureValidator(new MercadoPagoProperties(null, secret, null, null, null, null));
    }
}
