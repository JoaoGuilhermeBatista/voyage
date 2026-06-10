# LUVOY — Motion System

## Filosofia

Animações suaves reforçam a percepção premium.
Nunca chamativas, nunca aceleradas, nunca com bounce.

---

## Durations

| Nome | Duração | Uso |
|------|---------|-----|
| `fast` | 150ms | Feedback de toque, estados de botão, hover |
| `normal` | 300ms | Transições de página, aparecimento de cards |
| `slow` | 500ms | Hero animations, expansão de imagem |

---

## Curves

| Nome | Curve | Uso |
|------|-------|-----|
| `primaryCurve` | `Curves.easeInOut` | Transições gerais |
| `entranceCurve` | `Curves.easeOut` | Elementos entrando na tela |
| `exitCurve` | `Curves.easeIn` | Elementos saindo da tela |

---

## Transições de página

Padrão: fade + slide horizontal

```dart
CustomTransitionPage(
  transitionsBuilder: (context, animation, _, child) {
    return FadeTransition(
      opacity: animation,
      child: SlideTransition(
        position: Tween(
          begin: const Offset(0.04, 0),
          end: Offset.zero,
        ).animate(CurvedAnimation(
          parent: animation,
          curve: Curves.easeOut,
        )),
        child: child,
      ),
    );
  },
  transitionDuration: const Duration(milliseconds: 300),
)
```

---

## Cards em lista (staggered)

Cards entram com fadeIn + translateY:

```dart
AnimationConfiguration.staggeredList(
  position: index,
  delay: const Duration(milliseconds: 60),
  child: SlideAnimation(
    verticalOffset: 20,
    child: FadeInAnimation(child: card),
  ),
)
```

---

## Hero animation

Imagem de destino expande de card para tela cheia:

```dart
Hero(
  tag: 'destination-hero-${destination.id}',
  child: CachedNetworkImage(...),
)
```

---

## Shimmer loader

Usar shimmer em vez de CircularProgressIndicator em todos os estados de loading:

```dart
Shimmer.fromColors(
  baseColor: Color(0xFFE8E4DC),
  highlightColor: Color(0xFFF6F2EB),
  child: skeletonWidget,
)
```

---

## Rules

- NUNCA usar `Curves.bounceIn` ou `Curves.elasticIn` — quebra o tom premium
- NUNCA animações com duração > 600ms (parece travado)
- NUNCA `CircularProgressIndicator` — usar `LoadingShimmer`
- Shimmer sempre nas cores do design system (não cinza padrão)
- Hero tags devem incluir o ID do item para evitar conflito
