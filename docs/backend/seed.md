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
        seedDestinationSections();
    }
}
```

---

## Estratégia de conteúdo — LLM

O conteúdo editorial (taglines, descrições, seções, POIs) é gerado via OpenAI GPT-4
através do endpoint admin `POST /api/v1/admin/destinations/generate`.

O `DataInitializer` popula apenas a estrutura base de cada destino (slug, nome, país, cidade).
O conteúdo completo é gerado pela LLM e salvo no banco com `status: DRAFT` para revisão.

Ver `docs/integrations/ai-features.md` para detalhes do sistema prompt e fluxo de revisão.

---

## Destinos (38)

HeroImageUrl aponta para Supabase Storage. Novos destinos têm `imageUrl: ""` até upload das fotos.

Imagens hospedadas no Supabase Storage. Novos destinos têm `imageUrl: ""` até upload manual.

### Europa (21 destinos originais)

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

### Expansão global (17 novos destinos — conteúdo gerado via LLM)

| Slug | Name | Country | City |
|------|------|---------|------|
| rio-de-janeiro | Rio de Janeiro | Brasil | Rio de Janeiro |
| sao-paulo | São Paulo | Brasil | São Paulo |
| salvador | Salvador | Brasil | Salvador |
| florianopolis | Florianópolis | Brasil | Florianópolis |
| buenos-aires | Buenos Aires | Argentina | Buenos Aires |
| cartagena | Cartagena | Colômbia | Cartagena |
| nova-york | Nova York | EUA | Nova York |
| miami | Miami | EUA | Miami |
| marrakech | Marrakech | Marrocos | Marrakech |
| cidade-do-cabo | Cidade do Cabo | África do Sul | Cidade do Cabo |
| zanzibar | Zanzibar | Tanzânia | Zanzibar |
| tokyo | Tóquio | Japão | Tóquio |
| bali | Bali | Indonésia | Ubud / Seminyak |
| bangkok | Bangkok | Tailândia | Bangkok |
| dubai | Dubai | Emirados Árabes | Dubai |
| singapura | Singapura | Singapura | Singapura |
| kyoto | Quioto | Japão | Quioto |

---

## Coleções (9) — escopo global

| Slug | Title | Eyebrow | Destinos representativos |
|------|-------|---------|--------------------------|
| romantic | Destinos românticos | Para dois | paris, veneza, rio-de-janeiro, bali, bruges, florenca |
| summer | Verão & sol | Estação alta | costa-amalfitana, zanzibar, miami, cartagena, nice, barcelona |
| fashion | Alta moda | Style guide | paris, milao, nova-york, tokyo, london, dubai |
| shopping | Shopping de luxo | Maison & atelier | paris, milao, singapura, dubai, florenca, nova-york |
| hidden-gems | Lugares escondidos | Off the radar | bordeaux, ghent, kyoto, salvador, bath, utrecht |
| coastal | Escapadas costeiras | Mar & sal | costa-amalfitana, zanzibar, miami, cartagena, cidade-do-cabo |
| gastronomy | Alta gastronomia | À mesa | paris, sao-paulo, singapura, roma, barcelona, tokyo |
| family | Viagens em família | Para todos | amsterdam, london, roma, barcelona, dubai, nova-york |
| villages | Vilarejos & cidades pequenas | Devagar | bruges, ghent, bath, kyoto, florianopolis, utrecht |

A ordem dos destinos dentro de cada coleção deve ser preservada (`position` na tabela `collection_destinations`).

---

## DestinationSections

Criar as 14 seções para cada um dos 21 destinos com `content = []` (array JSONB vazio).
O conteúdo real (restaurantes, cafés, etc.) será preenchido editorialmente depois.

Ordem obrigatória por `orderIndex`:

| orderIndex | SectionType |
|-----------|-------------|
| 0 | ROTEIRO |
| 1 | RESTAURANTES |
| 2 | CAFES |
| 3 | ATRACOES |
| 4 | EXPERIENCIAS |
| 5 | RUAS |
| 6 | INSTAGRAM |
| 7 | LOJAS |
| 8 | ESCONDIDOS |
| 9 | ROOFTOPS |
| 10 | SKINCARE |
| 11 | COMPRAS |
| 12 | PRATOS |
| 13 | MAPA |

Total de seções a criar: 21 destinos × 14 seções = **294 registros**.

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
- As URLs do CDN do Lovable são temporárias e **não são garantidas após o período de preview**
