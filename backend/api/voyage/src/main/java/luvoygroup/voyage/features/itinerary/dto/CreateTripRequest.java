package luvoygroup.voyage.features.itinerary.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Requisição de criação de uma viagem multi-destino: uma sequência de
 * trechos, cada um com sua própria data/destino/preferências — mesmo shape
 * de {@link ItineraryRequest}, reaproveitado 1:1 (spec: "N cidades + datas
 * por trecho", decisão de arquitetura "Agrupar Itineraries existentes").
 * A ordem da lista É a ordem cronológica pretendida dos trechos — ver
 * validação de sobreposição de datas em ItineraryService#generateTrip.
 */
public record CreateTripRequest(
    @NotNull @Valid @Size(min = 2, message = "Uma viagem multi-destino precisa de pelo menos 2 trechos")
    List<ItineraryRequest> legs
) {}
