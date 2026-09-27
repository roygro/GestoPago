CREATE TABLE login (
    id                              BIGSERIAL PRIMARY KEY,
    cliente_id                      BIGINT           NOT NULL,

    -- Credenciales del usuario (una copia por cada intento de inicio de sesión)
    usuario                         VARCHAR(50)      NOT NULL,
    password_hash                   TEXT             NOT NULL,

    -- JWT / control de sesión
    jwt_token                       TEXT             NOT NULL,
    jwt_fecha_expiracion            TIMESTAMP        NOT NULL,
    sesion_activa                   BOOLEAN          NOT NULL DEFAULT TRUE,
    fecha_inicio_sesion             TIMESTAMP        NOT NULL DEFAULT now(),
    ultima_actividad                TIMESTAMP        NOT NULL DEFAULT now(),
    minutos_expiracion_inactividad  INTEGER          NOT NULL DEFAULT 5,

    -- Datos biométricos faciales de ese intento de login
    distancia_interocular           DOUBLE PRECISION,
    ancho_rostro                    DOUBLE PRECISION,
    confianza_deteccion             NUMERIC(5,2)     CHECK (confianza_deteccion BETWEEN 0 AND 100),
    num_puntos_referencia           INTEGER,
    plantilla_facial                TEXT,

    CONSTRAINT fk_login_cliente FOREIGN KEY (cliente_id)
        REFERENCES clientes (id) ON DELETE CASCADE,
    CONSTRAINT uk_login_jwt_token UNIQUE (jwt_token)
);

CREATE INDEX idx_login_cliente ON login (cliente_id);
CREATE INDEX idx_login_sesion_activa ON login (sesion_activa);
CREATE INDEX idx_login_fecha_inicio ON login (fecha_inicio_sesion);