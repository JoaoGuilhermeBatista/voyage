const CDN = "https://id-preview--8de9ade5-4335-46c0-830b-34e895cdfb4a.lovable.app/__l5e/assets-v1";

export type Destination = {
  slug: string;
  name: string;
  country: string;
  tagline: string;
  description: string;
  image: string;
  weather: { temp: string; condition: string };
  daylight: { sunrise: string; sunset: string };
  signature: string;
};

export type DestinationDetail = Destination & {
  sections: {
    sectionType: string;
    orderIndex: number;
    content: { name: string; desc: string }[];
  }[];
};

// Dados estáticos usados como fallback enquanto a API não está conectada.
// weather e daylight são sempre hardcoded — não vêm da API, virão do Google Maps API.
export const destinations: Destination[] = [
  { slug: "paris", name: "Paris", country: "França", tagline: "A capital do silêncio luxuoso", description: "Onde cada esquina é uma promessa de elegância — das farmácias do Marais aos rooftops escondidos de Pigalle.", image: `${CDN}/108831b8-1c79-45f5-b395-236bd88a8ae9/dest-paris.jpg`, weather: { temp: "18°C", condition: "Céu de Outono" }, daylight: { sunrise: "06:14", sunset: "20:38" }, signature: "Citypharma · Buly 1803 · Le Perchoir" },
  { slug: "bordeaux", name: "Bordeaux", country: "França", tagline: "O vinho como linguagem", description: "Châteaux históricos, ostras de Arcachon e ruas de pedra douradas pelo sol da Aquitânia.", image: `${CDN}/6cedd248-a8e9-4cda-b410-8c158858aadd/dest-bordeaux.jpg`, weather: { temp: "21°C", condition: "Sol Dourado" }, daylight: { sunrise: "06:32", sunset: "20:54" }, signature: "Château Margaux · La Tupina · Rue Sainte-Catherine" },
  { slug: "nice", name: "Nice", country: "França", tagline: "A Riviera em sua forma mais pura", description: "Águas turquesa, sombrinhas vintage e a luz mediterrânea que pintores perseguiram por séculos.", image: `${CDN}/66dd7cd1-1206-4adb-a785-7c9ee35afa64/dest-nice.jpg`, weather: { temp: "24°C", condition: "Mediterrâneo" }, daylight: { sunrise: "06:21", sunset: "20:42" }, signature: "Hôtel Negresco · Cours Saleya · Mont Boron" },
  { slug: "marselha", name: "Marselha", country: "França", tagline: "Porto velho, alma nova", description: "O Vieux-Port ao entardecer, bouillabaisse autêntica e calanques de águas impossíveis.", image: `${CDN}/59b863f1-5c54-4327-ae4e-ed334a16757f/dest-marseille.jpg`, weather: { temp: "23°C", condition: "Brisa do Mar" }, daylight: { sunrise: "06:27", sunset: "20:48" }, signature: "Le Petit Nice · Calanque de Sormiou · MuCEM" },
  { slug: "roma", name: "Roma", country: "Itália", tagline: "A cidade eterna ao amanhecer", description: "Caminhe pelas ruas antes do sol nascer — quando o Pantheon pertence apenas a você.", image: `${CDN}/936fc6c4-57d5-4abb-8f18-f7612abe90da/dest-rome.jpg`, weather: { temp: "22°C", condition: "Limpo" }, daylight: { sunrise: "06:42", sunset: "20:18" }, signature: "Roscioli · Trastevere · Giardino degli Aranci" },
  { slug: "florenca", name: "Florença", country: "Itália", tagline: "O Renascimento em escala humana", description: "Janelas medievais de vinho, gelato artesanal e a cúpula de Brunelleschi recortando o céu toscano.", image: `${CDN}/4344a979-73c5-4555-ae44-b142152d9010/dest-florence.jpg`, weather: { temp: "23°C", condition: "Toscana Pura" }, daylight: { sunrise: "06:48", sunset: "20:22" }, signature: "Gilli 1733 · Uffizi · Boboli Gardens" },
  { slug: "milao", name: "Milão", country: "Itália", tagline: "A elegância como segunda pele", description: "Galleria Vittorio Emanuele, aperitivos no Brera e as butiques mais hype de Via della Spiga.", image: `${CDN}/343c4b90-e359-4343-aff7-78c1dbd5bf74/dest-milan.jpg`, weather: { temp: "20°C", condition: "Suave" }, daylight: { sunrise: "06:50", sunset: "20:14" }, signature: "Camparino · 10 Corso Como · Navigli" },
  { slug: "veneza", name: "Veneza", country: "Itália", tagline: "Uma cidade que respira pela água", description: "Cale silenciosos ao amanhecer, cicchetti em bácaros escondidos e luz que se reflete em cada palácio.", image: `${CDN}/ba92ed12-ece5-4d85-994c-125251cf84c3/dest-venice.jpg`, weather: { temp: "19°C", condition: "Bruma Suave" }, daylight: { sunrise: "06:44", sunset: "20:10" }, signature: "Harry's Bar · Cannaregio · Acqua Alta" },
  { slug: "costa-amalfitana", name: "Costa Amalfitana", country: "Itália", tagline: "O litoral mais cinematográfico do mundo", description: "Positano em cascata, limones de Sorrento e o azul que define todos os outros azuis.", image: `${CDN}/3568ee17-d4e6-4f5d-b6d5-063ea7583fd8/dest-amalfi.jpg`, weather: { temp: "25°C", condition: "Sol & Mar" }, daylight: { sunrise: "06:35", sunset: "20:20" }, signature: "Le Sirenuse · Da Adolfo · Path of the Gods" },
  { slug: "londres", name: "Londres", country: "Reino Unido", tagline: "Tradição em conversa com o agora", description: "Notting Hill pastel, afternoon tea no Claridge's e antiquários escondidos em Marylebone.", image: `${CDN}/c7b5daa6-45b6-41d8-ab2b-aa30eab3f472/dest-london.jpg`, weather: { temp: "16°C", condition: "Nublado Sereno" }, daylight: { sunrise: "07:02", sunset: "19:24" }, signature: "Claridge's · Daunt Books · Soho Farmhouse" },
  { slug: "edimburgo", name: "Edimburgo", country: "Escócia", tagline: "Gótico, neblina e whisky", description: "A Royal Mile envolta em névoa, destilarias centenárias e o Arthur's Seat ao pôr do sol.", image: `${CDN}/2eb7afc6-06b2-47c7-bc67-e156096247d4/dest-edinburgh.jpg`, weather: { temp: "12°C", condition: "Neblina Atmosférica" }, daylight: { sunrise: "07:18", sunset: "18:56" }, signature: "The Witchery · Dean Village · Calton Hill" },
  { slug: "amsterdam", name: "Amsterdam", country: "Holanda", tagline: "Tulipas, canais e luz de primavera", description: "Cerejeiras em flor sobre os canais, bicicletas pastel e cafés ao ar livre banhados pela luz dourada da primavera europeia.", image: `${CDN}/76b92b69-0688-4127-94b7-a0401dfe9b93/dest-amsterdam.jpg`, weather: { temp: "18°C", condition: "Primavera Luminosa" }, daylight: { sunrise: "06:42", sunset: "20:36" }, signature: "Pluk · Keukenhof · Jordaan em flor" },
  { slug: "bruxelas", name: "Bruxelas", country: "Bélgica", tagline: "Onde o Art Nouveau encontra o cacau", description: "Grand-Place dourada ao entardecer, chocolataria Pierre Marcolini e galerias royaux.", image: `${CDN}/72195439-8fae-4af3-95a4-6e8d89edae16/dest-brussels.jpg`, weather: { temp: "14°C", condition: "Cinematográfico" }, daylight: { sunrise: "07:08", sunset: "19:20" }, signature: "Pierre Marcolini · Galeries Royales · Sablon" },
  { slug: "bruges", name: "Bruges", country: "Bélgica", tagline: "Um conto de fadas medieval", description: "Canais com cisnes, mansardas de tijolo e o silêncio dourado das tardes flamengas.", image: `${CDN}/132802c3-2d4d-4c78-b7a6-587e22c4539c/dest-bruges.jpg`, weather: { temp: "13°C", condition: "Sereno" }, daylight: { sunrise: "07:12", sunset: "19:22" }, signature: "Markt · Beguinage · De Halve Maan" },
  { slug: "madrid", name: "Madrid", country: "Espanha", tagline: "Calor latino com refinamento europeu", description: "Tapas de autor em San Miguel, ateliers de Malasaña e o Retiro ao entardecer.", image: `${CDN}/a6eb1db7-b565-41c7-8ad6-017a783ef559/dest-madrid.jpg`, weather: { temp: "26°C", condition: "Sol Castelhano" }, daylight: { sunrise: "07:08", sunset: "20:48" }, signature: "DiverXO · Mercado San Miguel · Retiro" },
  { slug: "barcelona", name: "Barcelona", country: "Espanha", tagline: "Gaudí, mar e modernismo", description: "Sagrada Família ao amanhecer, vermuth no Born e tardes na Barceloneta.", image: `${CDN}/65775edf-c8de-40de-a9d5-cea417fb0303/dest-barcelona.jpg`, weather: { temp: "24°C", condition: "Brisa Catalã" }, daylight: { sunrise: "07:00", sunset: "20:40" }, signature: "Disfrutar · Park Güell · El Born" },
  { slug: "sevilha", name: "Sevilha", country: "Espanha", tagline: "Andaluzia em sua forma mais pura", description: "Plaza de España dourada, laranjeiras em flor e flamenco em tablaos íntimos.", image: `${CDN}/587060e1-7830-4078-b4c3-ab142652d769/dest-seville.jpg`, weather: { temp: "30°C", condition: "Sol Andaluz" }, daylight: { sunrise: "07:18", sunset: "20:54" }, signature: "Alcázar · Triana · Casa Morales" },
  { slug: "bath", name: "Bath", country: "Reino Unido", tagline: "A elegância georgiana intacta", description: "Pedra cor de mel da Royal Crescent, termas romanas e tea rooms de outra era.", image: `${CDN}/3108818d-ed8c-460a-bc39-5e51b4e25601/dest-bath.jpg`, weather: { temp: "15°C", condition: "Bruma Inglesa" }, daylight: { sunrise: "07:00", sunset: "19:28" }, signature: "Royal Crescent · Pulteney Bridge · The Pump Room" },
  { slug: "rotterdam", name: "Rotterdam", country: "Holanda", tagline: "O futuro arquitetônico da Holanda", description: "Markthal cinematográfico, casas-cubo de Piet Blom e o porto reinventado.", image: `${CDN}/a9bd6517-3410-4eeb-9d3f-14187af5867d/dest-rotterdam.jpg`, weather: { temp: "15°C", condition: "Azul Frio" }, daylight: { sunrise: "07:14", sunset: "19:14" }, signature: "Markthal · Cube Houses · Hotel New York" },
  { slug: "utrecht", name: "Utrecht", country: "Holanda", tagline: "A Holanda que poucos conhecem", description: "Canais em dois níveis, Dom Tower medieval e cafés escondidos sob arcadas.", image: `${CDN}/91f1c8b8-3856-42c2-8d7b-05335c506465/dest-utrecht.jpg`, weather: { temp: "14°C", condition: "Crepúsculo Suave" }, daylight: { sunrise: "07:12", sunset: "19:16" }, signature: "Dom Tower · Oudegracht · Rietveld Schröder" },
  { slug: "ghent", name: "Ghent", country: "Bélgica", tagline: "Gótico flamengo iluminado", description: "Castelo Gravensteen, Graslei ao entardecer e o silêncio dourado das ruas medievais.", image: `${CDN}/a7bc3d57-6bf6-4d11-aa27-c27787f64b2f/dest-ghent.jpg`, weather: { temp: "13°C", condition: "Sereno" }, daylight: { sunrise: "07:14", sunset: "19:20" }, signature: "Gravensteen · Graslei · Patershol" },
];

export function getDestination(slug: string): Destination | undefined {
  return destinations.find((d) => d.slug === slug);
}
