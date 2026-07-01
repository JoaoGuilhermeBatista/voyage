export type Collection = {
  slug: string;
  title: string;
  italic: string;
  description: string;
  eyebrow: string;
  destinations: string[];
};

export const collections: Collection[] = [
  { slug: "romantic", title: "Europa", italic: "Romântica", eyebrow: "Para dois", description: "Cidades para se apaixonar — luz dourada, jantares íntimos e ruas que pedem mãos dadas.", destinations: ["paris", "veneza", "bruges", "florenca", "sevilha", "bordeaux"] },
  { slug: "summer", title: "Verão", italic: "europeu", eyebrow: "Estação alta", description: "Sol mediterrâneo, brisa do mar e tardes que não querem terminar.", destinations: ["costa-amalfitana", "nice", "barcelona", "marselha", "sevilha", "veneza"] },
  { slug: "fashion", title: "Capitais da", italic: "moda", eyebrow: "Style guide", description: "Onde a estética acontece — flagship stores, ateliers e o melhor people-watching da Europa.", destinations: ["paris", "milao", "londres", "amsterdam", "madrid", "florenca"] },
  { slug: "shopping", title: "Shopping", italic: "de luxo", eyebrow: "Maison & atelier", description: "Da Avenue Montaigne à Via della Spiga — endereços confidenciais para colecionadores.", destinations: ["paris", "milao", "londres", "florenca", "madrid", "barcelona"] },
  { slug: "hidden-gems", title: "Lugares", italic: "escondidos", eyebrow: "Off the radar", description: "Cidades pouco faladas, ruas esquecidas e endereços guardados como segredo.", destinations: ["bordeaux", "ghent", "utrecht", "bath", "bruges", "marselha"] },
  { slug: "coastal", title: "Escapadas", italic: "costeiras", eyebrow: "Mar & sal", description: "O azul que define todos os outros azuis — falésias, calanques e enseadas privadas.", destinations: ["costa-amalfitana", "nice", "marselha", "barcelona", "sevilha"] },
  { slug: "gastronomy", title: "Gastronomia", italic: "europeia", eyebrow: "À mesa", description: "Michelin, bistrots familiares e mesas onde a história se conta em três tempos.", destinations: ["paris", "florenca", "roma", "bordeaux", "barcelona", "bruxelas"] },
  { slug: "family", title: "Viagens em", italic: "família", eyebrow: "Para todos", description: "Cidades que encantam crianças e adultos — museus interativos, parques e ritmos suaves.", destinations: ["amsterdam", "londres", "roma", "barcelona", "bruges", "edimburgo"] },
  { slug: "villages", title: "Vilarejos", italic: "europeus", eyebrow: "Devagar", description: "Pedra, neblina e o tempo que passa noutro compasso. Os cantos mais íntimos da Europa.", destinations: ["bruges", "ghent", "bath", "utrecht", "edimburgo"] },
];

export type CountryGroup = {
  flag: string;
  name: string;
  italic: string;
  destinations: string[];
};

export const countries: CountryGroup[] = [
  { flag: "FR", name: "França", italic: "L'art de vivre", destinations: ["paris", "nice", "bordeaux", "marselha"] },
  { flag: "IT", name: "Itália", italic: "Dolce vita", destinations: ["roma", "florenca", "milao", "veneza", "costa-amalfitana"] },
  { flag: "ES", name: "Espanha", italic: "Alma latina", destinations: ["madrid", "barcelona", "sevilha"] },
  { flag: "UK", name: "Reino Unido", italic: "Tradição viva", destinations: ["londres", "edimburgo", "bath"] },
  { flag: "NL", name: "Holanda", italic: "Quiet luxury", destinations: ["amsterdam", "rotterdam", "utrecht"] },
  { flag: "BE", name: "Bélgica", italic: "Charme medieval", destinations: ["bruxelas", "bruges", "ghent"] },
];
