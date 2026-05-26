# LUVOY — Architecture

## Frontend

- Flutter
- Riverpod
- GoRouter
- Dio
- Clean Architecture
- Feature-first structure

---

## Backend

- Supabase
- PostgreSQL
- Edge Functions (future)
- REST APIs

---

## Architecture Style

The project follows:
- feature-first architecture
- scalable modular architecture
- separation of concerns
- reusable components

---

## Frontend Layers

### Presentation
- screens
- widgets
- providers

### Domain
- entities
- usecases
- repositories

### Data
- services
- DTOs
- API clients

---

## Rules

Frontend must NEVER:
- contain business logic inside widgets
- make direct database calls
- mix state management approaches