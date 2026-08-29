package luvoygroup.voyage.features.admin;

import luvoygroup.voyage.domain.Poi;
import luvoygroup.voyage.domain.enums.BudgetTier;
import luvoygroup.voyage.features.admin.dto.CatholicChurchClassification;
import luvoygroup.voyage.features.admin.dto.GeneratedContent;
import luvoygroup.voyage.features.admin.dto.PoiEnrichmentClassification;
import luvoygroup.voyage.features.admin.dto.PoisWrapper;
import luvoygroup.voyage.features.maps.dto.NeighborhoodRecommendation;
import luvoygroup.voyage.features.maps.dto.NeighborhoodRecommendationList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class LlmService {

    private final ChatClient chatClient;

    public GeneratedContent generateDestinationContent(String city, String country) {
        log.info("Gerando conteúdo via LLM para {}, {}", city, country);

        String prompt = """
            Você é um redator especialista em turismo. Gere conteúdo de marketing para o destino abaixo.
            Responda SOMENTE com JSON válido, sem markdown, sem explicações.

            Destino: %s, %s

            Formato exato:
            {
              "name": "Nome do destino (cidade, país)",
              "tagline": "Frase curta e impactante de até 8 palavras",
              "description": "Descrição envolvente de 2 a 3 parágrafos sobre o destino",
              "signature": "Experiência única ou característica marcante do destino em 1 frase",
              "tags": ["tag1", "tag2", "tag3", "tag4", "tag5"]
            }

            As tags devem refletir o estilo de viagem (ex: "praia", "cultura", "gastronomia", "aventura",
            "historia", "natureza", "compras", "vida-noturna", "arte", "família").
            """.formatted(city, country);

        return chatClient.prompt()
            .user(prompt)
            .call()
            .entity(GeneratedContent.class);
    }

    public PoisWrapper generatePois(String city, String country) {
        log.info("Gerando POIs via LLM para {}, {}", city, country);

        String prompt = """
            Você é um especialista em turismo europeu. Gere 20 pontos de interesse reais e populares para %s, %s.
            Responda SOMENTE com JSON válido, sem markdown, sem explicações.

            Categorias disponíveis (use exatamente): MUSEUM, RESTAURANT, PARK, SHOPPING, NIGHTLIFE, BEACH, LANDMARK

            Orçamentos disponíveis (use exatamente): ECONOMY, MODERATE, LUXURY

            Formato exato:
            {
              "pois": [
                {
                  "title": "Nome do local",
                  "category": "MUSEUM",
                  "description": "Descrição curta de 1-2 frases",
                  "address": "Endereço completo",
                  "latitude": 38.7194,
                  "longitude": -9.1153,
                  "budgetTier": "MODERATE",
                  "averageDurationMinutes": 90,
                  "openingTime": "09:00",
                  "closingTime": "18:00",
                  "tags": ["cultura", "historia"],
                  "mustSee": false,
                  "catholicChurch": false
                }
              ]
            }

            Regras:
            - Inclua pontos variados: museus, restaurantes, parques, bares, lojas icônicas, marcos históricos
            - Distribua os orçamentos: pelo menos 5 ECONOMY, 8 MODERATE e 5 LUXURY
            - As coordenadas devem ser as mais precisas possíveis para cada local
            - openingTime e closingTime no formato HH:mm (null se funcionar 24h ou sem horário fixo)
            - Tags devem refletir o perfil do POI (ex: "cultura", "gastronomia", "arte", "natureza", "compras")
            - mustSee: marque true para os 3 a 5 pontos mais icônicos/imperdíveis da cidade (aqueles que
              qualquer roteiro turístico de verdade incluiria, ex: monumentos símbolo, marcos históricos
              mundialmente conhecidos), false para os demais
            - catholicChurch: marque true SOMENTE para igrejas, basílicas, catedrais ou santuários da
              Igreja Católica (cristianismo católico romano) — ex: uma catedral católica, uma basílica
              católica, um santuário de peregrinação católico. Marque false para TODOS os demais locais,
              incluindo templos, igrejas, sinagogas, mesquitas ou locais de culto de outras religiões ou
              denominações cristãs não-católicas (ex: protestante, evangélica, ortodoxa), e também para
              marcos históricos/culturais em geral que não sejam especificamente um templo católico
            """.formatted(city, country);

        return chatClient.prompt()
            .user(prompt)
            .call()
            .entity(PoisWrapper.class);
    }

    /**
     * Contrato: dado um destino (city, country) e a lista de títulos de POI
     * JÁ CADASTRADOS e ativos desse destino, pergunta ao LLM quais desses
     * títulos — e SOMENTE esses, verbatim, sem inventar — são igrejas,
     * basílicas, catedrais ou santuários da Igreja Católica (não templos de
     * outras religiões/denominações, nem marcos históricos em geral). Usada
     * por AdminService#enrichExistingPoisWithCatholicChurchFlag para
     * classificar retroativamente POIs que já existem no banco (criados
     * antes do campo Poi#catholicChurch existir), sem recriá-los.
     */
    public CatholicChurchClassification classifyCatholicChurches(String city, String country, List<String> poiTitles) {
        log.info("Classificando igrejas católicas via LLM para {}, {}", city, country);

        String titlesList = poiTitles.stream()
            .map(title -> "- " + title)
            .collect(java.util.stream.Collectors.joining("\n"));

        String prompt = """
            Você é um especialista em turismo europeu e em arquitetura religiosa. Abaixo está a lista
            EXATA de pontos de interesse já cadastrados para %s, %s. Sua tarefa é identificar quais
            desses títulos são igrejas, basílicas, catedrais ou santuários da Igreja Católica.

            Lista de títulos (um por linha):
            %s

            Regras OBRIGATÓRIAS:
            - Responda SOMENTE com um subconjunto EXATO (verbatim) dos títulos fornecidos acima.
            - NUNCA invente títulos novos, NUNCA altere a grafia, capitalização ou acentuação dos
              títulos fornecidos — copie-os exatamente como aparecem na lista.
            - Se nenhum título da lista for uma igreja católica, retorne uma lista vazia.
            - Critério de classificação: marque um título como igreja católica SOMENTE se for
              igreja, basílica, catedral ou santuário da Igreja Católica (cristianismo católico
              romano) — ex: uma catedral católica, uma basílica católica, um santuário de
              peregrinação católico. NÃO marque templos, igrejas, sinagogas, mesquitas ou locais de
              culto de outras religiões ou denominações cristãs não-católicas (ex: protestante,
              evangélica, ortodoxa), e também NÃO marque marcos históricos/culturais em geral que
              não sejam especificamente um templo católico.

            Responda SOMENTE com JSON válido, sem markdown, sem explicações, no formato exato:
            {
              "catholicChurchTitles": ["Título Exato 1", "Título Exato 2"]
            }
            """.formatted(city, country, titlesList);

        return chatClient.prompt()
            .user(prompt)
            .call()
            .entity(CatholicChurchClassification.class);
    }

    /**
     * Contrato: dado um destino (city, country) e a lista de POIs JÁ
     * CADASTRADOS e ativos desse destino (título + categoria atual),
     * pergunta ao LLM: (1) quais desses títulos — e SOMENTE esses, verbatim,
     * sem inventar — são os 3 a 5 pontos mais icônicos/imperdíveis do
     * destino (mesmo critério de mustSee usado em generatePois); e (2)
     * quais desses títulos deveriam ser recategorizados como LANDMARK por
     * terem sido catalogados incorretamente em outra categoria (ex: um
     * monumento hoje como MUSEUM, uma praça/fonte hoje como PARK) — sem
     * incluir museus ou parques de verdade, que devem permanecer como
     * estão. Usada por AdminService#enrichExistingPoisWithMustSeeAndLandmark
     * para classificar retroativamente POIs que já existem no banco (sem
     * recriá-los), corrigindo destinos "clássicos" que não têm nenhum POI
     * mustSee nem LANDMARK cadastrado.
     */
    public PoiEnrichmentClassification classifyMustSeeAndLandmarks(String city, String country, List<Poi> pois) {
        log.info("Classificando mustSee e LANDMARK via LLM para {}, {}", city, country);

        String poisList = pois.stream()
            .map(poi -> "- %s (categoria atual: %s)".formatted(poi.getTitle(), poi.getCategory()))
            .collect(java.util.stream.Collectors.joining("\n"));

        String prompt = """
            Você é um especialista em turismo europeu. Abaixo está a lista EXATA de pontos de
            interesse já cadastrados para %s, %s, com a categoria atual de cada um. Sua tarefa é
            identificar dois subconjuntos dessa lista.

            Lista de títulos com categoria atual (um por linha):
            %s

            1. mustSeeTitles: os 3 a 5 títulos mais icônicos/imperdíveis do destino — aqueles que
               qualquer roteiro turístico de verdade incluiria (ex: monumentos símbolo, marcos
               históricos mundialmente conhecidos).
            2. landmarkTitles: títulos que deveriam ser recategorizados como LANDMARK — monumentos,
               marcos históricos, praças/fontes/pontos emblemáticos que estão catalogados
               incorretamente em outra categoria (ex: um monumento catalogado como MUSEUM, uma
               praça ou fonte catalogada como PARK). NÃO inclua museus de verdade (que devem
               continuar MUSEUM) nem parques de verdade (que devem continuar PARK).

            Regras OBRIGATÓRIAS:
            - Responda SOMENTE com subconjuntos EXATOS (verbatim) dos títulos fornecidos acima.
            - NUNCA invente títulos novos, NUNCA altere a grafia, capitalização ou acentuação dos
              títulos fornecidos — copie-os exatamente como aparecem na lista.
            - Se nenhum título da lista se encaixar em um dos critérios, retorne uma lista vazia
              para esse critério.

            Responda SOMENTE com JSON válido, sem markdown, sem explicações, no formato exato:
            {
              "mustSeeTitles": ["Título Exato 1", "Título Exato 2"],
              "landmarkTitles": ["Título Exato 3", "Título Exato 4"]
            }
            """.formatted(city, country, poisList);

        return chatClient.prompt()
            .user(prompt)
            .call()
            .entity(PoiEnrichmentClassification.class);
    }

    /**
     * STUB — a implementar por um próximo agente após revisão humana (esta
     * é uma genuína decisão de prompt, mesmo tratamento dado a
     * {@link #classifyMustSeeAndLandmarks(String, String, List)} quando foi
     * criado).
     *
     * Contrato pretendido:
     * <ul>
     *   <li>Monta um prompt no MESMO padrão de
     *       {@link #classifyMustSeeAndLandmarks(String, String, List)}: lista
     *       EXATA de {@code candidateNeighborhoods} fornecida (um por
     *       linha), pede pro LLM escolher um subconjunto de 3 a 6 bairros
     *       dessa lista que melhor combinam com a faixa de budget informada.</li>
     *   <li>Traduz {@code budgetTier} para termos em português no prompt:
     *       {@code ECONOMY} → "econômico", {@code MODERATE} →
     *       "custo-benefício"/"intermediário", {@code LUXURY} → "alto
     *       padrão/luxo".</li>
     *   <li>Pede, para cada bairro escolhido, uma frase curta (1 linha) em
     *       português descrevendo a VANTAGEM de se hospedar ali para esse
     *       perfil de budget.</li>
     *   <li>Regra obrigatória no prompt: os nomes retornados devem ser
     *       EXATAMENTE (verbatim) um subconjunto de
     *       {@code candidateNeighborhoods} — nunca inventar nome de bairro
     *       que não esteja na lista fornecida (mesmo espírito de
     *       "guarda contra alucinação" de
     *       {@link #classifyMustSeeAndLandmarks(String, String, List)}).</li>
     *   <li>Chama
     *       {@code chatClient.prompt().user(prompt).call().entity(NeighborhoodRecommendationList.class)}
     *       e extrai {@code .recommendations()}.</li>
     *   <li>Depois de receber a resposta do LLM: filtra fora qualquer
     *       {@code NeighborhoodRecommendation} cujo {@code name()} não
     *       esteja EXATAMENTE (verbatim, mesma grafia/acentuação) em
     *       {@code candidateNeighborhoods} — guarda contra alucinação — e
     *       loga um warning para cada recomendação filtrada dessa forma.</li>
     * </ul>
     *
     * @param city                   cidade do destino
     * @param country                país do destino
     * @param candidateNeighborhoods lista EXATA de nomes de bairro reais
     *                               descobertos via
     *                               {@code PlacesService#discoverRealNeighborhoods}
     *                               — o LLM só pode escolher entre eles
     * @param budgetTier             faixa de budget para a qual as
     *                               recomendações devem ser adequadas
     */
    public List<NeighborhoodRecommendation> recommendNeighborhoods(
        String city, String country, List<String> candidateNeighborhoods, BudgetTier budgetTier
    ) {
        log.info("Recomendando bairros via LLM para {}, {} (budget: {})", city, country, budgetTier);

        String neighborhoodsList = candidateNeighborhoods.stream()
            .map(name -> "- " + name)
            .collect(java.util.stream.Collectors.joining("\n"));

        String budgetTermInPortuguese = switch (budgetTier) {
            case ECONOMY -> "econômico";
            case MODERATE -> "custo-benefício/intermediário";
            case LUXURY -> "alto padrão/luxo";
        };

        String prompt = """
            Você é um especialista em turismo e hospedagem. Abaixo está a lista EXATA de bairros
            reais candidatos para hospedagem em %s, %s. Sua tarefa é escolher um subconjunto de 3
            a 6 bairros dessa lista que melhor combinam com o perfil de budget "%s".

            Lista de bairros candidatos (um por linha):
            %s

            Para cada bairro escolhido, escreva uma frase curta (1 linha) em português descrevendo
            a vantagem de se hospedar ali para esse perfil de budget.

            Regras OBRIGATÓRIAS:
            - Responda SOMENTE com um subconjunto EXATO (verbatim) dos bairros fornecidos acima.
            - NUNCA invente nome de bairro novo, NUNCA altere a grafia, capitalização ou acentuação
              dos nomes fornecidos — copie-os exatamente como aparecem na lista.
            - Escolha entre 3 e 6 bairros.

            Responda SOMENTE com JSON válido, sem markdown, sem explicações, no formato exato:
            {
              "recommendations": [
                { "name": "Nome Exato do Bairro", "advantage": "Frase curta descrevendo a vantagem." }
              ]
            }
            """.formatted(city, country, budgetTermInPortuguese, neighborhoodsList);

        NeighborhoodRecommendationList response = chatClient.prompt()
            .user(prompt)
            .call()
            .entity(NeighborhoodRecommendationList.class);

        List<NeighborhoodRecommendation> recommendations = response.recommendations();

        return recommendations.stream()
            .filter(recommendation -> {
                boolean valid = candidateNeighborhoods.contains(recommendation.name());
                if (!valid) {
                    log.warn("LLM recomendou bairro '{}' que não está na lista de candidatos — descartando",
                        recommendation.name());
                }
                return valid;
            })
            .toList();
    }
}
