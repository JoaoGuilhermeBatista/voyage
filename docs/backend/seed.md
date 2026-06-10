# LUVOY — Database Seed

## Objetivo

Popular o banco com os dados editoriais iniciais necessários para o app funcionar.
Sem seed, a home do React web fica vazia e o Itinerary Engine não tem destinos para trabalhar.

---

## Estratégia de implementação

Classe: `DataInitializer.java`
Anotação: `@Component` + implementa `CommandLineRunner`
Condição: só executa se `destinations` estiver vazio (idempotente)

```java
@Component
public class DataInitializer implements CommandLineRunner {
    @Override
    public void run(String... args) {
        if (destinationRepository.count() > 0) return;
        seedDestinations();
        seedCollections();
    }
}
```

---

## Destinos (21)

Dados editoriais estáticos — clima vem da API do Google Maps em tempo real.

| Slug | Name | Country | City |
|------|------|---------|------|
| paris | Paris | França | Paris |
| bordeaux | Bordeaux | França | Bordeaux |
| nice | Nice | França | Nice |
| marselha | Marselha | França | Marselha |
| roma | Roma | Itália | Roma |
| florenca | Florença | Itália | Florença |
| milao | Milão | Itália | Milão |
| veneza | Veneza | Itália | Veneza |
| costa-amalfitana | Costa Amalfitana | Itália | Amalfi |
| londres | Londres | Reino Unido | Londres |
| edimburgo | Edimburgo | Escócia | Edimburgo |
| amsterdam | Amsterdam | Holanda | Amsterdam |
| bruxelas | Bruxelas | Bélgica | Bruxelas |
| bruges | Bruges | Bélgica | Bruges |
| madrid | Madrid | Espanha | Madrid |
| barcelona | Barcelona | Espanha | Barcelona |
| sevilha | Sevilha | Espanha | Sevilha |
| bath | Bath | Reino Unido | Bath |
| rotterdam | Rotterdam | Holanda | Rotterdam |
| utrecht | Utrecht | Holanda | Utrecht |
| ghent | Ghent | Bélgica | Ghent |

Campos por destino a preencher: `slug`, `name`, `country`, `city`, `tagline`, `description`, `heroImageUrl`, `signature`, `tags`, `isActive=true`.

As `heroImageUrl` inicialmente podem apontar para o CDN do Lovable (temporário).
Migrar para storage permanente (S3 / Cloudflare R2) antes do lançamento.

---

## Coleções (9)

| Slug | Title | Italic | Eyebrow | Destinos (slugs) |
|------|-------|--------|---------|-----------------|
| romantic | Europa | Romântica | Para dois | paris, veneza, bruges, florenca, sevilha, bordeaux |
| summer | Verão | europeu | Estação alta | costa-amalfitana, nice, barcelona, marselha, sevilha, veneza |
| fashion | Capitais da | moda | Style guide | paris, milao, londres, amsterdam, madrid, florenca |
| shopping | Shopping | de luxo | Maison & atelier | paris, milao, londres, florenca, madrid, barcelona |
| hidden-gems | Lugares | escondidos | Off the radar | bordeaux, ghent, utrecht, bath, bruges, marselha |
| coastal | Escapadas | costeiras | Mar & sal | costa-amalfitana, nice, marselha, barcelona, sevilha |
| gastronomy | Gastronomia | europeia | À mesa | paris, florenca, roma, bordeaux, barcelona, bruxelas |
| family | Viagens em | família | Para todos | amsterdam, londres, roma, barcelona, bruges, edimburgo |
| villages | Vilarejos | europeus | Devagar | bruges, ghent, bath, utrecht, edimburgo |

A ordem dos destinos dentro de cada coleção deve ser preservada.

---

## DestinationSections

Inicialmente as seções podem ser criadas vazias (content = `[]`) para cada destino.
O conteúdo real (restaurantes, cafés, etc.) será preenchido editorialmente depois.

Criar as 14 seções para cada um dos 21 destinos na ordem:
0. ROTEIRO
1. RESTAURANTES
2. CAFES
3. ATRACOES
4. EXPERIENCIAS
5. RUAS
6. INSTAGRAM
7. LOJAS
8. ESCONDIDOS
9. ROOFTOPS
10. SKINCARE
11. COMPRAS
12. PRATOS
13. MAPA

---

## POIs

Nenhum POI no seed inicial.
POIs serão inseridos editorialmente via scripts SQL por destino quando o conteúdo estiver pronto.
O Itinerary Engine precisa de ao menos 5 POIs ativos por destino para gerar roteiros.

---

## Observações

- O seed nunca deve apagar dados existentes
- Sempre verificar `count() > 0` antes de executar
- Em produção, usar Flyway ou Liquibase para migrations versionadas (pós-MVP)
