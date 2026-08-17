ALTER TABLE orden_compra_linea DROP COLUMN precio_unitario;

ALTER TABLE lote ADD COLUMN precio_unitario DECIMAL(18,4);
