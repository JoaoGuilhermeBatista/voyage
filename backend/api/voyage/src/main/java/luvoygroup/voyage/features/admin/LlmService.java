package luvoygroup.voyage.features.admin;

import luvoygroup.voyage.features.admin.dto.GeneratedContent;
import luvoygroup.voyage.features.admin.dto.PoisWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

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

            Categorias disponíveis (use exatamente): MUSEUM, RESTAURANT, PARK, SHOPPING, NIGHTLIFE, BEACH

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
                  "tags": ["cultura", "historia"]
                }
              ]
            }

            Regras:
            - Inclua pontos variados: museus, restaurantes, parques, bares, lojas icônicas
            - Distribua os orçamentos: pelo menos 5 ECONOMY, 8 MODERATE e 5 LUXURY
            - As coordenadas devem ser as mais precisas possíveis para cada local
            - openingTime e closingTime no formato HH:mm (null se funcionar 24h ou sem horário fixo)
            - Tags devem refletir o perfil do POI (ex: "cultura", "gastronomia", "arte", "natureza", "compras")
            """.formatted(city, country);

        return chatClient.prompt()
            .user(prompt)
            .call()
            .entity(PoisWrapper.class);
    }
}
