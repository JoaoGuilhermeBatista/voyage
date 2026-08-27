package luvoygroup.voyage.features.admin.dto;

import java.util.List;

/**
 * Resposta do LLM para LlmService#classifyMustSeeAndLandmarks: quais dos
 * títulos de POI FORNECIDOS (verbatim, subconjuntos da lista de entrada)
 * são, respectivamente, os pontos mais icônicos/imperdíveis do destino
 * (mustSeeTitles) e quais deveriam ser recategorizados como LANDMARK
 * (landmarkTitles) por estarem catalogados em outra categoria por engano
 * (ex: um monumento hoje como MUSEUM, uma praça/fonte hoje como PARK). O
 * LLM não deve inventar títulos que não estejam na lista de entrada — a
 * responsabilidade de ignorar qualquer título alucinado que não bata
 * exatamente com um POI real é de quem consome esta resposta (ver contrato
 * documentado em AdminService#enrichExistingPoisWithMustSeeAndLandmark).
 */
public record PoiEnrichmentClassification(
    List<String> mustSeeTitles,
    List<String> landmarkTitles
) {}
