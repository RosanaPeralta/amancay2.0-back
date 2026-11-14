package com.amancay.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Reintentos de los consumidores de RabbitMQ antes de mandar un mensaje a la dead letter queue.
@ConfigurationProperties(prefix = "amancay.messaging")
public record MessagingProperties(Retry retry) {

    public MessagingProperties {
        retry = retry == null ? new Retry(2, Duration.ofSeconds(1), 2.0) : retry;
    }

    public record Retry(int maxRetries, Duration initialDelay, double multiplier) {

        public Retry {
            initialDelay = initialDelay == null ? Duration.ofSeconds(1) : initialDelay;
            multiplier = multiplier < 1 ? 1 : multiplier;
        }
    }
}
