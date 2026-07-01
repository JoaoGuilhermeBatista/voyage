# LUVOY — Docker & Supabase

## Arquitetura com Supabase

O Supabase substitui a infraestrutura local de banco, auth e storage. O Spring Boot continua sendo o servidor de negócio (business logic, Itinerary Engine, PDF), mas agora:

| Responsabilidade | Antes | Com Supabase |
|-----------------|-------|-------------|
| Banco de dados | PostgreSQL local/Docker | Supabase PostgreSQL (hospedado) |
| Autenticação | Spring Boot JWT próprio | Supabase Auth — Spring Boot valida o token |
| Storage de imagens | CDN temporário (Lovable) | Supabase Storage |
| Funções leves | — | Supabase Edge Functions (Deno) |

---

## Variáveis de ambiente

### Backend (`backend/api/voyage/.env`)

```env
# Supabase PostgreSQL
SPRING_DATASOURCE_URL=jdbc:postgresql://db.<project-ref>.supabase.co:5432/postgres
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=<supabase-db-password>

# JWT — usar o JWT secret do projeto Supabase (Settings > API > JWT Secret)
JWT_SECRET=<supabase-jwt-secret>

# Supabase (para Storage e chamadas internas)
SUPABASE_URL=https://<project-ref>.supabase.co
SUPABASE_SERVICE_ROLE_KEY=<service-role-key>
```

> O `JWT_SECRET` deve ser o mesmo configurado no Supabase — assim o Spring Boot valida tokens emitidos pelo Supabase Auth sem precisar emitir os próprios.

### React Web (`frontend/web/.env`)

```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_SUPABASE_URL=https://<project-ref>.supabase.co
VITE_SUPABASE_ANON_KEY=<anon-key>
```

### Flutter (`frontend/app/.env` — ainda não implementado)

```env
API_BASE_URL=http://10.0.2.2:8080/api/v1
SUPABASE_URL=https://<project-ref>.supabase.co
SUPABASE_ANON_KEY=<anon-key>
```

---

## Desenvolvimento local com Supabase CLI

O Supabase CLI sobe localmente um stack completo (PostgreSQL, Auth, Storage, Edge Functions) sem precisar de Docker manual.

**Instalar Supabase CLI:**
```bash
npm install -g supabase
```

**Iniciar o stack local:**
```bash
supabase start
```

Isso sobe automaticamente:
- PostgreSQL na porta `54322`
- Supabase Auth (GoTrue) na porta `54321`
- Supabase Storage na porta `54321/storage`
- Supabase Studio (dashboard local) na porta `54323`

**`.env` para dev local apontando para o Supabase local:**

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:54322/postgres
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
JWT_SECRET=super-secret-jwt-token-with-at-least-32-characters-long
SUPABASE_URL=http://localhost:54321
SUPABASE_SERVICE_ROLE_KEY=<service-role-key-do-output-do-supabase-start>
```

> Após `supabase start`, o CLI exibe as chaves locais (anon key, service role key, JWT secret) no terminal.

---

## Docker — apenas Spring Boot

Com Supabase gerenciando banco, auth e storage, o único container Docker necessário é o da API.

### Dockerfile

Arquivo: `backend/api/voyage/Dockerfile`

```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -q
COPY src ./src
RUN ./mvnw package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### docker-compose.yml (dev — API + Supabase local)

```yaml
services:
  api:
    build:
      context: backend/api/voyage
      dockerfile: Dockerfile
    container_name: app-api
    restart: unless-stopped
    env_file: backend/api/voyage/.env
    ports:
      - "8080:8080"
    extra_hosts:
      - "host.docker.internal:host-gateway"
```

> O `extra_hosts` permite que o container do Spring Boot alcance o Supabase CLI rodando no host (`localhost:54322`).

### .dockerignore

Arquivo: `backend/api/voyage/.dockerignore`

```
target/
.env
*.md
.git
.mvn/wrapper/maven-wrapper.jar
```

---

## Ordem de inicialização (dev local)

```
1. supabase start                              # sobe postgres, auth, storage locais
2. cd backend/api/voyage && cp .env.example .env  # preencher com keys do supabase start
3. ./mvnw spring-boot:run                      # api sobe em :8080
4. cd frontend/web && cp .env.example .env && npm run dev  # react sobe em :3000
```

---

## Impacto em código existente

### Spring Boot — AuthController

Com Supabase Auth, o `POST /auth/signup` e `POST /auth/login` passam a ser responsabilidade do Supabase — o cliente (React/Flutter) chama o Supabase diretamente.

O Spring Boot ainda precisa:
- Validar o JWT do Supabase nas rotas protegidas (o `JwtService` deve usar o mesmo `JWT_SECRET` do Supabase)
- Sincronizar o usuário no banco próprio na primeira requisição autenticada (`GET /api/v1/users/me` pode fazer upsert)

O `AuthController` pode ser removido ou reduzido a endpoints de perfil/onboarding.

### Spring Boot — Storage

Para upload de imagens para o Supabase Storage, adicionar a dependência do cliente Supabase ou usar chamadas HTTP diretas via `RestTemplate`/`WebClient` com a `SUPABASE_SERVICE_ROLE_KEY`.

### React Web

Adicionar `@supabase/supabase-js`:
```bash
npm install @supabase/supabase-js
```

Criar `src/lib/supabase.ts` com o cliente Supabase (usando `VITE_SUPABASE_URL` e `VITE_SUPABASE_ANON_KEY`).

### Flutter

Adicionar `supabase_flutter` ao `pubspec.yaml`:
```yaml
dependencies:
  supabase_flutter: ^2.0.0
```

---

## Produção

- `SPRING_DATASOURCE_URL` aponta para o Supabase cloud (`db.<ref>.supabase.co:5432`)
- `JWT_SECRET` = Supabase JWT Secret do projeto cloud (Settings > API)
- Imagens servidas via Supabase Storage CDN — URLs públicas permanentes
- Spring Boot deployado como container (Railway, Render, AWS ECS, etc.)
- Supabase Edge Functions deployadas via `supabase functions deploy`
