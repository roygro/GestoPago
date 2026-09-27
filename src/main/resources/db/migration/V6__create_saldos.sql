CREATE TABLE saldos (
    id                  BIGSERIAL PRIMARY KEY,
    cuenta_id           BIGINT        NOT NULL,
    saldo_disponible    NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (saldo_disponible >= 0),
    fecha_actualizacion TIMESTAMP     NOT NULL DEFAULT now(),
    CONSTRAINT fk_saldos_cuenta FOREIGN KEY (cuenta_id)
        REFERENCES cuentas (id) ON DELETE CASCADE,
    CONSTRAINT uk_saldos_cuenta UNIQUE (cuenta_id)
);