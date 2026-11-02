-- Preenche heroImageUrl dos 11 destinos que não tinham nenhuma foto —
-- curadoria manual via Unsplash (mesmo processo/padrão de qualidade da
-- leva original de 91), cada foto conferida visualmente (não só metadata)
-- antes de entrar aqui. Ver luvoy-pendencias.md, 2026-11-02.
--
-- IMPORTANTE: rodar isso DEPOIS do enriquecimento de fotos extras
-- (POST /api/v1/admin/destinations/enrich-images) — esse endpoint usa
-- heroImageUrl vazio como sinal de que o destino ainda não tem NENHUMA
-- foto (meta de 2 fotos extras); rodando este UPDATE antes faria esses 11
-- destinos serem tratados como "já tem 1 foto" (meta de só 1 extra).

UPDATE destinations SET hero_image_url = 'https://images.unsplash.com/photo-1764099363031-bdd67ecd737b?w=800&auto=format&fit=crop' WHERE slug = 'capadocia';
UPDATE destinations SET hero_image_url = 'https://images.unsplash.com/photo-1569383746724-6f1b882b8f46?w=800&auto=format&fit=crop' WHERE slug = 'chefchaouen';
UPDATE destinations SET hero_image_url = 'https://images.unsplash.com/photo-1729021540913-41ba79ee3c84?w=800&auto=format&fit=crop' WHERE slug = 'goteborg';
UPDATE destinations SET hero_image_url = 'https://images.unsplash.com/photo-1547668932-54be495edf50?w=800&auto=format&fit=crop' WHERE slug = 'ibiza';
UPDATE destinations SET hero_image_url = 'https://images.unsplash.com/photo-1736157515877-66ac6da03a2d?w=800&auto=format&fit=crop' WHERE slug = 'innsbruck';
UPDATE destinations SET hero_image_url = 'https://images.unsplash.com/photo-1690051057165-63e577779fc2?w=800&auto=format&fit=crop' WHERE slug = 'madeira';
UPDATE destinations SET hero_image_url = 'https://images.unsplash.com/photo-1546402956-b698857acdc1?w=800&auto=format&fit=crop' WHERE slug = 'mostar';
UPDATE destinations SET hero_image_url = 'https://images.unsplash.com/photo-1592297764373-c136db199cb3?w=800&auto=format&fit=crop' WHERE slug = 'mykonos';
UPDATE destinations SET hero_image_url = 'https://images.unsplash.com/photo-1670336864234-2d3e4a30ab44?w=800&auto=format&fit=crop' WHERE slug = 'orlando';
UPDATE destinations SET hero_image_url = 'https://images.unsplash.com/photo-1703757723822-c51c53c9359c?w=800&auto=format&fit=crop' WHERE slug = 'rovaniemi';
UPDATE destinations SET hero_image_url = 'https://images.unsplash.com/photo-1668518394615-42a3d0daf6b5?w=800&auto=format&fit=crop' WHERE slug = 'york';
