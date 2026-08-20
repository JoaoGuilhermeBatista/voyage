package luvoygroup.voyage.features.itinerarytemplates.dto;

/**
 * Shape de UM item de atividade dentro da resposta estruturada do LLM (ver
 * {@link GeneratedTemplateContent}). Campos em String/Integer "crus" de
 * propósito — mesma convenção de {@code GeneratedPoi} em
 * {@code features.admin.dto}: são validados/convertidos manualmente pelo
 * service (slot -> ItinerarySlot, poiId -> UUID conferido contra o catálogo
 * real de POIs do destino) em vez de deixar o Jackson falhar a
 * desserialização inteira por causa de um único campo malformado ou de um
 * poiId alucinado.
 */
public record GeneratedTemplateActivity(
    String poiId,
    Integer dayNumber,
    String slot,
    Integer orderInSlot
) {}
