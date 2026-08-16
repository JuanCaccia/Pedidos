ALTER TABLE lote ADD COLUMN proveedor_id BIGINT REFERENCES proveedor (id);

CREATE INDEX idx_lote_proveedor ON lote (proveedor_id);
