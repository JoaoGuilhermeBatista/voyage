import { createFileRoute, Link } from "@tanstack/react-router";
import { Nav } from "@/components/luvoy/Nav";
import { BottomBar } from "@/components/luvoy/BottomBar";

export const Route = createFileRoute("/favoritos")({
  head: () => ({ meta: [{ title: "Favoritos — LUVOY" }] }),
  component: () => <Stub eyebrow="Em Breve" title="Seus favoritos" subtitle="Salve destinos, dossiês e experiências para acessar offline." />,
});

export function Stub({ eyebrow, title, subtitle }: { eyebrow: string; title: string; subtitle: string }) {
  return (
    <div className="min-h-screen bg-ivory">
      <Nav variant="light" />
      <div className="min-h-screen flex flex-col items-center justify-center px-6 text-center">
        <p className="eyebrow text-gold mb-6">{eyebrow}</p>
        <h1 className="font-display text-6xl md:text-8xl text-navy leading-[0.95]">{title}</h1>
        <p className="mt-6 text-slate font-light max-w-md">{subtitle}</p>
        <Link to="/" className="mt-10 eyebrow text-navy border-b border-gold pb-1 hover:text-gold transition-colors">
          Voltar à coleção
        </Link>
      </div>
      <BottomBar />
    </div>
  );
}
