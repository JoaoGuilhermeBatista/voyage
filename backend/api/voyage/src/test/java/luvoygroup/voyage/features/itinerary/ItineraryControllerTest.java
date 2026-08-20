package luvoygroup.voyage.features.itinerary;

import luvoygroup.voyage.TestJwtFactory;
import luvoygroup.voyage.domain.enums.BudgetTier;
import luvoygroup.voyage.domain.enums.ItinerarySlot;
import luvoygroup.voyage.domain.enums.PoiCategory;
import luvoygroup.voyage.exception.ForbiddenAccessException;
import luvoygroup.voyage.exception.OnboardingNotCompletedException;
import luvoygroup.voyage.exception.ResourceNotFoundException;
import luvoygroup.voyage.features.itinerary.dto.ActivityDTO;
import luvoygroup.voyage.features.itinerary.dto.PoiDetailDTO;
import luvoygroup.voyage.security.JwtAuthFilter;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItineraryController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class ItineraryControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean ItineraryService itineraryService;
    @MockBean JwtService jwtService;

    final String validToken = TestJwtFactory.bearer();

    @BeforeEach
    void setupJwt() {
        when(jwtService.isValid(anyString())).thenReturn(true);
        when(jwtService.extractUserId(anyString())).thenReturn(TestJwtFactory.TEST_USER_ID.toString());
        when(jwtService.extractEmail(anyString())).thenReturn(TestJwtFactory.TEST_EMAIL);
    }

    // ─── Teste 14: Sem onboarding → 422 ──────────────────────────────────────

    @Test
    void teste14_shouldReturn422_whenUserHasNotDoneOnboarding() throws Exception {
        when(itineraryService.generate(any(), any()))
            .thenThrow(new OnboardingNotCompletedException("Complete o onboarding"));

        mockMvc.perform(post("/api/v1/itineraries")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "destinationId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                      "startDate": "2027-08-01",
                      "endDate": "2027-08-05"
                    }
                """))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error").value("ONBOARDING_NOT_COMPLETED"));
    }

    // ─── Teste 15: Deletar roteiro de outro usuário → 403 ─────────────────────

    @Test
    void teste15_shouldReturn403_whenDeletingAnotherUsersItinerary() throws Exception {
        doThrow(new ForbiddenAccessException("Acesso não autorizado"))
            .when(itineraryService).delete(any(), any());

        mockMvc.perform(delete("/api/v1/itineraries/" + UUID.randomUUID())
                .header("Authorization", validToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    // ─── Teste 16: Roteiro não encontrado → 404 ───────────────────────────────

    @Test
    void teste16_shouldReturn404_whenItineraryNotFound() throws Exception {
        when(itineraryService.findById(any(), any()))
            .thenThrow(new ResourceNotFoundException("Roteiro não encontrado"));

        mockMvc.perform(get("/api/v1/itineraries/" + UUID.randomUUID())
                .header("Authorization", validToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // ─── Fluxo correto: listar roteiros → 200 ─────────────────────────────────

    @Test
    void shouldReturn200_whenListingItineraries() throws Exception {
        when(itineraryService.findAll(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/itineraries")
                .header("Authorization", validToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    // ─── Helpers para os testes de alternatives/substituteActivity ───────────

    private PoiDetailDTO samplePoi(String title) {
        return new PoiDetailDTO(
            UUID.randomUUID(),
            title,
            PoiCategory.MUSEUM,
            "Descrição do POI",
            "Rua Exemplo, 123",
            new BigDecimal("-23.55"),
            new BigDecimal("-46.63"),
            List.of("https://example.com/foto.jpg"),
            BudgetTier.MODERATE,
            90
        );
    }

    // ─── Teste 31: GET alternatives com atividade válida → 200, até 3 POIs ───

    @Test
    void teste31_shouldReturn200WithAlternatives_whenActivityIsValid() throws Exception {
        UUID itineraryId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        List<PoiDetailDTO> alternatives = List.of(
            samplePoi("Museu A"), samplePoi("Museu B"), samplePoi("Museu C"));

        when(itineraryService.getAlternatives(eq(itineraryId), eq(activityId), any()))
            .thenReturn(alternatives);

        mockMvc.perform(get("/api/v1/itineraries/" + itineraryId + "/activities/" + activityId + "/alternatives")
                .header("Authorization", validToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].title").value("Museu A"))
            .andExpect(jsonPath("$[0].category").value("MUSEUM"));
    }

    // ─── Teste 32: GET alternatives de roteiro de outro usuário → 403 ────────

    @Test
    void teste32_shouldReturn403_whenGettingAlternativesOfAnotherUsersItinerary() throws Exception {
        when(itineraryService.getAlternatives(any(), any(), any()))
            .thenThrow(new ForbiddenAccessException("Acesso não autorizado a este roteiro"));

        mockMvc.perform(get("/api/v1/itineraries/" + UUID.randomUUID() + "/activities/" + UUID.randomUUID() + "/alternatives")
                .header("Authorization", validToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    // ─── Teste 33: GET alternatives de roteiro inexistente → 404 ─────────────

    @Test
    void teste33_shouldReturn404_whenGettingAlternativesOfNonexistentItinerary() throws Exception {
        when(itineraryService.getAlternatives(any(), any(), any()))
            .thenThrow(new ResourceNotFoundException("Roteiro não encontrado"));

        mockMvc.perform(get("/api/v1/itineraries/" + UUID.randomUUID() + "/activities/" + UUID.randomUUID() + "/alternatives")
                .header("Authorization", validToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // ─── Teste 34: GET alternatives de atividade inexistente no roteiro → 404 ─

    @Test
    void teste34_shouldReturn404_whenGettingAlternativesOfNonexistentActivity() throws Exception {
        when(itineraryService.getAlternatives(any(), any(), any()))
            .thenThrow(new ResourceNotFoundException("Atividade não encontrada"));

        mockMvc.perform(get("/api/v1/itineraries/" + UUID.randomUUID() + "/activities/" + UUID.randomUUID() + "/alternatives")
                .header("Authorization", validToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // ─── Teste 35: PATCH activities/{id} com poiId válido do mesmo destino → 200

    @Test
    void teste35_shouldReturn200WithUpdatedActivity_whenSubstitutingWithValidPoi() throws Exception {
        UUID itineraryId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        UUID poiId = UUID.randomUUID();
        ActivityDTO updated = new ActivityDTO(activityId, ItinerarySlot.MORNING, 1, samplePoi("Novo Museu"));

        when(itineraryService.substituteActivity(eq(itineraryId), eq(activityId), eq(poiId), any()))
            .thenReturn(updated);

        mockMvc.perform(patch("/api/v1/itineraries/" + itineraryId + "/activities/" + activityId)
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "poiId": "%s" }
                """.formatted(poiId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(activityId.toString()))
            .andExpect(jsonPath("$.poi.title").value("Novo Museu"));
    }

    // ─── Teste 36: PATCH activities/{id} com poiId de destino diferente → 403 ─

    @Test
    void teste36_shouldReturn403_whenSubstitutingWithPoiFromAnotherDestination() throws Exception {
        when(itineraryService.substituteActivity(any(), any(), any(), any()))
            .thenThrow(new ForbiddenAccessException("O POI não pertence ao destino deste roteiro"));

        mockMvc.perform(patch("/api/v1/itineraries/" + UUID.randomUUID() + "/activities/" + UUID.randomUUID())
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "poiId": "%s" }
                """.formatted(UUID.randomUUID())))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    // ─── Teste 37: PATCH activities/{id} de roteiro de outro usuário → 403 ───

    @Test
    void teste37_shouldReturn403_whenSubstitutingActivityOfAnotherUsersItinerary() throws Exception {
        when(itineraryService.substituteActivity(any(), any(), any(), any()))
            .thenThrow(new ForbiddenAccessException("Acesso não autorizado a este roteiro"));

        mockMvc.perform(patch("/api/v1/itineraries/" + UUID.randomUUID() + "/activities/" + UUID.randomUUID())
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "poiId": "%s" }
                """.formatted(UUID.randomUUID())))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    // ─── Teste 38: PATCH activities/{id} com poiId inexistente → 404 ─────────

    @Test
    void teste38_shouldReturn404_whenSubstitutingWithNonexistentPoi() throws Exception {
        when(itineraryService.substituteActivity(any(), any(), any(), any()))
            .thenThrow(new ResourceNotFoundException("POI não encontrado"));

        mockMvc.perform(patch("/api/v1/itineraries/" + UUID.randomUUID() + "/activities/" + UUID.randomUUID())
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "poiId": "%s" }
                """.formatted(UUID.randomUUID())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // ─── Teste 39: PATCH activities/{id} sem poiId no body → 400 (validação) ─

    @Test
    void teste39_shouldReturn400_whenSubstituteRequestHasNoPoiId() throws Exception {
        mockMvc.perform(patch("/api/v1/itineraries/" + UUID.randomUUID() + "/activities/" + UUID.randomUUID())
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }
}
