import { Link } from "@tanstack/react-router";
import type { Destination } from "@/data/destinations";

export function DestinationCard({ dest, index }: { dest: Destination; index: number }) {
  const offset = index % 3 === 1 ? "md:mt-24" : index % 3 === 2 ? "md:mt-12" : "";
  return (
    <Link to="/destinos/$slug" params={{ slug: dest.slug }} className={`group block ${offset}`}>
      <div className="golden-hour relative overflow-hidden aspect-[4/5] bg-beige mb-8">
        <img src={dest.image} alt={`${dest.name}, ${dest.country}`} loading="lazy" width={900} height={1200} className="w-full h-full object-cover transition-transform duration-[1400ms] ease-[cubic-bezier(0.19,1,0.22,1)] group-hover:scale-[1.06]" />
        <div className="absolute inset-0 bg-gradient-to-t from-ink/35 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-700" />
        <div className="absolute top-5 right-5 size-9 rounded-full bg-ivory/10 backdrop-blur-md border border-ivory/30 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-all duration-500 translate-y-2 group-hover:translate-y-1">
          <span className="text-ivory text-xs">↗</span>
        </div>
        <div className="absolute bottom-5 left-5 right-5 flex items-end justify-between text-ivory opacity-0 group-hover:opacity-100 transition-opacity duration-700 delay-100">
          <span className="eyebrow">Ver Dossiê</span>
          <span className="font-display-italic text-sm">{dest.signature.split("·")[0]}</span>
        </div>
      </div>
      <p className="eyebrow text-gold mb-2">{String(index + 1).padStart(2, "0")} · {dest.country}</p>
      <h3 className="font-display text-3xl text-navy leading-[1] mb-3 group-hover:italic transition-all duration-500">{dest.name}</h3>
      <p className="text-base text-slate font-light max-w-[36ch]">{dest.tagline}</p>
    </Link>
  );
}
