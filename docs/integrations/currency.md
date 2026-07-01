# LUVOY — Currency Exchange Integration

## Provedor: Open Exchange Rates / ExchangeRate-API

Provedor recomendado: **ExchangeRate-API** (plano gratuito: 1.500 req/mês, atualização diária).

---

## Por que faz sentido no LUVOY

O usuário brasileiro viajando para a Europa precisa saber quanto está gastando em reais.
Exibir a cotação do dia diretamente no app elimina a fricção de abrir outro app para converter.

---

## Moedas exibidas

| Moeda | Símbolo | Países cobertos |
|-------|---------|----------------|
| EUR | € | França, Itália, Espanha, Bélgica, Holanda |
| GBP | £ | Reino Unido |
| CHF | Fr | Suíça (pós-MVP) |
| USD | $ | Referência global |

Base: **BRL** (real brasileiro)

---

## Endpoint

```
GET https://v6.exchangerate-api.com/v6/{API_KEY}/latest/BRL
```

Resposta relevante:
```json
{
  "base_code": "BRL",
  "conversion_rates": {
    "EUR": 0.1621,
    "GBP": 0.1389,
    "USD": 0.1842
  },
  "time_last_update_utc": "Tue, 01 Jul 2026 00:00:01 +0000"
}
```

---

## Onde aparece no app

### Tela de Destino (Flutter e React Web)
Widget "Câmbio do dia" na context bar:
```
€ 1 = R$ 6,17  ·  £ 1 = R$ 7,20  ·  $ 1 = R$ 5,43
```
Exibido abaixo do clima, antes do dossiê.

### Tela de Roteiro (Flutter)
Estimativa de custo do dia em BRL baseada no `budgetTier` do POI:
- ECONOMY: referência de preço baixo
- MODERATE: referência média
- LUXURY: referência alta

(valores de referência definidos editorialmente, não calculados automaticamente)

---

## Estratégia de cache

- Cache de **24 horas** — cotação do dia é suficiente, não precisa de tempo real
- Flutter: armazenar em `SharedPreferences` com timestamp
- React Web: TanStack Query com `staleTime: 24h`
- Spring Boot: endpoint proxy `/api/v1/currency/rates` que cacheia a resposta por 24h (evita expor a API key no cliente)

---

## Arquitetura recomendada

```
Flutter/React → GET /api/v1/currency/rates (Spring Boot)
                  └─► verifica cache interno (24h)
                        └─► se expirado: chama ExchangeRate-API
                              └─► armazena em memória e retorna
```

O Spring Boot age como proxy — a API key nunca fica exposta no cliente.

---

## Variáveis de ambiente

Backend (`backend/api/voyage/.env`):
```env
EXCHANGE_RATE_API_KEY=<chave-exchangerate-api>
```

---

## Cadastro

1. Acessar exchangerate-api.com
2. Criar conta gratuita
3. Copiar a API key do dashboard
4. Plano gratuito: 1.500 req/mês — suficiente para MVP

---

## Endpoint Spring Boot

```
GET /api/v1/currency/rates
```

Response:
```json
{
  "base": "BRL",
  "updatedAt": "2026-07-01T00:00:01Z",
  "rates": {
    "EUR": 0.1621,
    "GBP": 0.1389,
    "USD": 0.1842
  }
}
```

Rota pública — não exige autenticação.
