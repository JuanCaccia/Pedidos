CREATE TABLE remito (
    id BIGSERIAL PRIMARY KEY,
    numero VARCHAR(20) NOT NULL,
    pedido_id BIGINT NOT NULL REFERENCES pedido (id),
    cliente_id BIGINT NOT NULL REFERENCES cliente (id),
    fecha_emision TIMESTAMP NOT NULL,
    monto_total NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100)
);

CREATE UNIQUE INDEX uk_remito_numero ON remito (numero);
CREATE INDEX idx_remito_pedido ON remito (pedido_id);
CREATE INDEX idx_remito_cliente ON remito (cliente_id);

CREATE TABLE remito_linea (
    id BIGSERIAL PRIMARY KEY,
    remito_id BIGINT NOT NULL REFERENCES remito (id) ON DELETE CASCADE,
    item_id BIGINT NOT NULL REFERENCES item (id),
    cantidad NUMERIC(12,3) NOT NULL,
    precio_unitario NUMERIC(12,2) NOT NULL,
    subtotal NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100)
);

CREATE INDEX idx_remito_linea_remito ON remito_linea (remito_id);

CREATE TABLE cobranza (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL REFERENCES cliente (id),
    pedido_id BIGINT REFERENCES pedido (id),
    monto NUMERIC(12,2) NOT NULL,
    forma_pago VARCHAR(20) NOT NULL,
    fecha TIMESTAMP NOT NULL,
    observaciones VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100)
);

CREATE INDEX idx_cobranza_cliente ON cobranza (cliente_id);
CREATE INDEX idx_cobranza_pedido ON cobranza (pedido_id);
