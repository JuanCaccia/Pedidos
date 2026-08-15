CREATE TABLE proveedor (
    id BIGSERIAL PRIMARY KEY,
    razon_social VARCHAR(200) NOT NULL,
    cuit VARCHAR(20) NOT NULL,
    email VARCHAR(150),
    telefono VARCHAR(50),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100)
);

CREATE UNIQUE INDEX uk_proveedor_cuit ON proveedor (cuit);

CREATE TABLE orden_compra (
    id BIGSERIAL PRIMARY KEY,
    numero VARCHAR(20) NOT NULL,
    proveedor_id BIGINT NOT NULL REFERENCES proveedor (id),
    fecha TIMESTAMP NOT NULL,
    estado VARCHAR(30) NOT NULL,
    observaciones VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100)
);

CREATE UNIQUE INDEX uk_orden_compra_numero ON orden_compra (numero);
CREATE INDEX idx_orden_compra_proveedor ON orden_compra (proveedor_id);
CREATE INDEX idx_orden_compra_estado ON orden_compra (estado);

CREATE TABLE orden_compra_linea (
    id BIGSERIAL PRIMARY KEY,
    orden_compra_id BIGINT NOT NULL REFERENCES orden_compra (id) ON DELETE CASCADE,
    item_id BIGINT NOT NULL REFERENCES item (id),
    cantidad_pedida NUMERIC(12,3) NOT NULL,
    cantidad_recibida NUMERIC(12,3) NOT NULL DEFAULT 0,
    precio_unitario NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100)
);

CREATE INDEX idx_orden_compra_linea_oc ON orden_compra_linea (orden_compra_id);
