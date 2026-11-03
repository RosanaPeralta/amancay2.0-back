# Integración con Mercado Pago (Checkout Pro)

Rama: `feature/integracion/meli` (solo local).

El método de pago `MERCADO_PAGO` reemplaza los dos pasos manuales de la transferencia
(el comprador carga el comprobante y el admin confirma). El comprador paga en Mercado Pago
y el pago se aprueba solo por dos caminos, que terminan en la misma lógica:

- **Verificación (plan B):** cuando el comprador vuelve del checkout, el front llama a
  `POST /api/payments/{paymentId}/verify`. Funciona sin túnel.
- **Webhook:** Mercado Pago llama a `POST /api/webhooks/mercadopago`. Necesita una URL
  pública (túnel).

En los dos casos el backend lee el pago desde la API de Mercado Pago (nunca confía en lo que
llega), y lo aprueba solo si está `approved`, su `external_reference` es una orden con un pago
`MERCADO_PAGO` pendiente y el monto coincide. Si algo no cierra, el pago queda `PENDIENTE` y
lo ve el admin en `GET /api/admin/payments/pending`, igual que antes.

`TRANSFERENCIA` con confirmación manual sigue funcionando como estaba.

## Flujo

```
Front                         Backend                              Mercado Pago
  | POST /api/orders            |                                       |
  |---------------------------->| orden CREADO                          |
  | POST /api/orders/{id}/payments {"method":"MERCADO_PAGO"}           |
  |---------------------------->| crea preferencia (external_reference  |
  |                             |   = id de la orden) ----------------->|
  |<----------------------------| Payment PENDIENTE + checkoutUrl       |
  | redirige a checkoutUrl ------------------------------------------->| el comprador paga
  |<-------------- vuelve a /checkout/resultado?... (en localhost: la pestaña original espera)
  | POST /api/payments/{paymentId}/verify                              |
  |---------------------------->| GET /v1/payments/search ------------>|
  |                             | aprobado -> Payment APROBADO,         |
  |<----------------------------|   descuenta stock, orden EN_PREPARACION
  |                             |<------- webhook (si hay túnel) -------|
  |                             | GET /v1/payments/{id}: ya aplicado,   |
  |                             |   no hace nada (idempotente)          |
```

## Puesta en marcha

### 1. Base de datos

Correr una vez `sql/mercadopago.sql`. Agrega `external_id` y `checkout_url` a `payments`,
un índice único sobre `external_id` y admite `MERCADO_PAGO` en el CHECK de `method`. La app usa
`ddl-auto=validate`, así que sin esto no levanta. Si la base es compartida con el equipo, las
columnas nuevas no rompen a quien siga en `main`.

### 2. Aplicación y cuentas de prueba

1. En [Tus integraciones](https://www.mercadopago.com.ar/developers/panel/app) crear una
   aplicación con Checkout Pro.
2. En **Cuentas de prueba** crear (si no existen) una cuenta **vendedor** y una **comprador**
   de Argentina, la del comprador con saldo ficticio.
3. En **Credenciales de prueba** copiar el *Access Token* para `MP_ACCESS_TOKEN`.
4. Para pagar en el checkout, iniciar sesión con el usuario comprador de prueba (conviene
   una ventana de incógnito, para no usar tu cuenta real).

### 3. Variables en `.env`

```properties
# Obligatoria
MP_ACCESS_TOKEN=TEST-xxxxxxxxxxxxxxxx

# Adonde vuelve el comprador después de pagar (ruta del front)
MP_FRONTEND_URL=http://localhost:5173
MP_RETURN_PATH=/checkout/resultado

# Solo para el webhook (opcionales)
MP_NOTIFICATION_URL=https://<tu-tunel>/api/webhooks/mercadopago?source_news=webhooks
MP_WEBHOOK_SECRET=<clave secreta de Webhooks en el panel>
```

- Sin `MP_NOTIFICATION_URL` no se pide webhook y funciona solo la verificación.
- `?source_news=webhooks` evita que además lleguen las notificaciones IPN viejas.
- Mercado Pago no redirige a `localhost` ni a `127.0.0.1`. Con `MP_FRONTEND_URL` en localhost el
  backend no manda URLs de retorno y el front abre el checkout en **otra pestaña**: la tienda queda
  en `/checkout/resultado?espera=1` y detecta sola el pago (consulta cada 3 segundos). Con una URL
  pública (por ejemplo un túnel https al front) vuelve a redirigir en la misma pestaña.
- Sin `MP_WEBHOOK_SECRET` la firma no se valida (se avisa en el log al arrancar). Sigue siendo
  seguro porque el pago se vuelve a leer de la API de Mercado Pago.

### 4. Webhook en local (opcional)

1. Levantar el backend (`./mvnw spring-boot:run`, puerto 8080).
2. Levantar un túnel, por ejemplo `ngrok http 8080`, y copiar la URL https.
3. Poner esa URL en `MP_NOTIFICATION_URL` (ver arriba) y reiniciar el backend.
4. Para la firma: en el panel, **Webhooks > Configurar notificaciones**, modo prueba, evento
   **Pagos**, misma URL; guardar y copiar la clave secreta en `MP_WEBHOOK_SECRET`.

En el log del backend aparece `Mercado Pago notification for payment ...` cuando llega uno.

## API

| Método | Ruta | Quién | Qué hace |
|---|---|---|---|
| POST | `/api/orders/{orderId}/payments` con `{"method":"MERCADO_PAGO"}` | comprador | Crea el pago `PENDIENTE` y devuelve `checkoutUrl` |
| POST | `/api/payments/{paymentId}/verify` | comprador | Revisa en Mercado Pago y, si está aprobado, aprueba el pago. Devuelve el pago |
| POST | `/api/webhooks/mercadopago` | Mercado Pago | Webhook. Público, validado con `x-signature` |

`PaymentResponse` suma dos campos: `checkoutUrl` (adonde redirigir) y `externalId` (id del pago
en Mercado Pago, una vez aprobado).

Al volver del checkout, Mercado Pago agrega a la URL de retorno, entre otros,
`external_reference` (el id de la orden), `status` y `payment_id`. Con el id de la orden el
front puede listar `GET /api/orders/{orderId}/payments`, tomar el `MERCADO_PAGO` pendiente
y llamar a `verify`.

## Probar sin front

```bash
# 1. crear el pago (con un JWT de comprador o AMANCAY_SECURITY_DEV_USER_ENABLED=true)
curl -X POST localhost:8080/api/orders/<orderId>/payments \
  -H 'Content-Type: application/json' -d '{"method":"MERCADO_PAGO"}'
# 2. abrir el checkoutUrl de la respuesta y pagar con el comprador de prueba
# 3. verificar
curl -X POST localhost:8080/api/payments/<paymentId>/verify
```
