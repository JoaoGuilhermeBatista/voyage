# LUVOY — CORS Configuration

## Por que CORS é necessário

O projeto tem dois frontends que consomem a mesma API Spring Boot:
- **Flutter mobile** — usa `http://10.0.2.2:8080` (Android emulator) ou conexão direta (iOS); não precisa de CORS pois não é browser
- **React web (Lovable)** — roda no browser, precisa de CORS configurado

CORS deve ser configurado no Spring Boot para permitir requisições do frontend React.

---

## Origens permitidas

| Ambiente | Origem |
|----------|--------|
| Dev local (Vite) | `http://localhost:3000` |
| Dev local (TanStack Start) | `http://localhost:5173` |
| Lovable preview | `https://*.lovable.app` |
| Produção web (futuro) | domínio a definir |

---

## Configuração

Implementar como `CorsConfigurationSource` bean dentro do `SecurityConfig.java`:

```java
@Bean
CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();

    config.setAllowedOriginPatterns(List.of(
        "http://localhost:3000",
        "http://localhost:5173",
        "https://*.lovable.app"
    ));

    config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
}
```

E referenciar no `SecurityFilterChain`:

```java
http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
```

---

## Rotas públicas vs protegidas

O CORS se aplica a todas as rotas `/api/**`.
A distinção de autenticação (JWT obrigatório ou não) é separada e controlada pelo `SecurityConfig`.

---

## Variável de ambiente (pós-MVP)

Quando o domínio de produção do React for definido, adicionar:

```
CORS_ALLOWED_ORIGINS=https://app.luvoy.com,https://*.lovable.app
```

E ler via `@Value` no `SecurityConfig` para não hardcodar URLs de produção.
