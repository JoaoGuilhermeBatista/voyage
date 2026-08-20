package luvoygroup.voyage.features.itinerarytemplates.dto;

import java.util.List;

/**
 * Shape esperado da resposta estruturada do GPT-4o para
 * ItineraryTemplateService#generateForDestination — mapeado via
 * {@code chatClient.prompt().user(prompt).call().entity(GeneratedTemplateContent.class)},
 * mesmo padrão de LlmService#generateDestinationContent /
 * LlmService#generatePois. Não fazia parte do contrato de DTOs públicos
 * dado na spec; é um DTO interno que documenta o contrato prompt/resposta
 * do LLM (ver nota de decisão de design no relatório final).
 */
public record GeneratedTemplateContent(
    String title,
    String description,
    List<GeneratedTemplateActivity> activities
) {}
