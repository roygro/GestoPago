CREATE TABLE transacciones_pagos (
    id                  BIGSERIAL PRIMARY KEY,
    numero_cuenta       VARCHAR(16)      NOT NULL,
    id_servicio         INTEGER          NOT NULL,
    id_producto         INTEGER          NOT NULL,
    monto               NUMERIC(15,2)    NOT NULL,
    telefono            VARCHAR(20),
    referencia          VARCHAR(100),
    upc                 VARCHAR(15)      NOT NULL UNIQUE,
    id_tx_proveedor     VARCHAR(60),
    num_autorizacion    VARCHAR(60),
    codigo_respuesta    VARCHAR(10),
    mensaje_respuesta   TEXT,
    payload_cifrado     TEXT,
    estatus             VARCHAR(30)      NOT NULL DEFAULT 'INICIADA'
                            CHECK (estatus IN ('INICIADA', 'EXITOSA', 'RECHAZADA', 'PENDIENTE_CONCILIACION')),
    fecha_creacion      TIMESTAMP        NOT NULL DEFAULT now(),
    fecha_actualizacion TIMESTAMP        NOT NULL DEFAULT now()
);

CREATE INDEX idx_tx_pagos_cuenta ON transacciones_pagos (numero_cuenta);
CREATE INDEX idx_tx_pagos_upc ON transacciones_pagos (upc);
CREATE INDEX idx_tx_pagos_fecha ON transacciones_pagos (fecha_creacion);
