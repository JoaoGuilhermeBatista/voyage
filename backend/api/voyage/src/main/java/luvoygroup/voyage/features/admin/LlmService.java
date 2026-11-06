package luvoygroup.voyage.features.admin;

import luvoygroup.voyage.domain.Poi;
import luvoygroup.voyage.domain.enums.BudgetTier;
import luvoygroup.voyage.features.admin.dto.CatholicChurchClassification;
import luvoygroup.voyage.features.admin.dto.GeneratedContent;
import luvoygroup.voyage.features.admin.dto.PoiEnrichmentClassification;
import luvoygroup.voyage.features.admin.dto.PoiTitleNormalization;
import luvoygroup.voyage.features.admin.dto.PoisWrapper;
import luvoygroup.voyage.features.maps.dto.NeighborhoodRecommendation;
import luvoygroup.voyage.features.maps.dto.NeighborhoodRecommendationList;
import luvoygroup.voyage.features.unusualplaces.dto.UnusualPlacesWrapper;
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
        return generatePois(city, country, null, List.of());
    }

    public PoisWrapper generatePois(String city, String country, String focusCategory, List<String> existingTitles) {
        log.info("Gerando POIs via LLM para {}, {}{}", city, country,
            focusCategory == null ? "" : " (foco: " + focusCategory + ")");

        String focusInstruction = focusCategory == null ? "" : """

            ATENÇÃO — reforço de categoria: priorize fortemente a categoria %s (pelo menos 12 dos 20 pontos
            devem ser dessa categoria), mantendo só uma pequena variedade das demais para realismo.
            """.formatted(focusCategory);

        String existingList = existingTitles.isEmpty()
            ? ""
            : "\nLugares JÁ cadastrados no catálogo deste destino (NÃO repita nenhum destes, nem variações " +
                "óbvias do mesmo local):\n" +
                existingTitles.stream().map(t -> "- " + t).collect(java.util.stream.Collectors.joining("\n")) + "\n";

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
                  "catholicChurch": false,
                  "fullDayActivity": false
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
            - fullDayActivity: marque true SOMENTE para parques temáticos de dia inteiro de verdade
              (ex: Disneyland, Universal Studios, Europa-Park) — um local que ocupa o dia inteiro da
              viagem sozinho, sem espaço pra outra atividade de manhã/tarde/noite no mesmo dia. Marque
              false para TODOS os demais locais, incluindo parques comuns, museus grandes e outras
              atrações que não tomam o dia inteiro
            %s%s""".formatted(city, country, focusInstruction, existingList);

        return chatClient.prompt()
            .user(prompt)
            .call()
            .entity(PoisWrapper.class);
    }

    /**
     * Identifica, entre os títulos de POI já cadastrados, quais são igrejas
     * católicas (não outras religiões/denominações) — usada por
     * {@code AdminService#enrichExistingPoisWithCatholicChurchFlag} pra
     * classificar retroativamente POIs já existentes, sem recriá-los.
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
     * Entre os POIs já cadastrados, identifica os 3-5 mais icônicos
     * (mustSee) e os que deveriam ser recategorizados como LANDMARK por
     * erro de categoria (ex. monumento como MUSEUM) — usada por
     * {@code AdminService#enrichExistingPoisWithMustSeeAndLandmark} pra
     * corrigir destinos sem nenhum POI mustSee/LANDMARK, sem recriá-los.
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
     * Identifica quais títulos de POI precisam mudar pra normalizar o
     * idioma dentro do mesmo destino (achado: POIs da mesma cidade com
     * título às vezes traduzido pro português, às vezes não, sem critério
     * — ver luvoy-pendencias.md). Retorna só os que precisam mudar.
     */
    public PoiTitleNormalization normalizePoiTitleLanguage(String city, String country, List<String> poiTitles) {
        log.info("Normalizando idioma de título de POI via LLM para {}, {}", city, country);

        String titlesList = poiTitles.stream()
            .map(title -> "- " + title)
            .collect(java.util.stream.Collectors.joining("\n"));

        String prompt = """
            Você é um editor de conteúdo de turismo especializado em guias de viagem em português
            brasileiro. Abaixo está a lista EXATA de títulos de pontos de interesse já cadastrados
            para %s, %s. O catálogo tem uma inconsistência: dentro do MESMO destino, alguns títulos
            estão traduzidos para português e outros permanecem no idioma original, sem critério —
            sua tarefa é normalizar isso.

            Lista de títulos (um por linha):
            %s

            Critério editorial:
            - Traduza para português títulos que são descrições genéricas do local e têm tradução
              natural e comum em guias de viagem em português (ex: "Fisherman's Bastion" →
              "Bastião dos Pescadores").
            - MANTENHA no idioma original títulos que são nomes próprios consagrados
              internacionalmente pelo nome original, mesmo em texto em português — ex: "Sagrada
              Família", "Wat Arun", "Rijksmuseum", "Neuschwanstein".
            - Em caso de dúvida real sobre se é nome próprio ou descrição genérica, prefira MANTER
              como está — só normalize quando tiver certeza do critério.

            Regras OBRIGATÓRIAS:
            - Retorne SOMENTE os títulos que precisam MUDAR — nunca inclua um título que já está
              correto (seja porque já está traduzido corretamente, seja porque já está,
              corretamente, mantido no original).
            - originalTitle deve ser EXATO (verbatim) — copie exatamente como aparece na lista
              acima, sem alterar grafia, capitalização ou acentuação.
            - NUNCA invente um título que não esteja na lista fornecida.

            Responda SOMENTE com JSON válido, sem markdown, sem explicações, no formato exato:
            {
              "changes": [
                { "originalTitle": "Título Exato Como Está", "normalizedTitle": "Novo Título Normalizado" }
              ]
            }
            """.formatted(city, country, titlesList);

        return chatClient.prompt()
            .user(prompt)
            .call()
            .entity(PoiTitleNormalization.class);
    }

    /**
     * Escolhe, via LLM, 3 a 6 bairros de {@code candidateNeighborhoods} mais
     * adequados ao {@code budgetTier}, com uma frase de vantagem cada.
     * Guarda contra alucinação: descarta qualquer recomendação cujo nome não
     * esteja verbatim na lista de candidatos (mesmo padrão de
     * {@link #classifyMustSeeAndLandmarks(String, String, List)}).
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

    /**
     * Feature premium "Lugares Incomuns" (escopo definido pelo agente que
     * implementou isso, não spec oficial da contratante — ver resumo da
     * sessão). Gera 4 a 6 lugares editoriais fora do roteiro turístico óbvio
     * do destino, passando o catálogo de POIs JÁ cadastrados como contexto
     * NEGATIVO (pra não duplicar o que já está no app como ponto turístico
     * principal). Conteúdo é salvo uma vez via endpoint admin
     * (ver {@code UnusualPlaceService#generateForDestination}) e servido
     * depois sem custo de LLM por requisição.
     */
    public UnusualPlacesWrapper generateUnusualPlaces(String city, String country, List<String> existingPoiTitles) {
        log.info("Gerando lugares incomuns via LLM para {}, {}", city, country);

        String existingList = existingPoiTitles.isEmpty()
            ? "(nenhum cadastrado ainda)"
            : existingPoiTitles.stream().map(t -> "- " + t).collect(java.util.stream.Collectors.joining("\n"));

        String prompt = """
            Você é um morador local e guia especializado em %s, %s — alguém que conhece a cidade de
            verdade, não um guia turístico genérico. Gere de 4 a 6 lugares incomuns, fora do roteiro
            turístico óbvio, que um morador recomendaria a um amigo visitando.

            Lugares turísticos JÁ cadastrados no catálogo do app (NÃO repita nenhum destes, nem
            variações óbvias do mesmo local):
            %s

            Responda SOMENTE com JSON válido, sem markdown, sem explicações.

            Formato exato:
            {
              "places": [
                {
                  "title": "Nome do lugar",
                  "description": "2 a 3 frases explicando por que é especial/incomum e o que faz dele uma recomendação de morador local",
                  "category": "Categoria livre curta, ex: Bar escondido, Mirante local, Ritual de bairro"
                }
              ]
            }

            Regras:
            - NUNCA inclua pontos turísticos de massa, marcos famosos mundialmente conhecidos, ou
              qualquer item (ou variação óbvia dele) que já esteja na lista de lugares já cadastrados.
            - Prefira lugares reais e verificáveis (existe de verdade), mesmo que pouco conhecidos.
            - category é um rótulo livre curto (não precisa seguir um enum fixo).
            """.formatted(city, country, existingList);

        return chatClient.prompt()
            .user(prompt)
            .call()
            .entity(UnusualPlacesWrapper.class);
    }
}
