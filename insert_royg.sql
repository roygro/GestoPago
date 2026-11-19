DO $$
DECLARE
    v_cliente_id BIGINT;
    v_cuenta_id BIGINT;
BEGIN
    -- 1. Insertar Cliente con el correo indicado
    INSERT INTO clientes (
        nombre, segundo_nombre, apellido_paterno, apellido_materno,
        fecha_nacimiento, curp, rfc, sexo, nacionalidad, estado_civil,
        correo_electronico, telefono_movil, telefono_alternativo,
        ocupacion, empresa, ingreso_mensual, activo, fecha_registro, fecha_actualizacion
    ) VALUES (
        'Roy', 'Alan', 'Guerrero', 'Reyes',
        '1999-08-15', 'GURR990815HGTRYS01', 'GURR990815XYZ', 'M', 'Mexicano', 'SOLTERO',
        'royguerrero106@gmail.com', '4181234567', '4187654321',
        'Ingeniero de Software', 'UTNG Dev', 25000.00, true, NOW(), NOW()
    )
    RETURNING id INTO v_cliente_id;

    -- 2. Insertar Domicilio
    INSERT INTO domicilios (
        cliente_id, calle, numero_exterior, numero_interior,
        colonia, municipio, estado, codigo_postal, pais
    ) VALUES (
        v_cliente_id, 'Av Universidad', '100', 'A',
        'Educacion', 'Dolores Hidalgo', 'Guanajuato', '37800', 'Mexico'
    );

    -- 3. Insertar Cuenta Bancaria con número único de 16 dígitos
    INSERT INTO cuentas (
        numero_cuenta, estatus, cliente_id
    ) VALUES (
        '41523134' || LPAD((FLOOR(RANDOM() * 100000000))::TEXT, 8, '0'), 'ACTIVA', v_cliente_id
    )
    RETURNING id INTO v_cuenta_id;

    -- 4. Insertar Saldo Inicial de $2,500.00 MXN para pagar servicios
    INSERT INTO saldos (
        cuenta_id, saldo_disponible, fecha_actualizacion
    ) VALUES (
        v_cuenta_id, 2500.00, NOW()
    );

    -- 5. Insertar Credenciales de Acceso
    -- Usuario: royg
    -- Password: password123 (Hash BCrypt verificado por Spring Security)
    INSERT INTO login (
        cliente_id, usuario, password_hash, jwt_token, jwt_fecha_expiracion,
        sesion_activa, fecha_inicio_sesion, ultima_actividad, minutos_expiracion_inactividad
    ) VALUES (
        v_cliente_id,
        'royg',
        '$2a$10$2/IQcdMiqeA/OV6X0T7qROUN476/YlM6VALuJeXUKJ.Fg6E0w8kNq',
        'INIT_TOKEN_' || v_cliente_id,
        NOW() + INTERVAL '30 days',
        false,
        NOW(),
        NOW(),
        60
    );

    RAISE NOTICE 'Cliente creado con éxito: ID=%, Usuario=royg, Password=password123', v_cliente_id;
END $$;
