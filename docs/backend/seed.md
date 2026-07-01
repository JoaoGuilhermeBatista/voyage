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

## Destinos (21)

Clima, nascer e pôr do sol são buscados em tempo real via Google Maps API — não armazenar no banco.
HeroImageUrl aponta para o CDN do Lovable provisoriamente. **Migrar para S3/Cloudflare R2 antes do lançamento.**

CDN base: `https://id-preview--8de9ade5-4335-46c0-830b-34e895cdfb4a.lovable.app/__l5e/assets-v1/`

| Slug | Name | Country | City | Tagline | Signature | HeroImageUrl (CDN) |
|------|------|---------|------|---------|-----------|-------------------|
| paris | Paris | França | Paris | A capital do silêncio luxuoso | Citypharma · Buly 1803 · Le Perchoir | `.../108831b8-1c79-45f5-b395-236bd88a8ae9/dest-paris.jpg` |
| bordeaux | Bordeaux | França | Bordeaux | O vinho como linguagem | Château Margaux · La Tupina · Rue Sainte-Catherine | `.../6cedd248-a8e9-4cda-b410-8c158858aadd/dest-bordeaux.jpg` |
| nice | Nice | França | Nice | A Riviera em sua forma mais pura | Hôtel Negresco · Cours Saleya · Mont Boron | `.../66dd7cd1-1206-4adb-a785-7c9ee35afa64/dest-nice.jpg` |
| marselha | Marselha | França | Marselha | Porto velho, alma nova | Le Petit Nice · Calanque de Sormiou · MuCEM | `.../59b863f1-5c54-4327-ae4e-ed334a16757f/dest-marseille.jpg` |
| roma | Roma | Itália | Roma | A cidade eterna ao amanhecer | Roscioli · Trastevere · Giardino degli Aranci | `.../936fc6c4-57d5-4abb-8f18-f7612abe90da/dest-rome.jpg` |
| florenca | Florença | Itália | Florença | O Renascimento em escala humana | Gilli 1733 · Uffizi · Boboli Gardens | `.../4344a979-73c5-4555-ae44-b142152d9010/dest-florence.jpg` |
| milao | Milão | Itália | Milão | A elegância como segunda pele | Camparino · 10 Corso Como · Navigli | `.../343c4b90-e359-4343-aff7-78c1dbd5bf74/dest-milan.jpg` |
| veneza | Veneza | Itália | Veneza | Uma cidade que respira pela água | Harry's Bar · Cannaregio · Acqua Alta | `.../ba92ed12-ece5-4d85-994c-125251cf84c3/dest-venice.jpg` |
| costa-amalfitana | Costa Amalfitana | Itália | Amalfi | O litoral mais cinematográfico do mundo | Le Sirenuse · Da Adolfo · Path of the Gods | `.../3568ee17-d4e6-4f5d-b6d5-063ea7583fd8/dest-amalfi.jpg` |
| londres | Londres | Reino Unido | Londres | Tradição em conversa com o agora | Claridge's · Daunt Books · Soho Farmhouse | `.../c7b5daa6-45b6-41d8-ab2b-aa30eab3f472/dest-london.jpg` |
| edimburgo | Edimburgo | Escócia | Edimburgo | Gótico, neblina e whisky | The Witchery · Dean Village · Calton Hill | `.../2eb7afc6-06b2-47c7-bc67-e156096247d4/dest-edinburgh.jpg` |
| amsterdam | Amsterdam | Holanda | Amsterdam | Tulipas, canais e luz de primavera | Pluk · Keukenhof · Jordaan em flor | `.../76b92b69-0688-4127-94b7-a0401dfe9b93/dest-amsterdam.jpg` |
| bruxelas | Bruxelas | Bélgica | Bruxelas | Onde o Art Nouveau encontra o cacau | Pierre Marcolini · Galeries Royales · Sablon | `.../72195439-8fae-4af3-95a4-6e8d89edae16/dest-brussels.jpg` |
| bruges | Bruges | Bélgica | Bruges | Um conto de fadas medieval | Markt · Beguinage · De Halve Maan | `.../132802c3-2d4d-4c78-b7a6-587e22c4539c/dest-bruges.jpg` |
| madrid | Madrid | Espanha | Madrid | Calor latino com refinamento europeu | DiverXO · Mercado San Miguel · Retiro | `.../a6eb1db7-b565-41c7-8ad6-017a783ef559/dest-madrid.jpg` |
| barcelona | Barcelona | Espanha | Barcelona | Gaudí, mar e modernismo | Disfrutar · Park Güell · El Born | `.../65775edf-c8de-40de-a9d5-cea417fb0303/dest-barcelona.jpg` |
| sevilha | Sevilha | Espanha | Sevilha | Andaluzia em sua forma mais pura | Alcázar · Triana · Casa Morales | `.../587060e1-7830-4078-b4c3-ab142652d769/dest-seville.jpg` |
| bath | Bath | Reino Unido | Bath | A elegância georgiana intacta | Royal Crescent · Pulteney Bridge · The Pump Room | `.../3108818d-ed8c-460a-bc39-5e51b4e25601/dest-bath.jpg` |
| rotterdam | Rotterdam | Holanda | Rotterdam | O futuro arquitetônico da Holanda | Markthal · Cube Houses · Hotel New York | `.../a9bd6517-3410-4eeb-9d3f-14187af5867d/dest-rotterdam.jpg` |
| utrecht | Utrecht | Holanda | Utrecht | A Holanda que poucos conhecem | Dom Tower · Oudegracht · Rietveld Schröder | `.../91f1c8b8-3856-42c2-8d7b-05335c506465/dest-utrecht.jpg` |
| ghent | Ghent | Bélgica | Ghent | Gótico flamengo iluminado | Gravensteen · Graslei · Patershol | `.../a7bc3d57-6bf6-4d11-aa27-c27787f64b2f/dest-ghent.jpg` |

