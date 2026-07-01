import { createFileRoute, Link, notFound } from "@tanstack/react-router";
import { destinationService } from "@/services/destinationService";
import { getDestination } from "@/data/destinations";
import { Nav } from "@/components/luvoy/Nav";
import { BottomBar } from "@/components/luvoy/BottomBar";

type Block = {
  label: string;
  hours: string;
  title: string;
  mood: string;
  items: { time: string; text: string; tag?: string }[];
};

const DAY_PLAN: { day: string; subtitle: string; blocks: Block[] }[] = [
  {
    day: "Dia 01", subtitle: "O Despertar",
    blocks: [
      { label: "Manhã", hours: "07:00 — 12:00", title: "Luz dourada", mood: "Silêncio das ruas antes que a cidade desperte.", items: [
        { time: "07:30", text: "Café da manhã no hotel · varanda com vista", tag: "Privado" },
        { time: "09:00", text: "Caminhada pelo centro histórico antes da multidão", tag: "Sem fila" },
        { time: "11:00", text: "Visita íntima a uma galeria escondida" },
      ]},
      { label: "Tarde", hours: "12:00 — 18:00", title: "O ritual lento", mood: "Almoço demorado, descobertas sem pressa.", items: [
        { time: "12:30", text: "Almoço em bistrot local recomendado", tag: "Reserva" },
        { time: "15:00", text: "Tarde livre · sugestões de bairro Luvoy" },
        { time: "17:30", text: "Aperitivo em rooftop selecionado" },
      ]},
      { label: "Noite", hours: "18:00 — 23:00", title: "Cinematográfica", mood: "Quando a cidade se veste de luzes baixas.", items: [
        { time: "19:30", text: "Caminhada ao pôr do sol · mirador secreto" },
        { time: "20:30", text: "Jantar com vista · reserva confirmada", tag: "Michelin" },
        { time: "22:30", text: "Drink autoral em bar subterrâneo" },
      ]},
    ],
  },
  {
    day: "Dia 02", subtitle: "A Imersão",
    blocks: [
      { label: "Manhã", hours: "08:00 — 12:00", title: "Mercados & aromas", mood: "O ritmo dos locais, sem filtro.", items: [
        { time: "08:00", text: "Mercado central · degustação artesanal" },
        { time: "10:30", text: "Atelier de cerâmica · peças únicas", tag: "Curadoria" },
      ]},
      { label: "Tarde", hours: "12:00 — 18:00", title: "Cultura íntima", mood: "Acessos privados, descobertas guardadas.", items: [
        { time: "13:00", text: "Almoço em trattoria familiar de 1923" },
        { time: "15:30", text: "Visita privada a coleção particular", tag: "Exclusivo" },
      ]},
      { label: "Noite", hours: "18:00 — 24:00", title: "Estrelas baixas", mood: "Música, vinho, conversas que se alongam.", items: [
        { time: "19:00", text: "Degustação de vinhos com sommelier" },
        { time: "21:30", text: "Concerto íntimo · curadoria Luvoy", tag: "Reservado" },
      ]},
    ],
  },
];

const ICONS: Record<string, string> = { Manhã: "☀", Tarde: "❍", Noite: "☾" };

export const Route = createFileRoute("/roteiro/$slug")({
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
          { title: `Roteiro ${loaderData.dest.name} — LUVOY` },
          { name: "description", content: `Roteiro em ${loaderData.dest.name}: manhã, tarde e noite curados pela LUVOY.` },
          { property: "og:image", content: loaderData.dest.image },
        ]
      : [],
  }),
  component: ItineraryPage,
});

