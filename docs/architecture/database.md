# LUVOY — Database

## Provider

- PostgreSQL 15
- Spring Boot + JPA + Hibernate (ddl-auto: update em dev, validate em produção)

---

## Schema SQL

```sql
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Users
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
  budget     VARCHAR(20) CHECK (budget IN ('ECONOMY','MODERATE','LUXURY')),
  rhythm     VARCHAR(20) CHECK (rhythm IN ('RELAXED','MODERATE','INTENSE')),
  group_type VARCHAR(20) CHECK (group_type IN ('SOLO','COUPLE','FAMILY','GROUP')),
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE preferences_interests (
  preferences_id UUID NOT NULL REFERENCES onboarding_preferences(id) ON DELETE CASCADE,
  interest       VARCHAR(100) NOT NULL
);

-- Destinations
CREATE TABLE destinations (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  slug           VARCHAR(100) UNIQUE NOT NULL,
  name           VARCHAR(255) NOT NULL,
  country        VARCHAR(100) NOT NULL,
  city           VARCHAR(100) NOT NULL,
  tagline        VARCHAR(500),
  description    TEXT,
  hero_image_url TEXT,
  signature      VARCHAR(500),
  is_active      BOOLEAN DEFAULT TRUE,
  created_at     TIMESTAMPTZ DEFAULT NOW(),
  updated_at     TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE destination_tags (
  destination_id UUID NOT NULL REFERENCES destinations(id) ON DELETE CASCADE,
  tag            VARCHAR(100) NOT NULL
);

-- Destination sections (14 seções do dossiê)
CREATE TABLE destination_sections (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  destination_id UUID NOT NULL REFERENCES destinations(id) ON DELETE CASCADE,
  section_type   VARCHAR(50) NOT NULL CHECK (section_type IN (
    'ROTEIRO','RESTAURANTES','CAFES','ATRACOES','EXPERIENCIAS','RUAS',
    'INSTAGRAM','LOJAS','ESCONDIDOS','ROOFTOPS','SKINCARE','COMPRAS','PRATOS','MAPA'
  )),
  content        JSONB NOT NULL DEFAULT '[]',
  order_index    INT NOT NULL
);

-- Collections (9 coleções editoriais)
CREATE TABLE collections (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  slug        VARCHAR(100) UNIQUE NOT NULL,
  title       VARCHAR(255) NOT NULL,
  italic      VARCHAR(255) NOT NULL,
  eyebrow     VARCHAR(255) NOT NULL,
  description TEXT,
  order_index INT NOT NULL,
  created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE collection_destinations (
  collection_id  UUID NOT NULL REFERENCES collections(id) ON DELETE CASCADE,
  destination_id UUID NOT NULL REFERENCES destinations(id),
  position       INT NOT NULL
);

-- POIs (para Itinerary Engine — mobile)
CREATE TABLE pois (
  id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  destination_id           UUID NOT NULL REFERENCES destinations(id),
  title                    VARCHAR(255) NOT NULL,
  category                 VARCHAR(50) NOT NULL CHECK (category IN (
    'MUSEUM','RESTAURANT','HOTEL','BEACH','PARK','SHOPPING','NIGHTLIFE'
  )),
  description              TEXT,
  address                  TEXT,
  latitude                 DECIMAL(10,7),
  longitude                DECIMAL(10,7),
  budget_tier              VARCHAR(20) CHECK (budget_tier IN ('ECONOMY','MODERATE','LUXURY')),
  opening_time             TIME,
  closing_time             TIME,
  average_duration_minutes INT,
  is_active                BOOLEAN DEFAULT TRUE,
  created_at               TIMESTAMPTZ DEFAULT NOW(),
  updated_at               TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE poi_images (
  poi_id    UUID NOT NULL REFERENCES pois(id) ON DELETE CASCADE,
  image_url TEXT NOT NULL
);

CREATE TABLE poi_tags (
  poi_id UUID NOT NULL REFERENCES pois(id) ON DELETE CASCADE,
  tag    VARCHAR(100) NOT NULL
);

-- Itineraries
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
  slot             VARCHAR(20) NOT NULL CHECK (slot IN ('MORNING','AFTERNOON','EVENING')),
  order_in_slot    INT NOT NULL,
  created_at       TIMESTAMPTZ DEFAULT NOW()
);

-- Favorites
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
CREATE INDEX idx_destinations_slug        ON destinations(slug);
CREATE INDEX idx_destinations_active      ON destinations(is_active);
CREATE INDEX idx_pois_destination_id      ON pois(destination_id);
CREATE INDEX idx_pois_budget_tier         ON pois(budget_tier);
CREATE INDEX idx_itineraries_user_id      ON itineraries(user_id);
CREATE INDEX idx_itinerary_days_itin_id   ON itinerary_days(itinerary_id);
CREATE INDEX idx_favorites_user_id        ON favorites(user_id);
CREATE INDEX idx_users_email              ON users(email);
CREATE INDEX idx_dest_sections_dest_id    ON destination_sections(destination_id);
CREATE INDEX idx_collection_dest_coll_id  ON collection_destinations(collection_id);
```

---

## Regras

- Todas as tabelas usam UUID como PK gerado por `gen_random_uuid()`
- Todas as tabelas têm `created_at`
- Soft delete via `deleted_at` em `users` e `itineraries`
- Enums armazenados como VARCHAR com CHECK constraint (valores em UPPER_CASE)
- Arrays de tags/images modelados como tabelas de join (não TEXT[]) para compatibilidade com JPA
- `content` de `destination_sections` armazenado como JSONB
- `ddl-auto: update` em desenvolvimento, `validate` em produção
