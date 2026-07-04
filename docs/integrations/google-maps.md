# LUVOY — Google Maps Integration

## APIs utilizadas

| API | Para que serve no app |
|-----|----------------------|
| **Maps SDK for Android** | Renderiza o mapa interativo no Flutter Android |
| **Maps SDK for iOS** | Renderiza o mapa interativo no Flutter iOS |
| **Places API** | Busca detalhes de POIs (nome, foto, horário, avaliação) |
| **Directions API** | Rota a pé entre POIs do roteiro (trajeto do dia) |
| **Distance Matrix API** | Calcula distância entre múltiplos POIs — usado pelo Itinerary Engine no algoritmo nearest-neighbor (Passo 7) |
| **Geocoding API** | Converte endereço em coordenadas (lat/lon) para POIs cadastrados sem coordenada |

> Clima, nascer e pôr do sol **não são Google Maps** — são cobertos pelo Open-Meteo. Ver `docs/integrations/weather.md`.

---

## Onde aparece no app (Flutter)

### Tela de Roteiro (`/itinerary/:id`)
- Mapa interativo mostrando todos os POIs do dia selecionado
- Marcadores numerados na ordem do roteiro (1 → 2 → 3)
- Linha de rota entre os POIs (Directions API — modo a pé)
- Tap no marcador abre card com nome, categoria e duração estimada

### Tela de Destino (`/destinations/:slug`)
- Mapa estático (thumbnail) com a localização da cidade
- Botão "Ver no mapa" abre Google Maps nativo via deep link

### Card de POI
- Distância do POI até o ponto atual do usuário (Distance Matrix API)
- "A 12 min a pé · 950m"

### Localização do usuário
- Flutter `geolocator` captura a posição GPS do dispositivo
- Google Maps exibe a posição com o pino azul padrão
- Usado para calcular distância até o próximo POI do roteiro

---

## Fluxo de localização

```
Flutter geolocator → lat/lon do usuário
  └─► Distance Matrix API → distância até próximo POI
        └─► exibido no card "Próxima parada: 8 min a pé"
```

O Spring Boot **não acessa** localização do usuário — isso é feito 100% no cliente Flutter.

---

## Itinerary Engine + Distance Matrix

O algoritmo de nearest-neighbor (Passo 7 do engine) usa a fórmula **Haversine** com as coordenadas do banco — sem custo de API.

A Distance Matrix API é usada **apenas na exibição** (tempo de caminhada real com tráfego, ruas, etc.), não na geração do roteiro.

---

## Configuração de chaves

Uma chave de API por ambiente, com restrições:

| Ambiente | Restrição |
|----------|-----------|
| Android dev | SHA-1 do keystore de debug |
| Android prod | SHA-1 do keystore de release |
| iOS dev | Bundle ID do app de dev |
| iOS prod | Bundle ID do app de prod |

**Nunca usar a mesma chave sem restrição em produção.**

### Variáveis de ambiente

Flutter (`frontend/app/.env`):
```env
GOOGLE_MAPS_API_KEY_ANDROID=<chave-android>
GOOGLE_MAPS_API_KEY_IOS=<chave-ios>
```

React web — mapa é exibido via iframe do Google Maps (sem SDK), não precisa de chave para embed básico.

---

## Custos estimados (Google Maps Platform)

| API | Preço | Volume gratuito/mês |
|-----|-------|---------------------|
| Maps SDK | $7 / 1.000 carregamentos | 28.500 carregamentos |
| Directions API | $5 / 1.000 req | 40.000 req |
| Distance Matrix API | $5 / 1.000 elementos | 40.000 elementos |
| Places API | $17 / 1.000 req | — |

Para o MVP com volume baixo, o crédito gratuito de $200/mês do Google cobre tudo.

---

## Regras de UX

- Mapa **complementa** o roteiro — nunca substitui o conteúdo editorial
- Estilo do mapa: usar tema escuro customizado que combine com a paleta navy/ink do LUVOY
- Nunca abrir o mapa automaticamente — sempre por ação do usuário
- Deep link para Google Maps nativo quando o usuário quer navegar de verdade
