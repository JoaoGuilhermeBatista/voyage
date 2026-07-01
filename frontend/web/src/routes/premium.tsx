import { createFileRoute } from "@tanstack/react-router";
import { Stub } from "./favoritos";

export const Route = createFileRoute("/premium")({
  head: () => ({ meta: [{ title: "LUVOY Noir — Premium" }] }),
  component: () => <Stub eyebrow="LUVOY Noir" title="Área Premium" subtitle="Roteiros completos, modo offline, alertas inteligentes e concierge 24/7 para assinantes." />,
});
