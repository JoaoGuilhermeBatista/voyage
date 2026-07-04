import { api } from "@/lib/api";
import type { Collection } from "@/data/collections";

interface CollectionDTO {
  id: string;
  slug: string;
  title: string;
  italic: string;
  eyebrow: string;
  description: string;
  destinations: { slug: string }[];
}

function toCollection(dto: CollectionDTO): Collection {
  return {
    slug: dto.slug,
    title: dto.title,
    italic: dto.italic,
    eyebrow: dto.eyebrow,
    description: dto.description,
    destinations: dto.destinations.map((d) => d.slug),
  };
}

export const collectionService = {
  async getAll(): Promise<Collection[]> {
    const { data } = await api.get<CollectionDTO[]>("/collections");
    return data.map(toCollection);
  },
};
