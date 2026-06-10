# LUVOY — Business Rules Tests

## Objetivo

Garantir que o Itinerary Engine respeita as regras de negócio do produto:
ritmo, budget, interesses, geografia e qualidade do roteiro gerado.

## Ferramentas

- JUnit 5 + Mockito
- Classe testada: `ItineraryEngineService`

---

## Regra 1 — Atividades por dia respeitam o ritmo

| Ritmo | Atividades/dia | Slots |
|-------|---------------|-------|
| relaxed | 2 | morning + afternoon |
| moderate | 3 | morning + afternoon + evening |
| intense | 4 | morning + 2x afternoon + evening |

```java
@Test
void shouldGenerate2ActivitiesPerDay_whenRhythmIsRelaxed() {
    // setup: user com rhythm=RELAXED, 10 POIs disponíveis, 3 dias
    ItineraryResponseDTO result = engineService.generate(...);

    result.days().forEach(day ->
        assertEquals(2, day.activities().size())
    );
}

@Test
void shouldGenerate3ActivitiesPerDay_whenRhythmIsModerate() {
    ItineraryResponseDTO result = engineService.generate(...);

    result.days().forEach(day ->
        assertEquals(3, day.activities().size())
    );
}

@Test
void shouldGenerate4ActivitiesPerDay_whenRhythmIsIntense() {
    ItineraryResponseDTO result = engineService.generate(...);

    result.days().forEach(day ->
        assertEquals(4, day.activities().size())
    );
}
```

---

## Regra 2 — Budget filtra corretamente os POIs

Budget `economy` → acessa apenas POIs `economy`
Budget `moderate` → acessa POIs `economy` e `moderate`
Budget `luxury` → acessa todos os tiers

```java
@Test
void shouldIncludeAllTiers_whenBudgetIsLuxury() {
    // banco: 2 economy + 2 moderate + 2 luxury POIs
    // user: budget=LUXURY

    ItineraryResponseDTO result = engineService.generate(...);

    List<String> tiers = extractTiers(result);
    assertThat(tiers).contains("economy", "moderate", "luxury");
}

@Test
void shouldExcludeLuxuryAndModerate_whenBudgetIsEconomy() {
    ItineraryResponseDTO result = engineService.generate(...);

    extractTiers(result).forEach(tier ->
        assertEquals("economy", tier)
    );
}
```

---

## Regra 3 — Score por interesses prioriza POIs relevantes

POIs com tags que coincidem com os interesses do usuário aparecem primeiro.

```java
@Test
void shouldRankPoiWithMatchingInterestHigher() {
    // user: interests=["gastronomia"]
    // POI A: tags=["gastronomia","cultura"] → score alto
    // POI B: tags=["shopping"] → score baixo

    ItineraryResponseDTO result = engineService.generate(...);

    // Primeiro POI do primeiro dia deve ser o A (maior score)
    UUID firstPoiId = result.days().get(0).activities().get(0).poi().id();
    assertEquals(poiA.getId(), firstPoiId);
}
```

---

## Regra 4 — Bônus de groupType aplicado corretamente

```java
@Test
void shouldBoostRestaurantsAndBeach_whenGroupTypeIsCouple() {
    // user: groupType=COUPLE, sem interests definidos
    // banco: 1 restaurant, 1 museum, 1 beach
    // esperado: restaurant e beach no top da lista

    ItineraryResponseDTO result = engineService.generate(...);

    List<String> categories = extractCategories(result);
    assertThat(categories).contains("restaurant", "beach");
}
```

---

## Regra 5 — Slots respeitam horário de funcionamento

POI com horário incompatível com o slot é ignorado (não causa erro).

```java
@Test
void shouldSkipPoi_whenOpeningTimeIncompatibleWithSlot() {
    // POI: opening_time = 20:00 (só funciona à noite)
    // Tentativa de alocar no slot morning → deve ser ignorado

    Poi eveningOnlyPoi = poiWithOpeningTime(LocalTime.of(20, 0));
    // setup com 5 outros POIs normais

    ItineraryResponseDTO result = engineService.generate(...);

    List<UUID> morningPoiIds = extractPoiIdsBySlot(result, "morning");
    assertFalse(morningPoiIds.contains(eveningOnlyPoi.getId()));
}
```

---

## Regra 6 — POIs sem horário são aceitos em qualquer slot

```java
@Test
void shouldAcceptPoiWithNoOpeningTime_inAnySlot() {
    // POI com openingTime=null e closingTime=null

    Poi noHoursPoi = poiWithNoHours();

    ItineraryResponseDTO result = engineService.generate(...);

    List<UUID> allPoiIds = extractAllPoiIds(result);
    assertTrue(allPoiIds.contains(noHoursPoi.getId()));
}
```

---

## Regra 7 — Slots vazios quando POIs insuficientes para preencher tudo

Quando há POIs mas não o suficiente para completar todos os slots:
o engine omite os slots vazios em vez de lançar erro.

```java
@Test
void shouldOmitEmptySlots_whenNotEnoughPoisForAllSlots() {
    // rhythm=INTENSE (4 atividades/dia), mas banco tem apenas 6 POIs para 3 dias
    // esperado: dias com menos de 4 atividades são aceitos (sem erro)

    assertDoesNotThrow(() -> engineService.generate(...));

    ItineraryResponseDTO result = engineService.generate(...);
    // nenhum dia deve ter mais atividades do que POIs disponíveis
    result.days().forEach(day ->
        assertTrue(day.activities().size() <= 4)
    );
}
```

---

## Regra 8 — Roteiro com apenas 1 dia funciona corretamente

```java
@Test
void shouldGenerateOneDayItinerary_whenStartEqualsEnd() {
    LocalDate today = LocalDate.now();

    ItineraryResponseDTO result = engineService.generate(
        userWithPrefs, destId, today, today
    );

    assertEquals(1, result.days().size());
    assertEquals(1, result.days().get(0).dayNumber());
}
```

---

## Resumo das regras testadas

| # | Regra | Consequência se falhar |
|---|-------|----------------------|
| 1 | Atividades por ritmo | Roteiro com volume errado de atividades |
| 2 | Filtro de budget | POI caro aparece para usuário economy |
| 3 | Score por interesse | Roteiro irrelevante para o perfil |
| 4 | Bônus de groupType | Casal vê museus em vez de restaurantes |
| 5 | Slot × horário | POI fechado aparece no roteiro da manhã |
| 6 | POI sem horário | POI válido excluído incorretamente |
| 7 | Slots vazios | Exception desnecessária com poucos POIs |
| 8 | Roteiro de 1 dia | Crash ou roteiro vazio |
