package luvoygroup.voyage.features.admin.dto;

import java.util.List;

/**
 * Resposta do LLM para LlmService#classifyCatholicChurches: quais dos
 * títulos de POI FORNECIDOS (verbatim, um subconjunto da lista de entrada)
 * são igrejas/basílicas/catedrais/santuários da Igreja Católica. O LLM não
 * deve inventar títulos que não estejam na lista de entrada — a
 * responsabilidade de ignorar qualquer título alucinado que não bata
 * exatamente com um POI real é de quem consome esta resposta (ver contrato
 * documentado em AdminService#enrichExistingPoisWithCatholicChurchFlag).
 */
public record CatholicChurchClassification(List<String> catholicChurchTitles) {}
