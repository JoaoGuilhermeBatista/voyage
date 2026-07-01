import { createFileRoute, Link } from "@tanstack/react-router";
import { destinationService } from "@/services/destinationService";
import { collectionService } from "@/services/collectionService";
import { destinations as fallbackDestinations } from "@/data/destinations";
import { collections as fallbackCollections, countries } from "@/data/collections";
import { Nav } from "@/components/luvoy/Nav";
import { BottomBar } from "@/components/luvoy/BottomBar";
import { DestinationCard } from "@/components/luvoy/DestinationCard";
import { CollectionRow } from "@/components/luvoy/CollectionRow";
import { CountryBlock } from "@/components/luvoy/CountryBlock";
import { EditorialStory } from "@/components/luvoy/EditorialStory";

const CDN = "https://id-preview--8de9ade5-4335-46c0-830b-34e895cdfb4a.lovable.app/__l5e/assets-v1";
const heroAmalfi = `${CDN}/b4011c8a-4a70-4032-a737-2c5761bdeb2a/hero-amalfi.jpg`;
const featPharmacy = `${CDN}/29ca362a-05a8-4a43-98e3-6a0add7abf69/feat-pharmacy.jpg`;
const featRooftop = `${CDN}/2869b0a5-cc02-4459-8cbb-242956da4973/feat-rooftop.jpg`;
const featCafe = `${CDN}/97aeae57-9557-4c4b-90cb-520d52281b76/feat-cafe.jpg`;
const featPasta = `${CDN}/6eedba3b-c137-4b26-b923-0b1873a2c085/feat-pasta.jpg`;
const storyNice = `${CDN}/66dd7cd1-1206-4adb-a785-7c9ee35afa64/dest-nice.jpg`;
const storyParis = `${CDN}/108831b8-1c79-45f5-b395-236bd88a8ae9/dest-paris.jpg`;
const storyMilan = `${CDN}/343c4b90-e359-4343-aff7-78c1dbd5bf74/dest-milan.jpg`;
const storyAmalfi = `${CDN}/3568ee17-d4e6-4f5d-b6d5-063ea7583fd8/dest-amalfi.jpg`;
const storyVenice = `${CDN}/ba92ed12-ece5-4d85-994c-125251cf84c3/dest-venice.jpg`;

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "LUVOY — Roteiros que transformam viagens em experiências" },
      { name: "description", content: "Curadoria europeia premium: 21 destinos cinematográficos, roteiros dia a dia, cafés escondidos, rooftops e experiências reservadas." },
    ],
  }),
  loader: async () => {
    const [destinations, collections] = await Promise.all([
      destinationService.getAll().catch(() => fallbackDestinations),
      collectionService.getAll().catch(() => fallbackCollections),
    ]);
    return { destinations, collections };
  },
  component: Home,
});

