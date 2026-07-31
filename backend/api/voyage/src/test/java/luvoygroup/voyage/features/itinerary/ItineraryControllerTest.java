package luvoygroup.voyage.features.itinerary;

import luvoygroup.voyage.TestJwtFactory;
import luvoygroup.voyage.exception.ForbiddenAccessException;
import luvoygroup.voyage.exception.OnboardingNotCompletedException;
import luvoygroup.voyage.exception.ResourceNotFoundException;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
}
