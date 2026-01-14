-- ============================================
-- QUERIES PARA VERIFICAR FUNCIONES EN LA BASE DE DATOS
-- Ejecuta estos queries en Supabase SQL Editor
-- ============================================

-- 1. VERIFICAR SI LAS FUNCIONES EXISTEN
SELECT 
    p.proname AS "Nombre Función",
    CASE p.prokind
        WHEN 'f' THEN 'FUNCTION'
        WHEN 'p' THEN 'PROCEDURE'
    END AS "Tipo",
    pg_get_function_arguments(p.oid) AS "Argumentos",
    pg_get_function_result(p.oid) AS "Tipo Retorno"
FROM pg_proc p
JOIN pg_namespace n ON p.pronamespace = n.oid
WHERE n.nspname = 'public'
  AND p.proname IN (
    'verificar_disponibilidad_pool',
    'verificar_disponibilidad_pool_simple',
    'calcular_precio_reserva',
    'crear_reserva',
    'consultar_tarifas'
  )
ORDER BY p.proname;

-- 2. VER LA DEFINICIÓN COMPLETA DE verificar_disponibilidad_pool
SELECT pg_get_functiondef(oid) 
FROM pg_proc 
WHERE proname = 'verificar_disponibilidad_pool';

-- 3. VER LA DEFINICIÓN COMPLETA DE calcular_precio_reserva
SELECT pg_get_functiondef(oid) 
FROM pg_proc 
WHERE proname = 'calcular_precio_reserva';

-- 3.1. VER LA DEFINICIÓN COMPLETA DE consultar_tarifas
SELECT pg_get_functiondef(oid) 
FROM pg_proc 
WHERE proname = 'consultar_tarifas';

-- 4. VERIFICAR ESTRUCTURA DE TABLA temporadas (para verificar tipos de datos)
SELECT 
    column_name,
    data_type,
    is_nullable
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'temporadas'
ORDER BY ordinal_position;

-- 5. PROBAR verificar_disponibilidad_pool
SELECT * FROM verificar_disponibilidad_pool(
    1::integer,
    1::integer,
    CURRENT_DATE::date,
    (CURRENT_DATE + INTERVAL '5 days')::date
);

-- 6. PROBAR calcular_precio_reserva
SELECT * FROM calcular_precio_reserva(
    1::integer,
    1::integer,
    CURRENT_DATE::date,
    (CURRENT_DATE + INTERVAL '5 days')::date,
    2::integer,
    4::integer
);

-- 7. VERIFICAR DATOS EN TABLA temporadas (para verificar formato de fechas)
SELECT 
    id_temporada,
    nombre,
    fecha_inicio,
    fecha_fin,
    anio,
    pg_typeof(fecha_inicio) AS tipo_fecha_inicio,
    pg_typeof(fecha_fin) AS tipo_fecha_fin
FROM temporadas
LIMIT 5;

-- 8. VERIFICAR DATOS EN TABLA tarifas
SELECT 
    t.id_tarifa,
    t.id_hotel,
    t.id_tipo,
    t.id_temporada,
    t.precio_base_noche,
    t.precio_persona_adicional,
    temp.fecha_inicio,
    temp.fecha_fin,
    pg_typeof(temp.fecha_inicio) AS tipo_fecha_inicio
FROM tarifas t
INNER JOIN temporadas temp ON t.id_temporada = temp.id_temporada
LIMIT 5;

-- 9. PROBAR FUNCIÓN consultar_tarifas (si existe)
SELECT * FROM consultar_tarifas(
    1::integer,
    1::integer,
    CURRENT_DATE::date
);

-- 9.1. PROBAR CONSULTA DE TARIFAS MANUALMENTE (para verificar sintaxis)
SELECT 
    t.id_tarifa,
    hot.nombre as hotel,
    th.nombre as tipo_habitacion,
    temp.nombre as temporada,
    t.precio_base_noche,
    t.precio_persona_adicional
FROM tarifas t
INNER JOIN hoteles hot ON t.id_hotel = hot.id_hotel
INNER JOIN tipos_habitacion th ON t.id_tipo = th.id_tipo
INNER JOIN temporadas temp ON t.id_temporada = temp.id_temporada
WHERE t.id_hotel = 1
  AND t.id_tipo = 1
  AND CURRENT_DATE::date BETWEEN temp.fecha_inicio::date AND temp.fecha_fin::date
ORDER BY th.nombre;

-- 10. VERIFICAR ESTRUCTURA DE RESULTADO DE calcular_precio_reserva
-- (Para verificar el tipo de dato del campo desglose)
SELECT 
    column_name,
    data_type,
    udt_name
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name IN (
    SELECT relname 
    FROM pg_class 
    WHERE relkind = 'r' 
    AND relname LIKE '%precio%'
  )
LIMIT 20;
