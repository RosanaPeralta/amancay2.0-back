INSERT INTO products (
    id,
    name,
    slug,
    short_description,
    description,
    is_active
)
VALUES
(
    '00000000-0000-0000-0000-000000000001',
    'Mochila Trekking 40L',
    'mochila-trekking-40l',
    'Mochila de trekking de 40 litros para excursiones y travesías.',
    'Mochila resistente de 40 litros con múltiples compartimentos, sistema de ajuste ergonómico, cubierta impermeable y espacio para hidratación.',
    TRUE
),
(
    '00000000-0000-0000-0000-000000000002',
    'Carpa Camping 4 Personas',
    'carpa-camping-4-personas',
    'Carpa impermeable para cuatro personas, ideal para camping y escapadas.',
    'Carpa de cuatro personas con doble techo impermeable, estructura de fibra de vidrio, ventilación superior y fácil armado.',
    TRUE
),
(
    '00000000-0000-0000-0000-000000000003',
    'Caña de Pesca Spinning 2.40m',
    'cana-pesca-spinning-240m',
    'Caña de pesca spinning de 2,40 metros para pesca recreativa.',
    'Caña de pesca spinning fabricada en fibra de carbono, liviana y resistente, ideal para pesca deportiva en ríos, lagunas y costas.',
    TRUE
);

INSERT INTO product_variants (
    id,
    product_id,
    price,
    stock_quantity
)
VALUES
(
    '00000000-0000-0000-0000-000000000011',
    '00000000-0000-0000-0000-000000000001',
    89999,
    18
),
(
    '00000000-0000-0000-0000-000000000012',
    '00000000-0000-0000-0000-000000000002',
    159999,
    12
),
(
    '00000000-0000-0000-0000-000000000013',
    '00000000-0000-0000-0000-000000000003',
    74999,
    30
);