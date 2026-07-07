# LUVOY — Monetização

## Filosofia

LUVOY não compete por preço.
Foco em valor percebido, exclusividade e experiência premium.

O produto não exibe nenhum tipo de anúncio — nem banners, nem pop-ups, nem rewarded ads. A ausência de publicidade é parte do posicionamento premium e da experiência de uso.

---

## MVP — Modelo Freemium

### Plano Gratuito

- 1 roteiro gerado por mês
- Acesso a todos os destinos curados
- Favoritos (limite de 5 itens)
- **1 PDF export gratuito** (vitalício — não por mês)

### Plano Premium (Stripe)

- Roteiros ilimitados
- PDF exports ilimitados
- Histórico completo de roteiros
- Acesso antecipado a novos destinos
- Seção "Escondidos" com coordenadas desbloqueadas
- Acesso à comunidade de viajantes

---

## Regra de PDF Export

```
pdfExportCount = 0  →  gera PDF, incrementa contador, gratuito
pdfExportCount >= 1 e não é Premium  →  bloqueia com 402, redireciona para upgrade
pdfExportCount >= 1 e é Premium  →  gera PDF normalmente
```

### Implementação no backend

Campo adicional na entidade `User`:
```
pdfExportCount   Integer   default: 0
isPremium        Boolean   default: false
```

`PdfService.exportItinerary()`:
1. Busca o usuário pelo JWT
2. Verifica `isPremium` — se sim, gera e retorna
3. Verifica `pdfExportCount == 0` — se sim, gera, incrementa e retorna
4. Caso contrário, lança `PaymentRequiredException` → HTTP 402

### Endpoint

```
POST /api/v1/pdf/export/:itineraryId
```

Erros:
- `402 Payment Required` — usuário free que já usou o PDF gratuito
- `403 Forbidden` — roteiro de outro usuário
- `404 Not Found` — roteiro não encontrado

---

## Provedor de Pagamento

| Provedor | Uso |
|----------|-----|
| Stripe | MVP — sem mensalidade, taxa por venda |
| Mercado Pago | Alternativa regional se necessário |

### Stripe — o que configurar

- **Product**: "LUVOY Premium"
- **Price**: recorrente mensal (valor a definir) + opção anual com desconto
- **Webhook**: `customer.subscription.created` e `customer.subscription.deleted` → atualiza `isPremium` no banco
- **Checkout**: hosted (Stripe gerencia a página de pagamento)

---

## Pós-MVP

### Afiliados

- Booking Affiliate Program
- Expedia Group Partner Solutions
- Comissão típica: 3–10% por reserva confirmada
- Aparece no roteiro como: "Reservar hotel" / "Ver voos"
- Não disponível no MVP

### Consultoria Premium

- Roteiros 100% personalizados por curador humano
- Produto de alto ticket

### AI Features (App 2.0)

- Refinamento de roteiro via chat com LLM
- Disponível apenas no plano premium
- Não disponível no MVP
