package luvoygroup.voyage.features.destinations;

import luvoygroup.voyage.security.JwtService;
import luvoygroup.voyage.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DestinationController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class DestinationControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean DestinationService destinationService;
    @MockBean JwtService jwtService;

    // ─── Teste 19: GET /destinations com banco vazio → 200 lista vazia ────────

    @Test
    void teste19_shouldReturn200WithEmptyList_whenNoDestinationsExist() throws Exception {
        when(destinationService.findAllActive()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/destinations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
    }

    // ─── Fluxo correto: GET /destinations com dados → 200 ────────────────────

    @Test
    void shouldReturn200WithData_whenDestinationsExist() throws Exception {
        when(destinationService.findAllActive()).thenReturn(List.of(
            new luvoygroup.voyage.features.destinations.dto.DestinationSummaryDTO(
                UUID.randomUUID(), "paris", "Paris", "França", "Paris",
                "Cidade Luz", "https://img.jpg", "Eiffel · Louvre", List.of("cultura"), "CITY")
        ));

        mockMvc.perform(get("/api/v1/destinations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].slug").value("paris"))
            .andExpect(jsonPath("$[0].name").value("Paris"));
    }
}
