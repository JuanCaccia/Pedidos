CREATE TABLE pedido (
    id BIGSERIAL PRIMARY KEY,
    numero VARCHAR(20) NOT NULL,
    cliente_id BIGINT NOT NULL REFERENCES cliente (id),
    vendedor_id BIGINT NOT NULL REFERENCES usuario (id),
    pedido_padre_id BIGINT REFERENCES pedido (id),
    estado VARCHAR(30) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL,
    fecha_jornada DATE,
    observaciones VARCHAR(255),
    total NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100)
);

CREATE UNIQUE INDEX uk_pedido_numero ON pedido (numero);
CREATE INDEX idx_pedido_estado ON pedido (estado);
CREATE INDEX idx_pedido_cliente ON pedido (cliente_id);
CREATE INDEX idx_pedido_padre ON pedido (pedido_padre_id);

CREATE TABLE pedido_item (
    id BIGSERIAL PRIMARY KEY,
    pedido_id BIGINT NOT NULL REFERENCES pedido (id) ON DELETE CASCADE,
    item_id BIGINT NOT NULL REFERENCES item (id),
    cantidad_pedida NUMERIC(12,3) NOT NULL,
    cantidad_reservada NUMERIC(12,3) NOT NULL DEFAULT 0,
    cantidad_entregada NUMERIC(12,3) NOT NULL DEFAULT 0,
    precio_unitario NUMERIC(12,2) NOT NULL,
    pendiente_stock BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100)
);

CREATE INDEX idx_pedido_item_pedido ON pedido_item (pedido_id);

ALTER TABLE movimiento_stock
    ADD CONSTRAINT fk_movimiento_stock_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id);
