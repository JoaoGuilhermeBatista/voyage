# LUVOY — Itinerary Engine

## O que é

Algoritmo determinístico que gera roteiros personalizados sem IA generativa.
- Custo zero por uso
- Latência < 800ms (P95)
- Zero alucinação — apenas POIs do banco curado
- Qualidade vem da curadoria editorial humana

IA entra na versão 2.0 como camada de refinamento sobre este base.

---

## Localização no código

```
luvoygroup.voyage.features.itinerary.service.ItineraryEngineService
```

---

## Input

```java
ItineraryRequest {
  UUID        userId
  UUID        destinationId
  LocalDate   startDate
  LocalDate   endDate
}
```

Preferências do usuário são carregadas do banco dentro do service
(não passadas como parâmetro — o engine busca por `userId`).

---

## Passos do Algoritmo

### Passo 1 — Validar input

```
Se startDate > endDate → InvalidDateRangeException
Se startDate < LocalDate.now() → InvalidDateRangeException
```

### Passo 2 — Carregar contexto

```
preferences = prefsRepository.findByUserId(userId)
Se preferences vazio → OnboardingNotCompletedException

dayCount = DAYS_BETWEEN(startDate, endDate) + 1
```

### Passo 3 — Filtrar POIs

```sql
SELECT * FROM pois
WHERE destination_id = :destinationId
  AND budget_tier IN (:budgetFilter)
  AND is_active = true
```

budgetFilter por tier do usuário:
```
ECONOMY  → ['economy']
MODERATE → ['economy', 'moderate']
LUXURY   → ['economy', 'moderate', 'luxury']
```

Se resultado < 5 POIs → `InsufficientContentException`

### Passo 4 — Pontuar POIs

```
score = 0

Para cada tag do POI que está em preferences.interests:
  score += 10

Bônus por groupType:
  COUPLE  → RESTAURANT +5, BEACH +3, HOTEL +3
  FAMILY  → PARK +5, BEACH +3, SHOPPING +2
  SOLO    → MUSEUM +5, NIGHTLIFE +3
  GROUP   → SHOPPING +3, NIGHTLIFE +3, PARK +2
```

### Passo 5 — Ordenar

```
POIs ordenados por score DESC
```

### Passo 6 — Distribuir por dias

Atividades por dia conforme rhythm:

| Rhythm | Total/dia | Slots |
|--------|-----------|-------|
| RELAXED | 2 | morning(1) + afternoon(1) |
| MODERATE | 3 | morning(1) + afternoon(1) + evening(1) |
| INTENSE | 4 | morning(1) + afternoon(2) + evening(1) |

Para cada dia (1 até dayCount):
  1. Pegar os próximos N POIs não utilizados da lista ordenada
  2. Para cada POI, verificar compatibilidade com o slot:
     ```
     morning   → poi.openingTime == null OU poi.openingTime <= 09:00
     afternoon → poi.openingTime == null OU poi.openingTime <= 13:00
     evening   → poi.openingTime == null OU poi.openingTime <= 18:00
     ```
  3. POI incompatível: pular para o próximo na lista (não é erro)
  4. Se não houver POI disponível para o slot: slot omitido (não é erro)

### Passo 7 — Ordenar por proximidade dentro do dia

Dentro de cada dia, aplicar nearest-neighbor:
```
Ponto inicial: primeiro POI do dia (maior score)
Para cada slot seguinte:
  Escolher o POI mais próximo geograficamente do anterior
  Distância calculada com fórmula Haversine (lat/lon)
```

### Passo 8 — Persistir e retornar

```
Salvar: Itinerary → ItineraryDay[] → ItineraryActivity[]
Retornar: ItineraryResponseDTO com estrutura completa
```

---

## Output

```java
ItineraryResponseDTO {
  UUID             id
  String           title  // ex: "Paris — 5 dias"
  DestinationDTO   destination
  LocalDate        startDate
  LocalDate        endDate
  List<DayDTO>     days
}

DayDTO {
  Integer          dayNumber
  LocalDate        date
  List<ActivityDTO> activities
}

ActivityDTO {
  String   slot  // "morning" | "afternoon" | "evening"
  Integer  orderInSlot
  PoiDTO   poi
}
```

---

## Exceções

| Exception | HTTP | Condição |
|-----------|------|----------|
| `InvalidDateRangeException` | 400 | startDate > endDate ou startDate no passado |
| `OnboardingNotCompletedException` | 422 | Usuário sem preferências cadastradas |
| `InsufficientContentException` | 422 | Destino com < 5 POIs ativos |
| `ResourceNotFoundException` | 404 | Destino não encontrado |

---

## Invariantes garantidas pelo engine

- O mesmo POI nunca aparece duas vezes no mesmo roteiro
- POIs fora do budget tier do usuário nunca aparecem
- POIs com horário incompatível com o slot nunca aparecem
- Slots vazios são omitidos (não causam erro)
- Roteiro de 1 dia (startDate == endDate) funciona normalmente
