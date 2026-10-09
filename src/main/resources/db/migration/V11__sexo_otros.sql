-- El sexo ahora es M, F u Otros (palabra completa). Reemplaza el criterio de la V10 (M, F, O).
-- La columna era CHAR(1): se ensancha a VARCHAR(5) para que quepa "Otros" sin rellenar con espacios.

-- 1) Se quita la restriccion de la V10 antes de cambiar el tipo y los datos
ALTER TABLE clientes DROP CONSTRAINT IF EXISTS ck_clientes_sexo;

-- 2) Ensanchar la columna (los M/F existentes se conservan igual)
ALTER TABLE clientes ALTER COLUMN sexo TYPE VARCHAR(5);

-- 3) Cualquier registro que se haya guardado con la letra O pasa a la palabra Otros
UPDATE clientes SET sexo = 'Otros' WHERE sexo = 'O';

-- 4) Nueva restriccion
ALTER TABLE clientes
    ADD CONSTRAINT ck_clientes_sexo CHECK (sexo IN ('M', 'F', 'Otros'));
