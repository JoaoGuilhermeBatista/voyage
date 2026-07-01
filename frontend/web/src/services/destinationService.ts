import { api } from "@/lib/api";
import type { Destination, DestinationDetail } from "@/data/destinations";

interface DestinationSummaryDTO {
  id: string;
  slug: string;
  name: string;
  country: string;
  city: string;
  tagline: string;
  heroImageUrl: string;
  signature: string;
  tags: string[];
}

interface DestinationDetailDTO extends DestinationSummaryDTO {
  description: string;
  sections: {
    sectionType: string;
    orderIndex: number;
    content: { name: string; desc: string }[];
  }[];
}

function toDestination(dto: DestinationSummaryDTO): Destination {
  return {
    slug: dto.slug,
    name: dto.name,
    country: dto.country,
    tagline: dto.tagline,
    description: "",
    image: dto.heroImageUrl,
    weather: { temp: "—", condition: "—" },
    daylight: { sunrise: "—", sunset: "—" },
    signature: dto.signature,
  };
}

export const destinationService = {
  async getAll(): Promise<Destination[]> {
    const { data } = await api.get<DestinationSummaryDTO[]>("/destinations");
    return data.map(toDestination);
  },

  async getBySlug(slug: string): Promise<DestinationDetail> {
    const { data } = await api.get<DestinationDetailDTO>(`/destinations/${slug}`);
    return {
      slug: data.slug,
      name: data.name,
      country: data.country,
      tagline: data.tagline,
      description: data.description,
      image: data.heroImageUrl,
      weather: { temp: "—", condition: "—" },
      daylight: { sunrise: "—", sunset: "—" },
      signature: data.signature,
      sections: data.sections,
    };
  },
};
