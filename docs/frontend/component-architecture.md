# LUVOY — Component Architecture

## Hierarquia de widgets

```
LuvoyApp (MaterialApp.router)
└── GoRouter
    └── Scaffold
        ├── LuvoyAppBar
        ├── Body (feature screen)
        └── LuvoyBottomNav
```

---

## Shared Widgets

Ficam em `lib/shared/widgets/`.
Nunca contêm lógica de feature específica.

| Widget | Descrição |
|--------|-----------|
| `LuvoyAppBar` | Barra superior com logo e ações contextuais |
| `LuvoyBottomNav` | Navegação entre Home, Favoritos, Perfil |
| `DestinationCard` | Card de destino com hero image e tags |
| `ItineraryCard` | Card de roteiro salvo com datas |
| `PoiCard` | Card de ponto de interesse com categoria e duração |
| `SlotSection` | Bloco de manhã / tarde / noite no roteiro |
| `PremiumButton` | Botão primário com estilo LUVOY |
| `GoldenTag` | Chip com cor dourada para tags editoriais |
| `LoadingShimmer` | Skeleton loader — nunca usar CircularProgressIndicator |
| `ErrorStateWidget` | Tela de erro com ação de retry |
| `EmptyStateWidget` | Tela vazia com mensagem editorial |

---

## Estrutura por feature

Cada feature em `lib/features/<name>/` segue:

```
<feature>/
├── data/
│   ├── datasources/      # chamadas Dio
│   ├── dtos/             # modelos de API (JSON)
│   └── repositories/     # implementações concretas
├── domain/
│   ├── entities/         # modelos de domínio (puros)
│   ├── repositories/     # interfaces abstratas
│   └── usecases/         # lógica de negócio isolada
└── presentation/
    ├── providers/        # Riverpod providers
    ├── screens/          # telas completas
    └── widgets/          # widgets específicos da feature
```

---

## Rules

- Widgets de feature nunca são importados por outra feature
- Shared widgets são StatelessWidget quando possível
- Widgets com lógica de estado usam `ConsumerWidget` (Riverpod)
- Telas grandes são compostas por widgets menores — nunca um único `build()` gigante
- `LoadingShimmer` em vez de spinner em todos os estados de loading
- `ErrorStateWidget` em vez de texto solto para erros

---

## Providers Riverpod

| Provider | Tipo | Descrição |
|----------|------|-----------|
| `destinationsProvider` | `AsyncNotifierProvider` | Lista de destinos (keepAlive) |
| `itinerariesProvider` | `AsyncNotifierProvider` | Roteiros do usuário |
| `authProvider` | `NotifierProvider` | Estado de autenticação e usuário atual |
| `favoritesProvider` | `AsyncNotifierProvider` | Favoritos do usuário |
| `appRouterProvider` | `Provider` | Instância do GoRouter |

`keepAlive: true` em `destinationsProvider` — dados estáticos que não mudam durante a sessão.
