package luvoygroup.voyage.features.admin;

import luvoygroup.voyage.domain.enums.BudgetTier;
import luvoygroup.voyage.features.admin.dto.PoiEnrichmentReport;
import luvoygroup.voyage.features.itinerarytemplates.dto.ItineraryTemplateDTO;
import luvoygroup.voyage.security.JwtService;
import luvoygroup.voyage.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Primeiro teste de AdminController (nenhum existia antes desta feature) —
 * cobre APENAS o novo endpoint
 * POST /api/v1/admin/destinations/{slug}/itinerary-templates/generate.
 *
 * AdminService é mockado via @MockBean, então a lógica real de geração
 * (ItineraryTemplateService, ainda stub) não é exercitada aqui — só o
 * roteamento, a checagem de X-Admin-Token e a validação de @RequestBody.
 *
 * IMPORTANTE (checagem de X-Admin-Token — ver AdminController.java):
 * /api/v1/admin/** já está em permitAll() no SecurityConfig (a auth não
 * passa pelo Spring Security), então o teste de "token ausente"/"token
 * errado" exercita o branch manual `if (token == null || !adminToken.equals(token))
 * return 403` dentro do controller, igual aos dois endpoints admin já
 * existentes (generateDestination, generatePoisBatch). Único ajuste feito
 * no novo endpoint: o @RequestHeader usa required=false (os dois
 * existentes usam o default required=true) para que token AUSENTE caia no
 * mesmo 403 explícito do token ERRADO, em vez de virar uma
 * MissingRequestHeaderException não mapeada (que cairia no
 * @ExceptionHandler(Exception.class) genérico do GlobalExceptionHandler e
 * viraria 500) — ver decisão de design detalhada no relatório final do
 * agente que escreveu este teste.
 */
@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class AdminControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean AdminService adminService;
    @MockBean JwtService jwtService;

    // ver application-test.yaml: admin.token
    private static final String VALID_ADMIN_TOKEN = "test-admin-token";

    // ─── Teste 48: token válido, body válido → 201 ─────────────────────────────

    @Test
    void teste48_shouldGenerateItineraryTemplate_whenAdminTokenIsValid() throws Exception {
        ItineraryTemplateDTO dto = new ItineraryTemplateDTO(
            UUID.randomUUID(), "Lisboa Clássica", "3 dias de cultura e gastronomia",
            3, BudgetTier.MODERATE, UUID.randomUUID()
        );
        when(adminService.generateItineraryTemplate(anyString(), any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/admin/destinations/lisboa/itinerary-templates/generate")
                .header("X-Admin-Token", VALID_ADMIN_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"durationDays\":3,\"budgetTier\":\"MODERATE\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Lisboa Clássica"))
            .andExpect(jsonPath("$.durationDays").value(3))
            .andExpect(jsonPath("$.budgetTier").value("MODERATE"));
    }

    // ─── Teste 49: token errado → 403 ──────────────────────────────────────────

    @Test
    void teste49_shouldReturn403_whenAdminTokenIsWrong() throws Exception {
        mockMvc.perform(post("/api/v1/admin/destinations/lisboa/itinerary-templates/generate")
                .header("X-Admin-Token", "wrong-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"durationDays\":3,\"budgetTier\":\"MODERATE\"}"))
            .andExpect(status().isForbidden());
    }

    // ─── Teste 50: token ausente → 403 ─────────────────────────────────────────

    @Test
    void teste50_shouldReturn403_whenAdminTokenIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/admin/destinations/lisboa/itinerary-templates/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"durationDays\":3,\"budgetTier\":\"MODERATE\"}"))
            .andExpect(status().isForbidden());
    }

    // ─── Teste 51: body inválido (durationDays não positivo) → 400 ────────────

    @Test
    void teste51_shouldReturn400_whenBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/admin/destinations/lisboa/itinerary-templates/generate")
                .header("X-Admin-Token", VALID_ADMIN_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"durationDays\":0,\"budgetTier\":\"MODERATE\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    // ─── POST /admin/pois/enrich-catholic-church-flags ─────────────────────────
    //
    // AdminService é mockado, então a lógica real (ainda um stub RED em
    // AdminService/LlmService) não é exercitada aqui — só roteamento e a
    // checagem de X-Admin-Token, exatamente igual ao endpoint de geração de
    // template acima (mesmo @RequestHeader(required=false) para que token
    // ausente caia no 403 explícito do controller, não em uma
    // MissingRequestHeaderException não mapeada).

    // ─── Teste 52: token válido → 200 ──────────────────────────────────────────

    @Test
    void teste52_shouldEnrichCatholicChurchFlags_whenAdminTokenIsValid() throws Exception {
        PoiEnrichmentReport report = new PoiEnrichmentReport(2, 0, 2, List.of("lisboa", "roma"), null, null);
        when(adminService.enrichExistingPoisWithCatholicChurchFlag()).thenReturn(report);

        mockMvc.perform(post("/api/v1/admin/pois/enrich-catholic-church-flags")
                .header("X-Admin-Token", VALID_ADMIN_TOKEN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.destinationsProcessed").value(2))
            .andExpect(jsonPath("$.totalPoisEnriched").value(2));
    }

    // ─── Teste 53: token errado → 403 ──────────────────────────────────────────

    @Test
    void teste53_shouldReturn403_whenAdminTokenIsWrong_forEnrichCatholicChurchFlags() throws Exception {
        mockMvc.perform(post("/api/v1/admin/pois/enrich-catholic-church-flags")
                .header("X-Admin-Token", "wrong-token"))
            .andExpect(status().isForbidden());
    }

    // ─── Teste 54: token ausente → 403 ─────────────────────────────────────────

    @Test
    void teste54_shouldReturn403_whenAdminTokenIsMissing_forEnrichCatholicChurchFlags() throws Exception {
        mockMvc.perform(post("/api/v1/admin/pois/enrich-catholic-church-flags"))
            .andExpect(status().isForbidden());
    }
}
