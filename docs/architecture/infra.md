# LUVOY — Infrastructure

## Stack de Infraestrutura

| Camada | Tecnologia | Responsabilidade |
|--------|-----------|-----------------|
| API | Spring Boot 3 + Java 21 | Business logic, Itinerary Engine, PDF |
| Banco de dados | Supabase PostgreSQL | Dados persistidos (usuários, destinos, roteiros, favoritos) |
| Autenticação | Supabase Auth | Signup, login, sessões — Spring Boot valida o JWT |
| Storage | Supabase Storage | Imagens dos destinos (substituindo CDN temporário do Lovable) |
| Edge Functions | Supabase Edge Functions (Deno) | Lógica leve: emails, webhooks, integrações |
| Analytics | Firebase Analytics | Eventos de uso no Flutter |
| Mapas | Google Maps Platform | Exibição de POIs, rotas, clima/nascer-pôr do sol (pós-MVP) |
| Pagamentos | Stripe | Plano Premium |

---

## Separação de ambientes

| Ambiente | Banco | API | Frontend |
|----------|-------|-----|----------|
| **Dev local** | Supabase CLI (`supabase start`) | `localhost:8080` | `localhost:3000` / emulador |
| **Staging** | Projeto Supabase separado | Container em staging | Preview URL |
| **Produção** | Projeto Supabase produção | Container em produção | App Store / Play Store / domínio web |

Cada ambiente deve ter:
- Projeto Supabase isolado (banco, auth, storage próprios)
- Variáveis de ambiente isoladas
- Chaves de API isoladas (Stripe, Google Maps, Firebase)

---

## Como Auth e Spring Boot se integram

```
Cliente (React/Flutter)
  │
  ├─► Supabase Auth ──────────────────► emite JWT
  │
  └─► Spring Boot API  (Bearer <jwt>)
        │
        └─► valida JWT com o mesmo JWT_SECRET do Supabase
            └─► executa a requisição
```

- O cliente autentica diretamente no Supabase (não passa pelo Spring Boot para login/signup)
- O Spring Boot valida o JWT do Supabase em cada requisição protegida
- Na primeira requisição autenticada, o Spring Boot faz upsert do usuário no banco usando o `sub` do JWT (UUID do Supabase Auth)

---

## Storage de imagens

- Bucket público: `destinations` — imagens dos dossiês
- Bucket privado: `itineraries` — PDFs gerados
- URLs públicas do bucket `destinations` substituem as URLs temporárias do CDN do Lovable
- Upload feito via Supabase SDK no cliente ou via Spring Boot com a `SERVICE_ROLE_KEY`

---

## Regras de segurança

Nunca expor:
- `SERVICE_ROLE_KEY` do Supabase no cliente (React/Flutter) — uso exclusivo do servidor
- `JWT_SECRET` — uso exclusivo do Spring Boot e do Supabase
- Credenciais do banco de dados

Sempre usar:
- `ANON_KEY` do Supabase nos clientes (React e Flutter)
- Variáveis de ambiente via `.env` (nunca hardcoded)
- HTTPS em todos os ambientes (exceto localhost)

---

## Performance

- Imagens dos destinos servidas pelo CDN do Supabase Storage
- Spring Boot com cache em memória para `GET /destinations` e `GET /collections` (pós-MVP)
- Lazy loading de imagens em React e Flutter
- Supabase Edge Functions para lógica que não requer a JVM

---

## Deployment

### Spring Boot (API)
- Container Docker (`backend/api/voyage/Dockerfile`)
- Opções: Railway, Render, AWS ECS, Fly.io
- Porta: `8080`

### Supabase
- Banco, auth, storage e edge functions gerenciados pelo Supabase cloud
- CLI local para desenvolvimento: `supabase start`

### React Web
- Build estático servido por Vercel, Netlify ou Cloudflare Pages (a definir)

### Flutter
- Google Play Store (Android)
- Apple App Store (iOS)
