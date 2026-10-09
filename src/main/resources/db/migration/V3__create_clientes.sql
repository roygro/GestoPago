CREATE TABLE clientes (
    id                   BIGSERIAL PRIMARY KEY,
    nombre               VARCHAR(50)  NOT NULL,
    segundo_nombre       VARCHAR(50),
    apellido_paterno     VARCHAR(50)  NOT NULL,
    apellido_materno     VARCHAR(50)  NOT NULL,
    fecha_nacimiento     DATE         NOT NULL,
    curp                 CHAR(18)     NOT NULL,
    rfc                  VARCHAR(13)  NOT NULL,
    sexo                 CHAR(1)      NOT NULL CHECK (sexo IN ('M','F')),
    nacionalidad         VARCHAR(50)  NOT NULL,
    estado_civil         VARCHAR(20)  NOT NULL,
    correo_electronico   VARCHAR(100) NOT NULL,
    telefono_movil       CHAR(10)     NOT NULL,
    telefono_alternativo CHAR(10),
    ocupacion            VARCHAR(100) NOT NULL,
    empresa              VARCHAR(150),
    ingreso_mensual      NUMERIC(12,2) NOT NULL CHECK (ingreso_mensual > 0),
    activo               BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_registro       TIMESTAMP    NOT NULL DEFAULT now(),
    fecha_actualizacion  TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uk_clientes_curp   UNIQUE (curp),
    CONSTRAINT uk_clientes_rfc    UNIQUE (rfc),
    CONSTRAINT uk_clientes_correo UNIQUE (correo_electronico)
);

CREATE INDEX idx_clientes_activo ON clientes (activo);
CREATE INDEX idx_clientes_fecha_registro ON clientes (fecha_registro);