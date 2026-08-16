CREATE TABLE categoria (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    activo BOOLEAN NOT NULL DEFAULT true,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now()
);

INSERT INTO categoria (nombre) SELECT DISTINCT TRIM(categoria) FROM item
    WHERE categoria IS NOT NULL AND TRIM(categoria) <> '';

ALTER TABLE item ADD COLUMN categoria_id BIGINT REFERENCES categoria(id);

UPDATE item SET categoria_id = c.id FROM categoria c WHERE c.nombre = TRIM(item.categoria);

ALTER TABLE item DROP COLUMN categoria;
