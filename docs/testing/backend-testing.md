# LUVOY — Backend Unit Tests

## Objetivo

Testar isoladamente a lógica de negócio das camadas de serviço.
Nenhum teste desta camada acessa banco ou HTTP.

## Ferramentas

- JUnit 5
- Mockito (`@ExtendWith(MockitoExtension.class)`)
- AssertJ para assertions

---

## Auth Service — Testes de Erro

Classe: `AuthServiceTest.java`
Pacote: `luvoygroup.voyage.features.auth.service`

### Teste 7 — Email já cadastrado

```java
@Test
void shouldThrow_whenEmailAlreadyRegistered() {
    when(userRepository.existsByEmail("j@j.com")).thenReturn(true);

    assertThrows(EmailAlreadyExistsException.class, () ->
        authService.signup("João", "j@j.com", "senha123")
    );
}
```

Cenário: usuário tenta criar conta com email que já existe no banco.
Resultado esperado: `EmailAlreadyExistsException` → HTTP 409.

---

### Teste 8 — Senha incorreta no login

```java
@Test
void shouldThrow_whenPasswordIsWrong() {
    User user = fakeUser("hash_correto");
    when(userRepository.findByEmail("j@j.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("senha_errada", "hash_correto")).thenReturn(false);

    assertThrows(InvalidCredentialsException.class, () ->
        authService.login("j@j.com", "senha_errada")
    );
}
```

Cenário: email existe mas a senha não bate com o hash BCrypt.
Resultado esperado: `InvalidCredentialsException` → HTTP 401.

---

### Teste 9 — Email não encontrado no login

```java
@Test
void shouldThrow_whenEmailNotFound() {
    when(userRepository.findByEmail("naoexiste@j.com")).thenReturn(Optional.empty());

    assertThrows(InvalidCredentialsException.class, () ->
        authService.login("naoexiste@j.com", "qualquer")
    );
}
```

Cenário: email não existe no banco.
Resultado esperado: mesma exception que senha errada — não revelar qual dado está errado.

---

## Itinerary Engine Service — Testes de Erro

Classe: `ItineraryEngineServiceTest.java`
Pacote: `luvoygroup.voyage.features.itinerary.service`

### Teste 1 — Onboarding não concluído

```java
@Test
void shouldThrow_whenUserHasNotCompletedOnboarding() {
    when(prefsRepository.findByUserId(userId)).thenReturn(Optional.empty());

    assertThrows(OnboardingNotCompletedException.class, () ->
        engineService.generate(userId, destId, LocalDate.now(), LocalDate.now().plusDays(3))
    );
}
```

Cenário: usuário pulou o onboarding e tenta gerar um roteiro.
Resultado esperado: `OnboardingNotCompletedException` → HTTP 422.

---

### Teste 2 — Destino com POIs insuficientes

```java
@Test
void shouldThrow_whenDestinationHasTooFewPois() {
    when(poiRepository.findActiveByDestination(destId))
        .thenReturn(List.of(poi1, poi2)); // apenas 2

    assertThrows(InsufficientContentException.class, () ->
        engineService.generate(userWithPrefs, destId, LocalDate.now(), LocalDate.now().plusDays(5))
    );
}
```

Cenário: destino tem menos de 5 POIs ativos — não há conteúdo suficiente para gerar roteiro.
Resultado esperado: `InsufficientContentException` → HTTP 422.

---

### Teste 3 — Data de início depois da data de fim

```java
@Test
void shouldThrow_whenStartDateIsAfterEndDate() {
    LocalDate start = LocalDate.of(2026, 8, 10);
    LocalDate end   = LocalDate.of(2026, 8, 5);

    assertThrows(InvalidDateRangeException.class, () ->
        engineService.generate(userWithPrefs, destId, start, end)
    );
}
```

Cenário: datas invertidas na requisição.
Resultado esperado: `InvalidDateRangeException` → HTTP 400.

---

### Teste 4 — Data de início no passado

```java
@Test
void shouldThrow_whenStartDateIsInThePast() {
    LocalDate yesterday = LocalDate.now().minusDays(1);

    assertThrows(InvalidDateRangeException.class, () ->
        engineService.generate(userWithPrefs, destId, yesterday, LocalDate.now().plusDays(3))
    );
}
```

Cenário: usuário tenta criar roteiro para uma data que já passou.
Resultado esperado: `InvalidDateRangeException` → HTTP 400.

---

## Itinerary Engine Service — Testes de Invariante

### Teste 5 — POIs não se repetem entre dias

```java
@Test
void shouldNotRepeatPoisAcrossDays() {
    ItineraryResponseDTO result = engineService.generate(
        userWithPrefs, destId, LocalDate.now(), LocalDate.now().plusDays(2)
    );

    List<UUID> allPoiIds = result.days().stream()
        .flatMap(day -> day.activities().stream())
        .map(act -> act.poi().id())
        .toList();

    assertEquals(allPoiIds.size(), new HashSet<>(allPoiIds).size());
}
```

Cenário: o engine nunca deve alocar o mesmo POI em dias diferentes.
Resultado esperado: zero duplicatas na lista de POI IDs.

---

### Teste 6 — Engine respeita filtro de budget

```java
@Test
void shouldOnlyIncludePoisMatchingUserBudget() {
    // prefências: budget=ECONOMY
    // banco tem 3 POIs economy + 2 POIs luxury

    ItineraryResponseDTO result = engineService.generate(
        userWithEconomyBudget, destId, LocalDate.now(), LocalDate.now().plusDays(1)
    );

    result.days().forEach(day ->
        day.activities().forEach(act ->
            assertEquals(BudgetTier.ECONOMY, act.poi().budgetTier())
        )
    );
}
```

Cenário: usuário economy não deve ver POIs de tier luxury ou moderate.
Resultado esperado: todos os POIs no resultado têm `budgetTier = ECONOMY`.

---

## Exceptions customizadas esperadas

| Exception | HTTP Status | Quando |
|-----------|-------------|--------|
| `EmailAlreadyExistsException` | 409 | Email já cadastrado |
| `InvalidCredentialsException` | 401 | Login com email/senha incorretos |
| `OnboardingNotCompletedException` | 422 | Geração sem onboarding |
| `InsufficientContentException` | 422 | Destino com < 5 POIs ativos |
| `InvalidDateRangeException` | 400 | Datas inválidas |
| `ResourceNotFoundException` | 404 | Entidade não encontrada |
| `ForbiddenAccessException` | 403 | Acesso a recurso de outro usuário |