function ItineraryPage() {
  const { dest } = Route.useLoaderData();

  return (
    <div className="min-h-screen bg-ivory text-navy">
      <Nav variant="dark" />

      <section className="relative h-[80vh] overflow-hidden">
        <img src={dest.image} alt={dest.name} className="absolute inset-0 w-full h-full object-cover anim-zoom" />
        <div className="absolute inset-0 bg-gradient-to-b from-ink/20 via-transparent to-ivory" />
        <div className="absolute inset-x-0 bottom-0 h-2/3 bg-gradient-to-t from-ink/35 via-ink/5 to-transparent" />
        <div className="relative z-10 h-full flex flex-col justify-end px-6 md:px-12 pb-16 max-w-[1600px] mx-auto">
          <Link to="/destinos/$slug" params={{ slug: dest.slug }} className="eyebrow text-ivory/80 hover:text-gold transition-colors mb-6 anim-fade">← Dossiê {dest.name}</Link>
          <p className="eyebrow text-gold-soft anim-fade">Roteiro Luvoy · Editorial</p>
          <h1 className="font-display text-6xl md:text-8xl lg:text-[9rem] text-ivory leading-[0.9] mt-4 anim-rise">
            {dest.name} <span className="font-display-italic">hora a hora</span>
          </h1>
        </div>
      </section>

      <section className="px-6 md:px-12 py-20 max-w-3xl mx-auto text-center">
        <p className="eyebrow text-gold mb-6">A coreografia</p>
        <p className="font-display text-2xl md:text-4xl leading-[1.2] text-balance">
          Cada momento do dia foi escolhido pela sua luz, ritmo e emoção —
          <span className="font-display-italic"> da manhã serena à noite cinematográfica.</span>
        </p>
      </section>

      <section className="px-6 md:px-12 pb-32 max-w-[1400px] mx-auto space-y-32">
        {DAY_PLAN.map((d) => (
          <div key={d.day}>
            <div className="flex items-baseline gap-6 mb-16">
              <p className="eyebrow text-gold">{d.day}</p>
              <span className="luvoy-hairline flex-1" />
              <p className="font-display-italic text-3xl md:text-4xl">{d.subtitle}</p>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6 md:gap-8">
              {d.blocks.map((b) => (
                <article key={b.label} className="group relative p-8 md:p-10 bg-cream hover:bg-sand transition-colors duration-700 flex flex-col min-h-[460px]">
                  <div className="flex items-start justify-between mb-10">
                    <div>
                      <p className="eyebrow text-gold mb-2">{b.label}</p>
                      <p className="font-mono text-xs opacity-60">{b.hours}</p>
                    </div>
                    <span className="font-display-italic text-3xl text-gold opacity-80">{ICONS[b.label]}</span>
                  </div>
                  <h3 className="font-display text-3xl md:text-4xl mb-3 group-hover:italic transition-all duration-500">{b.title}</h3>
                  <p className="text-sm font-light opacity-70 mb-8 leading-relaxed">{b.mood}</p>
                  <span className="luvoy-hairline mb-6" />
                  <ul className="space-y-5 flex-1">
                    {b.items.map((item, i) => (
                      <li key={i} className="flex gap-4 text-sm font-light">
                        <span className="font-mono opacity-50 w-12 shrink-0">{item.time}</span>
                        <span className="flex-1 leading-relaxed">
                          {item.text}
                          {item.tag && <span className="ml-2 text-[10px] tracking-[0.2em] uppercase text-gold">· {item.tag}</span>}
                        </span>
                      </li>
                    ))}
                  </ul>
                </article>
              ))}
            </div>
          </div>
        ))}
      </section>

      <section className="bg-sand text-navy px-6 md:px-12 py-24 text-center">
        <p className="eyebrow text-gold mb-6">Continue a jornada</p>
        <h2 className="font-display text-4xl md:text-6xl leading-tight max-w-3xl mx-auto">
          O dossiê completo de <span className="font-display-italic text-slate">{dest.name}</span>
        </h2>
        <Link to="/destinos/$slug" params={{ slug: dest.slug }} className="inline-block mt-10 px-10 py-4 border border-navy/30 hover:border-gold hover:text-gold transition-all duration-500 rounded-full eyebrow">
          Abrir Dossiê →
        </Link>
      </section>

      <BottomBar />
    </div>
  );
}
