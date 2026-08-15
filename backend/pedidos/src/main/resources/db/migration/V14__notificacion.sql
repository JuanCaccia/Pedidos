CREATE TABLE notificacion (
    id BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(50) NOT NULL,
    mensaje VARCHAR(500) NOT NULL,
    para_usuario_id BIGINT NOT NULL REFERENCES usuario (id),
    pedido_id BIGINT REFERENCES pedido (id),
    leida BOOLEAN NOT NULL DEFAULT FALSE,
    fecha TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100)
);
CREATE INDEX idx_notificacion_para ON notificacion (para_usuario_id, leida);
