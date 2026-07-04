# LUVOY — Lugares Escondidos (Hidden Gems)

## O que define um lugar escondido no LUVOY

Um lugar escondido não é necessariamente barato nem caro — é **difícil de encontrar por meios convencionais**.

Critérios de curadoria:

| Critério | Descrição |
|---------|-----------|
| **Invisível no Google** | Não aparece nas primeiras páginas de busca para o destino |
| **Sem placa ou fachada chamativa** | Entrada discreta, boca-a-boca ou conhecimento local |
| **Sem presença em TripAdvisor/Booking** | Ou presença mínima, com poucas avaliações |
| **Frequentado por locais** | Não por turistas — o clientele é predominantemente da cidade |
| **Experiência não replicável** | Não tem filial, não é franquia, depende da pessoa que o criou |

---

## Categorias de lugares escondidos

| Categoria | Exemplo |
|-----------|---------|
| Cafés e padarias de bairro | Torrefação familiar sem placa, só quem conhece entra |
| Ateliês e galerias privadas | Artistas que recebem por indicação |
| Restaurantes sem cardápio fixo | Menu do dia decidido na hora com o que tem no mercado |
| Jardins e pátios secretos | Passagens entre edifícios, claustros de igrejas não turísticas |
| Lojas de nicho | Antiquário de família, livraria especializada em uma só era |
| Bares sem nome | Endereço sussurado, entrada por campainha |
| Farmácias e perfumarias históricas | Com fórmulas próprias, frequentadas por locais há gerações |

---

## Como são armazenados no banco

Lugares escondidos são **POIs com a tag `"escondido"` e/ou `SectionType.ESCONDIDOS`** nas seções do dossiê.

### Como POI (para o Itinerary Engine)
```
poi.tags = ["escondido", "cafe"]
poi.isActive = true
poi.budgetTier = ECONOMY | MODERATE | LUXURY
```
O engine pode incluir esses POIs em roteiros de usuários que marcaram `interests: ["lugares_escondidos"]`.

### Como DestinationSection (para o dossiê web)
```
sectionType = ESCONDIDOS
content = [
  { name: "Café Procope dos fundos", desc: "Pátio interno do café mais antigo de Paris. Entrada pelo corredor à esquerda — sem placa. Frequentado por estudantes de filosofia desde 1686." },
  ...
]
```

---

## Regra de curadoria para o onboarding

O usuário que marcar interesse em **"Lugares Escondidos"** no onboarding recebe:
- Score bônus de `+15` em POIs com tag `"escondido"` (mais alto que qualquer outro interesse)
- Seção "Escondidos" em destaque no dossiê

---

## Como encontrar lugares escondidos para curadoria

Fontes recomendadas para pesquisa editorial:
- Blogs de expats morando na cidade (não de turistas)
- Grupos de Facebook de brasileiros no exterior
- Reddit: r/Paris, r/italy, r/london etc — posts de moradores locais
- Guias impressos locais (não internacionais)
- Contato direto com brasileiros que moram nos destinos

---

## Apresentação no app

No dossiê web e no Flutter, a seção "Escondidos" tem tratamento visual diferenciado:
- Eyebrow: "Só quem sabe, sabe"
- Sem foto do lugar (propositalmente — preserva o mistério)
- Descrição em tom de "dica de amigo local"
- Coordenadas disponíveis apenas após o usuário salvar o lugar nos favoritos
