# LUVOY — Community Feature Spec

**Status:** Em definição — aguardando decisões de produto.
Criar este doc antes de implementar. Preencher as seções marcadas com `[A DEFINIR]`.

---

## Visão geral

Uma aba de comunidade dentro do app LUVOY onde usuários podem compartilhar
experiências de viagem, dicas e descobertas — mantendo o posicionamento premium
e editorial do produto.

**Premissa:** A comunidade deve sentir-se como uma revista de viagem colaborativa,
não como uma rede social genérica. Qualidade sobre quantidade.

---

## O que ainda precisa ser definido

### 1. Tipo de conteúdo
`[A DEFINIR]` O que os usuários podem publicar?
- Dica/recomendação de um lugar específico?
- Relato curto de uma viagem?
- Foto com legenda?
- Review de um destino?

### 2. Associação a destinos
`[A DEFINIR]` O conteúdo é sempre vinculado a um destino específico?
- Sim → cada post pertence a um `Destination`
- Não → posts livres, sem vínculo
- Opcional → pode ou não ter destino associado

### 3. Interações entre usuários
`[A DEFINIR]` Que interações existem?
- Curtidas / likes?
- Comentários?
- Salvar post nos favoritos?
- Seguir outros usuários?

### 4. Moderação
`[A DEFINIR]` Como o conteúdo é moderado?
- Sem moderação (tudo público automaticamente)
- Moderação manual antes de publicar
- Denúncia + revisão

### 5. Privacidade
`[A DEFINIR]` O conteúdo é público para todos os usuários do app?
- Apenas para usuários logados?
- Apenas para usuários premium?
- Público para qualquer visitante?

### 6. Relação com o Premium
`[A DEFINIR]` A comunidade é um benefício exclusivo do plano premium ou aberta a todos?

---

## O que JÁ foi decidido

- Haverá uma aba de comunidade no app
- Deve manter o posicionamento "quiet luxury" do produto
- Não é uma rede social genérica — é curada/editorial por natureza

---

## Entidades prováveis (a confirmar após decisões acima)

```
Post
  id           UUID   PK
  user         User   ManyToOne FK
  destination  Destination  nullable FK    ← se vinculado a destino
  content      Text                        ← texto da dica/relato
  imageUrl     String  nullable            ← [A DEFINIR] se terá imagens
  createdAt    LocalDateTime
  deletedAt    LocalDateTime  nullable (soft delete)

[A DEFINIR] Like / Save / Comment
```

---

## Endpoints prováveis (a confirmar)

```
GET  /api/v1/community              → feed de posts (paginado)
POST /api/v1/community              → criar post
GET  /api/v1/community/:id          → post individual
DELETE /api/v1/community/:id        → deletar próprio post (soft delete)

[A DEFINIR]
GET  /api/v1/community?destination=slug  → posts filtrados por destino
POST /api/v1/community/:id/like          → curtir post
GET  /api/v1/community/:id/comments      → comentários
POST /api/v1/community/:id/comments      → comentar
```

---

## Flutter — posição na navegação

`[A DEFINIR]` A aba de comunidade entra na bottom navigation bar, substituindo ou adicionando a uma aba existente?

Estrutura atual do app_router:
- Home
- Destinos
- Roteiro (gerado)
- Favoritos
- Perfil

---

## React Web — presença

`[A DEFINIR]` A comunidade aparece no frontend React (Lovable) ou é exclusiva do mobile?

---

## Impacto nos specs existentes

Quando as decisões acima forem tomadas, atualizar:
- `docs/backend/entities.md` — adicionar Post e entidades de interação
- `docs/backend/endpoints.md` — adicionar rotas /community
- `docs/architecture/database.md` — adicionar tabelas
- `docs/product/mvp.md` — atualizar scope (community estava como "NOT included")
- `docs/frontend/flutter-structure.md` — adicionar feature `community/`
- `docs/ux/ux-flow.md` — adicionar community no fluxo de navegação
