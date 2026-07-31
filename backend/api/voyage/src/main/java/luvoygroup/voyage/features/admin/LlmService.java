package luvoygroup.voyage.features.admin;

import luvoygroup.voyage.features.admin.dto.GeneratedContent;
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
}
