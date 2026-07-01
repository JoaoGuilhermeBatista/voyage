# LUVOY — Weather & Daylight Integration

## Provedor: Open-Meteo

**Gratuito, sem chave de API, sem cadastro.**
Dados precisos por coordenada geográfica (lat/lon).

---

## O que busca

| Dado | Onde aparece |
|------|-------------|
| Temperatura atual | Tela do destino — "23°C · Ensolarado" |
| Condição climática | Tela do destino — ícone + texto |
| Nascer do sol | Tela do destino e card do CountryBlock |
| Pôr do sol | Tela do destino e card do CountryBlock |

---

## Endpoint

```
GET https://api.open-meteo.com/v1/forecast
  ?latitude={lat}
  &longitude={lon}
  &current=temperature_2m,weathercode
  &daily=sunrise,sunset
  &timezone=auto
  &forecast_days=1
```

Exemplo para Paris (lat: 48.8566, lon: 2.3522):
```
https://api.open-meteo.com/v1/forecast?latitude=48.8566&longitude=2.3522&current=temperature_2m,weathercode&daily=sunrise,sunset&timezone=auto&forecast_days=1
```

Resposta relevante:
```json
{
  "current": {
    "temperature_2m": 19.2,
    "weathercode": 2
  },
  "daily": {
    "sunrise": ["2026-07-01T06:10"],
    "sunset": ["2026-07-01T21:58"]
  }
}
```

---

## Weather codes → texto e ícone

| Code | Condição PT-BR |
|------|---------------|
| 0 | Céu limpo |
| 1–3 | Parcialmente nublado |
| 45–48 | Nevoeiro |
| 51–67 | Chuvoso |
| 71–77 | Neve |
| 80–82 | Pancadas de chuva |
| 95–99 | Tempestade |

---

## Onde é chamado

### Flutter
- Chamada na abertura da tela de destino (`DestinationDetailScreen`)
- Usando o `lat/lon` do destino armazenado nos POIs do banco
- Cache de 30 minutos (não refazer a chamada a cada abertura)

### React Web
- Chamada no loader da rota `/destinos/$slug`
- Substitui os valores hardcoded em `src/data/destinations.ts`
- Cache via TanStack Query (staleTime: 30min)

---

## Coordenadas dos destinos

As coordenadas para a chamada de clima são extraídas do campo `latitude/longitude` do primeiro POI ativo do destino no banco.

Se o destino não tiver POIs ainda, usar coordenadas hardcoded por cidade (definidas no seed).

---

## Variáveis de ambiente

Nenhuma — Open-Meteo é público e não exige chave de API.

---

## Fallback

Se a API falhar ou estiver fora do ar:
- Exibir `"— · —"` no lugar da temperatura
- Exibir `"—"` no lugar do nascer/pôr do sol
- Nunca travar a tela por falha de clima
