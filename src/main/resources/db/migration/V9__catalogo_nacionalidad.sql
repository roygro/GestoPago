-- Catalogo de nacionalidades. El valor que se manda y se guarda es el gentilicio (Mexicano, Estadounidense...).
-- codigo_pais (ISO 3166-1 alfa-2) es solo informativo.
-- Archivo solo ASCII a proposito (los acentos usan escapes unicode de Postgres)
-- para que el checksum de Flyway no cambie si el editor lo guarda con otra codificacion.
-- Para agregar un pais nuevo: INSERT INTO cat_nacionalidad (nacionalidad, pais, codigo_pais) VALUES (...) en una V10.

CREATE TABLE cat_nacionalidad (
    id           SMALLSERIAL  PRIMARY KEY,
    nacionalidad VARCHAR(50)  NOT NULL,
    pais         VARCHAR(60)  NOT NULL,
    codigo_pais  CHAR(2)      NOT NULL,
    activo       BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_cat_nacionalidad_nacionalidad UNIQUE (nacionalidad),
    CONSTRAINT uk_cat_nacionalidad_codigo_pais  UNIQUE (codigo_pais)
);

INSERT INTO cat_nacionalidad (nacionalidad, pais, codigo_pais, activo) VALUES
    ('Mexicano', U&'M\00E9xico', 'MX', TRUE),
    ('Estadounidense', 'Estados Unidos', 'US', TRUE),
    ('Canadiense', U&'Canad\00E1', 'CA', TRUE),
    ('Guatemalteco', 'Guatemala', 'GT', TRUE),
    (U&'Belice\00F1o', 'Belice', 'BZ', TRUE),
    (U&'Salvadore\00F1o', 'El Salvador', 'SV', TRUE),
    (U&'Hondure\00F1o', 'Honduras', 'HN', TRUE),
    (U&'Nicarag\00FCense', 'Nicaragua', 'NI', TRUE),
    ('Costarricense', 'Costa Rica', 'CR', TRUE),
    (U&'Paname\00F1o', U&'Panam\00E1', 'PA', TRUE),
    ('Cubano', 'Cuba', 'CU', TRUE),
    ('Dominicano', U&'Rep\00FAblica Dominicana', 'DO', TRUE),
    ('Haitiano', U&'Hait\00ED', 'HT', TRUE),
    ('Jamaiquino', 'Jamaica', 'JM', TRUE),
    (U&'Puertorrique\00F1o', 'Puerto Rico', 'PR', TRUE),
    ('Colombiano', 'Colombia', 'CO', TRUE),
    ('Venezolano', 'Venezuela', 'VE', TRUE),
    ('Ecuatoriano', 'Ecuador', 'EC', TRUE),
    ('Peruano', U&'Per\00FA', 'PE', TRUE),
    ('Boliviano', 'Bolivia', 'BO', TRUE),
    ('Chileno', 'Chile', 'CL', TRUE),
    ('Argentino', 'Argentina', 'AR', TRUE),
    ('Uruguayo', 'Uruguay', 'UY', TRUE),
    ('Paraguayo', 'Paraguay', 'PY', TRUE),
    (U&'Brasile\00F1o', 'Brasil', 'BR', TRUE),
    (U&'Espa\00F1ol', U&'Espa\00F1a', 'ES', TRUE),
    (U&'Portugu\00E9s', 'Portugal', 'PT', TRUE),
    (U&'Franc\00E9s', 'Francia', 'FR', TRUE),
    (U&'Alem\00E1n', 'Alemania', 'DE', TRUE),
    ('Italiano', 'Italia', 'IT', TRUE),
    (U&'Brit\00E1nico', 'Reino Unido', 'GB', TRUE),
    (U&'Irland\00E9s', 'Irlanda', 'IE', TRUE),
    (U&'Neerland\00E9s', U&'Pa\00EDses Bajos', 'NL', TRUE),
    ('Belga', U&'B\00E9lgica', 'BE', TRUE),
    ('Suizo', 'Suiza', 'CH', TRUE),
    ('Austriaco', 'Austria', 'AT', TRUE),
    ('Sueco', 'Suecia', 'SE', TRUE),
    ('Noruego', 'Noruega', 'NO', TRUE),
    (U&'Dan\00E9s', 'Dinamarca', 'DK', TRUE),
    (U&'Finland\00E9s', 'Finlandia', 'FI', TRUE),
    ('Polaco', 'Polonia', 'PL', TRUE),
    ('Checo', U&'Rep\00FAblica Checa', 'CZ', TRUE),
    (U&'H\00FAngaro', U&'Hungr\00EDa', 'HU', TRUE),
    ('Rumano', 'Rumania', 'RO', TRUE),
    ('Griego', 'Grecia', 'GR', TRUE),
    ('Ucraniano', 'Ucrania', 'UA', TRUE),
    ('Ruso', 'Rusia', 'RU', TRUE),
    ('Turco', U&'Turqu\00EDa', 'TR', TRUE),
    ('Chino', 'China', 'CN', TRUE),
    (U&'Japon\00E9s', U&'Jap\00F3n', 'JP', TRUE),
    ('Surcoreano', 'Corea del Sur', 'KR', TRUE),
    ('Indio', 'India', 'IN', TRUE),
    (U&'Israel\00ED', 'Israel', 'IL', TRUE),
    (U&'Liban\00E9s', U&'L\00EDbano', 'LB', TRUE),
    (U&'Saud\00ED', 'Arabia Saudita', 'SA', TRUE),
    (U&'Emirat\00ED', U&'Emiratos \00C1rabes Unidos', 'AE', TRUE),
    ('Filipino', 'Filipinas', 'PH', TRUE),
    ('Vietnamita', 'Vietnam', 'VN', TRUE),
    (U&'Tailand\00E9s', 'Tailandia', 'TH', TRUE),
    ('Indonesio', 'Indonesia', 'ID', TRUE),
    (U&'Pakistan\00ED', U&'Pakist\00E1n', 'PK', TRUE),
    ('Sudafricano', U&'Sud\00E1frica', 'ZA', TRUE),
    ('Egipcio', 'Egipto', 'EG', TRUE),
    (U&'Marroqu\00ED', 'Marruecos', 'MA', TRUE),
    ('Nigeriano', 'Nigeria', 'NG', TRUE),
    ('Australiano', 'Australia', 'AU', TRUE),
    (U&'Neozeland\00E9s', 'Nueva Zelanda', 'NZ', TRUE),
    -- Solo para datos historicos que no se pudieron clasificar. activo = FALSE:
    -- la API NO lo acepta en altas nuevas, pero la llave foranea lo permite.
    ('No especificado', 'No especificado', 'XX', FALSE);

