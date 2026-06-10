# LUVOY — Performance Targets

## Backend

| Endpoint | Target P95 | Motivo |
|----------|-----------|--------|
| POST /itineraries (engine) | < 800ms | Algoritmo com scoring + nearest-neighbor |
| GET /destinations | < 200ms | Lista estática com cache |
| GET /destinations/:id | < 200ms | Query simples + JOIN com POIs |
| GET /itineraries/:id | < 200ms | Query com days + activities |
| GET /itineraries | < 200ms | Lista do usuário |
| POST /auth/login | < 300ms | BCrypt hash comparison |
| POST /auth/signup | < 300ms | BCrypt + insert |
| POST /pdf/export/:id | < 2000ms | Geração de arquivo — tolerância maior |

---

## Banco de Dados

### Índices obrigatórios

```sql
CREATE INDEX idx_pois_destination_id    ON pois(destination_id);
CREATE INDEX idx_pois_budget_tier       ON pois(budget_tier);
CREATE INDEX idx_itineraries_user_id    ON itineraries(user_id);
CREATE INDEX idx_itinerary_days_itin_id ON itinerary_days(itinerary_id);
CREATE INDEX idx_favorites_user_id      ON favorites(user_id);
CREATE INDEX idx_users_email            ON users(email);
```

### Query do engine

A query de filtragem + scoring do engine deve retornar em < 100ms
para até 500 POIs por destino.

Nunca fazer N+1 queries no engine — carregar todos os POIs do destino
em uma única query com os filtros aplicados no SQL (não em memória).

---

## Frontend

| Métrica | Target |
|---------|--------|
| First meaningful paint | < 2s |
| Time to interactive | < 3s |
| Frame rate | 60fps constante |
| Frames lentos (jank) | < 1% do total |

---

## Imagens

- Formato: WebP obrigatório
- Lazy loading em listas
- Cache local: `cached_network_image` package
- Hero images: pré-carregadas na tela anterior antes da transição

---

## Regras gerais

- Não fazer request de API dentro de `build()`
- Providers Riverpod com `keepAlive: true` para dados que não mudam (destinations)
- Não carregar lista completa de POIs no frontend — o engine roda no backend
- JSON de resposta sem campos nulos desnecessários (`@JsonInclude(NON_NULL)` no Spring)