function Home() {
  const { destinations, collections } = Route.useLoaderData();

  return (
    <div className="min-h-screen bg-ivory text-navy">
      <Nav variant="light" />

      {/* HERO */}
      <section className="relative h-[100vh] flex items-end overflow-hidden">
        <img src={heroAmalfi} alt="Costa Amalfitana ao amanhecer" width={1920} height={1280} className="absolute inset-0 w-full h-full object-cover anim-zoom" />
        <div className="absolute inset-0 bg-gradient-to-b from-ink/15 via-transparent to-ink/25" />
        <div className="absolute inset-x-0 bottom-0 h-[55%] bg-gradient-to-t from-ink/45 via-ink/10 to-transparent" />
        <div className="relative z-10 w-full px-6 md:px-12 pb-24 md:pb-32">
          <div className="max-w-5xl">
            <p className="eyebrow text-ivory/80 anim-fade mb-8">LUVOY · Collection N°01</p>
            <h1 className="font-display text-6xl md:text-8xl lg:text-[10rem] leading-[0.9] text-ivory anim-rise">
              Viagens em <span className="font-display-italic text-gold-soft">experiências</span>.
            </h1>
          </div>
          <div className="anim-rise delay-300 mt-16 md:mt-24 flex items-end justify-between border-t border-ivory/20 pt-6">
            <p className="eyebrow text-ivory/70 hidden md:block">Costa Amalfitana · 25° · 06:35 → 20:20</p>
            <Link to="/destinos/$slug" params={{ slug: "costa-amalfitana" }} className="px-8 py-4 border border-ivory/40 hover:border-gold hover:bg-ivory/5 transition-all duration-500 rounded-full eyebrow text-ivory">
              Abrir Dossiê →
            </Link>
          </div>
        </div>
      </section>

      {/* FEATURED STRIP */}
      <section className="bg-ivory px-8 md:px-16 lg:px-24 pt-28 pb-16 max-w-[1600px] mx-auto">
        <div className="flex items-end justify-between mb-14">
          <div>
            <p className="eyebrow text-gold mb-4">Em Destaque</p>
            <h2 className="font-display text-3xl md:text-5xl lg:text-[3.5rem] leading-[1.05]">Seis cidades, <span className="font-display-italic">um convite</span>.</h2>
          </div>
          <p className="eyebrow text-slate hidden md:block">Deslize →</p>
        </div>
        <div className="flex gap-8 overflow-x-auto snap-x snap-mandatory pb-8 -mx-8 px-8 md:mx-1 md:px-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
          {destinations.slice(0, 6).map((d) => (
            <Link key={d.slug} to="/destinos/$slug" params={{ slug: d.slug }} className="group relative shrink-0 w-[78vw] md:w-[480px] aspect-[3/4] overflow-hidden snap-start">
              <img src={d.image} alt={d.name} loading="lazy" className="absolute inset-1 w-full h-full object-cover transition-transform duration-[1600ms] ease-[cubic-bezier(0.19,1,0.22,1)] group-hover:scale-[1.06]" />
              <div className="absolute inset-x-0 bottom-0 h-1/2 bg-gradient-to-t from-ink/55 via-ink/15 to-transparent" />
              <div className="absolute bottom-8 left-8 right-8 text-ivory">
                <p className="eyebrow text-gold-soft mb-3">{d.country}</p>
                <h3 className="font-display text-4xl md:text-5xl leading-[1]">{d.name}</h3>
                <p className="font-display-italic text-sm mt-3 opacity-80">{d.tagline}</p>
              </div>
            </Link>
          ))}
        </div>
      </section>

      <EditorialStory variant="fullbleed" tone="light" eyebrow="Editorial N°02 · European Summer" title="A luz dourada de" italic="um verão europeu." quote="Há uma hora exata em que o sol se rende às fachadas color albicocca — e tudo, por um momento, parece eterno." body="De junho a setembro, o continente desacelera. As cidades se entregam ao ritual lento dos aperitivos longos, das janelas abertas, dos passos sem destino. Um capítulo dedicado à estação mais cinematográfica do ano." meta="Junho · Setembro" image={storyAmalfi} imageAlt="Verão europeu na Costa Amalfitana" cta={{ label: "Abrir o ensaio", to: "/destinos/$slug" }} />

      {/* DESTINATIONS */}
      <section className="px-8 md:px-16 lg:px-24 py-40 max-w-[1600px] mx-auto">
        <div className="flex flex-col md:flex-row md:items-end justify-between mb-28 gap-10">
          <div>
            <p className="eyebrow text-gold mb-5">A Coleção</p>
            <h2 className="font-display text-5xl md:text-7xl leading-[1]">
              Vinte e uma cidades.<br />
              <span className="font-display-italic text-slate">Uma única curadoria.</span>
            </h2>
          </div>
          <p className="max-w-md text-base text-slate font-light leading-[1.7]">
            Cada destino é um dossiê completo — roteiro dia a dia, restaurantes, cafés, rooftops, ruas mais bonitas, lugares escondidos e dicas que só um concierge local conheceria.
          </p>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-x-10 gap-y-24">
          {destinations.map((dest, i) => (
            <DestinationCard key={dest.slug} dest={dest} index={i} />
          ))}
        </div>
      </section>

      <EditorialStory variant="split-left" tone="warm" eyebrow="Editorial N°03 · Hidden Cafés" title="Os cafés que" italic="ninguém te conta." quote="A primeira xícara é sempre um segredo compartilhado entre a cidade e quem chega antes do barulho." body="Pequenas torrefações em pátios escondidos, balcões de mármore onde o espresso ainda custa um euro, padarias com fila silenciosa às sete da manhã." meta="42 endereços · 9 cidades" image={featCafe} imageAlt="Café escondido em uma manhã europeia" cta={{ label: "Ler o capítulo", to: "/concierge" }} />

      {/* EDITORIAL DARK */}
      <section className="relative bg-ink text-ivory px-8 md:px-16 lg:px-24 py-40 overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-b from-ink via-ink to-navy opacity-95" aria-hidden />
        <div className="absolute -top-40 -right-40 size-[600px] rounded-full bg-gold/10 blur-[120px]" aria-hidden />
        <div className="absolute -bottom-40 -left-40 size-[500px] rounded-full bg-gold/5 blur-[120px]" aria-hidden />
        <div className="relative max-w-[1600px] mx-auto">
          <div className="flex flex-col md:flex-row items-baseline justify-between gap-10 mb-28">
            <div>
              <p className="eyebrow text-gold mb-6">After Dark · Curadoria Premium</p>
              <h2 className="font-display text-4xl md:text-6xl lg:text-7xl leading-[1]">Quando a cidade <span className="font-display-italic text-gold-soft">acende</span>.</h2>
            </div>
            <p className="max-w-md text-base text-ivory/60 font-light leading-[1.7]">Rooftops com senha, mesas de chef sem placa, bares subterrâneos que só os locais sussurram.</p>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-10">
            {[
              { img: featRooftop, label: "Rooftops", title: "Terraços que o hype ainda não revelou" },
              { img: featPasta, label: "Restaurantes", title: "As mesas escondidas dos chefs Michelin", className: "md:mt-24" },
              { img: featPharmacy, label: "Nightlife", title: "Bares subterrâneos · senha mensal" },
            ].map((item) => (
              <article key={item.label} className={`group cursor-pointer ${item.className ?? ""}`}>
                <div className="aspect-[3/4] overflow-hidden mb-7 bg-navy">
                  <img src={item.img} loading="lazy" alt={item.label} className="w-full h-full object-cover opacity-90 transition-all duration-[1400ms] group-hover:scale-105 group-hover:opacity-100" />
                </div>
                <p className="eyebrow text-gold mb-3">{item.label}</p>
                <h3 className="font-display text-2xl md:text-[1.75rem] text-ivory leading-[1.15]">{item.title}</h3>
              </article>
            ))}
          </div>
          <div className="mt-28 flex items-center gap-6">
            <span className="luvoy-hairline flex-1" />
            <Link to="/premium" className="eyebrow text-gold-soft hover:text-gold transition-colors">Ver a coleção noturna →</Link>
          </div>
        </div>
      </section>

      <EditorialStory variant="split-right" tone="dark" eyebrow="Editorial N°04 · Slow Luxury" title="O luxo de" italic="não ter pressa." quote="Verdadeiro luxo é poder ficar — ler uma manhã inteira, almoçar três horas, voltar amanhã ao mesmo banco da praça." body="Uma filosofia oposta ao roteiro lotado. Hotéis pequenos com proprietários presentes, almoços que se estendem até o pôr do sol, vinícolas onde se chega sem agenda." meta="Curadoria silenciosa" image={storyVenice} imageAlt="Luxo silencioso em Veneza ao crepúsculo" cta={{ label: "Conhecer a filosofia", to: "/premium" }} />

      {/* COLLECTIONS */}
      <section className="bg-ivory pt-32">
        <div className="px-8 md:px-16 lg:px-24 max-w-[1600px] mx-auto mb-10">
          <p className="eyebrow text-gold mb-6">As Coleções LUVOY</p>
          <h2 className="font-display text-5xl md:text-7xl lg:text-8xl leading-[1] max-w-5xl">Nove maneiras de <span className="font-display-italic">atravessar</span> a Europa.</h2>
          <p className="mt-10 max-w-xl text-base md:text-lg text-slate font-light leading-[1.7]">Curadorias editoriais que reorganizam o continente por emoção — do romantismo de Veneza ao verão amalfitano.</p>
        </div>
        {collections.map((c, i) => (
          <CollectionRow key={c.slug} collection={c} allDestinations={destinations} index={i} />
        ))}
      </section>

      <EditorialStory variant="split-left" tone="light" eyebrow="Editorial N°05 · Fashion Cities" title="As capitais que" italic="vestem o continente." quote="Milão pela manhã, Paris ao entardecer, Londres no domingo — cada cidade tem o seu próprio sotaque de elegância." body="Showrooms privados, ateliers que recebem por convite, vintage curado por gerações de famílias, ruas onde o street style é língua oficial." meta="Milão · Paris · Londres · Antuérpia" image={storyMilan} imageAlt="Capital da moda europeia" cta={{ label: "Abrir o roteiro", to: "/destinos/$slug" }} />

      {/* BY COUNTRY */}
      <section className="bg-beige px-8 md:px-16 lg:px-24 py-40">
        <div className="max-w-[1600px] mx-auto">
          <div className="flex flex-col md:flex-row md:items-end justify-between gap-10 mb-28">
            <div>
              <p className="eyebrow text-gold mb-5">Por país</p>
              <h2 className="font-display text-5xl md:text-7xl leading-[1]">Seis nações,<br /><span className="font-display-italic text-slate">vinte e uma cidades.</span></h2>
            </div>
            <p className="max-w-md text-base text-slate font-light leading-[1.7]">Cada país tem seu próprio ritmo, sua própria luz. Escolha pelo destino — ou pela alma do lugar.</p>
          </div>
          {countries.map((g, i) => (
            <CountryBlock key={g.flag} group={g} allDestinations={destinations} index={i} />
          ))}
        </div>
      </section>

      <EditorialStory variant="fullbleed" tone="light" eyebrow="Editorial N°06 · Coastal Escapes" title="O Mediterrâneo" italic="começa onde o asfalto termina." quote="Há vilas costeiras onde o tempo se mede pelo passo dos barcos — não pelo relógio." body="Calanques secretas da Provença, enseadas escondidas da Ligúria, vilas brancas do Algarve. Refúgios à beira-mar onde a água tem cor de cartão postal." meta="Mar · Sol · Silêncio" image={storyNice} imageAlt="Vila costeira mediterrânea" cta={{ label: "Ver os refúgios", to: "/destinos/$slug" }} />

      {/* PREMIUM TEASER */}
      <section className="relative bg-ink text-ivory px-8 md:px-16 lg:px-24 py-40 overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-br from-ink via-navy to-ink" aria-hidden />
        <div className="absolute top-1/2 -translate-y-1/2 -right-40 size-[700px] rounded-full bg-gold/8 blur-[140px]" aria-hidden />
        <div className="relative max-w-5xl mx-auto">
          <div className="flex flex-col md:flex-row justify-between items-start gap-16 mb-28">
            <div className="space-y-8 max-w-2xl">
              <p className="eyebrow text-gold">Assinatura LUVOY · Noir</p>
              <h2 className="font-display text-5xl md:text-7xl leading-[1] text-ivory">O roteiro perfeito para<br /><span className="font-display-italic font-light text-gold-soft">7 dias em Florença.</span></h2>
              <p className="text-ivory/65 font-light text-lg leading-[1.7] max-w-lg">Acesso aos roteiros completos, modo offline, alertas de horário, checklists personalizados e suporte concierge 24/7.</p>
            </div>
          </div>
          <div className="border-t border-ivory/15 pt-20 text-center">
            <Link to="/premium" className="inline-block mt-8 px-10 py-4 bg-gold text-ink eyebrow hover:bg-ivory transition-colors duration-500 rounded-full">
              Tornar-se Premium
            </Link>
          </div>
        </div>
      </section>

      <EditorialStory variant="split-right" tone="warm" eyebrow="Editorial N°07 · Romantic Getaways" title="Cidades feitas" italic="para dois." quote="Há lugares na Europa que parecem ter sido desenhados para serem caminhados de mãos dadas, em silêncio, ao fim da tarde." body="Pequenas pensões com varandas sobre canais, jantares à luz de velas em adegas medievais, passeios de gôndola ao amanhecer." meta="Veneza · Paris · Bruges · Bordeaux" image={storyParis} imageAlt="Cenário romântico em Paris" cta={{ label: "Planejar a viagem", to: "/concierge" }} />

      {/* FOOTER */}
      <footer className="border-t border-navy/10 bg-ivory py-20 px-8 md:px-16 lg:px-24">
        <div className="max-w-[1600px] mx-auto flex flex-col md:flex-row justify-between items-center gap-10">
          <div className="text-center md:text-left">
            <p className="font-display-italic text-3xl tracking-[0.3em] uppercase">Luvoy</p>
            <p className="mt-4 text-sm text-slate font-light">Roteiros que transformam viagens em experiências</p>
          </div>
          <div className="flex gap-12 eyebrow text-slate">
            <a href="#" className="hover:text-gold transition-colors">Newsletter</a>
            <a href="#" className="hover:text-gold transition-colors">Instagram</a>
            <a href="#" className="hover:text-gold transition-colors">Contato</a>
          </div>
        </div>
      </footer>

      <BottomBar />
    </div>
  );
}
