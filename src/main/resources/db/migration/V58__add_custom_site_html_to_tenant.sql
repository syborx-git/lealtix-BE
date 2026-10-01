-- Agrega la columna para el HTML del sitio personalizado construido en el Web Studio (Admin Page).
-- El landing publico la renderiza cuando el tenant la tiene guardada.
ALTER TABLE tenant ADD COLUMN IF NOT EXISTS custom_site_html TEXT;
