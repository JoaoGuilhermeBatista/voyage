package luvoygroup.voyage.features.itinerary;

import luvoygroup.voyage.domain.Poi;
import luvoygroup.voyage.domain.enums.PoiCategory;

import java.util.EnumMap;
import java.util.Map;

/**
 * Acompanha, ao longo de TODA a geração de uma viagem (um destino avulso ou
 * todos os trechos de uma viagem multi-destino — ver
 * {@link ItineraryService#generateTrip}), quantas atividades de cada
 * categoria e quantos POIs mustSee já foram escolhidos. Usado por
 * {@link ItineraryEngineService#scoreFor} para desencorajar repetição
 * excessiva de categoria e para favorecer POIs não-mustSee depois que a
 * viagem já ficou saturada de imperdíveis — mesmo que isso aconteça em dias
 * não-consecutivos. Vive só durante uma chamada de geração, nunca é
 * persistido.
 */
class TripSaturationLedger {

    private final Map<PoiCategory, Integer> categoryCounts = new EnumMap<>(PoiCategory.class);
    private int mustSeeCount = 0;
    private int totalCount = 0;

    void record(Poi poi) {
        categoryCounts.merge(poi.getCategory(), 1, Integer::sum);
        totalCount++;
        if (poi.isMustSee()) mustSeeCount++;
    }

    /**
     * Penalidade suave e crescente pra categoria já muito usada na viagem —
     * linear com teto, pra nunca anular sozinha o bônus de mustSee (+20).
     */
    int categoryPenalty(PoiCategory category) {
        return Math.min(categoryCounts.getOrDefault(category, 0) * 2, 10);
    }

    /**
     * Verdadeiro quando a viagem já tem uma amostra mínima de atividades e
     * mais da metade delas são mustSee — sinal de que vale a pena variar
     * pros POIs não-tão-óbvios do catálogo nos próximos slots (de qualquer
     * dia, não só o seguinte).
     */
    boolean isMustSeeSaturated() {
        return totalCount >= 4 && (double) mustSeeCount / totalCount > 0.5;
    }
}
