-- AUD-009: estado de ciclo de vida del lote.
-- Patrón consistente con el resto del proyecto: enum en Java + VARCHAR en BD (ej. EstadoPedido).
ALTER TABLE lote ADD COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'VIGENTE';
ALTER TABLE lote ADD CONSTRAINT ck_lote_estado CHECK (estado IN ('VIGENTE', 'AGOTADO', 'VENCIDO', 'DESCARTADO'));
