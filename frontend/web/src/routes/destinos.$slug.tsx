import { createFileRoute, Link, notFound } from "@tanstack/react-router";
import { useState } from "react";
import { destinationService } from "@/services/destinationService";
import { getDestination } from "@/data/destinations";
import { Nav } from "@/components/luvoy/Nav";
import { BottomBar } from "@/components/luvoy/BottomBar";

const SECTIONS = [
  { id: "roteiro", label: "Roteiro Dia a Dia" },
  { id: "restaurantes", label: "Restaurantes" },
  { id: "cafes", label: "Cafés" },
  { id: "atracoes", label: "Atrações" },
  { id: "experiencias", label: "Experiências Reservadas" },
  { id: "ruas", label: "Ruas Mais Bonitas" },
  { id: "instagram", label: "Instagramáveis" },
  { id: "lojas", label: "Lojas Autênticas" },
  { id: "escondidos", label: "Lugares Escondidos" },
  { id: "rooftops", label: "Rooftops" },
  { id: "skincare", label: "Skincare & Farmácias" },
  { id: "compras", label: "Dicas de Compras" },
  { id: "pratos", label: "Pratos Típicos" },
  { id: "mapa", label: "Mapa" },
] as const;

const ROTEIRO_PLACEHOLDER = [
  { day: "Dia 01", title: "O Despertar", items: [
    { time: "07:30", text: "Café da manhã no hotel · luz dourada do quarto" },
    { time: "09:30", text: "Caminhada pelo centro histórico antes da multidão", highlight: true },
    { time: "12:30", text: "Almoço em bistrot local recomendado" },
    { time: "16:00", text: "Visita privada ao museu icônico", highlight: true },
    { time: "20:00", text: "Jantar com vista · reserva confirmada" },
  ]},
  { day: "Dia 02", title: "A Imersão", items: [
    { time: "08:00", text: "Mercado local · degustação artesanal" },
    { time: "11:00", text: "Galeria escondida · acesso exclusivo Luvoy", highlight: true },
    { time: "14:30", text: "Tarde livre · sugestões de bairro" },
    { time: "19:00", text: "Aperitivo em rooftop selecionado", highlight: true },
  ]},
];

export const Route = createFileRoute("/destinos/$slug")({
  loader: async ({ params }) => {
    const dest = await destinationService.getBySlug(params.slug).catch(() => {
      const fallback = getDestination(params.slug);
      if (!fallback) throw notFound();
      return { ...fallback, sections: [] };
    });
    if (!dest) throw notFound();
    return { dest };
  },
  head: ({ loaderData }) => ({
    meta: loaderData
      ? [
          { title: `${loaderData.dest.name}, ${loaderData.dest.country} — LUVOY` },
          { name: "description", content: `${loaderData.dest.tagline}. ${loaderData.dest.description}` },
          { property: "og:title", content: `${loaderData.dest.name} — LUVOY` },
          { property: "og:description", content: loaderData.dest.tagline },
          { property: "og:image", content: loaderData.dest.image },
        ]
      : [],
  }),
  component: DestinationPage,
});

