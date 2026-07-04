import { createFileRoute } from "@tanstack/react-router";
import { Stub } from "./favoritos";

export const Route = createFileRoute("/concierge")({
  head: () => ({ meta: [{ title: "Concierge — LUVOY" }] }),
  component: () => <Stub eyebrow="LUVOY Noir" title="Concierge 24/7" subtitle="Suporte premium para reservas, mudanças de roteiro e recomendações em tempo real." />,
});
