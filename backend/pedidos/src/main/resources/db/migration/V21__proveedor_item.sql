-- Catálogo de provisión: qué items provee cada proveedor.
-- PK compuesta (proveedor_id, item_id); activo permite desvincular sin borrar historial.
CREATE TABLE proveedor_item (
    proveedor_id BIGINT NOT NULL REFERENCES proveedor (id),
    item_id BIGINT NOT NULL REFERENCES item (id),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (proveedor_id, item_id)
);

-- Index para consultas inversas ("qué proveedores ofrecen un item") y borrados por proveedor.
CREATE INDEX idx_proveedor_item_item ON proveedor_item (item_id);

-- Backfill: los items que un proveedor ya recibió en un lote lo proveen de facto.
-- Protege la validación de OC para los datos preexistentes.
INSERT INTO proveedor_item (proveedor_id, item_id, activo)
SELECT DISTINCT l.proveedor_id, l.item_id, TRUE
FROM lote l
WHERE l.proveedor_id IS NOT NULL;
