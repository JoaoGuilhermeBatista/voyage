# LUVOY — Community Feature Spec

**Status:** Em definição — aguardando decisões de produto.

---

## Visão geral

Espaço dentro da aba **Para Você** onde usuários Premium podem compartilhar
experiências de viagem, dicas e descobertas — mantendo o posicionamento premium
e editorial do produto.

**Premissa:** A comunidade deve sentir-se como uma revista de viagem colaborativa,
não como uma rede social genérica. Qualidade sobre quantidade.

**Acesso:** Exclusivo para assinantes Premium. Usuários do plano gratuito veem
o bloco de comunidade com um CTA de upgrade, mas não acessam o conteúdo.

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

---

## O que JÁ foi decidido

- A comunidade vive dentro da aba **Para Você**, não em aba própria
- É exclusiva do plano **Premium** — usuários free veem CTA de upgrade
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
  imageUrl     String  nullable
  createdAt    LocalDateTime
  deletedAt    LocalDateTime  nullable (soft delete)

[A DEFINIR] Like / Save / Comment
```

---

## Endpoints prováveis (a confirmar)

```
GET  /api/v1/community              → feed de posts (paginado)
POST /api/v1/community              → criar post (Premium only → 402 se free)
GET  /api/v1/community/:id          → post individual
DELETE /api/v1/community/:id        → deletar próprio post (soft delete)

[A DEFINIR]
GET  /api/v1/community?destination=slug  → posts filtrados por destino
POST /api/v1/community/:id/like          → curtir post
GET  /api/v1/community/:id/comments      → comentários
POST /api/v1/community/:id/comments      → comentar
```

---

## Impacto nos specs existentes

Quando as decisões acima forem tomadas, atualizar:
- `docs/backend/entities.md` — adicionar Post e entidades de interação
- `docs/backend/endpoints.md` — adicionar rotas /community
- `docs/architecture/database.md` — adicionar tabelas