### Descrições completas por destino

```
paris:             "Onde cada esquina é uma promessa de elegância — das farmácias do Marais aos rooftops escondidos de Pigalle."
bordeaux:          "Châteaux históricos, ostras de Arcachon e ruas de pedra douradas pelo sol da Aquitânia."
nice:              "Águas turquesa, sombrinhas vintage e a luz mediterrânea que pintores perseguiram por séculos."
marselha:          "O Vieux-Port ao entardecer, bouillabaisse autêntica e calanques de águas impossíveis."
roma:              "Caminhe pelas ruas antes do sol nascer — quando o Pantheon pertence apenas a você."
florenca:          "Janelas medievais de vinho, gelato artesanal e a cúpula de Brunelleschi recortando o céu toscano."
milao:             "Galleria Vittorio Emanuele, aperitivos no Brera e as butiques mais hype de Via della Spiga."
veneza:            "Cale silenciosos ao amanhecer, cicchetti em bácaros escondidos e luz que se reflete em cada palácio."
costa-amalfitana:  "Positano em cascata, limones de Sorrento e o azul que define todos os outros azuis."
londres:           "Notting Hill pastel, afternoon tea no Claridge's e antiquários escondidos em Marylebone."
edimburgo:         "A Royal Mile envolta em névoa, destilarias centenárias e o Arthur's Seat ao pôr do sol."
amsterdam:         "Cerejeiras em flor sobre os canais, bicicletas pastel e cafés ao ar livre banhados pela luz dourada da primavera europeia."
bruxelas:          "Grand-Place dourada ao entardecer, chocolataria Pierre Marcolini e galerias royaux."
bruges:            "Canais com cisnes, mansardas de tijolo e o silêncio dourado das tardes flamengas."
madrid:            "Tapas de autor em San Miguel, ateliers de Malasaña e o Retiro ao entardecer."
barcelona:         "Sagrada Família ao amanhecer, vermuth no Born e tardes na Barceloneta."
sevilha:           "Plaza de España dourada, laranjeiras em flor e flamenco em tablaos íntimos."
bath:              "Pedra cor de mel da Royal Crescent, termas romanas e tea rooms de outra era."
rotterdam:         "Markthal cinematográfico, casas-cubo de Piet Blom e o porto reinventado."
utrecht:           "Canais em dois níveis, Dom Tower medieval e cafés escondidos sob arcadas."
ghent:             "Castelo Gravensteen, Graslei ao entardecer e o silêncio dourado das ruas medievais."
```

---

## Coleções (9)

| Slug | Title | Italic | Eyebrow | Description | Destinos (slugs, em ordem) |
|------|-------|--------|---------|-------------|--------------------------|
| romantic | Europa | Romântica | Para dois | Cidades para se apaixonar — luz dourada, jantares íntimos e ruas que pedem mãos dadas. | paris, veneza, bruges, florenca, sevilha, bordeaux |
| summer | Verão | europeu | Estação alta | Sol mediterrâneo, brisa do mar e tardes que não querem terminar. | costa-amalfitana, nice, barcelona, marselha, sevilha, veneza |
| fashion | Capitais da | moda | Style guide | Onde a estética acontece — flagship stores, ateliers e o melhor people-watching da Europa. | paris, milao, londres, amsterdam, madrid, florenca |
| shopping | Shopping | de luxo | Maison & atelier | Da Avenue Montaigne à Via della Spiga — endereços confidenciais para colecionadores. | paris, milao, londres, florenca, madrid, barcelona |
| hidden-gems | Lugares | escondidos | Off the radar | Cidades pouco faladas, ruas esquecidas e endereços guardados como segredo. | bordeaux, ghent, utrecht, bath, bruges, marselha |
| coastal | Escapadas | costeiras | Mar & sal | O azul que define todos os outros azuis — falésias, calanques e enseadas privadas. | costa-amalfitana, nice, marselha, barcelona, sevilha |
| gastronomy | Gastronomia | europeia | À mesa | Michelin, bistrots familiares e mesas onde a história se conta em três tempos. | paris, florenca, roma, bordeaux, barcelona, bruxelas |
| family | Viagens em | família | Para todos | Cidades que encantam crianças e adultos — museus interativos, parques e ritmos suaves. | amsterdam, londres, roma, barcelona, bruges, edimburgo |
| villages | Vilarejos | europeus | Devagar | Pedra, neblina e o tempo que passa noutro compasso. Os cantos mais íntimos da Europa. | bruges, ghent, bath, utrecht, edimburgo |

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
