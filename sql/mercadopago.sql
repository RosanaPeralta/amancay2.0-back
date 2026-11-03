-- Integracion Mercado Pago (rama feature/integracion/meli).
-- La app usa spring.jpa.hibernate.ddl-auto=validate, asi que estas columnas tienen que
-- existir antes de levantarla. Correr una sola vez sobre la base (es idempotente).

ALTER TABLE payments ADD COLUMN IF NOT EXISTS external_id VARCHAR(64);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS checkout_url VARCHAR(512);

-- Un pago de Mercado Pago no puede aplicarse a dos pagos nuestros (idempotencia del webhook).
CREATE UNIQUE INDEX IF NOT EXISTS ux_payments_external_id ON payments (external_id);

-- Si la tabla la creo Hibernate, puede tener un CHECK con los metodos de pago viejos.
-- Se recrea incluyendo MERCADO_PAGO.
ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_method_check;
ALTER TABLE payments ADD CONSTRAINT payments_method_check
    CHECK (method IN ('TARJETA_CREDITO', 'TARJETA_DEBITO', 'TRANSFERENCIA', 'MERCADO_PAGO'));
