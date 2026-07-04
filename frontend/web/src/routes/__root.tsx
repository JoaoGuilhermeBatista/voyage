import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  Outlet,
  Link,
  createRootRouteWithContext,
  useRouter,
  HeadContent,
  Scripts,
} from "@tanstack/react-router";

import appCss from "../styles.css?url";

function NotFoundComponent() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-ivory px-4">
      <div className="max-w-md text-center">
        <p className="eyebrow text-gold mb-6">Erro 404</p>
        <h1 className="font-display text-6xl text-navy mb-4">Destino não encontrado</h1>
        <p className="mt-2 text-sm text-slate font-light">
          Esta página se perdeu entre as ruas. Vamos voltar ao início da jornada.
        </p>
        <Link to="/" className="inline-block mt-8 eyebrow text-navy border-b border-gold pb-1 hover:text-gold transition-colors">
          Retornar ao Dossiê
        </Link>
      </div>
    </div>
  );
}

function ErrorComponent({ error, reset }: { error: Error; reset: () => void }) {
  console.error(error);
  const router = useRouter();
  return (
    <div className="flex min-h-screen items-center justify-center bg-ivory px-4">
      <div className="max-w-md text-center">
        <p className="eyebrow text-gold mb-6">Interrupção</p>
        <h1 className="font-display text-4xl text-navy">Algo silenciou inesperadamente.</h1>
        <p className="mt-4 text-sm text-slate font-light">{error.message}</p>
        <div className="mt-8 flex justify-center gap-6">
          <button
            onClick={() => { router.invalidate(); reset(); }}
            className="eyebrow text-navy border-b border-gold pb-1 hover:text-gold transition-colors"
          >
            Tentar novamente
          </button>
          <a href="/" className="eyebrow text-slate border-b border-slate/30 pb-1 hover:text-navy transition-colors">
            Início
          </a>
        </div>
      </div>
    </div>
  );
}

export const Route = createRootRouteWithContext<{ queryClient: QueryClient }>()({
  head: () => ({
    meta: [
      { charSet: "utf-8" },
      { name: "viewport", content: "width=device-width, initial-scale=1" },
      { title: "LUVOY — Roteiros que transformam viagens em experiências" },
      { name: "description", content: "Curadoria europeia de luxo silencioso: roteiros, cafés escondidos, rooftops, farmácias icônicas e experiências cinematográficas." },
      { property: "og:title", content: "LUVOY — Roteiros de viagem premium" },
      { property: "og:description", content: "Concierge digital para viagens europeias inesquecíveis." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
    links: [
      { rel: "stylesheet", href: appCss },
      { rel: "preconnect", href: "https://fonts.googleapis.com" },
      { rel: "preconnect", href: "https://fonts.gstatic.com", crossOrigin: "anonymous" },
      { rel: "stylesheet", href: "https://fonts.googleapis.com/css2?family=Cormorant+Garamond:ital,wght@0,300;0,500;0,600;1,300;1,500&family=Inter:wght@300;400;500&display=swap" },
    ],
  }),
  shellComponent: RootShell,
  component: RootComponent,
  notFoundComponent: NotFoundComponent,
  errorComponent: ErrorComponent,
});

function RootShell({ children }: { children: React.ReactNode }) {
  return (
    <html lang="pt-BR">
      <head><HeadContent /></head>
      <body>
        {children}
        <Scripts />
      </body>
    </html>
  );
}

function RootComponent() {
  const { queryClient } = Route.useRouteContext();
  return (
    <QueryClientProvider client={queryClient}>
      <Outlet />
    </QueryClientProvider>
  );
}
