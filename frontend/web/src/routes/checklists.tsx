import { createFileRoute } from "@tanstack/react-router";
import { Stub } from "./favoritos";

export const Route = createFileRoute("/checklists")({
  head: () => ({ meta: [{ title: "Checklists — LUVOY" }] }),
  component: () => <Stub eyebrow="Antes da Partida" title="Checklists" subtitle="Listas personalizadas de viagem, mala e documentos para cada destino europeu." />,
});
