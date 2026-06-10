# LUVOY — API Endpoints

## Base URL

`http://localhost:8080/api/v1`

## Autenticação

Todas as rotas (exceto signup, login e listagem pública de destinos/coleções) exigem:
```
Authorization: Bearer <jwt_token>
```

---

## Auth

### POST /auth/signup

Request:
```json
{
  "name": "João Silva",
  "email": "joao@email.com",
  "password": "senha123"
}
```

Response 201:
```json
{
  "token": "<jwt>",
  "user": {
    "id": "uuid",
    "name": "João Silva",
    "email": "joao@email.com",
    "onboardingCompleted": false
  }
}
```

Erros:
- `400` — campo ausente ou email inválido
- `409` — email já cadastrado

---

### POST /auth/login

Request:
```json
{
  "email": "joao@email.com",
  "password": "senha123"
}
```

Response 200:
```json
{
  "token": "<jwt>",
  "user": {
    "id": "uuid",
    "name": "João Silva",
    "email": "joao@email.com",
    "onboardingCompleted": true
  }
}
```

Erros:
- `400` — campo ausente
- `401` — credenciais inválidas

---

## Users

### GET /users/me

Response 200:
```json
{
  "id": "uuid",
  "name": "João Silva",
  "email": "joao@email.com",
  "avatarUrl": null,
  "onboardingCompleted": true
}
```

Erros:
- `401` — token ausente ou inválido

---

### PATCH /users/preferences

Request:
```json
{
  "budget": "luxury",
  "rhythm": "moderate",
  "groupType": "couple",
  "interests": ["gastronomia", "cultura", "natureza"]
}
```

Response 200:
```json
{
  "message": "Preferences updated"
}
```

Erros:
- `400` — valor de enum inválido
- `401` — não autenticado

---

## Destinations

Rotas públicas — não exigem autenticação.

### GET /destinations

Response 200:
```json
[
  {
    "id": "uuid",
    "slug": "paris",
    "name": "Paris",
    "country": "França",
    "city": "Paris",
    "tagline": "A capital do silêncio luxuoso",
    "heroImageUrl": "https://...",
    "signature": "Citypharma · Buly 1803 · Le Perchoir",
    "tags": ["romantico", "luxo", "cultura"]
  }
]
```

Retorna `[]` quando não há destinos ativos. Nunca retorna 500.

---

### GET /destinations/:slug

Parâmetro: `slug` (string, ex: `paris`, `costa-amalfitana`)

Response 200:
```json
{
  "id": "uuid",
  "slug": "paris",
  "name": "Paris",
  "country": "França",
  "city": "Paris",
  "tagline": "A capital do silêncio luxuoso",
  "description": "Onde cada esquina é uma promessa de elegância...",
  "heroImageUrl": "https://...",
  "signature": "Citypharma · Buly 1803 · Le Perchoir",
  "tags": ["romantico", "luxo"],
  "sections": [
    {
      "sectionType": "RESTAURANTES",
      "orderIndex": 1,
      "content": [
        { "name": "Le Petit Atelier", "desc": "Bistrot íntimo · 12 lugares · menu degustação sazonal." },
        { "name": "Maison Verre", "desc": "Estrelado Michelin · reserva com 3 meses de antecedência." }
      ]
    }
  ],
  "pois": [
    {
      "id": "uuid",
      "title": "Museu do Louvre",
      "category": "MUSEUM",
      "budgetTier": "MODERATE",
      "latitude": 48.8606,
      "longitude": 2.3376
    }
  ]
}
```

Erros:
- `404` — destino não encontrado

---

## Collections

Rotas públicas — não exigem autenticação.

### GET /collections

Response 200:
```json
[
  {
    "id": "uuid",
    "slug": "romantic",
    "title": "Europa",
    "italic": "Romântica",
    "eyebrow": "Para dois",
    "description": "Cidades para se apaixonar — luz dourada, jantares íntimos e ruas que pedem mãos dadas.",
    "orderIndex": 0,
    "destinations": [
      {
        "slug": "paris",
        "name": "Paris",
        "country": "França",
        "heroImageUrl": "https://...",
        "tagline": "A capital do silêncio luxuoso"
      }
    ]
  }
]
```

---

### GET /collections/:slug

