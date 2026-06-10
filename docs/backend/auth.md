# LUVOY — Authentication

## Strategy

Spring Security + JWT (stateless — sem sessão no servidor)

---

## Flow

```
POST /auth/signup
  → valida input (name, email, password)
  → verifica se email já existe → EmailAlreadyExistsException se sim
  → cria user com BCrypt(password)
  → gera JWT
  → retorna { token, user }

POST /auth/login
  → busca user por email → InvalidCredentialsException se não encontrar
  → valida senha com BCrypt → InvalidCredentialsException se não bater
  → gera JWT
  → retorna { token, user }

Rotas protegidas
  → Spring Security intercepta
  → valida header Authorization: Bearer <token>
  → extrai userId do JWT
  → injeta no SecurityContext
```

---

## JWT

### Payload

```json
{
  "sub": "<user_uuid>",
  "email": "user@email.com",
  "iat": 1700000000,
  "exp": 1700604800
}
```

### Configuração

- Expiração: 7 dias
- Algoritmo: HS256
- Chave secreta: variável de ambiente `JWT_SECRET` (nunca no código)

---

## Rotas públicas

- `POST /api/v1/auth/signup`
- `POST /api/v1/auth/login`

Todas as demais rotas exigem token válido.

---

## Exceções mapeadas

| Situação | Exception | HTTP |
|----------|-----------|------|
| Email já cadastrado | `EmailAlreadyExistsException` | 409 |
| Email não encontrado | `InvalidCredentialsException` | 401 |
| Senha incorreta | `InvalidCredentialsException` | 401 |
| Token ausente | Spring Security | 401 |
| Token malformado | Spring Security | 401 |
| Token expirado | Spring Security | 401 |

---

## Security Rules

- Senhas hasheadas com BCrypt (nunca armazenar plain text)
- `JWT_SECRET` nunca commitado no repositório
- Logout é stateless: o client descarta o token
- Email e senha inválidos retornam a mesma exception (não revelar qual campo errou)

---

## Futuro (pós-MVP)

- Google OAuth
- Apple Sign In
- Refresh token com rotação
