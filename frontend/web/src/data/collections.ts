export type Collection = {
  slug: string;
  title: string;
  italic: string;
  description: string;
  eyebrow: string;
  destinations: string[];
};

export const collections: Collection[] = [
  { slug: "romantic", title: "Destinos", italic: "românticos", eyebrow: "Para dois", description: "Cidades para se apaixonar — luz dourada, jantares íntimos e ruas que pedem mãos dadas.", destinations: ["paris", "veneza", "rio-de-janeiro", "quioto", "cartagena", "bruges"] },
  { slug: "summer", title: "Verão", italic: "& sol", eyebrow: "Estação alta", description: "Praias, brisa do mar e tardes que não querem terminar — em qualquer meridiano.", destinations: ["bali", "costa-amalfitana", "miami", "zanzibar", "florianopolis", "nice"] },
  { slug: "fashion", title: "Capitais da", italic: "moda", eyebrow: "Style guide", description: "Onde a estética acontece — flagship stores, ateliers e o melhor people-watching do mundo.", destinations: ["paris", "milao", "toquio", "new-york", "dubai", "londres"] },
  { slug: "gastronomy", title: "Alta", italic: "gastronomia", eyebrow: "À mesa", description: "Estrelas Michelin, mercados populares e mesas onde culturas se traduzem em sabor.", destinations: ["toquio", "paris", "cidade-do-mexico", "sao-paulo", "bangkok", "florenca"] },
  { slug: "hidden-gems", title: "Lugares", italic: "escondidos", eyebrow: "Off the radar", description: "Cidades pouco faladas, ruas esquecidas e endereços guardados como segredo.", destinations: ["zanzibar", "quioto", "cartagena", "ghent", "bath", "florianopolis"] },
  { slug: "coastal", title: "Escapadas", italic: "costeiras", eyebrow: "Mar & sal", description: "O azul que define todos os outros azuis — falésias, enseadas e oceanos de todo o mundo.", destinations: ["bali", "costa-amalfitana", "cape-town", "zanzibar", "rio-de-janeiro", "miami"] },
  { slug: "adventure", title: "Aventura", italic: "& natureza", eyebrow: "Explorar", description: "Trilhas, vulcões, safáris e paisagens que provam que o planeta ainda surpreende.", destinations: ["cape-town", "zanzibar", "bali", "florianopolis", "edimburgo", "marrakech"] },
  { slug: "family", title: "Viagens em", italic: "família", eyebrow: "Para todos", description: "Cidades que encantam crianças e adultos — museus interativos, parques e ritmos suaves.", destinations: ["singapura", "amsterdam", "toquio", "barcelona", "new-york", "roma"] },
  { slug: "urban", title: "Metrópoles", italic: "vibrantes", eyebrow: "Pulso urbano", description: "Cidades que nunca param — onde arquitetura, arte e gastronomia se reinventam a cada esquina.", destinations: ["new-york", "sao-paulo", "toquio", "dubai", "cidade-do-mexico", "bangkok"] },
];

export type CountryGroup = {
  flag: string;
  name: string;
  italic: string;
  destinations: string[];
};

export const countries: CountryGroup[] = [
  { flag: "BR", name: "Brasil", italic: "Diversidade infinita", destinations: ["rio-de-janeiro", "sao-paulo", "salvador", "florianopolis"] },
  { flag: "JP", name: "Japão", italic: "Tradição & futuro", destinations: ["toquio", "quioto"] },
  { flag: "IT", name: "Itália", italic: "Dolce vita", destinations: ["roma", "florenca", "milao", "veneza", "costa-amalfitana"] },
  { flag: "FR", name: "França", italic: "L'art de vivre", destinations: ["paris", "nice", "bordeaux", "marselha"] },
  { flag: "US", name: "Estados Unidos", italic: "Coast to coast", destinations: ["new-york", "miami", "new-orleans"] },
  { flag: "ES", name: "Espanha", italic: "Alma latina", destinations: ["madrid", "barcelona", "sevilha"] },
  { flag: "TH", name: "Tailândia", italic: "Land of smiles", destinations: ["bangkok"] },
  { flag: "ID", name: "Indonésia", italic: "Ilha dos deuses", destinations: ["bali"] },
  { flag: "ZA", name: "África do Sul", italic: "Rainbow nation", destinations: ["cape-town"] },
  { flag: "MA", name: "Marrocos", italic: "1001 cores", destinations: ["marrakech"] },
  { flag: "MX", name: "México", italic: "Civilização viva", destinations: ["cidade-do-mexico"] },
  { flag: "AR", name: "Argentina", italic: "Paixão portenha", destinations: ["buenos-aires"] },
  { flag: "CO", name: "Colômbia", italic: "Magia Caribe", destinations: ["cartagena"] },
  { flag: "GB", name: "Reino Unido", italic: "Tradição viva", destinations: ["londres", "edimburgo", "bath"] },
  { flag: "SG", name: "Singapura", italic: "City of tomorrow", destinations: ["singapura"] },
  { flag: "AE", name: "Emirados Árabes", italic: "Ambição sem limites", destinations: ["dubai"] },
];