function DestinationPage() {
  const { dest } = Route.useLoaderData();
  const [saved, setSaved] = useState(false);
  const [active, setActive] = useState<string>("roteiro");

  const getSectionContent = (type: string) => {
    const section = dest.sections?.find(
      (s) => s.sectionType.toUpperCase() === type.toUpperCase()
    );
    return section?.content ?? [];
  };

  return (
    <div className="min-h-screen bg-ivory text-navy">
      <Nav variant="dark" />

      {/* HERO */}
      <section className="relative h-[95vh] overflow-hidden">
        <img src={dest.image} alt={`${dest.name}, ${dest.country}`} width={1920} height={1280} className="absolute inset-0 w-full h-full object-cover anim-zoom" />
        <div className="absolute inset-0 bg-gradient-to-b from-ink/20 via-transparent to-ivory" />
        <div className="absolute inset-x-0 bottom-0 h-2/3 bg-gradient-to-t from-ink/40 via-ink/5 to-transparent" />
        <div className="relative z-10 h-full flex flex-col justify-end px-6 md:px-12 pb-20">
          <Link to="/" className="eyebrow text-ivory/80 hover:text-gold transition-colors mb-8 anim-fade">← Voltar à coleção</Link>
          <p className="eyebrow text-gold-soft anim-fade delay-100">{dest.country}</p>
          <h1 className="font-display-italic text-7xl md:text-9xl lg:text-[11rem] text-ivory leading-[0.85] mt-4 anim-rise">{dest.name}</h1>
          <p className="font-display text-2xl md:text-3xl text-ivory/90 mt-6 max-w-3xl anim-rise delay-200">{dest.tagline}</p>
        </div>
      </section>

      {/* CONTEXT BAR */}
      <section className="border-b border-navy/10 px-6 md:px-12 py-8">
        <div className="max-w-[1600px] mx-auto flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-8 md:gap-16">
            <div>
              <p className="eyebrow text-slate mb-2">Clima Hoje</p>
              <p className="font-display text-xl">{dest.weather.temp} · {dest.weather.condition}</p>
            </div>
            <div>
              <p className="eyebrow text-slate mb-2">Nascer do Sol</p>
              <p className="font-display-italic text-xl">{dest.daylight.sunrise}</p>
            </div>
            <div>
              <p className="eyebrow text-slate mb-2">Pôr do Sol</p>
              <p className="font-display-italic text-xl">{dest.daylight.sunset}</p>
            </div>
            <div>
              <p className="eyebrow text-slate mb-2">Modo Offline</p>
              <p className="font-display text-xl text-gold">Disponível</p>
            </div>
          </div>
          <button
            onClick={() => setSaved((s) => !s)}
            className={`px-6 py-3 border rounded-full eyebrow transition-all duration-500 ${saved ? "bg-navy border-navy text-ivory" : "border-navy/30 text-navy hover:border-gold hover:text-gold"}`}
          >
            {saved ? "♥ Salvo nos Favoritos" : "♡ Salvar Destino"}
          </button>
        </div>
      </section>

      {/* INTRO */}
      <section className="px-6 md:px-12 py-24">
        <div className="max-w-4xl mx-auto text-center">
          <p className="eyebrow text-gold mb-6">Dossiê N°14</p>
          <p className="font-display text-3xl md:text-5xl leading-[1.15] text-balance">"{dest.description}"</p>
          <p className="mt-8 text-xs text-slate font-light">Curadoria · {dest.signature}</p>
        </div>
      </section>

      {/* SECTION NAV + CONTENT */}
      <section className="px-6 md:px-12 pb-32 max-w-[1600px] mx-auto">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12">
          <aside className="lg:col-span-3">
            <div className="lg:sticky lg:top-28">
              <p className="eyebrow text-gold mb-6">Navegar Dossiê</p>
              <ul className="space-y-3 border-l border-navy/10">
                {SECTIONS.map((s) => (
                  <li key={s.id}>
                    <a href={`#${s.id}`} onClick={() => setActive(s.id)} className={`block pl-4 -ml-px border-l text-sm font-light transition-all duration-300 ${active === s.id ? "border-gold text-navy font-medium" : "border-transparent text-slate hover:text-navy hover:border-navy/30"}`}>
                      {s.label}
                    </a>
                  </li>
                ))}
              </ul>
            </div>
          </aside>

          <div className="lg:col-span-9 space-y-24">
            {/* Roteiro */}
            <DossierSection id="roteiro" eyebrow="01" title="Roteiro dia a dia">
              <div className="space-y-12">
                {ROTEIRO_PLACEHOLDER.map((day) => (
                  <div key={day.day}>
                    <div className="flex items-baseline gap-6 mb-6">
                      <p className="eyebrow text-gold">{day.day}</p>
                      <span className="luvoy-hairline flex-1" />
                      <p className="font-display-italic text-2xl">{day.title}</p>
                    </div>
                    <ul className="space-y-4">
                      {day.items.map((item, i) => (
                        <li key={i} className={`flex gap-8 py-4 border-b border-navy/5 ${item.highlight ? "text-gold" : ""}`}>
                          <span className="font-mono text-sm opacity-60 w-16">{item.time}</span>
                          <span className="font-light">{item.text}</span>
                        </li>
                      ))}
                    </ul>
                  </div>
                ))}
              </div>
              <Link to="/roteiro/$slug" params={{ slug: dest.slug }} className="inline-block mt-10 px-8 py-4 border border-navy/30 hover:border-gold hover:text-gold rounded-full eyebrow transition-all duration-500">
                Ver roteiro hora a hora · manhã · tarde · noite →
              </Link>
            </DossierSection>

            {/* Seções dinâmicas do banco */}
            {(["restaurantes","cafes","atracoes","experiencias","ruas","instagram","lojas","escondidos","rooftops","skincare","compras","pratos"] as const).map((id, idx) => {
              const label = SECTIONS.find((s) => s.id === id)?.label ?? id;
              const items = getSectionContent(id);
              return (
                <CardSection
                  key={id}
                  id={id}
                  eyebrow={String(idx + 2).padStart(2, "0")}
                  title={label}
                  items={items.length > 0 ? items : [
                    { name: "Em breve", desc: "Conteúdo editorial sendo preparado pela equipe LUVOY." },
                  ]}
                />
              );
            })}

            {/* Mapa */}
            <DossierSection id="mapa" eyebrow="14" title="Mapa do roteiro">
              <div className="aspect-[16/10] bg-beige border border-navy/10 flex flex-col items-center justify-center text-center p-12">
                <p className="eyebrow text-gold mb-4">Mapa Interativo</p>
                <p className="font-display-italic text-3xl mb-2">Em breve</p>
                <p className="text-sm text-slate font-light max-w-md">Mapa Premium com todos os pontos do dossiê, rotas otimizadas a pé e modo offline para assinantes LUVOY Noir.</p>
              </div>
            </DossierSection>
          </div>
        </div>
      </section>

      {/* CHECKLIST CTA */}
      <section className="bg-cream text-navy px-6 md:px-12 py-24">
        <div className="max-w-5xl mx-auto grid grid-cols-1 md:grid-cols-2 gap-12">
          <div>
            <p className="eyebrow text-gold mb-4">Antes da partida</p>
            <h3 className="font-display text-4xl md:text-5xl leading-tight">Checklist de viagem & mala</h3>
            <p className="mt-6 text-slate font-light leading-relaxed">Listas personalizadas para {dest.name} · clima, dress code, documentos, voltagem e essenciais que só quem já foi conhece.</p>
          </div>
          <div className="space-y-3">
            {["Passaporte e visto verificados", "Adaptador europeu Type C/E", "Trench coat ou linho leve", "Sapatos de caminhada elegantes", "Apps offline baixados", "Reservas confirmadas por email"].map((item) => (
              <label key={item} className="flex items-center gap-4 py-3 border-b border-navy/15 cursor-pointer group">
                <span className="size-4 border border-navy/30 group-hover:border-gold group-hover:bg-gold/20 transition-all" />
                <span className="text-sm font-light">{item}</span>
              </label>
            ))}
          </div>
        </div>
      </section>

      <BottomBar />
    </div>
  );
}

function DossierSection({ id, eyebrow, title, children }: { id: string; eyebrow: string; title: string; children: React.ReactNode }) {
  return (
    <section id={id} className="scroll-mt-28">
      <div className="flex items-baseline gap-6 mb-10">
        <span className="font-mono text-xs text-gold">{eyebrow}</span>
        <h2 className="font-display text-4xl md:text-5xl">{title}</h2>
      </div>
      {children}
    </section>
  );
}

function CardSection({ id, eyebrow, title, items }: { id: string; eyebrow: string; title: string; items: { name: string; desc: string }[] }) {
  return (
    <DossierSection id={id} eyebrow={eyebrow} title={title}>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {items.map((item) => (
          <article key={item.name} className="group p-8 bg-cream hover:bg-sand transition-all duration-700 cursor-pointer">
            <h4 className="font-display text-2xl mb-3 text-navy group-hover:italic transition-all">{item.name}</h4>
            <p className="text-sm font-light leading-relaxed text-slate">{item.desc}</p>
            <span className="eyebrow text-gold mt-6 inline-block opacity-0 group-hover:opacity-100 transition-opacity duration-500">Ver detalhes →</span>
          </article>
        ))}
      </div>
    </DossierSection>
  );
}
