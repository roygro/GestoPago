CREATE TABLE domicilios (
    id               BIGSERIAL PRIMARY KEY,
    cliente_id       BIGINT       NOT NULL,
    calle            VARCHAR(100) NOT NULL,
    numero_exterior  VARCHAR(10)  NOT NULL,
    numero_interior  VARCHAR(10),
    colonia          VARCHAR(100) NOT NULL,
    municipio        VARCHAR(100) NOT NULL,
    estado           VARCHAR(100) NOT NULL,
    codigo_postal    CHAR(5)      NOT NULL,
    pais             VARCHAR(50)  NOT NULL DEFAULT 'México',
    CONSTRAINT fk_domicilios_cliente FOREIGN KEY (cliente_id)
        REFERENCES clientes (id) ON DELETE CASCADE,
    CONSTRAINT uk_domicilios_cliente UNIQUE (cliente_id)
);