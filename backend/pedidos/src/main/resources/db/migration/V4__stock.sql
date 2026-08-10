CREATE TABLE item (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(50) NOT NULL,
    nombre VARCHAR(200) NOT NULL,
    unidad_medida VARCHAR(20) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100)
);

CREATE UNIQUE INDEX uk_item_sku ON item (sku);

CREATE TABLE lote (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL REFERENCES item (id),
    codigo_lote VARCHAR(100) NOT NULL,
    fecha_ingreso DATE NOT NULL,
    fecha_vencimiento DATE,
    cantidad_ingresada NUMERIC(12,3) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100)
);

CREATE INDEX idx_lote_item ON lote (item_id);

CREATE TABLE movimiento_stock (
    id BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(30) NOT NULL,
    item_id BIGINT NOT NULL REFERENCES item (id),
    lote_id BIGINT REFERENCES lote (id),
    pedido_id BIGINT,
    cantidad NUMERIC(12,3) NOT NULL,
    fecha TIMESTAMP NOT NULL,
    motivo VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100)
);

CREATE INDEX idx_movimiento_stock_item ON movimiento_stock (item_id, fecha);
CREATE INDEX idx_movimiento_stock_lote ON movimiento_stock (lote_id);
CREATE INDEX idx_movimiento_stock_pedido ON movimiento_stock (pedido_id);
