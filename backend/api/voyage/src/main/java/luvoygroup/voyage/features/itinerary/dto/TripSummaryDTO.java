package luvoygroup.voyage.features.itinerary.dto;

import luvoygroup.voyage.domain.Itinerary;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * View agrupada de uma viagem pra tela "Roteiros" (spec: "Um card
 * agrupado"). {@code tripGroupId} é null quando o grupo representa um
 * roteiro avulso (não multi-destino) — nesse caso {@code legs} tem
 * exatamente 1 elemento e o front-end trata como hoje (sem UI de trechos).
 * {@code startDate}/{@code endDate} cobrem a viagem inteira (do início do
 * primeiro trecho ao fim do último).
 */
public record TripSummaryDTO(
    UUID tripGroupId,
    String title,
    LocalDate startDate,
    LocalDate endDate,
    List<ItinerarySummaryDTO> legs
) {
    /**
     * @param legsInOrder trechos da viagem, já ordenados cronologicamente
     *                    (mesma ordem de {@code tripGroupOrder}) — 1
     *                    elemento pra um roteiro avulso, 2+ pra multi-destino.
     */
    public static TripSummaryDTO from(List<Itinerary> legsInOrder) {
        List<ItinerarySummaryDTO> legSummaries = legsInOrder.stream()
            .map(ItinerarySummaryDTO::from)
            .toList();

        LocalDate startDate = legsInOrder.stream()
            .map(Itinerary::getStartDate)
            .min(Comparator.naturalOrder())
            .orElseThrow();
        LocalDate endDate = legsInOrder.stream()
            .map(Itinerary::getEndDate)
            .max(Comparator.naturalOrder())
            .orElseThrow();

        boolean isMultiDestino = legsInOrder.size() > 1;
        String title = isMultiDestino
            ? legsInOrder.stream().map(it -> it.getDestination().getName()).reduce((a, b) -> a + " + " + b).orElseThrow()
            : legsInOrder.get(0).getTitle();

        return new TripSummaryDTO(
            isMultiDestino ? legsInOrder.get(0).getTripGroupId() : null,
            title,
            startDate,
            endDate,
            legSummaries
        );
    }
}
