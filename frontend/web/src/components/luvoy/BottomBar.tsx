import { Link } from "@tanstack/react-router";

export function BottomBar() {
  return (
    <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-50 flex items-center gap-1 bg-navy/95 backdrop-blur-xl border border-ivory/10 px-3 py-3 rounded-full shadow-elegant">
      <Link to="/favoritos" className="px-4 py-1 eyebrow text-gold hover:text-ivory transition-colors">Favoritos</Link>
      <span className="w-px h-4 bg-ivory/20" />
      <Link to="/checklists" className="px-4 py-1 eyebrow text-ivory hover:text-gold transition-colors">Checklists</Link>
      <span className="w-px h-4 bg-ivory/20" />
      <Link to="/premium" className="px-4 py-1 eyebrow text-ivory hover:text-gold transition-colors">Premium</Link>
    </div>
  );
}
