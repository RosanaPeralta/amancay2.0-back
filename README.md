# Amancay

Backend de Amancay construido con Java, Spring Boot y Maven.

## Stack

- Java 21
- Spring Boot 4.1.1
- Maven
- Spring Web, Spring Data JPA, Validation, Lombok
- PostgreSQL (Supabase)
- RabbitMQ (Spring AMQP) para el avance de estados del pedido
- SMTP (Spring Mail) para los mails al comprador
- OpenAPI / Swagger UI

## Requisitos

- JDK 21+
- PostgreSQL corriendo localmente (o accesible por red)

## Configuración

La conexión a la base de datos se configura por variables de entorno (con valores por defecto para desarrollo local):

| Variable      | Default     |
|---------------|-------------|
| `DB_HOST`     | `localhost` |
| `DB_PORT`     | `5432`      |
| `DB_NAME`     | `amancay`   |
| `DB_USER`     | `postgres`  |
| `DB_PASSWORD` | `postgres`  |

Además, la autenticación y el front requieren:

| Variable                             | Default                 | Descripción                                                    |
|--------------------------------------|-------------------------|----------------------------------------------------------------|
| `SUPABASE_URL`                       | *(obligatoria)*         | Project URL de Supabase. Sin ella la aplicación no arranca.    |
| `CORS_ALLOWED_ORIGINS`               | `http://localhost:5173` | Orígenes permitidos para el front, separados por coma.         |
| `AMANCAY_SECURITY_DEV_USER_ENABLED`  | `false`                 | Bypass de autenticación para desarrollo (ver Autenticación).   |

Todas pueden definirse en un archivo `.env` en la raíz del proyecto (ver `.env.example`; el archivo está ignorado por git).

### Supabase - Primeros pasos para levantar el proyecto:

Para levantar el proyecto usando una base compartida:

