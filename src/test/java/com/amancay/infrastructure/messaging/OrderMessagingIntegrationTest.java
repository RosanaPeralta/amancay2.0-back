package com.amancay.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.amancay.application.port.in.AdvanceOrderStatusCommand;
import com.amancay.application.port.in.ApplyOrderStatusCommandUseCase;
import com.amancay.application.port.out.SendOrderCommandPort;
import com.amancay.domain.exception.InvalidOrderStatusTransitionException;
import com.amancay.domain.model.OrderStatus;
import com.amancay.domain.model.OrderTransitionTrigger;
import com.amancay.infrastructure.config.RabbitMqConfig;

// Los adaptadores de mensajeria contra un RabbitMQ real, con los casos de uso reemplazados
// por fakes: comandos, mensajes de productores externos, reintentos y dead letter queue.
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = OrderMessagingIntegrationTest.MessagingTestApp.class, properties = {
        "amancay.messaging.retry.max-retries=2",
        "amancay.messaging.retry.initial-delay=PT0.05S" })
class OrderMessagingIntegrationTest {

    @Container
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:4.1-management");

    @DynamicPropertySource
    static void rabbitProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
    }

    @Autowired
    private SendOrderCommandPort sendOrderCommandPort;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private FakeApplyOrderStatusCommand applyUseCase;

    @BeforeEach
    void reset() {
        applyUseCase.reset();
        amqpAdmin.purgeQueue(RabbitMqTopology.STATUS_COMMANDS_DLQ, false);
    }

    @Test
    void commandSentByTheBackendIsAppliedByTheConsumer() {
        AdvanceOrderStatusCommand command = new AdvanceOrderStatusCommand(UUID.randomUUID(), UUID.randomUUID(),
                OrderStatus.DESPACHADO, "admin:test", Instant.parse("2026-11-14T12:00:00Z"));

        sendOrderCommandPort.send(command);

        waitUntil(() -> !applyUseCase.received.isEmpty());
        assertThat(applyUseCase.received).containsExactly(command);
    }

    @Test
    void plainJsonFromAnExternalProducerIsUnderstood() {
        UUID orderId = UUID.randomUUID();

        sendRawCommand("""
                {"messageId":"%s","orderId":"%s","targetStatus":"ENTREGADO","requestedBy":"logistics-console",
                 "requestedAt":"2026-11-14T15:00:00Z"}
                """.formatted(UUID.randomUUID(), orderId));

        waitUntil(() -> !applyUseCase.received.isEmpty());
        assertThat(applyUseCase.received).singleElement().satisfies(command -> {
            assertThat(command.orderId()).isEqualTo(orderId);
            assertThat(command.targetStatus()).isEqualTo(OrderStatus.ENTREGADO);
            assertThat(command.requestedBy()).isEqualTo("logistics-console");
        });
    }

    @Test
    void invalidTransitionGoesStraightToTheDeadLetterQueue() {
        applyUseCase.behavior = command -> {
            throw new InvalidOrderStatusTransitionException(OrderStatus.CREADO, command.targetStatus(),
                    OrderTransitionTrigger.FULFILLMENT);
        };
        UUID orderId = UUID.randomUUID();

        sendRawCommand("""
                {"orderId":"%s","targetStatus":"DESPACHADO"}
                """.formatted(orderId));

        Message dead = rabbitTemplate.receive(RabbitMqTopology.STATUS_COMMANDS_DLQ, 10_000);
        assertThat(dead).isNotNull();
        assertThat(new String(dead.getBody(), StandardCharsets.UTF_8)).contains(orderId.toString());
        assertThat((String) dead.getMessageProperties().getHeader(RepublishMessageRecoverer.X_EXCEPTION_MESSAGE))
                .contains("Cannot transition order from CREADO to DESPACHADO");
        assertThat(applyUseCase.received).hasSize(1);
    }

    @Test
    void malformedJsonGoesToTheDeadLetterQueueWithoutReachingTheUseCase() {
        sendRawCommand("{ esto no es json");

        Message dead = rabbitTemplate.receive(RabbitMqTopology.STATUS_COMMANDS_DLQ, 10_000);
        assertThat(dead).isNotNull();
        assertThat(applyUseCase.received).isEmpty();
    }

    @Test
    void incompleteCommandGoesToTheDeadLetterQueue() {
        sendRawCommand("""
                {"targetStatus":"DESPACHADO"}
                """);

        Message dead = rabbitTemplate.receive(RabbitMqTopology.STATUS_COMMANDS_DLQ, 10_000);
        assertThat(dead).isNotNull();
        assertThat((String) dead.getMessageProperties().getHeader(RepublishMessageRecoverer.X_EXCEPTION_MESSAGE))
                .contains("orderId and targetStatus are required");
        assertThat(applyUseCase.received).isEmpty();
    }

    @Test
    void transientFailureIsRetriedAndThenDeadLettered() {
        applyUseCase.behavior = command -> {
            throw new IllegalStateException("database is down");
        };

        sendRawCommand("""
                {"orderId":"%s","targetStatus":"DESPACHADO"}
                """.formatted(UUID.randomUUID()));

        Message dead = rabbitTemplate.receive(RabbitMqTopology.STATUS_COMMANDS_DLQ, 10_000);
        assertThat(dead).isNotNull();
        assertThat(applyUseCase.received).hasSize(3);
    }

    @Test
    void transientFailureThatRecoversIsNotDeadLettered() {
        applyUseCase.behavior = command -> {
            if (applyUseCase.received.size() < 2) {
                throw new IllegalStateException("database is down");
            }
            return ApplyOrderStatusCommandUseCase.Result.APPLIED;
        };

        sendRawCommand("""
                {"orderId":"%s","targetStatus":"DESPACHADO"}
                """.formatted(UUID.randomUUID()));

        waitUntil(() -> applyUseCase.received.size() == 2);
        assertThat(rabbitTemplate.receive(RabbitMqTopology.STATUS_COMMANDS_DLQ, 500)).isNull();
    }

    private void sendRawCommand(String json) {
        Message message = MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8))
                .setContentType("application/json")
                .build();
        rabbitTemplate.send(RabbitMqTopology.ORDERS_EXCHANGE, RabbitMqTopology.ADVANCE_ORDER_ROUTING_KEY, message);
    }

    private static void waitUntil(BooleanSupplier condition) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        while (!condition.getAsBoolean()) {
            if (Instant.now().isAfter(deadline)) {
                throw new AssertionError("Condition not met within 10 seconds");
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError(exception);
            }
        }
    }

    static final class FakeApplyOrderStatusCommand implements ApplyOrderStatusCommandUseCase {

        final List<AdvanceOrderStatusCommand> received = new CopyOnWriteArrayList<>();
        volatile Function<AdvanceOrderStatusCommand, Result> behavior = command -> Result.APPLIED;

        void reset() {
            received.clear();
            behavior = command -> Result.APPLIED;
        }

        @Override
        public Result apply(AdvanceOrderStatusCommand command) {
            received.add(command);
            return behavior.apply(command);
        }
    }

    @SpringBootConfiguration
    @ImportAutoConfiguration({ JacksonAutoConfiguration.class, RabbitAutoConfiguration.class })
    @Import(RabbitMqConfig.class)
    @ComponentScan(basePackages = {
            "com.amancay.infrastructure.adapter.in.messaging",
            "com.amancay.infrastructure.adapter.out.messaging" })
    static class MessagingTestApp {

        @Bean
        FakeApplyOrderStatusCommand applyOrderStatusCommandUseCase() {
            return new FakeApplyOrderStatusCommand();
        }

        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }
    }
}
