# LUVOY — Testing Strategy

## Objetivo

Garantir estabilidade, segurança e qualidade do produto em todas as camadas.
Testes de erro são tão importantes quanto testes de caminho feliz.

---

## Ferramentas

| Camada | Ferramenta |
|--------|-----------|
| Unit (backend) | JUnit 5 + Mockito |
| Controller (backend) | @WebMvcTest + MockMvc |
| Widget (frontend) | flutter_test |
| Navegação (frontend) | go_router + flutter_test |

---

## Pirâmide de Testes

```
        [  E2E  ]          ← futuro (pós-MVP)
      [Integration]        ← controller HTTP tests
    [  Unit Tests  ]       ← service + engine tests
```

O foco do MVP está nos dois níveis inferiores da pirâmide.

---

## Cobertura Mínima Esperada

| Área | Cobertura alvo |
|------|---------------|
| ItineraryEngineService | 100% dos caminhos de erro |
| AuthService | 100% dos caminhos de erro |
| Controllers (HTTP) | todos os endpoints documentados |
| Business rules | todas as regras do engine |

---

## Distribuição dos testes por arquivo

### Backend — Unit Tests (`backend-testing.md`)

Testa lógica isolada dos services com Mockito.

- `AuthServiceTest` → testes #7, #8, #9
- `ItineraryEngineServiceTest` → testes #1, #2, #3, #4, #5, #6

### Backend — Business Rules (`business-rules-testing.md`)

Testa as regras de negócio do Itinerary Engine.

- Ritmo × quantidade de atividades por dia
- Budget × filtro de POIs
- Score × interesses e groupType
- Slot × horário de funcionamento
- Comportamento com slots vazios e roteiro de 1 dia

### Backend — Integration Tests (`integration-tests.md`)

Testa os controllers via MockMvc verificando HTTP status e resposta.

- `AuthControllerTest` → testes #10, #11, #12, #13
- `ItineraryControllerTest` → testes #14, #15, #16
- `FavoriteControllerTest` → testes #17, #18
- `DestinationControllerTest` → teste #19
- `PdfControllerTest` → testes #20, #21

### Backend — Performance (`performance.md`)

Targets de latência por endpoint e targets de banco.

---

## Prioridade MVP

### Alta (bloqueia entrega)
- Testes do Itinerary Engine (#1–6 + business rules)
- Testes de autenticação (#7–9, #12, #13)
- Testes de autorização (#15, #20)

### Média
- Validações de input (#10, #11, #17, #18)
- Testes de 404 (#16, #21)
- Lista vazia (#19)

### Baixa (mas obrigatório antes de lançar)
- Frontend widget tests
- Navegação entre rotas

---

## Regras

- Cada exception customizada deve ter pelo menos um teste unitário
- Nenhum controller pode retornar 500 em cenário de erro previsível
- Testes de autorização (403) são obrigatórios para todo endpoint que
  acessa recurso de um usuário específico
- O engine nunca deve retornar POI duplicado no mesmo roteiro
- O engine nunca deve incluir POI fora do budget tier do usuário

---

## Filosofia

Qualidade de UX começa no backend.
Um app premium não pode travar, crashar, ou retornar dados incorretos.
Cada erro previsível deve ser tratado de forma silenciosa e elegante para o usuário.
