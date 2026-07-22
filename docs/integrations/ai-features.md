# LUVOY — AI Features

## Posicionamento

A IA no LUVOY tem dois papéis distintos: **geração de conteúdo editorial** (interno, invisível ao usuário)
e **features de usuário** (pós-MVP). O Itinerary Engine é determinístico e não usa IA — garante
zero alucinação e latência previsível.

---

## Provedor atual

**OpenAI GPT-4** — créditos da contratante em uso para MVP e testes.

Variável de ambiente a adicionar:
```env
OPENAI_API_KEY=<chave-openai>
```

Propriedade no `application.yaml`:
```yaml
openai:
  api-key: ${OPENAI_API_KEY}
  model: gpt-4o
```

Dependência Maven (Spring AI):
```xml
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>
```

---

## Uso 1 — Geração de conteúdo editorial (MVP)

### Objetivo

Popular o banco com conteúdo editorial para cada destino — taglines, descrições, POIs (restaurantes,
museus, cafés, atrações), seções da página de destino — no tom "quiet luxury" do LUVOY em PT-BR.

Sem a LLM, esse conteúdo seria escrito manualmente para 38+ destinos × 14 seções = centenas de textos.

### Arquitetura

```
POST /admin/destinations/generate
  body: { "name": "Cartagena", "country": "Colômbia" }
  └─► LlmService.generateDestinationContent(name, country)
        └─► Chama OpenAI GPT-4 com system prompt LUVOY
              └─► Retorna JSON estruturado com tagline, descrição, POIs, seções
                    └─► Salva no banco via DestinationService
```

### System prompt (tom LUVOY)

```
Você é um editor de uma revista de viagens premium estilo Monocle/Cereal/Kinfolk.
Escreva em PT-BR, tratamento "você", tom editorial sofisticado sem arrogância.
Nunca use clichês turísticos ("paraíso", "imperdível", "encantador").
Prefira detalhes sensoriais específicos: luz, textura, ritmo, temperatura.
Cada texto deve ter uma voz única para o destino — não use fórmula genérica.
```

### Endpoint admin

```
POST /api/v1/admin/destinations/generate
```

Protegido por header de admin key (não requer Supabase JWT). Apenas para uso interno.

### Fluxo de revisão

O conteúdo gerado é salvo com `status: DRAFT`. A contratante revisa via painel admin e
publica com `status: ACTIVE`. Destinos com `status: DRAFT` não aparecem no app.

---

## Uso 2 — Concierge (pós-MVP)

Chat 24/7 para assinantes Premium. Assistente responde perguntas sobre destinos,
ajuda a reservar restaurantes, sugere alterações no roteiro em tempo real.

- Rota: `/concierge`
- Gate: Premium only
- Histórico de conversa persistido no banco (`ConciergeThread`)
- Integração via Spring AI com streaming de resposta
- System prompt inclui contexto do destino atual e roteiro do usuário

Não implementar no MVP — apenas manter a UI mockada com "em breve".

---

## Uso 3 — Recomendações personalizadas (pós-MVP)

`GET /api/v1/recommendations` — usado pela aba Para Você.

Por ora, retornar destinos ordenados por match com as preferências do onboarding
(sem LLM — lógica simples de score igual ao Itinerary Engine).

LLM entra na versão 2.0 para refinamento via texto natural ("quero algo mais relaxado que Paris").

---

## Regras de uso da IA

- Nunca expor a API key no cliente (React/Flutter) — todas as chamadas passam pelo backend
- Sempre cachear respostas de conteúdo editorial (não regenerar a cada request)
- Conteúdo gerado por LLM é revisado antes de ir para produção
- Streaming só para o Concierge — demais chamadas são síncronas com timeout de 30s
- Custo controlado: conteúdo editorial é gerado uma vez e armazenado no banco

---

## Custos estimados (OpenAI GPT-4o)

| Uso | Frequência | Custo estimado |
|-----|-----------|----------------|
| Seed 38 destinos (conteúdo completo) | 1× | ~$2–5 total |
| Novos destinos via admin panel | Por demanda | ~$0,05–0,15 por destino |
| Concierge (pós-MVP) | Por mensagem | ~$0,01–0,03 por resposta |
