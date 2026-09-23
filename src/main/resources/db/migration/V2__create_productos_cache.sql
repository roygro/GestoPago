CREATE TABLE productos_cache (
    id SERIAL PRIMARY KEY,
    cache_key VARCHAR(100) NOT NULL,
    valor TEXT NOT NULL,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_productos_cache_key UNIQUE (cache_key)
);