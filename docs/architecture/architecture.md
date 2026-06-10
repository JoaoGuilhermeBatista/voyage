# LUVOY — Architecture

## Stack

| Layer | Technology |
|-------|-----------|
| Frontend | Flutter 3 + Riverpod + GoRouter + Dio |
| Backend | Spring Boot 3 + Java 21 |
| Security | Spring Security + JWT |
| ORM | JPA + Hibernate + Lombok |
| Database | PostgreSQL 15 |
| Analytics | Firebase Analytics |
| Maps | Google Maps Platform |
| Payments | Stripe |
| PDF | Flutter packages: pdf, printing |

---

## Architecture Style

Feature-first + Clean Architecture

### Frontend Layers

- Presentation: screens, widgets, providers
- Domain: entities, usecases, repositories (abstract)
- Data: services (Dio), DTOs, repository implementations

### Backend Layers

- Controller → Service → Repository
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
