# LUVOY — Monetização

## Filosofia

LUVOY não compete por preço.
Foco em valor percebido, exclusividade e experiência premium.

NUNCA usar banner ads — destrói a percepção de luxo.

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

### Rewarded Ad (alternativa gratuita)

- Usuário assiste 1 ad para desbloquear 1 roteiro extra
- Não intrusivo — acionado apenas quando o usuário quer mais roteiros
- Preserva a percepção premium ao manter ads fora do fluxo principal

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
