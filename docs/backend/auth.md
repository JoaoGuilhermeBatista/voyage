# LUVOY — Authentication

## Estratégia

Autenticação gerenciada pelo **Supabase Auth**. O Spring Boot não emite tokens — apenas valida os JWTs emitidos pelo Supabase.

---

## Fluxo completo

```
[React / Flutter]
  │
  ├─► supabase.auth.signUp({ email, password })
  │     └─► Supabase cria usuário em auth.users
  │         └─► retorna JWT assinado com JWT_SECRET do projeto
  │
  ├─► supabase.auth.signInWithPassword({ email, password })
  │     └─► Supabase autentica
  │         └─► retorna JWT assinado com JWT_SECRET do projeto
  │
  └─► Spring Boot API  →  Authorization: Bearer <jwt>
        └─► JwtService valida assinatura com JWT_SECRET
            └─► extrai sub (UUID do usuário no Supabase)
                └─► executa a requisição
```

**O Spring Boot não tem endpoints de signup nem login.**
Toda autenticação é delegada ao Supabase Auth no cliente.

---

## JWT (emitido pelo Supabase)

### Payload relevante

```json
{
  "sub": "<uuid-do-usuario-no-supabase>",
  "email": "user@email.com",
  "role": "authenticated",
  "iat": 1700000000,
  "exp": 1700604800
}
```

### Configuração

- Expiração: configurada no Supabase (padrão: 1 hora com refresh token)
- Algoritmo: HS256
- Chave secreta: `JWT_SECRET` — deve ser o mesmo valor de Settings → API → JWT Secret no dashboard Supabase

---

## Sincronização do usuário no banco LUVOY

Na **primeira requisição autenticada** (`GET /api/v1/users/me`), o Spring Boot:

1. Extrai o `sub` (UUID) e o `email` do JWT
2. Verifica se existe um `User` com esse UUID no banco
3. Se não existir, cria o registro (`upsert`)
4. Retorna o perfil

Isso garante que cada usuário do Supabase Auth tenha um registro correspondente na tabela `users` do banco — sem necessidade de um endpoint de "registro" separado no Spring Boot.

---

## Entidade User (ajustada)

Com Supabase Auth, o Spring Boot não gerencia senhas. A entidade `User` usa o UUID do Supabase como PK:

```
id                   UUID        PK  ← mesmo UUID do sub no JWT do Supabase
name                 String
email                String      unique
avatarUrl            String      nullable
onboardingCompleted  Boolean     default: false
preferences          OnboardingPreferences  OneToOne (nullable)
createdAt            LocalDateTime
updatedAt            LocalDateTime
deletedAt            LocalDateTime  nullable (soft delete)
```

> `passwordHash` foi removido — o Supabase Auth gerencia credenciais.

---

## Rotas públicas (Spring Boot)

- `GET /api/v1/destinations`
- `GET /api/v1/destinations/:slug`
- `GET /api/v1/collections`
- `GET /api/v1/collections/:slug`

Todas as demais rotas exigem `Authorization: Bearer <jwt>` válido.

---

## Exceções mapeadas (Spring Boot)

| Situação | HTTP |
|----------|------|
| Token ausente | 401 |
| Token malformado | 401 |
| Token expirado | 401 |
| Acesso a recurso de outro usuário | 403 |

Signup/login com email duplicado, senha errada, etc. são tratados **pelo Supabase** — o Spring Boot nunca vê essas situações.

---

## Clientes por plataforma

### React Web
```ts
import { createClient } from "@supabase/supabase-js";

export const supabase = createClient(
  import.meta.env.VITE_SUPABASE_URL,
  import.meta.env.VITE_SUPABASE_ANON_KEY
);

// Signup
await supabase.auth.signUp({ email, password });

// Login
const { data } = await supabase.auth.signInWithPassword({ email, password });
const jwt = data.session?.access_token; // enviar no header para o Spring Boot
```

### Flutter
```dart
await Supabase.initialize(
  url: Env.supabaseUrl,
  anonKey: Env.supabaseAnonKey,
);

// Signup
await supabase.auth.signUp(email: email, password: password);

// Login
final res = await supabase.auth.signInWithPassword(email: email, password: password);
final jwt = res.session?.accessToken; // enviar no header para o Spring Boot
```

---

## Futuro (pós-MVP)

- Google OAuth via Supabase Auth (um toggle no dashboard)
- Apple Sign In via Supabase Auth
- Magic Link (login sem senha)
