# LUVOY — Navigation

## Sistema de navegação

Flutter: **GoRouter** com bottom navigation bar de 5 abas.
React Web: **TanStack Router** com navegação por abas no topo (desktop) ou bottom bar (mobile).

---

## Estrutura de abas

| # | Aba | Ícone | Rota Flutter | Rota React | Acesso |
|---|-----|-------|-------------|-----------|--------|
| 1 | **Explorar** | bússola | `/explore` | `/` | Público |
| 2 | **Para Você** | estrela | `/for-you` | `/para-voce` | Requer auth |
| 3 | **Roteiros** | mapa | `/itineraries` | `/roteiros` | Requer auth |
| 4 | **Favoritos** | coração | `/favorites` | `/favoritos` | Requer auth |
| 5 | **Perfil** | pessoa | `/profile` | `/perfil` | Requer auth |

---

## Aba 1 — Explorar

Conteúdo editorial estático + destinos curados. Única aba pública (sem login).

Seções:
- Hero cinematográfico
- Destinos em destaque (carrossel)
- Editoriais (fullbleed / split)
- Grade de todos os destinos
- As 9 Coleções LUVOY
- Por país / continente
- Widget de câmbio do dia — cotação EUR, GBP, USD em BRL (público, sem auth)
- **Seção After Dark** — bloco Noir escuro com 3 cards premium → CTA para `/premium`
- **Preview Premium** — amostra de roteiro hora a hora → CTA "Tornar-se Premium"
- **Ícone de busca** (fixo no topo da aba) — busca destinos por nome, país ou cidade

Rotas filhas:
- `/destinos/:slug` — dossiê do destino

---

## Aba 2 — Para Você

Recomendações personalizadas baseadas nas preferências do onboarding.
**Aba exclusiva** — conteúdo diferente por usuário.

Seções:
- "Seu próximo destino" — destino top recomendado pelo engine
- "Baseado nos seus interesses" — grid de destinos pontuados
- "Coleções para você" — coleções filtradas por preferência
- Clima atual nos seus destinos favoritos
- **Comunidade** — feed de posts de viajantes (exclusivo Premium; usuários free veem CTA de upgrade)

Backend: `GET /api/v1/recommendations` — endpoint a criar (usa a lógica de score do Itinerary Engine sem gerar roteiro completo).

---

## Aba 3 — Roteiros

Centraliza tanto os roteiros editoriais prontos quanto os roteiros personalizados gerados pelo engine.

Seções:
- **Roteiros editoriais** — itinerários curados pela equipe para cada destino (leitura, sem personalização)
- **Meus roteiros** — lista de roteiros personalizados gerados pelo usuário (card com cidade, datas, dias)
- Tap no card → roteiro completo dividido em três atos: **Manhã · Tarde · Noite**
- Botão "Exportar PDF" em cada roteiro pessoal
- Botão "Criar novo roteiro" (fixo no topo)

### Gating por plano nos atos do roteiro

| Ato | Usuário Free | Usuário Premium |
|-----|-------------|-----------------|
| Manhã | Acesso completo | Acesso completo |
| Tarde | Blur + CTA "Desbloquear com Noir" | Acesso completo |
| Noite | Blur + CTA "Desbloquear com Noir" | Acesso completo (tema dark ink) |

### Edição de POI

Cada atividade do roteiro tem um botão "Editar" que abre um painel com alternativas:
- Exibe 3 opções da mesma categoria no mesmo destino
- Botão "Ver todas as opções" expande para lista completa
- Usuário seleciona → POI substituído, slot preservado
- Disponível para todos os usuários (free e Premium)
- Botão oculto quando destino tem menos de 2 POIs na mesma categoria

Vazio state (meus roteiros): CTA para criar o primeiro roteiro.

Backend:
- `GET /api/v1/itineraries` — lista de roteiros do usuário
- `POST /api/v1/itineraries` — criar roteiro
- `GET /api/v1/itineraries/:id` — roteiro completo
- `DELETE /api/v1/itineraries/:id` — deletar roteiro
- `GET /api/v1/itineraries/:id/activities/:activityId/alternatives` — alternativas de POI
- `PATCH /api/v1/itineraries/:id/activities/:activityId` — substituir POI
- `POST /api/v1/pdf/export/:id` — exportar PDF

---

## Aba 4 — Favoritos

Destinos e POIs salvos pelo usuário.
Vazio state: CTA para explorar destinos.

Seções:
- Destinos salvos (grid de cards)
- POIs salvos (lista com categoria e destino)

Backend:
- `GET /api/v1/favorites`
- `POST /api/v1/favorites`
- `DELETE /api/v1/favorites/:id`

---

## Aba 5 — Perfil

Configurações do usuário e conta.

Seções:
- Avatar + nome + email
- "Minhas preferências" → editar onboarding (`PATCH /api/v1/users/preferences`)
- "Meu plano" → exibe Free ou Premium
- "Tornar-se Premium" → abre Stripe Checkout (se free)
- "Sair" → logout Supabase

---

## Fluxo de autenticação

```
App abre
  └─► Supabase Auth verifica sessão
        ├─► Sessão ativa → aba Explorar
        └─► Sem sessão → tela de Login/Signup
              └─► Login bem-sucedido
                    └─► onboardingCompleted?
                          ├─► Sim → aba Explorar
                          └─► Não → tela de Onboarding → aba Explorar
```

---

## Guardas de rota

Abas 2–5 exigem autenticação. Usuário não autenticado que tenta acessar qualquer aba protegida é redirecionado para Login.

Comunidade (dentro da aba Para Você) exige Premium. Usuário free vê o conteúdo da aba normalmente mas o bloco de comunidade exibe um CTA de upgrade.

No React Web: redirecionamento via loader do TanStack Router.
No Flutter: GoRouter redirect com verificação de sessão Supabase.

---

## Transições

- Entre abas: fade suave (sem slide — evita sensação de carrossel)
- Dentro de aba: slide da esquerda para direita (navegação em profundidade)
- Modal (criar roteiro, export PDF): bottom sheet com spring animation

---

## Regras de UX

- Bottom navigation sempre visível (exceto telas de onboarding e login)
- Aba ativa indicada por ponto dourado abaixo do ícone (não highlight de cor)
- Labels visíveis em todas as abas (não só ícone)
- Badge numérico na aba Roteiros quando há roteiro novo gerado
