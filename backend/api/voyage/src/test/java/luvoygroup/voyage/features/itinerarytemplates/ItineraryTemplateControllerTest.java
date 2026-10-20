package luvoygroup.voyage.features.itinerarytemplates;

import luvoygroup.voyage.TestJwtFactory;
import luvoygroup.voyage.domain.enums.BudgetTier;
import luvoygroup.voyage.domain.enums.PoiCategory;
import luvoygroup.voyage.exception.ResourceNotFoundException;
import luvoygroup.voyage.features.itinerary.dto.ItineraryResponseDTO;
import luvoygroup.voyage.features.itinerary.dto.PoiDetailDTO;
import luvoygroup.voyage.features.itinerarytemplates.dto.ItineraryTemplateDetailDTO;
import luvoygroup.voyage.features.itinerarytemplates.dto.ItineraryTemplateSummaryDTO;
import luvoygroup.voyage.features.itinerarytemplates.dto.TemplateActivityDTO;
import luvoygroup.voyage.features.itinerarytemplates.dto.TemplateDayDTO;
import luvoygroup.voyage.security.JwtService;
import luvoygroup.voyage.security.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobre as 3 rotas de ItineraryTemplateController:
 *   GET  /api/v1/destinations/{slug}/itinerary-templates
 *   GET  /api/v1/destinations/{slug}/itinerary-templates/{templateId}
 *   POST /api/v1/itinerary-templates/{templateId}/adopt
 *
 * ItineraryTemplateService é mockado via @MockBean, então estes testes só
 * validam o CONTRATO real do controller (roteamento, propagação de
 * exceções via GlobalExceptionHandler, shape de resposta, validação de
 * @RequestBody, exigência de JWT no /adopt) — não a lógica de negócio, que
 * ainda é um stub (ver ItineraryTemplateService.java /
 * ItineraryTemplateServiceTest.java).
 *
 * As duas rotas GET vivem sob /api/v1/destinations/** e por isso não
 * exigem Authorization (permitAll() já configurado no SecurityConfig para
 * esse prefixo) — não há teste de 401 para elas. /adopt não bate nesse
 * prefixo e cai no anyRequest().authenticated() default.
 */
@WebMvcTest(ItineraryTemplateController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class ItineraryTemplateControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean ItineraryTemplateService itineraryTemplateService;
    @MockBean JwtService jwtService;

    final String validToken = TestJwtFactory.bearer();
    final UUID templateId = UUID.randomUUID();
    final UUID destinationId = UUID.randomUUID();

    @BeforeEach
    void setupJwt() {
        when(jwtService.isValid(anyString())).thenReturn(true);
        when(jwtService.extractUserId(anyString())).thenReturn(TestJwtFactory.TEST_USER_ID.toString());
        when(jwtService.extractEmail(anyString())).thenReturn(TestJwtFactory.TEST_EMAIL);
    }

    // ─── Teste 52: GET /destinations/{slug}/itinerary-templates → 200 ─────────

    @Test
    void teste52_shouldListTemplates_whenDestinationExists() throws Exception {
        when(itineraryTemplateService.findAllForDestination("lisboa")).thenReturn(List.of(
            new ItineraryTemplateSummaryDTO(templateId, "Lisboa Clássica", "3 dias de cultura e gastronomia", 3, BudgetTier.MODERATE)
        ));

        mockMvc.perform(get("/api/v1/destinations/lisboa/itinerary-templates"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(templateId.toString()))
            .andExpect(jsonPath("$[0].title").value("Lisboa Clássica"))
            .andExpect(jsonPath("$[0].durationDays").value(3))
            .andExpect(jsonPath("$[0].budgetTier").value("MODERATE"));
    }

    // ─── Teste 53: GET /destinations/{slug}/itinerary-templates, destino inexistente → 404 ─

    @Test
    void teste53_shouldReturn404_whenListingTemplatesForUnknownDestination() throws Exception {
        when(itineraryTemplateService.findAllForDestination("unknown"))
            .thenThrow(new ResourceNotFoundException("Destino não encontrado"));

        mockMvc.perform(get("/api/v1/destinations/unknown/itinerary-templates"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // ─── Teste 54: GET /destinations/{slug}/itinerary-templates/{id} → 200 ────

    @Test
    void teste54_shouldReturnTemplateDetail_whenTemplateExists() throws Exception {
        PoiDetailDTO poi = new PoiDetailDTO(
            UUID.randomUUID(), "Torre de Belém", PoiCategory.MUSEUM, "Monumento histórico à beira-rio", "Av. Brasília, Lisboa",
            BigDecimal.valueOf(38.6916), BigDecimal.valueOf(-9.2160), List.of(), BudgetTier.MODERATE, 90, false
        );
        ItineraryTemplateDetailDTO detail = new ItineraryTemplateDetailDTO(
            templateId, "Lisboa Clássica", "3 dias de cultura e gastronomia", 3, BudgetTier.MODERATE,
            destinationId, "Lisboa",
            List.of(new TemplateDayDTO(1, List.of(new TemplateActivityDTO("MORNING", 0, poi))))
        );
        when(itineraryTemplateService.findById(templateId)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/destinations/lisboa/itinerary-templates/" + templateId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(templateId.toString()))
            .andExpect(jsonPath("$.destinationName").value("Lisboa"))
            .andExpect(jsonPath("$.days[0].dayNumber").value(1))
            .andExpect(jsonPath("$.days[0].activities[0].slot").value("MORNING"))
            .andExpect(jsonPath("$.days[0].activities[0].poi.title").value("Torre de Belém"));
    }

    // ─── Teste 55: GET /destinations/{slug}/itinerary-templates/{id}, template inexistente → 404 ─

    @Test
    void teste55_shouldReturn404_whenTemplateDoesNotExist() throws Exception {
        when(itineraryTemplateService.findById(templateId))
            .thenThrow(new ResourceNotFoundException("Template não encontrado"));

        mockMvc.perform(get("/api/v1/destinations/lisboa/itinerary-templates/" + templateId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // ─── Teste 56: POST /itinerary-templates/{id}/adopt, autenticado → 200 ────

    @Test
    void teste56_shouldAdoptTemplate_whenAuthenticated() throws Exception {
        LocalDate startDate = LocalDate.of(2026, 9, 1);
        ItineraryResponseDTO response = new ItineraryResponseDTO(
            UUID.randomUUID(), "Lisboa Clássica", destinationId, "Lisboa", "https://img.example/lisboa.jpg",
            "Portugal",
            startDate, startDate.plusDays(2), List.of()
        );
        when(itineraryTemplateService.adopt(eq(templateId), eq(TestJwtFactory.TEST_USER_ID), eq(startDate)))
            .thenReturn(response);

        mockMvc.perform(post("/api/v1/itinerary-templates/" + templateId + "/adopt")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"startDate\":\"2026-09-01\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.destinationName").value("Lisboa"))
            .andExpect(jsonPath("$.startDate").value("2026-09-01"))
            .andExpect(jsonPath("$.endDate").value("2026-09-03"));
    }

    // ─── Teste 57: POST /itinerary-templates/{id}/adopt, sem token → 401 ──────

    @Test
    void teste57_shouldReturn401_whenAdoptingWithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/itinerary-templates/" + templateId + "/adopt")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"startDate\":\"2026-09-01\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    // ─── Teste 58: POST /itinerary-templates/{id}/adopt, sem startDate → 400 ──

    @Test
    void teste58_shouldReturn400_whenAdoptingWithoutStartDate() throws Exception {
        mockMvc.perform(post("/api/v1/itinerary-templates/" + templateId + "/adopt")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }
}
