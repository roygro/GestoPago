-- Catalogo de estado civil.
-- Archivo solo ASCII a proposito (los acentos usan escapes unicode de Postgres)
-- para que el checksum de Flyway no cambie si el editor lo guarda con otra codificacion.

CREATE TABLE cat_estado_civil (
    id          SMALLSERIAL  PRIMARY KEY,
    clave       VARCHAR(20)  NOT NULL,
    descripcion VARCHAR(50)  NOT NULL,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_cat_estado_civil_clave UNIQUE (clave)
);

INSERT INTO cat_estado_civil (clave, descripcion, activo) VALUES
    ('SOLTERO',         'Soltero(a)',                   TRUE),
    ('CASADO',          'Casado(a)',                    TRUE),
    ('DIVORCIADO',      'Divorciado(a)',                TRUE),
    ('VIUDO',           'Viudo(a)',                     TRUE),
    ('UNION_LIBRE',     U&'Uni\00F3n libre',            TRUE),
    ('SEPARADO',        'Separado(a)',                  TRUE),
    -- Solo para datos historicos que no se pudieron clasificar. activo = FALSE:
    -- la API NO lo acepta en altas nuevas, pero la llave foranea lo permite.
    ('NO_ESPECIFICADO', 'No especificado',              FALSE);

-- Normaliza los valores de texto libre que ya existan en clientes
-- (quita acentos, espacios, guiones y parentesis: "Soltero(a)" -> SOLTEROA, "Union libre" -> UNIONLIBRE).
UPDATE clientes c
SET estado_civil = CASE
        WHEN n.norm IN ('SOLTERO', 'SOLTERA', 'SOLTEROA')         THEN 'SOLTERO'
        WHEN n.norm IN ('CASADO', 'CASADA', 'CASADOA')            THEN 'CASADO'
        WHEN n.norm IN ('DIVORCIADO', 'DIVORCIADA', 'DIVORCIADOA') THEN 'DIVORCIADO'
        WHEN n.norm IN ('VIUDO', 'VIUDA', 'VIUDOA')               THEN 'VIUDO'
        WHEN n.norm IN ('UNIONLIBRE')                             THEN 'UNION_LIBRE'
        WHEN n.norm IN ('SEPARADO', 'SEPARADA', 'SEPARADOA')      THEN 'SEPARADO'
        ELSE 'NO_ESPECIFICADO'
    END
FROM (
    SELECT id,
           regexp_replace(
               translate(upper(trim(estado_civil)), U&'\00C1\00C9\00CD\00D3\00DA', 'AEIOU'),
               '[^A-Z]', '', 'g') AS norm
    FROM clientes
) n
WHERE c.id = n.id;

ALTER TABLE clientes
    ADD CONSTRAINT fk_clientes_estado_civil
    FOREIGN KEY (estado_civil) REFERENCES cat_estado_civil (clave);

CREATE INDEX idx_clientes_estado_civil ON clientes (estado_civil);
