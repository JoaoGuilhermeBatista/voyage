# LUVOY — Backend Entities (JPA)

## User

```
id                   UUID        PK
name                 String
email                String      unique
passwordHash         String
avatarUrl            String      nullable
onboardingCompleted  Boolean     default: false
preferences          OnboardingPreferences  OneToOne (nullable)
createdAt            LocalDateTime
updatedAt            LocalDateTime
deletedAt            LocalDateTime  nullable (soft delete)
```

---

## OnboardingPreferences

```
id         UUID    PK
user       User    OneToOne FK → users.id
budget     Enum    ECONOMY | MODERATE | LUXURY
rhythm     Enum    RELAXED | MODERATE | INTENSE
groupType  Enum    SOLO | COUPLE | FAMILY | GROUP
interests  List<String>  ex: ["gastronomia","cultura","aventura"]
createdAt  LocalDateTime
updatedAt  LocalDateTime
```

---

## Destination

```
id           UUID    PK
slug         String  unique  ← identificador de URL (ex: "paris", "costa-amalfitana")
name         String          ← nome da cidade (ex: "Paris")
country      String
city         String
tagline      String          ← frase editorial curta (ex: "A capital do silêncio luxuoso")
description  String
heroImageUrl String
signature    String          ← destaques curatoriais (ex: "Citypharma · Buly 1803 · Le Perchoir")
tags         List<String>    ex: ["romantico","luxo","familia"]
isActive     Boolean         default: true
pois         List<Poi>       OneToMany
sections     List<DestinationSection>  OneToMany
createdAt    LocalDateTime
updatedAt    LocalDateTime
```

Nota: dados de clima, nascer/pôr do sol são buscados em tempo real via Google Maps API no app.
Não armazenar weatherTemp, sunrise, sunset no banco.

---

## DestinationSection (nova)

Representa uma das 14 seções do dossiê de cada destino.

```
id            UUID         PK
destination   Destination  ManyToOne FK → destinations.id
sectionType   Enum         ROTEIRO | RESTAURANTES | CAFES | ATRACOES |
                           EXPERIENCIAS | RUAS | INSTAGRAM | LOJAS |
                           ESCONDIDOS | ROOFTOPS | SKINCARE | COMPRAS |
                           PRATOS | MAPA
content       JSONB        ← List<{ name: String, desc: String }>
orderIndex    Integer      ← ordem de exibição (0–13)
```

Exemplo de content:
```json
[
  { "name": "Le Petit Atelier", "desc": "Bistrot íntimo · 12 lugares · menu degustação sazonal." },
  { "name": "Maison Verre", "desc": "Estrelado Michelin · reserva com 3 meses de antecedência." }
]
```

---

## Collection (nova)

Representa uma das 9 coleções editoriais.

```
id           UUID    PK
slug         String  unique  ← ex: "romantic", "hidden-gems"
title        String          ← ex: "Europa"
italic       String          ← ex: "Romântica" (renderizado em itálico)
eyebrow      String          ← ex: "Para dois"
description  String
orderIndex   Integer         ← ordem de exibição
destinations List<Destination>  ManyToMany (ordem importa)
createdAt    LocalDateTime
```

---

## Poi (Point of Interest)

Usado pelo Itinerary Engine no app mobile.

```
id                     UUID    PK
destination            Destination  ManyToOne FK → destinations.id
title                  String
category               Enum    MUSEUM | RESTAURANT | HOTEL | BEACH | PARK | SHOPPING | NIGHTLIFE
description            String
address                String
latitude               BigDecimal
longitude              BigDecimal
images                 List<String>
tags                   List<String>  ex: ["gastronomia","romantico"]
budgetTier             Enum    ECONOMY | MODERATE | LUXURY
openingTime            LocalTime  nullable
closingTime            LocalTime  nullable
averageDurationMinutes Integer
isActive               Boolean  default: true
createdAt              LocalDateTime
updatedAt              LocalDateTime
```

---

## Itinerary

```
id            UUID         PK
user          User         ManyToOne FK → users.id
destination   Destination  ManyToOne FK → destinations.id
startDate     LocalDate
endDate       LocalDate
title         String
pdfUrl        String       nullable
days          List<ItineraryDay>  OneToMany
createdAt     LocalDateTime
updatedAt     LocalDateTime
deletedAt     LocalDateTime  nullable (soft delete)
```

---

## ItineraryDay

```
id           UUID       PK
itinerary    Itinerary  ManyToOne FK → itineraries.id
dayNumber    Integer
date         LocalDate
activities   List<ItineraryActivity>  OneToMany
createdAt    LocalDateTime
```

---

## ItineraryActivity

```
id           UUID            PK
itineraryDay ItineraryDay    ManyToOne FK → itinerary_days.id
poi          Poi             ManyToOne FK → pois.id
slot         Enum            MORNING | AFTERNOON | EVENING
orderInSlot  Integer
createdAt    LocalDateTime
```

---

## Favorite

```
id            UUID         PK
user          User         ManyToOne FK → users.id
destination   Destination  nullable FK
poi           Poi          nullable FK
itinerary     Itinerary    nullable FK
createdAt     LocalDateTime
```

Regra: exatamente um dos três campos (destination, poi, itinerary) deve ser não-nulo.
Validação feita na camada de serviço antes de persistir.

---

## Enums

### BudgetTier
`ECONOMY` | `MODERATE` | `LUXURY`

### TravelRhythm
`RELAXED` | `MODERATE` | `INTENSE`

### GroupType
`SOLO` | `COUPLE` | `FAMILY` | `GROUP`

### PoiCategory
`MUSEUM` | `RESTAURANT` | `HOTEL` | `BEACH` | `PARK` | `SHOPPING` | `NIGHTLIFE`

### ItinerarySlot
`MORNING` | `AFTERNOON` | `EVENING`

### SectionType
`ROTEIRO` | `RESTAURANTES` | `CAFES` | `ATRACOES` | `EXPERIENCIAS` | `RUAS` |
`INSTAGRAM` | `LOJAS` | `ESCONDIDOS` | `ROOFTOPS` | `SKINCARE` | `COMPRAS` |
`PRATOS` | `MAPA`
