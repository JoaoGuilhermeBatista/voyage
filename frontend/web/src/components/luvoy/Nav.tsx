import { Link } from "@tanstack/react-router";

export function Nav({ variant = "light" }: { variant?: "light" | "dark" }) {
  const text = variant === "dark" ? "text-ivory" : "text-navy";
  return (
    <nav className={`fixed top-0 left-0 right-0 z-50 px-6 md:px-12 py-6 flex justify-between items-center ${text}`}>
      <Link to="/" className="font-display-italic text-2xl md:text-3xl tracking-[0.3em] uppercase">
        Luvoy
      </Link>
      <div className="hidden md:flex items-center gap-10 eyebrow">
        <Link to="/" className="hover:text-gold transition-colors duration-500" activeOptions={{ exact: true }} activeProps={{ className: "text-gold" }}>Explorar</Link>
        <Link to="/favoritos" className="hover:text-gold transition-colors duration-500" activeProps={{ className: "text-gold" }}>Meus Roteiros</Link>
        <Link to="/concierge" className="hover:text-gold transition-colors duration-500" activeProps={{ className: "text-gold" }}>Concierge</Link>
        <div className="size-1.5 rounded-full bg-gold animate-pulse" aria-hidden />
      </div>
    </nav>
  );
}
