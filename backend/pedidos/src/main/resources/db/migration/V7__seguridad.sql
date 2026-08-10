CREATE EXTENSION IF NOT EXISTS pgcrypto;

UPDATE usuario SET password_hash = crypt('admin123', gen_salt('bf'))
WHERE email = 'admin@pedidos.com' AND password_hash = 'admin123';

UPDATE usuario SET password_hash = crypt('repartidor123', gen_salt('bf'))
WHERE email = 'repartidor@pedidos.com' AND password_hash = 'repartidor123';

UPDATE usuario SET password_hash = crypt('secreto123', gen_salt('bf'))
WHERE email = 'v1@test.com' AND password_hash = 'secreto123';
