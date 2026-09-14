-- =====================================================================
-- V10: Plantillas de Campañas por Defecto
-- Catálogo de 11 plantillas predeterminadas para creación de campañas
-- =====================================================================

INSERT INTO campaign_template 
(id, name, category, default_title, default_subtitle, default_description, default_image_url, default_promo_type, is_active)
VALUES
(1, 'Bienvenida', 'General', '¡Bienvenido(a)!', 'Gracias por elegirnos', 'Agradece a tus nuevos clientes con una promoción de bienvenida.', 'https://res.cloudinary.com/lealtix-media/image/upload/v1763671322/bienvenido_promo_vft9ud.jpg', 'CUSTOM', true),
(2, 'Cumpleaños', 'Celebraciones', '¡Feliz cumpleaños!', 'Celebra con nosotros', 'Ofrece un detalle especial por el cumpleaños del cliente.', 'https://res.cloudinary.com/lealtix-media/image/upload/v1763671826/concepto-letras-feliz-cumpleanos_23-2148499329_svaq6m.avif', 'DISCOUNT', true),
(3, 'Aniversario', 'Celebraciones', '¡Feliz aniversario!', 'Un año más contigo', 'Celebra el aniversario de tus clientes con una promoción especial.', 'https://res.cloudinary.com/lealtix-media/image/upload/v1763671940/aniversario_igogfr.jpg', 'CUSTOM', true),
(4, 'Halloween', 'Festividades', 'Promoción de Halloween', 'Dulce o Trato', 'Aprovecha la temporada para atraer clientes con una oferta temática.', 'https://res.cloudinary.com/lealtix-media/image/upload/v1763671982/halloween-f_cymkko.webp', 'DISCOUNT', true),
(5, 'Día de las Madres', 'Festividades', 'Celebra a Mamá', 'Un detalle para ella', 'Promoción especial por el Día de las Madres.', 'https://res.cloudinary.com/lealtix-media/image/upload/v1763672075/plantilla-letras-dia-madre-dibujada-mano_52683-109346_f1xjzf.avif', 'DISCOUNT', true),
(6, 'Día del Padre', 'Festividades', 'Celebremos a Papá', 'Un regalo especial', 'Promoción temática para el Día del Padre.', 'https://res.cloudinary.com/lealtix-media/image/upload/v1763672075/plantilla-letras-dia-madre-dibujada-mano_52683-109346_f1xjzf.avif', 'DISCOUNT', true),
(7, 'Navidad', 'Festividades', 'Promoción Navideña', 'La mejor temporada', 'Campaña especial para celebrar la Navidad.', 'https://res.cloudinary.com/lealtix-media/image/upload/v1763672198/navidad_um1kuh.jpg', 'CUSTOM', true),
(8, 'San Valentín', 'Festividades', 'Especial de San Valentín', 'Enamorados ahorran', 'Promoción temática por el Día del Amor y la Amistad.', 'https://res.cloudinary.com/lealtix-media/image/upload/v1763672246/sanvalentin_kqu8dl.avif', 'BOGO', true),
(9, '15 de septiembre', 'Festividades', 'Fiestas Patrias', 'Viva México', 'Campaña especial para celebrar la Independencia.', 'https://res.cloudinary.com/lealtix-media/image/upload/v1763672288/15septiembre_y4bzjv.jpg', 'DISCOUNT', true),
(10, 'Día de Reyes', 'Festividades', 'Promoción Día de Reyes', 'Un regalo especial', 'Campaña temática para el 6 de enero.', 'https://res.cloudinary.com/lealtix-media/image/upload/v1763672335/6enero_er8ksy.jpg', 'CUSTOM', true),
(11, '2x1 Genérico', 'Promociones', '¡2x1 Hoy!', 'Paga 1 y lleva 2', 'Plantilla base para campañas 2x1 personalizables.', 'https://res.cloudinary.com/lealtix-media/image/upload/v1763672335/6enero_er8ksy.jpg', 'BOGO', true)
ON CONFLICT (id) DO UPDATE SET
    name                = EXCLUDED.name,
    category            = EXCLUDED.category,
    default_title       = EXCLUDED.default_title,
    default_subtitle    = EXCLUDED.default_subtitle,
    default_description = EXCLUDED.default_description,
    default_image_url   = EXCLUDED.default_image_url,
    default_promo_type  = EXCLUDED.default_promo_type,
    is_active           = EXCLUDED.is_active;

-- Sincronizar secuencia para evitar colisión de claves en nuevos registros
SELECT setval('campaign_template_id_seq', (SELECT COALESCE(MAX(id), 1) FROM campaign_template));
