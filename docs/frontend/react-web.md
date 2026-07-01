# LUVOY — React Web Frontend Spec

## Contexto

Protótipo visual criado em Lovable como referência de design para o Flutter mobile.
**Não é o produto final** — é um frontend web estático que serve como guia visual e será progressivamente conectado à API Spring Boot.

---

## Stack

| | |
|-|-|
| Framework | TanStack Start v1 (React 19, SSR, file-based routing) |
| Build | Vite 7 |
| Estilos | Tailwind CSS v4 (via `src/styles.css` com `@theme`, sem `tailwind.config.js`) |
| Componentes | shadcn/ui (`src/components/ui/`) |
| Data fetching | TanStack Query (provider montado, ainda não usado para dados reais) |
| Roteamento | File-based em `src/routes/` |

---

## Rotas

| Rota | Arquivo | Status | Backend |
|------|---------|--------|---------|
| `/` | `index.tsx` | ✅ Implementada (estática) | `GET /api/v1/destinations`, `GET /api/v1/collections` |
| `/destinos/$slug` | `destinos.$slug.tsx` | ✅ Implementada (estática) | `GET /api/v1/destinations/:slug` |
| `/roteiro/$slug` | `roteiro.$slug.tsx` | ✅ Implementada (conteúdo hardcoded) | Não conectar ao engine — ver nota abaixo |
| `/favoritos` | `favoritos.tsx` | 🔲 Stub | `GET /api/v1/favorites` |
| `/checklists` | `checklists.tsx` | 🔲 Stub | — |
| `/concierge` | `concierge.tsx` | 🔲 Stub | — |
| `/premium` | `premium.tsx` | 🔲 Stub | — |

---

## Nota importante: `/roteiro/$slug` vs roteiro do engine

O `/roteiro/$slug` do React exibe um **roteiro editorial estático** (manhã/tarde/noite com
conteúdo curado manualmente, igual para todos os visitantes). Este conteúdo virá
futuramente das `DestinationSections` do tipo `ROTEIRO` do banco.

O **Itinerary Engine** do Flutter gera roteiros **personalizados por usuário** via
`POST /api/v1/itineraries`. São conceitos distintos e servem propósitos diferentes.

**Não conectar `/roteiro/$slug` ao endpoint do Itinerary Engine.**

---

## Data layer atual (hardcoded)

### `src/data/destinations.ts`
21 destinos com os campos:
- `slug`, `name`, `country`, `tagline`, `description`, `image` (import local)
- `weather: { temp, condition }` — **HARDCODED**, não vem da API
- `daylight: { sunrise, sunset }` — **HARDCODED**, não vem da API
- `signature`

Quando conectar ao backend:
- `slug`, `name`, `country`, `tagline`, `description`, `heroImageUrl`, `signature` → vêm da API
- `weather` e `daylight` → chamar Google Maps API client-side (ou endpoint dedicado no backend)

### `src/data/collections.ts`
9 coleções com: `slug`, `title`, `italic`, `eyebrow`, `description`, `destinations[]`

6 grupos por país (`countries[]`) com: `flag`, `name`, `italic`, `destinations[]`

> Os grupos por país são **estrutura exclusivamente do frontend** — não há entidade `Country` no backend.
> O agrupamento é feito filtrando destinos por `country`.

---

## Componentes principais

| Componente | Localização | Função |
|-----------|-------------|--------|
| `Nav` | `components/luvoy/Nav.tsx` | Barra de navegação fixa topo |
| `BottomBar` | `components/luvoy/BottomBar.tsx` | Barra flutuante (Favoritos, Checklists, Premium) |
| `DestinationCard` | `components/luvoy/DestinationCard.tsx` | Card de destino no grid da home |
| `CollectionRow` | `components/luvoy/CollectionRow.tsx` | Carrossel horizontal de uma coleção |
| `CountryBlock` | `components/luvoy/CountryBlock.tsx` | Bloco de destinos agrupados por país |
| `EditorialStory` | `components/luvoy/EditorialStory.tsx` | Seção editorial (fullbleed/split-left/split-right) |

---

## Design tokens (src/styles.css)

