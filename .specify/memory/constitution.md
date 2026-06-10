# LUVOY — Constitution

## Product

LUVOY é um app premium de roteiros de viagem.
Posicionamento: Luxury Editorial / Concierge Digital.

## Stack

| Layer | Technology |
|-------|-----------|
| Frontend | Flutter 3 + Riverpod + GoRouter + Dio |
| Backend | Spring Boot 3 + Java 21 + Spring Security + JWT |
| ORM | JPA + Hibernate + Lombok |
| Database | PostgreSQL 15 |
| Maps | Google Maps Platform |
| Payments | Stripe |
| PDF | Flutter packages: pdf, printing |

## Non-Negotiable Rules

### Produto
- Sem IA generativa no MVP
- Sem banner ads (destrói percepção de luxo)
- Sem booking de hotel ou voos no MVP
- Afiliados só pós-MVP

### Backend
- JWT stateless — sem sessão no servidor
- JPA entities nunca expostas diretamente nas respostas (sempre DTO)
- Business logic sempre na camada de Service, nunca no Controller
- Todo endpoint com erro previsível retorna formato: { error, message, status }
- Exceções mapeadas por GlobalExceptionHandler (@ControllerAdvice)

### Frontend
- Business logic nunca dentro de widgets
- LoadingShimmer em vez de CircularProgressIndicator
- ConsumerWidget apenas onde há lógica de estado (Riverpod)
- Widgets de feature não importados por outra feature
- URL da API em desenvolvimento: http://10.0.2.2:8080/api/v1 (Android Emulator)

### Design
- Paleta: #1D407A (azul primário) + #F2B15A (dourado) + #F6F2EB (fundo)
- Border radius: button=14, card=24, input=16
- Animações: nunca bounce, nunca > 600ms
- Fonts: Cormorant Garamond (editorial) + Manrope (UI)

## Testes

Todo erro previsível deve ter teste unitário ou de controller.
Testes de autorização (403) obrigatórios para endpoints de recurso de usuário.
Engine nunca deve retornar POI duplicado ou fora do budget do usuário.