Parâmetro: `slug` (string, ex: `romantic`, `hidden-gems`)

Response 200: estrutura completa igual ao item da lista acima.

Erros:
- `404` — coleção não encontrada

---

## Itineraries

### POST /itineraries

Request:
```json
{
  "destinationId": "uuid",
  "startDate": "2026-08-01",
  "endDate": "2026-08-05"
}
```

Response 201:
```json
{
  "id": "uuid",
  "title": "Paris — 5 dias",
  "destination": { "id": "uuid", "name": "Paris", "city": "Paris" },
  "startDate": "2026-08-01",
  "endDate": "2026-08-05",
  "days": [
    {
      "dayNumber": 1,
      "date": "2026-08-01",
      "activities": [
        {
          "slot": "MORNING",
          "orderInSlot": 1,
          "poi": {
            "id": "uuid",
            "title": "Museu do Louvre",
            "category": "MUSEUM",
            "address": "Rue de Rivoli, Paris",
            "latitude": 48.8606,
            "longitude": 2.3376,
            "averageDurationMinutes": 180
          }
        }
      ]
    }
  ]
}
```

Erros:
- `400` — datas inválidas (start > end, data no passado)
- `401` — não autenticado
- `404` — destino não encontrado
- `422` — onboarding não concluído
- `422` — destino com POIs insuficientes (< 5 ativos)

---

### GET /itineraries

Response 200:
```json
[
  {
    "id": "uuid",
    "title": "Paris — 5 dias",
    "destination": { "id": "uuid", "name": "Paris", "heroImageUrl": "https://..." },
    "startDate": "2026-08-01",
    "endDate": "2026-08-05",
    "createdAt": "2026-06-01T10:00:00Z"
  }
]
```

---

### GET /itineraries/:id

Response 200: estrutura completa igual ao POST /itineraries.

Erros:
- `404` — roteiro não encontrado

---

### DELETE /itineraries/:id

Response 204 (sem body) — soft delete.

Erros:
- `403` — roteiro pertence a outro usuário
- `404` — roteiro não encontrado

---

## Favorites

### POST /favorites

Aceita exatamente um dos três campos:

Request (favoritar destino):
```json
{ "destinationId": "uuid" }
```

Request (favoritar POI):
```json
{ "poiId": "uuid" }
```

Request (favoritar roteiro):
```json
{ "itineraryId": "uuid" }
```

Response 201:
```json
{
  "id": "uuid",
  "createdAt": "2026-06-08T10:00:00Z"
}
```

Erros:
- `400` — nenhum target definido
- `400` — mais de um target definido

---

### GET /favorites

Response 200:
```json
[
  {
    "id": "uuid",
    "createdAt": "2026-06-08T10:00:00Z",
    "destination": { "id": "uuid", "slug": "paris", "name": "Paris", "heroImageUrl": "https://..." }
  },
  {
    "id": "uuid",
    "createdAt": "2026-06-08T11:00:00Z",
    "poi": { "id": "uuid", "title": "Museu do Louvre", "category": "MUSEUM" }
  }
]
```

---

### DELETE /favorites/:id

Response 204.

Erros:
- `403` — favorito pertence a outro usuário
- `404` — favorito não encontrado

---

## PDF

### POST /pdf/export/:itineraryId

Response 200:
```json
{
  "pdfUrl": "https://storage.../itinerary-uuid.pdf"
}
```

Erros:
- `403` — roteiro pertence a outro usuário
- `404` — roteiro não encontrado

---

## Padrão de resposta de erro

```json
{
  "error": "UNAUTHORIZED",
  "message": "Token inválido ou expirado",
  "status": 401
}
```

| Code | Constante | Quando |
|------|-----------|--------|
| 400 | BAD_REQUEST | Validação de input falhou |
| 401 | UNAUTHORIZED | Token ausente, inválido ou expirado |
| 403 | FORBIDDEN | Acesso a recurso de outro usuário |
| 404 | NOT_FOUND | Entidade não encontrada |
| 409 | CONFLICT | Email já cadastrado |
| 422 | UNPROCESSABLE_ENTITY | Regra de negócio violada |
| 500 | INTERNAL_ERROR | Erro inesperado (nunca em cenário previsível) |
