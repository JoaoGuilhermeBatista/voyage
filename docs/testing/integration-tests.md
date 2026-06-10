# LUVOY — Integration Tests (Controller Layer)

## Objetivo

Testar os endpoints HTTP verificando status codes, payloads de resposta
e comportamento com tokens JWT válidos e inválidos.

## Ferramentas

- JUnit 5
- `@WebMvcTest` — testa controllers sem subir o servidor completo
- `MockMvc` — simula requisições HTTP
- `@MockBean` — mocka os services injetados nos controllers

---

## Auth Controller

Classe: `AuthControllerTest.java`

### Teste 10 — Signup com body vazio → 400

```java
@Test
void shouldReturn400_whenSignupBodyIsEmpty() throws Exception {
    mockMvc.perform(post("/api/v1/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest());
}
```

### Teste 11 — Signup com email inválido → 400

```java
@Test
void shouldReturn400_whenEmailFormatIsInvalid() throws Exception {
    mockMvc.perform(post("/api/v1/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                { "name": "João", "email": "nao-e-email", "password": "senha123" }
            """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
}
```

### Teste 12 — Rota protegida sem token → 401

```java
@Test
void shouldReturn401_whenNoTokenProvided() throws Exception {
    mockMvc.perform(get("/api/v1/users/me"))
        .andExpect(status().isUnauthorized());
}
```

### Teste 13 — Token JWT malformado → 401

```java
@Test
void shouldReturn401_whenJwtIsMalformed() throws Exception {
    mockMvc.perform(get("/api/v1/users/me")
            .header("Authorization", "Bearer token.invalido.aqui"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
}
```

---

## Itinerary Controller

Classe: `ItineraryControllerTest.java`

### Teste 14 — Gerar roteiro sem onboarding → 422

```java
@Test
void shouldReturn422_whenUserHasNotDoneOnboarding() throws Exception {
    when(engineService.generate(any(), any(), any(), any()))
        .thenThrow(new OnboardingNotCompletedException());

    mockMvc.perform(post("/api/v1/itineraries")
            .header("Authorization", bearerToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "destinationId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "startDate": "2026-08-01",
                  "endDate": "2026-08-05"
                }
            """))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.error").value("ONBOARDING_NOT_COMPLETED"));
}
```

### Teste 15 — Deletar roteiro de outro usuário → 403

```java
@Test
void shouldReturn403_whenDeletingAnotherUsersItinerary() throws Exception {
    UUID outroUserId = UUID.randomUUID();
    when(itineraryRepository.findById(any()))
        .thenReturn(Optional.of(itineraryBelongingTo(outroUserId)));

    mockMvc.perform(delete("/api/v1/itineraries/algum-uuid")
            .header("Authorization", bearerTokenDoUsuarioLogado()))
        .andExpect(status().isForbidden());
}
```

### Teste 16 — Buscar roteiro inexistente → 404

```java
@Test
void shouldReturn404_whenItineraryNotFound() throws Exception {
    when(itineraryService.findById(any())).thenThrow(new ResourceNotFoundException());

    mockMvc.perform(get("/api/v1/itineraries/uuid-que-nao-existe")
            .header("Authorization", bearerToken()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("NOT_FOUND"));
}
```

---

## Favorites Controller

Classe: `FavoriteControllerTest.java`

### Teste 17 — POST /favorites sem nenhum target → 400

```java
@Test
void shouldReturn400_whenFavoriteHasNoTarget() throws Exception {
    mockMvc.perform(post("/api/v1/favorites")
            .header("Authorization", bearerToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
}
```

### Teste 18 — POST /favorites com dois targets simultâneos → 400

```java
@Test
void shouldReturn400_whenFavoriteHasMultipleTargets() throws Exception {
    mockMvc.perform(post("/api/v1/favorites")
            .header("Authorization", bearerToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "destinationId": "uuid-1",
                  "poiId": "uuid-2"
                }
            """))
        .andExpect(status().isBadRequest());
}
```

---

## Destinations Controller

Classe: `DestinationControllerTest.java`

### Teste 19 — GET /destinations com banco vazio → 200 lista vazia

```java
@Test
void shouldReturn200WithEmptyList_whenNoDestinationsExist() throws Exception {
    when(destinationService.findAllActive()).thenReturn(List.of());

    mockMvc.perform(get("/api/v1/destinations")
            .header("Authorization", bearerToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());
}
```

Nunca deve retornar 500 quando a lista está vazia.

---

## PDF Controller

Classe: `PdfControllerTest.java`

### Teste 20 — Export de roteiro de outro usuário → 403

```java
@Test
void shouldReturn403_whenExportingAnotherUsersPdf() throws Exception {
    when(itineraryService.findById(any()))
        .thenReturn(itineraryBelongingTo(outroUserId));

    mockMvc.perform(post("/api/v1/pdf/export/algum-uuid")
            .header("Authorization", bearerTokenDoUsuarioLogado()))
        .andExpect(status().isForbidden());
}
```

### Teste 21 — Export de roteiro inexistente → 404

```java
@Test
void shouldReturn404_whenItineraryForPdfNotFound() throws Exception {
    when(itineraryService.findById(any())).thenThrow(new ResourceNotFoundException());

    mockMvc.perform(post("/api/v1/pdf/export/uuid-invalido")
            .header("Authorization", bearerToken()))
        .andExpect(status().isNotFound());
}
```

---

## Padrão de resposta de erro

Todos os endpoints de erro retornam o mesmo formato:

```json
{
  "error": "UNAUTHORIZED",
  "message": "Token inválido ou expirado",
  "status": 401
}
```

O `GlobalExceptionHandler` (`@ControllerAdvice`) é responsável por mapear
cada exception customizada para o status HTTP e formato correto.

---

## Resumo dos testes de controller

| # | Endpoint | Cenário | Status esperado |
|---|----------|---------|----------------|
| 10 | POST /auth/signup | Body vazio | 400 |
| 11 | POST /auth/signup | Email inválido | 400 |
| 12 | GET /users/me | Sem token | 401 |
| 13 | GET /users/me | JWT malformado | 401 |
| 14 | POST /itineraries | Sem onboarding | 422 |
| 15 | DELETE /itineraries/:id | Roteiro alheio | 403 |
| 16 | GET /itineraries/:id | Não encontrado | 404 |
| 17 | POST /favorites | Sem target | 400 |
| 18 | POST /favorites | Dois targets | 400 |
| 19 | GET /destinations | Banco vazio | 200 [] |
| 20 | POST /pdf/export/:id | Roteiro alheio | 403 |
| 21 | POST /pdf/export/:id | Não encontrado | 404 |
