# LUVOY — Architecture

## Stack

| Layer | Technology |
|-------|-----------|
| Frontend Web | React 19 + TanStack Start v1 + Tailwind v4 + shadcn/ui |
| Frontend Mobile | Flutter 3 + Riverpod + GoRouter + Dio |
| Backend | Spring Boot 3 + Java 21 |
| Security | Spring Security + JWT |
| ORM | JPA + Hibernate + Lombok |
| Database | PostgreSQL 15 |
| Analytics | Firebase Analytics |
| Maps | Google Maps Platform |
| Payments | Stripe |
| PDF | Flutter packages: pdf, printing |

---

## Dois frontends, um backend

O Spring Boot serve tanto o React web quanto o Flutter mobile via REST API.

```
React Web (Lovable)  ──┐
                        ├──► Spring Boot API ──► PostgreSQL
Flutter Mobile       ──┘
```

**CORS** configurado no `SecurityConfig` para aceitar origens do React web.
O Flutter mobile não precisa de CORS (não é browser).

---

## Frontends

### React Web (Lovable)
- Protótipo visual já existente, todo estático/hardcoded por enquanto
- Serve como referência de design e estrutura de páginas
- Será conectado à API Spring Boot progressivamente
- Rotas: `/`, `/destinos/$slug`, `/roteiro/$slug`, `/favoritos`, `/checklists`, `/concierge`, `/premium`
- A rota `/roteiro/$slug` exibe um **roteiro editorial estático** (conteúdo curado manualmente, igual para todos os visitantes — diferente do roteiro personalizado gerado pelo Itinerary Engine)

### Flutter Mobile
- Foco em mobile (Android/iOS)
- Consome a mesma API Spring Boot
- Roteiros gerados dinamicamente pelo Itinerary Engine (personalizados por usuário)
- URL base no emulador Android: `http://10.0.2.2:8080/api/v1`

---

## Architecture Style

Feature-first + Clean Architecture

### Frontend Layers (Flutter)

- **Presentation**: screens, widgets, providers (Riverpod)
- **Domain**: entities, usecases, repositories (abstract)
- **Data**: services (Dio), DTOs, repository implementations

### Backend Layers (Spring Boot)

- **Controller** → **Service** → **Repository**
- DTOs para todo I/O de API
- JPA Entities nunca expostas diretamente nas respostas

---

## Rules

### Frontend

Must NEVER:
- conter business logic dentro de widgets
- fazer chamadas diretas ao banco
- misturar abordagens de state management

### Backend

Must NEVER:
- expor JPA entities diretamente nas respostas
- colocar business logic em controllers
- pular validação de DTOs
