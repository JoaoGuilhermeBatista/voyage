import { Link } from "@tanstack/react-router";
import { useRef } from "react";
import type { Collection } from "@/data/collections";
import type { Destination } from "@/data/destinations";

interface Props {
  collection: Collection;
  allDestinations: Destination[];
  index: number;
}

export function CollectionRow({ collection, allDestinations, index }: Props) {
  const ref = useRef<HTMLDivElement>(null);
  const items = collection.destinations
    .map((slug) => allDestinations.find((d) => d.slug === slug))
    .filter((d): d is Destination => Boolean(d));

  const scroll = (dir: 1 | -1) => {
    const el = ref.current;
    if (!el) return;
    el.scrollBy({ left: dir * (el.clientWidth * 0.8), behavior: "smooth" });
  };

  return (
    <section className="border-t border-navy/10 py-24 md:py-32">
      <div className="px-8 md:px-16 lg:px-24 max-w-[1600px] mx-auto mb-14">
        <div className="flex items-end justify-between gap-8">
          <div>
            <p className="eyebrow text-gold mb-4">N°{String(index + 1).padStart(2, "0")} · {collection.eyebrow}</p>
            <h2 className="font-display text-4xl md:text-6xl lg:text-7xl leading-[1]">
              {collection.title} <span className="font-display-italic text-slate">{collection.italic}</span>
            </h2>
            <p className="mt-7 max-w-xl text-base md:text-lg text-slate font-light leading-[1.7]">{collection.description}</p>
          </div>
          <div className="hidden md:flex gap-3">
            <button onClick={() => scroll(-1)} aria-label="Anterior" className="size-12 border border-navy/20 hover:border-gold hover:text-gold rounded-full transition-all duration-500 flex items-center justify-center">←</button>
            <button onClick={() => scroll(1)} aria-label="Próximo" className="size-12 border border-navy/20 hover:border-gold hover:text-gold rounded-full transition-all duration-500 flex items-center justify-center">→</button>
          </div>
        </div>
      </div>
      <div ref={ref} className="flex gap-8 overflow-x-auto snap-x snap-mandatory px-8 md:px-16 lg:px-24 pb-8 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
        {items.map((d, i) => (
          <Link key={d.slug} to="/destinos/$slug" params={{ slug: d.slug }} className="golden-hour group relative shrink-0 w-[82vw] md:w-[520px] lg:w-[560px] aspect-[3/4] overflow-hidden snap-start bg-beige">
            <img src={d.image} alt={`${d.name}, ${d.country}`} loading="lazy" width={900} height={1200} className="absolute inset-0 w-full h-full object-cover transition-transform duration-[1600ms] ease-[cubic-bezier(0.19,1,0.22,1)] group-hover:scale-[1.06]" />
            <div className="absolute inset-x-0 bottom-0 h-1/2 bg-gradient-to-t from-ink/55 via-ink/10 to-transparent" />
            <div className="absolute top-8 left-8 right-8 flex items-start justify-between">
              <span className="eyebrow text-ivory/80">{String(i + 1).padStart(2, "0")}</span>
              <span className="eyebrow text-ivory/80">{d.country}</span>
            </div>
            <div className="absolute bottom-9 left-9 right-9 text-ivory">
              <h3 className="font-display text-4xl md:text-5xl leading-[1] group-hover:italic transition-all duration-500">{d.name}</h3>
              <p className="font-display-italic text-base mt-4 opacity-85 max-w-[28ch]">{d.tagline}</p>
              <span className="eyebrow text-gold-soft mt-6 inline-block opacity-0 group-hover:opacity-100 translate-y-1 group-hover:translate-y-0 transition-all duration-500">Abrir dossiê →</span>
            </div>
          </Link>
        ))}
      </div>
    </section>
  );
}
