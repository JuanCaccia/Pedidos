CREATE TABLE sustitucion (
    id BIGSERIAL PRIMARY KEY,
    pedido_id BIGINT NOT NULL REFERENCES pedido (id),
    item_original_id BIGINT NOT NULL REFERENCES item (id),
    item_sustituto_id BIGINT NOT NULL REFERENCES item (id),
    cantidad NUMERIC(12,3) NOT NULL,
    diferencia_precio NUMERIC(12,2) NOT NULL DEFAULT 0,
    fecha TIMESTAMP NOT NULL,
    observaciones VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100)
);
CREATE INDEX idx_sustitucion_pedido ON sustitucion (pedido_id);
