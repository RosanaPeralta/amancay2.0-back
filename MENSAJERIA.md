# Mensajería: estados del pedido por cola (RabbitMQ)

Hasta que se aprueba el pago, el pedido lo maneja la app. **Desde `EN_PREPARACION`, el estado avanza por cola**,
y cada cambio de estado se publica como evento para que otros consumidores reaccionen (por ejemplo, el mailer).

```
[App, síncrono]                          [RabbitMQ, asíncrono]
CREADO ──pago aprobado──► EN_PREPARACION ──comando──► DESPACHADO ──comando──► ENTREGADO
         (stock + estado en la            │                │
          misma transacción)              │                └─ evento order.status.despachado ─► mail "tu pedido está en camino"
                                          └─ evento order.status.en_preparacion ─► mail "confirmamos tu compra"
ENTREGADO ──devolución aprobada (App)──► DEVUELTO
```

Quién puede disparar cada transición está en el dominio (`OrderStatus` + `OrderTransitionTrigger`):

| Desde | Hacia | Lo dispara |
|---|---|---|
| CREADO | EN_PREPARACION | `PAYMENT`: el pago aprobado (`OrderService.markAsPaid`) |
| EN_PREPARACION | DESPACHADO | `FULFILLMENT`: la cola de logística |
| DESPACHADO | ENTREGADO | `FULFILLMENT`: la cola de logística |
| ENTREGADO | DEVUELTO | `RETURN_REQUEST`: la devolución aprobada |

## Topología

Exchange `amancay.orders` (topic, durable):

| Routing key | Cola | Consumidor | Qué hace |
|---|---|---|---|
| `order.command.advance` | `amancay.orders.status-commands` | `OrderStatusCommandListener` | Aplica el comando (`ApplyOrderStatusCommandUseCase`) |
| `order.status.*` | `amancay.notifications.order-status` | `OrderStatusNotificationListener` | Manda el mail del estado, si tiene (`NotifyOrderStatusChangeUseCase`) |

Productores:
- **Comandos**: el panel de admin (`PATCH /api/orders/{id}/status`, responde **202**) y la consola de logística
  (`DDA2/amancay-mq-console`).
- **Eventos**: `RabbitOrderEventRelay` reenvía cada `OrderStatusChangedEvent` **después del commit**, así nunca sale a la
  cola un cambio que no se guardó.

### Contratos (JSON, `content_type: application/json`)

Comando `order.command.advance`:
```json
{ "messageId": "uuid", "orderId": "uuid", "targetStatus": "DESPACHADO", "requestedBy": "logistics-console",
  "requestedAt": "2026-11-14T15:00:00Z" }
```
`orderId` y `targetStatus` son obligatorios. No hace falta el header `__TypeId__`: el tipo sale del parámetro del listener.

Evento `order.status.<estado en minúsculas>`:
```json
{ "eventId": "uuid", "orderId": "uuid", "buyerId": "uuid", "previousStatus": "EN_PREPARACION",
  "newStatus": "DESPACHADO", "occurredAt": "2026-11-14T15:00:00Z" }
```

## Dead letter queues

Cada cola de trabajo tiene su DLQ, alimentada por el exchange `amancay.orders.dlx` (topic):

| Cola | DLQ | Routing key en el DLX |
|---|---|---|
| `amancay.orders.status-commands` | `amancay.orders.status-commands.dlq` | `error.order.command.#` |
| `amancay.notifications.order-status` | `amancay.notifications.order-status.dlq` | `error.order.status.#` |

Cómo llega un mensaje a la DLQ:

| Caso | Tipo de error | Qué pasa |
|---|---|---|
| JSON mal formado, comando sin `orderId`/`targetStatus` | permanente | DLQ sin reintentos |
| Orden inexistente, transición inválida, comprador sin email | permanente | DLQ sin reintentos |
| Base caída, SMTP caído, cualquier otra excepción | transitorio | reintenta (`MESSAGING_MAX_RETRIES`, default 2, con espera 1s, 2s…) y después DLQ |
| Comando repetido (la orden ya está en ese estado) | no es error | se confirma y no hace nada (consumidor idempotente) |

El mensaje en la DLQ conserva el cuerpo original y suma los headers `x-exception-message`, `x-exception-stacktrace`,
`x-original-exchange` y `x-original-routingKey`.

**Dónde se ve:**
1. UI de RabbitMQ, http://localhost:15672 (guest/guest) → *Queues* → la `.dlq` → *Get messages*.
2. La consola de logística (http://localhost:3001) → *Dead letter queues*.
3. Logs del backend: `WARN ... rejected: <motivo>`.

**Reprocesar:** en la UI de RabbitMQ, dentro de la DLQ, *Move messages* (plugin shovel, ya habilitado en el
docker-compose) hacia la cola original, una vez corregido el problema.

## Correr local

```bash
# En la raíz de DDA2
docker compose up -d                 # RabbitMQ (5672 / 15672) + Mailpit (1025 / 8025)

# Backend: mails por SMTP a Mailpit (sin MAIL_ENABLED=true solo se loguean)
MAIL_ENABLED=true ./mvnw spring-boot:run

# Consola de logística (opcional)
cd ../amancay-mq-console && npm install && npm start
```

Mails enviados: http://localhost:8025. Para otro broker (por ejemplo CloudAMQP), configurar `RABBITMQ_HOST`,
`RABBITMQ_PORT`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD`, `RABBITMQ_VHOST` y `RABBITMQ_SSL=true`.

## Patrones

| Patrón | Dónde |
|---|---|
| **State** | `OrderStatus`: a qué estado puede pasar cada uno y quién dispara el paso (`OrderTransitionTrigger`) |
| **Command** | `AdvanceOrderStatusCommand`: el pedido de cambio viaja por la cola y lo ejecuta otro componente |
| **Observer / Publish-Subscribe** | `OrderStatusChangedEvent` → exchange topic → N suscriptores (hoy, el mailer) |
| **Strategy** | `OrderStatusMail`: un mail por estado (`PurchaseConfirmedMail`, `OrderDispatchedMail`), elegido por `OrderStatusNotificationService` |
| **Template Method** | `OrderStatusMail.build()`: estructura común del mail; cada subclase completa asunto, intro y cierre |
| **Adapter** (puertos y adaptadores) | `SendOrderCommandPort` → `RabbitOrderCommandSender`, `MailSenderPort` → `SmtpMailSender` / `LoggingMailSender` |
| **Idempotent Consumer / Dead Letter Channel** | los listeners y las DLQ |

## Limitaciones conocidas

- Si RabbitMQ no responde justo después del commit, el evento se pierde (queda en el log). Para garantizar la
  entrega habría que pasar a un **Transactional Outbox** (tabla de eventos pendientes + publicador periódico).
- Si un evento se reentrega después de mandar el mail (por ejemplo, la app se cae antes del ack), el comprador puede
  recibirlo dos veces. Se resolvería registrando los mails enviados por `eventId`.
