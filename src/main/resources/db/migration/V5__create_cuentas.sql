CREATE TABLE cuentas (
    id              BIGSERIAL PRIMARY KEY,
    cliente_id      BIGINT      NOT NULL,
    numero_cuenta   VARCHAR(16) NOT NULL,
    fecha_apertura  TIMESTAMP   NOT NULL DEFAULT now(),
    estatus         VARCHAR(10) NOT NULL DEFAULT 'ACTIVA'
                        CHECK (estatus IN ('ACTIVA','INACTIVA')),
    CONSTRAINT fk_cuentas_cliente FOREIGN KEY (cliente_id)
        REFERENCES clientes (id) ON DELETE CASCADE,
    CONSTRAINT uk_cuentas_numero UNIQUE (numero_cuenta)
);

CREATE INDEX idx_cuentas_cliente ON cuentas (cliente_id);