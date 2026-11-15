package com.amancay.infrastructure.config;

import static com.amancay.infrastructure.messaging.RabbitMqTopology.ADVANCE_ORDER_ROUTING_KEY;
import static com.amancay.infrastructure.messaging.RabbitMqTopology.DEAD_LETTER_EXCHANGE;
import static com.amancay.infrastructure.messaging.RabbitMqTopology.ERROR_ROUTING_KEY_PREFIX;
import static com.amancay.infrastructure.messaging.RabbitMqTopology.ORDERS_EXCHANGE;
import static com.amancay.infrastructure.messaging.RabbitMqTopology.ORDER_NOTIFICATIONS_DLQ;
import static com.amancay.infrastructure.messaging.RabbitMqTopology.ORDER_NOTIFICATIONS_QUEUE;
import static com.amancay.infrastructure.messaging.RabbitMqTopology.STATUS_COMMANDS_DLQ;
import static com.amancay.infrastructure.messaging.RabbitMqTopology.STATUS_COMMANDS_QUEUE;
import static com.amancay.infrastructure.messaging.RabbitMqTopology.STATUS_EVENT_ROUTING_KEY_PREFIX;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.json.JsonMapper;

// Declara en RabbitMQ (al conectarse) el exchange de pedidos, las colas de trabajo y sus
// dead letter queues. Si ya existen con la misma configuracion, no hace nada.
@Configuration
@EnableConfigurationProperties(MessagingProperties.class)
public class RabbitMqConfig {

    @Bean
    public Declarables orderMessagingTopology() {
        TopicExchange orders = new TopicExchange(ORDERS_EXCHANGE, true, false);
        TopicExchange deadLetters = new TopicExchange(DEAD_LETTER_EXCHANGE, true, false);

        Queue statusCommands = workQueue(STATUS_COMMANDS_QUEUE, ERROR_ROUTING_KEY_PREFIX + ADVANCE_ORDER_ROUTING_KEY);
        Queue statusCommandsDlq = QueueBuilder.durable(STATUS_COMMANDS_DLQ).build();
        Queue notifications = workQueue(ORDER_NOTIFICATIONS_QUEUE,
                ERROR_ROUTING_KEY_PREFIX + STATUS_EVENT_ROUTING_KEY_PREFIX + "rejected");
        Queue notificationsDlq = QueueBuilder.durable(ORDER_NOTIFICATIONS_DLQ).build();

        return new Declarables(
                orders, deadLetters,
                statusCommands, statusCommandsDlq, notifications, notificationsDlq,
                bind(statusCommands, orders, ADVANCE_ORDER_ROUTING_KEY),
                bind(notifications, orders, STATUS_EVENT_ROUTING_KEY_PREFIX + "*"),
                bind(statusCommandsDlq, deadLetters, ERROR_ROUTING_KEY_PREFIX + "order.command.#"),
                bind(notificationsDlq, deadLetters, ERROR_ROUTING_KEY_PREFIX + STATUS_EVENT_ROUTING_KEY_PREFIX + "#"));
    }

    // Mensajes en JSON. INFERRED: el tipo sale del parametro del listener y no del header
    // __TypeId__, asi un productor que no es Java (la consola) no necesita conocer clases.
    @Bean
    public MessageConverter rabbitMessageConverter(JsonMapper jsonMapper) {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter(jsonMapper);
        converter.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }

    // Fabrica de todos los @RabbitListener: reintenta los errores transitorios con espera
    // creciente y, cuando se agotan (o el error es permanente), manda el mensaje a la dead
    // letter queue con el motivo en los headers x-exception-message / x-exception-stacktrace.
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory,
            MessageRecoverer deadLetterRecoverer, MessagingProperties properties) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setDefaultRequeueRejected(false);
        MessagingProperties.Retry retry = properties.retry();
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .configureRetryPolicy(policy -> policy
                        .maxRetries(retry.maxRetries())
                        .delay(retry.initialDelay())
                        .multiplier(retry.multiplier())
                        .predicate(RabbitMqConfig::isTransient))
                .recoverer(deadLetterRecoverer)
                .build());
        return factory;
    }

    // Republica en el dead letter exchange con routing key "error.<routing key original>".
    @Bean
    public MessageRecoverer deadLetterRecoverer(AmqpTemplate amqpTemplate) {
        return new RepublishMessageRecoverer(amqpTemplate, DEAD_LETTER_EXCHANGE)
                .errorRoutingKeyPrefix(ERROR_ROUTING_KEY_PREFIX);
    }

    // Reintentar solo tiene sentido si el error puede desaparecer solo. Un mensaje mal
    // formado o rechazado a proposito por el consumidor va directo a la DLQ.
    static boolean isTransient(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof AmqpRejectAndDontRequeueException || cause instanceof MessageConversionException) {
                return false;
            }
        }
        return true;
    }

    // Una cola que, si un mensaje se rechaza sin reencolar, lo deriva a la dead letter queue.
    private static Queue workQueue(String name, String deadLetterRoutingKey) {
        return QueueBuilder.durable(name)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(deadLetterRoutingKey)
                .build();
    }

    private static Binding bind(Queue queue, TopicExchange exchange, String routingKey) {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
    }
}
