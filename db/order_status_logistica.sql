-- Estados de logistica agregados al enum nativo orders.status (order_status).
-- Es aditivo e idempotente. Postgres no permite borrar un valor de un enum.
ALTER TYPE order_status ADD VALUE IF NOT EXISTS 'LISTO_PARA_DESPACHAR' BEFORE 'DESPACHADO';
ALTER TYPE order_status ADD VALUE IF NOT EXISTS 'ESPERANDO_RETIRO' BEFORE 'DEVUELTO';
ALTER TYPE order_status ADD VALUE IF NOT EXISTS 'RETIRADO' BEFORE 'DEVUELTO';