-- Normaliza la nacionalidad de texto libre (o codigos ISO de un intento anterior) que ya exista en clientes.
-- Reconoce: el gentilicio en masculino o femenino (Mexicano / Mexicana), el pais (Mexico), el codigo (MX),
-- sin importar acentos, mayusculas ni espacios. Americano/Americana se toma como Estadounidense.
-- Lo que no se pueda clasificar pasa a "No especificado".
WITH alias AS (
    SELECT nacionalidad AS destino, regexp_replace(upper(translate(trim(nacionalidad), U&'\00E1\00E9\00ED\00F3\00FA\00FC\00F1\00C1\00C9\00CD\00D3\00DA\00DC\00D1', 'aeiouunAEIOUUN')), '[^A-Z]', '', 'g') AS alias FROM cat_nacionalidad
    UNION
    SELECT nacionalidad, regexp_replace(upper(translate(trim(nacionalidad), U&'\00E1\00E9\00ED\00F3\00FA\00FC\00F1\00C1\00C9\00CD\00D3\00DA\00DC\00D1', 'aeiouunAEIOUUN')), '[^A-Z]', '', 'g') || 'A' FROM cat_nacionalidad
    UNION
    SELECT nacionalidad, regexp_replace(regexp_replace(upper(translate(trim(nacionalidad), U&'\00E1\00E9\00ED\00F3\00FA\00FC\00F1\00C1\00C9\00CD\00D3\00DA\00DC\00D1', 'aeiouunAEIOUUN')), '[^A-Z]', '', 'g'), 'O$', 'A') FROM cat_nacionalidad
    UNION
    SELECT nacionalidad, regexp_replace(upper(translate(trim(pais), U&'\00E1\00E9\00ED\00F3\00FA\00FC\00F1\00C1\00C9\00CD\00D3\00DA\00DC\00D1', 'aeiouunAEIOUUN')), '[^A-Z]', '', 'g') FROM cat_nacionalidad
    UNION
    SELECT nacionalidad, codigo_pais FROM cat_nacionalidad
    UNION
    SELECT 'Estadounidense', 'AMERICANO'
    UNION
    SELECT 'Estadounidense', 'AMERICANA'
)
UPDATE clientes c
SET nacionalidad = COALESCE(
        (SELECT min(a.destino) FROM alias a WHERE a.alias = n.norm),
        'No especificado')
FROM (
    SELECT id, regexp_replace(upper(translate(trim(nacionalidad), U&'\00E1\00E9\00ED\00F3\00FA\00FC\00F1\00C1\00C9\00CD\00D3\00DA\00DC\00D1', 'aeiouunAEIOUUN')), '[^A-Z]', '', 'g') AS norm FROM clientes
) n
WHERE c.id = n.id;

ALTER TABLE clientes
    ADD CONSTRAINT fk_clientes_nacionalidad
    FOREIGN KEY (nacionalidad) REFERENCES cat_nacionalidad (nacionalidad);

CREATE INDEX idx_clientes_nacionalidad ON clientes (nacionalidad);
