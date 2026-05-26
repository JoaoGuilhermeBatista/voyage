# Frontend Tests

# Objetivo

Garantir estabilidade visual e funcional do Flutter app.

---

# Tipos de Teste

## Unit Tests

Testar:
- providers Riverpod
- services
- utils
- lógica local

---

## Widget Tests

Testar:
- onboarding
- cards
- botões
- estados
- loading
- navegação

---

# Regras UX

## O app deve:
- manter consistência visual;
- evitar overflow;
- funcionar em múltiplos tamanhos;
- respeitar dark mode futuramente.

---

# Fluxos Críticos

## Onboarding
Validar:
- respostas salvas corretamente;
- progressão entre etapas;
- estados persistidos.

---

## Home
Validar:
- carregamento;
- exibição de roteiros;
- navegação.

---

## Itinerary
Validar:
- agrupamento por dia;
- exibição de cards;
- scroll;
- loading.

---

# Performance

O frontend deve:
- evitar rebuilds desnecessários;
- manter animações suaves;
- evitar travamentos.