import { Link } from "@tanstack/react-router";

type Tone = "light" | "dark" | "warm";
type Variant = "fullbleed" | "split-left" | "split-right";

interface EditorialStoryProps {
  eyebrow: string;
  title: string;
  italic?: string;
  body: string;
  quote?: string;
  image: string;
  imageAlt: string;
  meta?: string;
  cta?: { label: string; to: string };
  tone?: Tone;
  variant?: Variant;
}

const toneClasses: Record<Tone, { bg: string; text: string; muted: string; eyebrow: string; rule: string }> = {
  light: { bg: "bg-ivory", text: "text-navy", muted: "text-slate", eyebrow: "text-gold", rule: "border-navy/15" },
  dark: { bg: "bg-ink", text: "text-ivory", muted: "text-ivory/65", eyebrow: "text-gold-soft", rule: "border-ivory/15" },
  warm: { bg: "bg-beige", text: "text-navy", muted: "text-slate", eyebrow: "text-gold", rule: "border-navy/15" },
};

export function EditorialStory({ eyebrow, title, italic, body, quote, image, imageAlt, meta, cta, tone = "light", variant = "split-left" }: EditorialStoryProps) {
  const t = toneClasses[tone];

  if (variant === "fullbleed") {
    return (
      <section className={`relative ${t.bg} ${t.text} overflow-hidden`}>
        <div className="relative h-[88vh] min-h-[640px] w-full overflow-hidden">
          <img src={image} alt={imageAlt} loading="lazy" className="absolute inset-0 w-full h-full object-cover scale-105 transition-transform duration-[2400ms] ease-out hover:scale-110" />
          <div className={`absolute inset-0 ${tone === "dark" ? "bg-gradient-to-t from-ink via-ink/40 to-ink/10" : "bg-gradient-to-t from-ivory/85 via-ivory/20 to-transparent"}`} />
          <div className="relative z-10 h-full flex items-end">
            <div className="w-full px-8 md:px-16 lg:px-24 pb-24 md:pb-32 max-w-[1600px] mx-auto">
              <div className="max-w-3xl">
                <p className={`eyebrow ${t.eyebrow} mb-8`}>{eyebrow}</p>
                <h2 className={`font-display text-5xl md:text-7xl lg:text-8xl leading-[0.95] ${t.text}`}>
                  {title}
                  {italic && <><br /><span className="font-display-italic text-gold-soft">{italic}</span></>}
                </h2>
                {quote && <p className={`mt-10 font-display-italic text-2xl md:text-3xl leading-[1.4] ${t.muted} max-w-2xl`}>"{quote}"</p>}
                <div className={`mt-12 flex items-end justify-between gap-10 border-t ${t.rule} pt-6`}>
                  <p className={`text-base md:text-lg font-light leading-[1.7] ${t.muted} max-w-xl`}>{body}</p>
                  {meta && <p className={`eyebrow ${t.muted} hidden md:block whitespace-nowrap`}>{meta}</p>}
                </div>
                {cta && (
                  <Link to={cta.to} className={`inline-block mt-10 eyebrow ${t.eyebrow} border-b border-gold pb-1 hover:opacity-80 transition-opacity`}>
                    {cta.label} →
                  </Link>
                )}
              </div>
            </div>
          </div>
        </div>
      </section>
    );
  }

  const reverse = variant === "split-right";
  return (
    <section className={`${t.bg} ${t.text} px-8 md:px-16 lg:px-24 py-40`}>
      <div className="max-w-[1600px] mx-auto">
        <div className={`grid grid-cols-1 lg:grid-cols-12 gap-16 lg:gap-24 items-center ${reverse ? "lg:[&>*:first-child]:order-2" : ""}`}>
          <div className="lg:col-span-7 relative">
            <div className="aspect-[4/5] lg:aspect-[5/6] overflow-hidden">
              <img src={image} alt={imageAlt} loading="lazy" className="w-full h-full object-cover transition-transform duration-[1800ms] ease-out hover:scale-[1.04]" />
            </div>
            {meta && <p className={`absolute -bottom-3 ${reverse ? "right-0" : "left-0"} translate-y-full eyebrow ${t.muted} mt-4`}>{meta}</p>}
          </div>
          <div className="lg:col-span-5 space-y-10">
            <p className={`eyebrow ${t.eyebrow}`}>{eyebrow}</p>
            <h2 className={`font-display text-4xl md:text-6xl leading-[1] ${t.text}`}>
              {title}
              {italic && <> <span className="font-display-italic text-gold-soft">{italic}</span></>}
            </h2>
            {quote && <p className={`font-display-italic text-2xl md:text-[1.75rem] leading-[1.4] ${t.muted} border-l-2 border-gold pl-6`}>"{quote}"</p>}
            <p className={`text-base md:text-lg font-light leading-[1.8] ${t.muted}`}>{body}</p>
            {cta && (
              <Link to={cta.to} className={`inline-block eyebrow ${t.eyebrow} border-b border-gold pb-1 hover:opacity-80 transition-opacity`}>
                {cta.label} →
              </Link>
            )}
          </div>
        </div>
      </div>
    </section>
  );
}
