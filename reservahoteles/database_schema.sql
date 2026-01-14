

-- =====================================================
-- TABLA: hoteles
-- =====================================================
CREATE TABLE IF NOT EXISTS hoteles (
    id_hotel SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    ubicacion VARCHAR(200),
    cupo_maximo_personas INT NOT NULL,
    activo BOOLEAN DEFAULT TRUE
);

-- =====================================================
-- TABLA: tipos_habitacion
-- =====================================================
CREATE TABLE IF NOT EXISTS tipos_habitacion (
    id_tipo SERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    capacidad_personas INT NOT NULL,
    descripcion TEXT
);

-- =====================================================
-- TABLA: inventario_habitaciones (POOL)
-- =====================================================
CREATE TABLE IF NOT EXISTS inventario_habitaciones (
    id_inventario SERIAL PRIMARY KEY,
    id_hotel INT NOT NULL,
    id_tipo INT NOT NULL,
    cantidad_total INT NOT NULL CHECK (cantidad_total > 0),
    FOREIGN KEY (id_hotel) REFERENCES hoteles(id_hotel) ON DELETE CASCADE,
    FOREIGN KEY (id_tipo) REFERENCES tipos_habitacion(id_tipo) ON DELETE RESTRICT,
    UNIQUE (id_hotel, id_tipo)
);

-- =====================================================
-- TABLA: temporadas
-- =====================================================
CREATE TABLE IF NOT EXISTS temporadas (
    id_temporada SERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    anio INT NOT NULL,
    descripcion TEXT,
    CHECK (fecha_fin > fecha_inicio)
);

-- =====================================================
-- TABLA: tarifas
-- =====================================================
CREATE TABLE IF NOT EXISTS tarifas (
    id_tarifa SERIAL PRIMARY KEY,
    id_hotel INT NOT NULL,
    id_tipo INT NOT NULL,
    id_temporada INT NOT NULL,
    precio_base_noche DECIMAL(10,2) NOT NULL CHECK (precio_base_noche > 0),
    precio_persona_adicional DECIMAL(10,2) DEFAULT 0 CHECK (precio_persona_adicional >= 0),
    FOREIGN KEY (id_hotel) REFERENCES hoteles(id_hotel) ON DELETE CASCADE,
    FOREIGN KEY (id_tipo) REFERENCES tipos_habitacion(id_tipo) ON DELETE RESTRICT,
    FOREIGN KEY (id_temporada) REFERENCES temporadas(id_temporada) ON DELETE RESTRICT,
    UNIQUE (id_hotel, id_tipo, id_temporada)
);

-- =====================================================
-- TABLA: reservas
-- =====================================================
CREATE TABLE IF NOT EXISTS reservas (
    id_reserva SERIAL PRIMARY KEY,
    id_hotel INT NOT NULL,
    id_tipo INT NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    numero_personas INT NOT NULL CHECK (numero_personas > 0),
    cantidad_habitaciones INT NOT NULL CHECK (cantidad_habitaciones > 0),
    total_calculado DECIMAL(10,2) NOT NULL CHECK (total_calculado >= 0),
    fecha_creacion TIMESTAMP DEFAULT NOW(),
    estado VARCHAR(20) DEFAULT 'Confirmada' CHECK (estado IN ('Confirmada', 'Cancelada', 'Completada')),
    FOREIGN KEY (id_hotel) REFERENCES hoteles(id_hotel) ON DELETE RESTRICT,
    FOREIGN KEY (id_tipo) REFERENCES tipos_habitacion(id_tipo) ON DELETE RESTRICT,
    CHECK (fecha_fin > fecha_inicio)
);

