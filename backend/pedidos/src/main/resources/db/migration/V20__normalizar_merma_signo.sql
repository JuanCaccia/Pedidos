-- Normalización del signo de las mermas históricas.
-- Desde el cambio de dominio, las mermas se persisten con signo NEGATIVO
-- (coherente con EGRESO_VENTA) y el disponible se calcula sumando algebraicamente.
-- Las mermas guardadas con signo positivo (+N) antes del cambio se inflarían si
-- se suman, así que se normalizan a su valor negativo.
UPDATE movimiento_stock SET cantidad = -cantidad WHERE tipo = 'MERMA' AND cantidad > 0;
