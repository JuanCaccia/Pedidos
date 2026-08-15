ALTER TABLE pedido ADD COLUMN express BOOLEAN NOT NULL DEFAULT false;
CREATE INDEX idx_pedido_queue_prioridad ON pedido (estado, express DESC, fecha_creacion ASC);
