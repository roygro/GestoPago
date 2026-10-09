-- El sexo ahora acepta M, F u O (otro).
-- La restriccion original se creo en linea en V3 (sin nombre), por eso Postgres la nombro clientes_sexo_check.
ALTER TABLE clientes DROP CONSTRAINT IF EXISTS clientes_sexo_check;

ALTER TABLE clientes
    ADD CONSTRAINT ck_clientes_sexo CHECK (sexo IN ('M', 'F', 'O'));
