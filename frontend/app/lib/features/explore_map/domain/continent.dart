/// Agrupamento de país -> continente pra aba "Mapa" (#26): mundo -> toque
/// num continente -> cidades reais do catálogo naquele continente. Chave
/// de busca é o MESMO texto de `Destination.country` salvo no banco (em
/// português), não um código ISO — evita depender de um campo novo no
/// backend pra algo que é puramente derivado do país.
enum Continent {
  europa('Europa'),
  asia('Ásia'),
  africa('África'),
  americaDoNorte('América do Norte'),
  americaDoSul('América do Sul'),
  oceania('Oceania');

  final String label;
  const Continent(this.label);
}

/// País (exatamente como salvo em `destinations.country`) -> continente.
/// Casos de fronteira decididos (ver luvoy-pendencias.md): Turquia ->
/// Ásia (apesar de Istambul ter lado europeu), Chipre -> Europa (membro
/// da UE, apesar de geograficamente mais perto da Ásia).
const Map<String, Continent> countryToContinent = {
  'África do Sul': Continent.africa,
  'Albânia': Continent.europa,
  'Alemanha': Continent.europa,
  'Argentina': Continent.americaDoSul,
  'Austrália': Continent.oceania,
  'Áustria': Continent.europa,
  'Bélgica': Continent.europa,
  'Bósnia e Herzegovina': Continent.europa,
  'Brasil': Continent.americaDoSul,
  'Bulgária': Continent.europa,
  'Chile': Continent.americaDoSul,
  'Chipre': Continent.europa,
  'Colômbia': Continent.americaDoSul,
  'Coreia do Sul': Continent.asia,
  'Croácia': Continent.europa,
  'Dinamarca': Continent.europa,
  'Egito': Continent.africa,
  'Emirados Árabes': Continent.asia,
  'Eslováquia': Continent.europa,
  'Eslovênia': Continent.europa,
  'Espanha': Continent.europa,
  'Estônia': Continent.europa,
  'EUA': Continent.americaDoNorte,
  'Estados Unidos': Continent.americaDoNorte,
  'Finlândia': Continent.europa,
  'França': Continent.europa,
  'Grécia': Continent.europa,
  'Holanda': Continent.europa,
  'Hungria': Continent.europa,
  'Indonésia': Continent.asia,
  'Irlanda': Continent.europa,
  'Islândia': Continent.europa,
  'Itália': Continent.europa,
  'Japão': Continent.asia,
  'Letônia': Continent.europa,
  'Lituânia': Continent.europa,
  'Luxemburgo': Continent.europa,
  'Malta': Continent.europa,
  'Marrocos': Continent.africa,
  'México': Continent.americaDoNorte,
  'Mônaco': Continent.europa,
  'Montenegro': Continent.europa,
  'Noruega': Continent.europa,
  'Peru': Continent.americaDoSul,
  'Polônia': Continent.europa,
  'Portugal': Continent.europa,
  'Reino Unido': Continent.europa,
  'Escócia': Continent.europa,
  'República Tcheca': Continent.europa,
  'Romênia': Continent.europa,
  'Sérvia': Continent.europa,
  'Singapura': Continent.asia,
  'Suécia': Continent.europa,
  'Suíça': Continent.europa,
  'Tailândia': Continent.asia,
  'Tanzânia': Continent.africa,
  'Turquia': Continent.asia,
  'Vietnã': Continent.asia,
};

/// `null` quando o país não está no mapeamento (ex.: país novo adicionado
/// ao catálogo que ainda não foi classificado) — chamador decide se
/// ignora o destino ou avisa, nunca quebra a tela.
Continent? continentOf(String country) => countryToContinent[country];
