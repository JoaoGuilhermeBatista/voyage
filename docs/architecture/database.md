# LUVOY — Database

## Provider

- PostgreSQL 15
- Spring Boot + JPA + Hibernate (ddl-auto: validate em produção)

---

## Schema SQL

```sql
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
  id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name                 VARCHAR(255) NOT NULL,
  email                VARCHAR(255) UNIQUE NOT NULL,
  password_hash        VARCHAR(255) NOT NULL,
  avatar_url           TEXT,
  onboarding_completed BOOLEAN DEFAULT FALSE,
  created_at           TIMESTAMPTZ DEFAULT NOW(),
  updated_at           TIMESTAMPTZ DEFAULT NOW(),
  deleted_at           TIMESTAMPTZ
);

CREATE TABLE onboarding_preferences (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  budget     VARCHAR(20) CHECK (budget IN ('economy','moderate','luxury')),
  rhythm     VARCHAR(20) CHECK (rhythm IN ('relaxed','moderate','intense')),
  group_type VARCHAR(20) CHECK (group_type IN ('solo','couple','family','group')),
  interests  TEXT[],
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE destinations (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  title          VARCHAR(255) NOT NULL,
  country        VARCHAR(100) NOT NULL,
  city           VARCHAR(100) NOT NULL,
  hero_image_url TEXT,
  description    TEXT,
  tags           TEXT[],
  is_active      BOOLEAN DEFAULT TRUE,
  created_at     TIMESTAMPTZ DEFAULT NOW(),
  updated_at     TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE pois (
  id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  destination_id           UUID NOT NULL REFERENCES destinations(id),
  title                    VARCHAR(255) NOT NULL,
  category                 VARCHAR(50) NOT NULL,
  description              TEXT,
  address                  TEXT,
  latitude                 DECIMAL(10,8),
  longitude                DECIMAL(11,8),
  images                   TEXT[],
  tags                     TEXT[],
  budget_tier              VARCHAR(20) CHECK (budget_tier IN ('economy','moderate','luxury')),
  opening_time             TIME,
  closing_time             TIME,
  average_duration_minutes INT,
  is_active                BOOLEAN DEFAULT TRUE,
  created_at               TIMESTAMPTZ DEFAULT NOW(),
  updated_at               TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE itineraries (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id        UUID NOT NULL REFERENCES users(id),
  destination_id UUID NOT NULL REFERENCES destinations(id),
  start_date     DATE NOT NULL,
  end_date       DATE NOT NULL,
  title          VARCHAR(255),
  pdf_url        TEXT,
  created_at     TIMESTAMPTZ DEFAULT NOW(),
  updated_at     TIMESTAMPTZ DEFAULT NOW(),
  deleted_at     TIMESTAMPTZ
);

CREATE TABLE itinerary_days (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  itinerary_id UUID NOT NULL REFERENCES itineraries(id) ON DELETE CASCADE,
  day_number   INT NOT NULL,
  date         DATE NOT NULL,
  created_at   TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE itinerary_activities (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  itinerary_day_id UUID NOT NULL REFERENCES itinerary_days(id) ON DELETE CASCADE,
  poi_id           UUID NOT NULL REFERENCES pois(id),
  slot             VARCHAR(20) NOT NULL CHECK (slot IN ('morning','afternoon','evening')),
  order_in_slot    INT NOT NULL,
  created_at       TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE favorites (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  destination_id UUID REFERENCES destinations(id),
  poi_id         UUID REFERENCES pois(id),
  itinerary_id   UUID REFERENCES itineraries(id),
  created_at     TIMESTAMPTZ DEFAULT NOW()
);
```

---

## Índices obrigatórios

```sql
CREATE INDEX idx_pois_destination_id    ON pois(destination_id);
CREATE INDEX idx_pois_budget_tier       ON pois(budget_tier);
CREATE INDEX idx_itineraries_user_id    ON itineraries(user_id);
CREATE INDEX idx_itinerary_days_itin_id ON itinerary_days(itinerary_id);
CREATE INDEX idx_favorites_user_id      ON favorites(user_id);
CREATE INDEX idx_users_email            ON users(email);
```

---

## Categorias válidas para POIs

- `museum` — museus e galerias
- `restaurant` — restaurantes e cafés
- `hotel` — hotéis e pousadas
- `beach` — praias e lagos
- `park` — parques e jardins
- `shopping` — lojas e mercados
- `nightlife` — bares e vida noturna

---

## Rules

- Todas as tabelas usam UUID como PK
- Todas as tabelas têm `created_at`
- Soft delete via `deleted_at` em `users` e `itineraries`
- Array de texto (`TEXT[]`) para tags, interests e images
- `ddl-auto: update` em desenvolvimento, `validate` em produção
