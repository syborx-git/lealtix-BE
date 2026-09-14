-- V6: Asegurar que la columna description en promotion_reward existe y tiene longitud 500
-- Nota: Esta migración es segura para PostgreSQL. Ajustar para otras bases de datos si aplica.

DO $$
BEGIN
    -- Si la columna no existe, añadirla
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'promotion_reward' AND column_name = 'description'
    ) THEN
        ALTER TABLE promotion_reward ADD COLUMN description VARCHAR(500);
    ELSE
        -- Si existe, intentar cambiar tipo a VARCHAR(500)
        BEGIN
            ALTER TABLE promotion_reward ALTER COLUMN description TYPE VARCHAR(500);
        EXCEPTION WHEN OTHERS THEN
            -- Si no se puede cambiar (ej: contiene texto mayor), cambiar a TEXT para no perder datos y loguear
            RAISE NOTICE 'No se pudo convertir description a VARCHAR(500). Manteniendo tipo actual para evitar pérdida de datos.';
        END;
    END IF;
END$$;