-- =====================================================
-- FUNCIÓN: verificar_disponibilidad_pool
-- Verifica la disponibilidad de habitaciones en un pool
-- =====================================================
CREATE OR REPLACE FUNCTION verificar_disponibilidad_pool(
    p_id_hotel INT,
    p_id_tipo INT,
    p_fecha_inicio DATE,
    p_fecha_fin DATE
)
RETURNS TABLE (
    tipo_habitacion VARCHAR,
    cantidad_total INT,
    cantidad_disponible INT,
    capacidad_personas INT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        th.nombre::VARCHAR as tipo_habitacion,
        COALESCE(ih.cantidad_total, 0)::INTEGER as cantidad_total,
        GREATEST(0, COALESCE(ih.cantidad_total, 0) - COALESCE(
            (SELECT SUM(r.cantidad_habitaciones)
             FROM reservas r
             WHERE r.id_hotel = p_id_hotel
               AND r.id_tipo = p_id_tipo
               AND r.estado = 'Confirmada'
               AND NOT (r.fecha_fin <= p_fecha_inicio OR r.fecha_inicio >= p_fecha_fin)
            ), 0
        ))::INTEGER as cantidad_disponible,
        th.capacidad_personas::INTEGER
    FROM tipos_habitacion th
    LEFT JOIN inventario_habitaciones ih ON th.id_tipo = ih.id_tipo AND ih.id_hotel = p_id_hotel
    WHERE th.id_tipo = p_id_tipo
    GROUP BY th.id_tipo, th.nombre, th.capacidad_personas, ih.cantidad_total;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- FUNCIÓN: calcular_precio_reserva
-- Calcula el precio total de una reserva
-- =====================================================
CREATE OR REPLACE FUNCTION calcular_precio_reserva(
    p_id_hotel INT,
    p_id_tipo INT,
    p_fecha_inicio DATE,
    p_fecha_fin DATE,
    p_cantidad_habitaciones INT,
    p_numero_personas INT
)
RETURNS TABLE (
    precio_total DECIMAL,
    precio_por_noche DECIMAL,
    numero_noches INT,
    temporada VARCHAR,
    desglose JSONB
) AS $$
DECLARE
    v_num_noches INT;
    v_capacidad_base INT;
    v_personas_extra INT;
    v_precio_base DECIMAL;
    v_precio_adicional DECIMAL;
    v_precio_total DECIMAL;
    v_temporada VARCHAR;
BEGIN
    -- Calcular número de noches
    v_num_noches := p_fecha_fin - p_fecha_inicio;
    
    -- Obtener capacidad base del tipo de habitación
    SELECT capacidad_personas INTO v_capacidad_base
    FROM tipos_habitacion
    WHERE id_tipo = p_id_tipo;
    
    -- Calcular personas extra
    v_personas_extra := GREATEST(0, p_numero_personas - (v_capacidad_base * p_cantidad_habitaciones));
    
    -- Obtener tarifas
    SELECT t.precio_base_noche, t.precio_persona_adicional, temp.nombre
    INTO v_precio_base, v_precio_adicional, v_temporada
    FROM tarifas t
    INNER JOIN temporadas temp ON t.id_temporada = temp.id_temporada
    WHERE t.id_hotel = p_id_hotel
      AND t.id_tipo = p_id_tipo
      AND p_fecha_inicio BETWEEN temp.fecha_inicio AND temp.fecha_fin
    LIMIT 1;
    
    -- Calcular precio total
    v_precio_total := (v_precio_base * v_num_noches * p_cantidad_habitaciones) + 
                      (v_precio_adicional * v_personas_extra * v_num_noches);
    
    RETURN QUERY
    SELECT 
        v_precio_total as precio_total,
        v_precio_base as precio_por_noche,
        v_num_noches as numero_noches,
        v_temporada as temporada,
        jsonb_build_object(
            'precio_base_noche', v_precio_base,
            'numero_noches', v_num_noches,
            'cantidad_habitaciones', p_cantidad_habitaciones,
            'subtotal_habitaciones', v_precio_base * v_num_noches * p_cantidad_habitaciones,
            'personas_extra', v_personas_extra,
            'precio_persona_adicional', v_precio_adicional,
            'subtotal_personas_extra', v_precio_adicional * v_personas_extra * v_num_noches
        ) as desglose;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- FUNCIÓN: crear_reserva
-- Crea una nueva reserva en el sistema
-- =====================================================
CREATE OR REPLACE FUNCTION crear_reserva(
    p_id_hotel INT,
    p_id_tipo INT,
    p_fecha_inicio DATE,
    p_fecha_fin DATE,
    p_numero_personas INT,
    p_cantidad_habitaciones INT
)
RETURNS TABLE (
    id_reserva INT,
    exito BOOLEAN,
    mensaje VARCHAR,
    total_calculado DECIMAL
) AS $$
DECLARE
    v_precio_total DECIMAL;
    v_id_reserva INT;
    v_disponibilidad INT;
BEGIN
    -- Verificar disponibilidad
    SELECT cantidad_disponible INTO v_disponibilidad
    FROM verificar_disponibilidad_pool(p_id_hotel, p_id_tipo, p_fecha_inicio, p_fecha_fin)
    LIMIT 1;
    
    IF v_disponibilidad < p_cantidad_habitaciones THEN
        RETURN QUERY
        SELECT 
            NULL::INT as id_reserva,
            false as exito,
            'No hay suficientes habitaciones disponibles'::VARCHAR as mensaje,
            0::DECIMAL as total_calculado;
        RETURN;
    END IF;
    
    -- Calcular precio
    SELECT precio_total INTO v_precio_total
    FROM calcular_precio_reserva(
        p_id_hotel, 
        p_id_tipo, 
        p_fecha_inicio, 
        p_fecha_fin, 
        p_cantidad_habitaciones, 
        p_numero_personas
    )
    LIMIT 1;
    
    -- Crear reserva
    INSERT INTO reservas (
        id_hotel,
        id_tipo,
        fecha_inicio,
        fecha_fin,
        numero_personas,
        cantidad_habitaciones,
        total_calculado,
        estado
    ) VALUES (
        p_id_hotel,
        p_id_tipo,
        p_fecha_inicio,
        p_fecha_fin,
        p_numero_personas,
        p_cantidad_habitaciones,
        v_precio_total,
        'Confirmada'
    )
    RETURNING id_reserva INTO v_id_reserva;
    
    RETURN QUERY
    SELECT 
        v_id_reserva as id_reserva,
        true as exito,
        'Reserva creada exitosamente'::VARCHAR as mensaje,
        v_precio_total as total_calculado;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- FUNCIÓN: obtener_tarifas (No Usada)
-- Obtiene las tarifas aplicables para un hotel y tipo
-- =====================================================
CREATE OR REPLACE FUNCTION obtener_tarifas(
    p_id_hotel INT,
    p_id_tipo INT,
    p_fecha_inicio DATE
)
RETURNS TABLE (
    id_tarifa INT,
    hotel VARCHAR,
    tipo_habitacion VARCHAR,
    temporada VARCHAR,
    precio_base_noche DECIMAL,
    precio_persona_adicional DECIMAL
) AS $$
BEGIN
    RETURN QUERY
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
    WHERE t.id_hotel = p_id_hotel
      AND (p_id_tipo IS NULL OR t.id_tipo = p_id_tipo)
      AND p_fecha_inicio BETWEEN temp.fecha_inicio AND temp.fecha_fin
    ORDER BY th.nombre;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- FIN DEL SCRIPT
-- =====================================================
