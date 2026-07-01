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

Conteúdo editorial estático + destinos curados.
Equivale ao home atual do Lovable.

Seções:
- Hero cinematográfico
- Destinos em destaque (carrossel)
- Editoriais (fullbleed / split)
- Grade de todos os 21 destinos
- As 9 Coleções LUVOY
- Por país (6 grupos)

Rotas filhas:
- `/explore/destinations/:slug` — dossiê do destino
- `/explore/destinations/:slug/itinerary` — roteiro editorial estático

---

## Aba 2 — Para Você

Recomendações personalizadas baseadas nas preferências do onboarding.
**Aba exclusiva** — conteúdo diferente por usuário.

Seções:
- "Seu próximo destino" — destino top recomendado pelo engine
- "Baseado nos seus interesses" — grid de destinos pontuados
- "Coleções para você" — coleções filtradas por preferência
- "Câmbio do dia" — cotação EUR, GBP, USD em BRL
- Clima atual nos seus destinos favoritos

Backend: `GET /api/v1/recommendations` — endpoint a criar (usa a lógica de score do Itinerary Engine sem gerar roteiro completo).

---

## Aba 3 — Roteiros

Histórico de roteiros gerados pelo Itinerary Engine.
Vazio state: CTA para criar o primeiro roteiro.

Seções:
- Lista de roteiros salvos (card com cidade, datas, dias)
- Tap no card → roteiro completo com dias e POIs
- Botão "Exportar PDF" em cada roteiro
- Botão "Criar novo roteiro" (fixo no topo)

Backend:
- `GET /api/v1/itineraries` — lista
- `POST /api/v1/itineraries` — criar
- `DELETE /api/v1/itineraries/:id` — deletar
- `POST /api/v1/pdf/export/:id` — exportar

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
- "Câmbio do dia" (atalho)
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
