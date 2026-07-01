import { api } from "@/lib/api";

export interface FavoriteItem {
  id: string;
  type: "DESTINATION" | "POI" | "ITINERARY";
  destinationId?: string;
  poiId?: string;
  itineraryId?: string;
  createdAt: string;
}

export const favoriteService = {
  async getAll(): Promise<FavoriteItem[]> {
    const { data } = await api.get<FavoriteItem[]>("/favorites");
    return data;
  },

  async addDestination(destinationId: string): Promise<FavoriteItem> {
    const { data } = await api.post<FavoriteItem>("/favorites", { destinationId });
    return data;
  },

  async remove(favoriteId: string): Promise<void> {
    await api.delete(`/favorites/${favoriteId}`);
  },
};