1. Crear un proyecto en [Supabase](https://supabase.com/).
2. Entrar en **Connect**.
3. En **Connection Method**, seleccionar **Session Pooler**.
4. Revisar los datos de conexión desplazándose hacia abajo en la página y copiar el host, puerto, base de datos y usuario que muestra Supabase. (Al ejecutar el powershell reemplazar los datos por los propios siguiendo estos pasos)

No es necesario modificar `application.properties`. Cada integrante debe configurar sus propias variables en el terminal de PowerShell de Visual Studio Code. Reemplazar los valores de ejemplo por los datos reales de Supabase:

```powershell
$env:SUPABASE_URL="https://<project-ref>.supabase.co"
$env:SUPABASE_JWT_SECRET="<JWT_SECRET>"
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://HOST_SUPABASE:PUERTO_SUPABASE/postgres?sslmode=require"
$env:DB_USER="USUARIO_SUPABASE"
$env:DB_PASSWORD="CONTRASEÑA_SUPABASE"

./mvnw.cmd spring-boot:run
```

La supabase_URL sale de:
 Ingresar a Project Settings desde supabase.
 Dentro de la sección Configuration, haz clic en API.
 En la sección Project URL esta con un formato similar a: [https://xxxxxxxxxxxxxxxxxxxx.supabase.co]

 La SUPABASE_JWT_SECRET sale de:
 Ingresar a Project Settings desde supabase.
 Dentro de la seccion JWT Keys.
 Copiar la KeyId

El schema **vive en Supabase** y la aplicación solo lo valida al arrancar (`spring.jpa.hibernate.ddl-auto=validate`). No hay migraciones automáticas: los cambios de schema se aplican en el SQL Editor de Supabase.

Con la aplicación funcionando, Swagger se puede abrir en:

```text
http://localhost:8080/swagger-ui/index.html
```

## Cómo correr el proyecto

1. Tener el `.env` completo (ver [Configuración](#configuración), [RabbitMQ y mails](#rabbitmq-y-mails) y
   [Integración con Mercado Pago](#integracion-con-mercado-pago)).
2. Levantar RabbitMQ y Mailpit (ver abajo; después quedan corriendo en Docker).
3. Levantar el backend:

```bash
./mvnw spring-boot:run
```

## RabbitMQ y mails

Hasta que se aprueba el pago, el pedido lo maneja la app. Desde `EN_PREPARACION` lo avanza un **sistema de
logística externo** publicando comandos en RabbitMQ (exchange `amancay.orders`, routing key `order.command.advance`);
el backend solo los consume. Para desarrollo, ese sistema es la consola de logística (`DDA2/amancay-mq-console`, ver
[MENSAJERIA.md](MENSAJERIA.md#correr-local)). El panel de admin solo muestra el estado; lo único que hace el admin
es aprobar devoluciones, que dejan el pedido esperando que logística lo retire.

Estados: `CREADO` → `EN_PREPARACION` → `LISTO_PARA_DESPACHAR` → `DESPACHADO` → `ENTREGADO`, y para una devolución
aprobada `ESPERANDO_RETIRO` → `RETIRADO` → `DEVUELTO` (detalle en [MENSAJERIA.md](MENSAJERIA.md)).

Cada cambio de estado se publica como evento; el mailer avisa al comprador al confirmarse la compra y cuando el
pedido se despacha ("tu pedido está en camino"). Sin RabbitMQ la app arranca, pero los pedidos no avanzan desde
`EN_PREPARACION` y no salen los mails.

### Setup local (Docker)

Requiere [Docker Desktop](https://www.docker.com/products/docker-desktop/) abierto. La primera vez:

```bash
# RabbitMQ con la UI de administración
docker run -d --name amancay-rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:4.1-management

# Mailpit: servidor SMTP de prueba que atrapa todos los mails (no le llegan a nadie de verdad)
docker run -d --name amancay-mailpit -p 1025:1025 -p 8025:8025 axllent/mailpit:v1.27
```

Después, **recién cuando http://localhost:15672 ya abre** (unos segundos), habilitar los plugins para reprocesar
la dead letter queue (shovel) y para ver los mensajes consumidos (tracing). Si se corre antes, mientras RabbitMQ
arranca, el contenedor se cae.

```bash
docker exec amancay-rabbitmq rabbitmq-plugins enable rabbitmq_shovel rabbitmq_shovel_management rabbitmq_tracing
```

Las próximas veces alcanza con `docker start amancay-rabbitmq amancay-mailpit` (los plugins quedan habilitados).

| Qué | Dónde |
|---|---|
| RabbitMQ (AMQP) | `localhost:5672` |
| UI de RabbitMQ: colas, mensajes, dead letter queues | http://localhost:15672 · usuario `guest`, clave `guest` |
| Mails enviados (Mailpit) | http://localhost:8025 |

Al arrancar, el backend crea solo el exchange, las colas y las dead letter queues. Para que los mails salgan por
SMTP (a Mailpit) agregar `MAIL_ENABLED=true` al `.env`; si no, solo se escriben en el log.

### Ver los mensajes en RabbitMQ

RabbitMQ borra cada mensaje en cuanto el consumidor lo confirma: de lo ya consumido solo quedan contadores.

- **Colas y tráfico:** pestaña **Queues** (mensajes en espera, consumidores, gráfico de entrada / entrega / ack).
- **Dead letter queues:** **Queues** → `amancay.orders.status-commands.dlq` o `amancay.notifications.order-status.dlq`
  → **Get messages** con *Ack Mode* "Nack message requeue true" (para mirarlos sin sacarlos). El motivo del error está
  en el header `x-exception-message`. **Move messages** los devuelve a la cola original para reprocesarlos.
- **Mensajes consumidos:** **Admin** → **Tracing** → agregar una traza con *Format* `Text`, *Pattern* `deliver.#` →
  el archivo `.log` que aparece abajo registra cada mensaje entregado a un consumidor. Las trazas no sobreviven a un
  reinicio del contenedor; es para desarrollo.

### Variables

Los defaults sirven para el setup local de arriba; solo hace falta tocarlas para otro broker o un SMTP real.

| Variable | Default | Descripción |
|---|---|---|
| `RABBITMQ_HOST` / `RABBITMQ_PORT` | `localhost` / `5672` | Broker |
| `RABBITMQ_USER` / `RABBITMQ_PASSWORD` | `guest` / `guest` | Credenciales |
| `RABBITMQ_VHOST` | `/` | Virtual host (en CloudAMQP es el nombre de usuario) |
| `RABBITMQ_SSL` | `false` | `true` para brokers en la nube (puerto 5671) |
| `MESSAGING_MAX_RETRIES` | `2` | Reintentos antes de mandar un mensaje a la dead letter queue |
| `MAIL_ENABLED` | `false` | `true` manda por SMTP; si no, los mails solo se loguean |
| `MAIL_FROM` | `Amancay <no-reply@amancay.local>` | Remitente |
| `MAIL_HOST` / `MAIL_PORT` | `localhost` / `1025` | SMTP |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | vacíos | Credenciales SMTP (Mailpit no las pide) |
| `MAIL_SMTP_AUTH` / `MAIL_SMTP_STARTTLS` | `false` / `false` | `true` y `true` para un SMTP real (por ejemplo Gmail, puerto 587) |

Topología, contratos de los mensajes, dead letter queues y patrones: ver [MENSAJERIA.md](MENSAJERIA.md).

## Productos

La API CRUD está disponible en `/api/products`. Las operaciones de creación y actualización reciben el producto junto con sus variantes; una actualización elimina las variantes existentes que no se envían.

- `GET /api/products?page=0&size=20&q=cafe&is_active=true`
- `GET /api/products/{id}`
- `POST /api/products`
- `PUT /api/products/{id}`
- `DELETE /api/products/{id}`

Swagger UI está disponible en `/swagger-ui/index.html` y el contrato adicional en `src/main/resources/openapi/products.yaml`.

## Usuarios y Reseñas

Perfil (`/api/me`), direcciones (`/api/me/addresses`), favoritos (`/api/me/favorites`), reseñas (`/api/products/{id}/reviews`, `/api/reviews/{id}`, `/api/me/reviews`) y administración (`/api/admin/**`, solo ADMIN). Todos los endpoints están en Swagger UI.

## Envíos

El costo de envío se calcula con servicios gratuitos de OpenStreetMap, que no requieren API key: **Nominatim** convierte la dirección en coordenadas y **OSRM** calcula la distancia en auto desde el depósito.

```text
costo = SHIPPING_BASE_COST + km * SHIPPING_COST_PER_KM
```

- `GET /api/orders/shipping-quote?addressId=...` devuelve `{ "distanceKm": 11.1, "cost": 10.54 }` para mostrar en el checkout.
- `POST /api/orders` vuelve a cotizar y guarda el resultado en `shippingCost` (el `total` ya lo incluye).
- Si la dirección no se encuentra o el servicio no responde, se devuelve `503` y la orden no se crea.

| Variable               | Default    | Descripción                    |
|------------------------|------------|--------------------------------|
| `SHIPPING_ORIGIN_LAT`  | `-34.6170` | Latitud del depósito (UADE).   |
| `SHIPPING_ORIGIN_LON`  | `-58.3817` | Longitud del depósito.         |
| `SHIPPING_BASE_COST`   | `5.00`     | Costo fijo por envío.          |
| `SHIPPING_COST_PER_KM` | `0.50`     | Costo por kilómetro recorrido. |
| `SHIPPING_COUNTRY_CODES` | `ar`     | Países donde se buscan las direcciones (ISO, separados por coma). Fuera de ellos la cotización falla. |

Los servidores públicos de Nominatim y OSRM sirven para desarrollo y demos (Nominatim permite ~1 request por segundo), no para producción.

## Recomendaciones con IA

`GET /api/me/recommendations?limit=8` (requiere usuario autenticado) devuelve productos activos que el usuario todavía no marcó como favoritos, ordenados por afinidad con sus favoritos:

```json
{ "source": "AI", "favoritesCount": 3, "products": [ /* mismo formato que GET /api/products/{id} */ ] }
```

- **`AI`**: el ranking lo arma un LLM vía una API compatible con OpenAI (`chat/completions`); por defecto [Groq](https://console.groq.com), que tiene capa gratuita sin tarjeta, a partir de nombre, categorías y descripción corta de favoritos y candidatos. Los ids que devuelve se validan contra el catálogo (se descartan los inventados o repetidos) y el resultado se cachea en memoria por usuario mientras sus favoritos no cambien (`AMANCAY_RECOMMENDATIONS_CACHE_TTL`, default 30 min).
- **`CATEGORIES`**: fallback sin IA (no hay `AI_API_KEY`, timeout o error): productos que comparten más categorías con los favoritos.
- **`NONE`**: el usuario no tiene favoritos activos.

| Variable                             | Default                     | Descripción                                   |
|--------------------------------------|-----------------------------|-----------------------------------------------|
| `AI_API_KEY`                         | *(vacía)*                   | API key de [Groq](https://console.groq.com/keys). Opcional. |
| `AI_BASE_URL`                        | `https://api.groq.com/openai/v1` | Cualquier API compatible con OpenAI (xAI, OpenRouter, Ollama...). |
| `AI_MODEL`                           | `openai/gpt-oss-120b`   | Modelo a usar.                                |
| `AMANCAY_RECOMMENDATIONS_CACHE_TTL`  | `PT30M`                     | Vigencia del ranking cacheado (ISO-8601).     |

## Integracion con mercado pago

Pago con Checkout Pro, en modo prueba (no se mueve plata real). Variables del `.env`:

| Variable | Obligatoria | Qué es |
|---|---|---|
| `MP_ACCESS_TOKEN` | sí | Access Token de prueba de tu aplicación. Sin él la app arranca, pero falla el pago con Mercado Pago |
| `MP_FRONTEND_URL` | no (`http://localhost:5173`) | URL del front, para volver después de pagar |
| `MP_RETURN_PATH` | no (`/checkout/resultado`) | Ruta del front a la que vuelve el comprador |
| `MP_NOTIFICATION_URL` | no | URL pública del webhook: `https://<tu-tunel>/api/webhooks/mercadopago` |
| `MP_WEBHOOK_SECRET` | no | Clave secreta del webhook, para validar la firma `x-signature` |

### 1. Access Token (`MP_ACCESS_TOKEN`)

1. Entrar a [Tus integraciones](https://www.mercadopago.com.ar/developers/panel/app) con tu cuenta de
   Mercado Pago y crear una aplicación de **Checkout Pro** (o entrar a la que ya tengas). Al crearla, Mercado Pago
   genera solo las credenciales de prueba y una cuenta de prueba vendedora.
2. En la aplicación, con el selector de entorno de arriba en **Prueba**, ir a **Credenciales** y copiar el
   **Access Token** (empieza con `APP_USR-`) a `MP_ACCESS_TOKEN`.

No hace falta crear cuentas de prueba de comprador: se paga como invitado con tarjetas de prueba.

### Pagar en modo prueba

1. Hacer **toda la compra en una ventana de incógnito** (front incluido). Si el navegador tiene abierta una sesión
   real de Mercado Pago, el checkout falla con "Una de las partes con la que intentás hacer el pago es de prueba".
2. En el checkout **no iniciar sesión**: pagar con tarjeta.
   - Email: `test@testuser.com` (con otros emails puede fallar con "No podés pagar con un e-mail corporativo").
   - Tarjeta: Mastercard `5031 7557 3453 0604` o Visa `4509 9535 6623 3704`, vencimiento `11/30`, CVV `123`.
   - Titular: `APRO` (aprobado), `OTHE` (rechazado) o `CONT` (pendiente). DNI: `12345678`.
3. No pagar con el usuario de prueba que figura en **Credenciales**: es el vendedor (dueño del token).

Con solo esto el flujo funciona en local: como Mercado Pago no redirige a `localhost`, el front abre el checkout en
otra pestaña y, al volver, consulta `POST /api/payments/{paymentId}/verify`, que revisa el pago en Mercado Pago y lo
aprueba.

### 2. Webhook (opcional: `MP_NOTIFICATION_URL` y `MP_WEBHOOK_SECRET`)

Sirve para que el pago se apruebe aunque el comprador no vuelva al sitio. Mercado Pago tiene que poder llegar al
backend desde internet, así que en local hace falta un túnel:

1. Exponer el puerto 8080, por ejemplo con `cloudflared tunnel --url http://localhost:8080` o `ngrok http 8080`, y
   copiar la URL `https://...` que muestra.
2. `MP_NOTIFICATION_URL=https://<url-del-tunel>/api/webhooks/mercadopago`.
3. En la aplicación de Mercado Pago, **Webhooks** → **Configurar notificaciones** → **Modo de prueba**: pegar la
   misma URL, marcar el evento **Pagos** y guardar.
4. Ahí mismo aparece la **Clave secreta**: copiarla a `MP_WEBHOOK_SECRET`. Sin ella el backend acepta las
   notificaciones sin validar la firma (lo avisa en el log al arrancar).

La URL del túnel cambia cada vez que se reinicia (salvo con un dominio fijo): hay que actualizar
`MP_NOTIFICATION_URL` y la URL del panel.

### Ejemplo de `.env`

```properties
MP_ACCESS_TOKEN=APP_USR-0000000000000000-000000-00000000000000000000000000000000-000000000
MP_FRONTEND_URL=http://localhost:5173
MP_RETURN_PATH=/checkout/resultado
# Solo con túnel:
MP_NOTIFICATION_URL=
MP_WEBHOOK_SECRET=
```

| Método | Ruta | Quién | Qué hace |
|---|---|---|---|
| POST | `/api/orders/{orderId}/payments` con `{"method":"MERCADO_PAGO"}` | comprador | Crea el pago `PENDIENTE` y devuelve `checkoutUrl` |
| POST | `/api/payments/{paymentId}/verify` | comprador | Revisa en Mercado Pago y, si está aprobado, aprueba el pago. Devuelve el pago |
| POST | `/api/webhooks/mercadopago` | Mercado Pago | Webhook. Público, validado con `x-signature` |

`PaymentResponse` suma dos campos: `checkoutUrl` (adonde redirigir) y `externalId` (id del pago
en Mercado Pago, una vez aprobado).

## Autenticación

El login y el registro ocurren en **Supabase Auth** (desde el front). El backend no tiene endpoints de login: actúa como *resource server* y valida en cada request el JWT que emite Supabase, enviado como `Authorization: Bearer <access_token>`.

- La firma se verifica contra el JWKS del proyecto (`SUPABASE_URL/auth/v1/.well-known/jwks.json`); también se validan el issuer y el audience `authenticated`.
- El `sub` del token es el `id` de la tabla `users`. En la primera request autenticada, si el usuario no existe, se crea automáticamente con rol `BUYER` (nombre tomado de `user_metadata.name`, si viene en el token).
- Requieren usuario autenticado: `/api/me/**`, `/api/admin/**` (además exige rol `ADMIN`) y la creación/edición/borrado de reseñas. El resto es público.
- Errores de autenticación responden `401 {"error": "Authentication required"}` y de autorización `403 {"error": "Access denied"}`.

### Probar endpoints autenticados en Swagger

1. Obtener un `access_token` iniciando sesión desde el front (en la consola del navegador: `JSON.parse(localStorage.getItem(Object.keys(localStorage).find(k => k.startsWith('sb-')))).access_token`).
2. En Swagger UI, botón **Authorize** y pegar el token.

### Bypass de autenticación para desarrollo

Con `AMANCAY_SECURITY_DEV_USER_ENABLED=true` toda request sin token corre como un usuario fijo (`00000000-0000-0000-0000-000000000001`, `dev@amancay.local`), sin necesidad de Supabase. Un token real sigue teniendo prioridad si se envía. **Nunca habilitarlo fuera de desarrollo local.**

## Tests

Pruebas unitarias e integración con Testcontainers (requiere Docker). `OrderMessagingIntegrationTest` levanta un
RabbitMQ real y prueba comandos, eventos, reintentos y dead letter queues:

```bash
./mvnw test
```

Sin Docker, excluyendo la integración:

```powershell
.\mvnw.cmd test "-Dtest=!ProductIntegrationTest" -DfailIfNoSpecifiedTests=false
```
