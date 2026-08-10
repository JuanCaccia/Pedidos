CREATE TABLE ruta (
    id BIGSERIAL PRIMARY KEY,
    zona_id BIGINT NOT NULL REFERENCES zona (id),
    repartidor_id BIGINT NOT NULL REFERENCES usuario (id),
    fecha_jornada DATE NOT NULL,
    estado VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100)
);

CREATE INDEX idx_ruta_fecha ON ruta (fecha_jornada);
CREATE INDEX idx_ruta_repartidor ON ruta (repartidor_id);
CREATE INDEX idx_ruta_estado ON ruta (estado);

CREATE TABLE ruta_pedido (
    id BIGSERIAL PRIMARY KEY,
    ruta_id BIGINT NOT NULL REFERENCES ruta (id) ON DELETE CASCADE,
    pedido_id BIGINT NOT NULL REFERENCES pedido (id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100)
);

CREATE UNIQUE INDEX uk_ruta_pedido ON ruta_pedido (ruta_id, pedido_id);
CREATE INDEX idx_ruta_pedido_pedido ON ruta_pedido (pedido_id);
