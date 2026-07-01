import { Link } from "@tanstack/react-router";
import type { CountryGroup } from "@/data/collections";
import type { Destination } from "@/data/destinations";

interface Props {
  group: CountryGroup;
  allDestinations: Destination[];
  index: number;
}

export function CountryBlock({ group, allDestinations, index }: Props) {
  const items = group.destinations
    .map((slug) => allDestinations.find((d) => d.slug === slug))
    .filter((d): d is Destination => Boolean(d));

  return (
    <article className="grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-20 py-24 border-t border-navy/10">
      <header className="lg:col-span-3">
        <div className="lg:sticky lg:top-28">
          <p className="eyebrow text-gold mb-5">País N°{String(index + 1).padStart(2, "0")} · {group.flag}</p>
          <h3 className="font-display text-5xl md:text-6xl leading-[1]">{group.name}</h3>
          <p className="font-display-italic text-2xl text-slate mt-4">{group.italic}</p>
          <p className="mt-8 text-base text-slate font-light">{items.length} cidades curadas</p>
        </div>
      </header>
      <ul className="lg:col-span-9 divide-y divide-navy/10">
        {items.map((d, i) => (
          <li key={d.slug}>
            <Link to="/destinos/$slug" params={{ slug: d.slug }} className="group grid grid-cols-12 items-center gap-8 py-8 transition-colors duration-500">
              <span className="col-span-1 font-mono text-xs text-slate group-hover:text-gold transition-colors">{String(i + 1).padStart(2, "0")}</span>
              <div className="golden-hour col-span-3 md:col-span-2 aspect-[4/5] overflow-hidden bg-beige">
                <img src={d.image} alt={d.name} loading="lazy" width={400} height={500} className="w-full h-full object-cover transition-transform duration-[1400ms] ease-[cubic-bezier(0.19,1,0.22,1)] group-hover:scale-110" />
              </div>
              <div className="col-span-6 md:col-span-7">
                <h4 className="font-display text-3xl md:text-4xl leading-[1] group-hover:italic transition-all duration-500">{d.name}</h4>
                <p className="text-base font-light text-slate mt-3 max-w-[42ch]">{d.tagline}</p>
              </div>
              <span className="col-span-2 text-right eyebrow text-slate group-hover:text-gold transition-colors duration-500 hidden md:block">{d.weather.temp} · {d.daylight.sunset}</span>
            </Link>
          </li>
        ))}
      </ul>
    </article>
  );
}