### Paleta de cores (brand)
| Token | Valor | Uso |
|-------|-------|-----|
| `--ivory` | `oklch(0.982 0.012 88)` | Background principal |
| `--beige` | `oklch(0.955 0.022 82)` | Cards, seções alternadas |
| `--cream` | `oklch(0.93 0.03 78)` | Cards hover, destaques suaves |
| `--sand` | `oklch(0.89 0.04 76)` | Hover states |
| `--gold` | `oklch(0.78 0.1 78)` | Accent principal (eyebrows, CTAs) |
| `--gold-soft` | `oklch(0.9 0.06 86)` | Gold em contexto claro/ivory |
| `--navy` | `oklch(0.28 0.04 250)` | Texto principal, elementos primários |
| `--slate` | `oklch(0.55 0.025 245)` | Texto secundário, metadados |
| `--ink` | `oklch(0.18 0.012 260)` | Fundos escuros (seções dark) |

### Tipografia
- **Display (serif):** Cormorant Garamond — usada em títulos e elementos editoriais
- **Sans:** Inter — usada em body text e eyebrows

### Classes utilitárias da marca
```css
.font-display          /* Cormorant Garamond, weight 500 */
.font-display-italic   /* Cormorant Garamond, italic weight 300 */
.eyebrow               /* 10px, uppercase, letter-spacing 0.3em */
.luvoy-hairline        /* Divisor gold com gradiente */
.golden-hour           /* Overlay quente sobre imagens */
.anim-rise             /* Animação de entrada (rise + blur) */
.anim-fade             /* Fade in */
.anim-zoom             /* Zoom out cinematográfico */
.lift-soft             /* Hover lift para cards */
.glow-soft/warm/ring   /* Glows dourados */
```

> Usar **sempre os tokens semânticos** (`text-navy`, `bg-ivory`, `text-gold`).
> Nunca usar `text-white`, `bg-black` ou cores hardcoded.

---

## Imagens

Atualmente importadas de `src/assets/*.jpg` e servidas pelo CDN do Lovable.

**CDN Base:** `https://id-preview--8de9ade5-4335-46c0-830b-34e895cdfb4a.lovable.app/__l5e/assets-v1/`

URLs completas estão documentadas em `docs/backend/seed.md`.

> ⚠️ URLs do CDN do Lovable são temporárias e podem expirar.
> Migrar para S3 ou Cloudflare R2 antes do lançamento.

Imagens "feature" (usadas em seções editoriais da home):
- `feat-cafe.jpg` — cafés europeus
- `feat-pasta.jpg` — gastronomia italiana
- `feat-pharmacy.jpg` — farmácias/nightlife
- `feat-rooftop.jpg` — rooftops
- `hero-amalfi.jpg` — hero da home (Costa Amalfitana)

---

## O que ainda precisa ser feito para conectar ao backend

### Alta prioridade
- [ ] Trocar `src/data/destinations.ts` por chamadas `GET /api/v1/destinations`
- [ ] Trocar `src/data/collections.ts` por chamadas `GET /api/v1/collections`
- [ ] Implementar auth no frontend (login/signup → `POST /api/v1/auth/signup`, `POST /api/v1/auth/login`)
- [ ] Conectar `/favoritos` à API (`GET/POST/DELETE /api/v1/favorites`)

### Média prioridade
- [ ] Migrar imagens do CDN Lovable para storage permanente
- [ ] Conectar `/destinos/$slug` para buscar seções reais do banco
- [ ] Implementar clima/sunrise/sunset via Google Maps API no cliente

### Baixa prioridade (pós-MVP)
- [ ] `/checklists` — definir estrutura de dados
- [ ] `/concierge` — integrar chat/suporte
- [ ] `/premium` — wire Stripe

---

## Páginas stub (sem funcionalidade)

As páginas abaixo renderizam apenas título + subtítulo via componente `Stub`:
- `/favoritos` — "Seus favoritos"
- `/checklists` — "Checklists"
- `/concierge` — "Concierge 24/7"
- `/premium` — "Área Premium"

O botão "Salvar Destino" em `/destinos/$slug` usa `useState` local — não persiste.
Os checkboxes da checklist na página de destino não salvam estado.
