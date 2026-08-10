CREATE TABLE cliente (
    id BIGSERIAL PRIMARY KEY,
    razon_social VARCHAR(200) NOT NULL,
    cuit VARCHAR(20) NOT NULL,
    email VARCHAR(150),
    telefono VARCHAR(50),
    domicilio VARCHAR(255),
    zona_id BIGINT NOT NULL REFERENCES zona (id),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100)
);

CREATE UNIQUE INDEX uk_cliente_cuit ON cliente (cuit);
