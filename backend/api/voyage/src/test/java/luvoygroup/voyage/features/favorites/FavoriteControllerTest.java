package luvoygroup.voyage.features.favorites;

import luvoygroup.voyage.TestJwtFactory;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FavoriteController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class FavoriteControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean FavoriteService favoriteService;
    @MockBean JwtService jwtService;

    final String validToken = TestJwtFactory.bearer();

    @BeforeEach
    void setupJwt() {
        when(jwtService.isValid(anyString())).thenReturn(true);
        when(jwtService.extractUserId(anyString())).thenReturn(TestJwtFactory.TEST_USER_ID.toString());
        when(jwtService.extractEmail(anyString())).thenReturn(TestJwtFactory.TEST_EMAIL);
    }

    // ─── Teste 17: POST /favorites sem nenhum target → 400 ────────────────────

    @Test
    void teste17_shouldReturn400_whenFavoriteHasNoTarget() throws Exception {
        when(favoriteService.create(any(), any()))
            .thenThrow(new ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "Nenhum target definido"));

        mockMvc.perform(post("/api/v1/favorites")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    // ─── Teste 18: POST /favorites com dois targets → 400 ─────────────────────

    @Test
    void teste18_shouldReturn400_whenFavoriteHasMultipleTargets() throws Exception {
        when(favoriteService.create(any(), any()))
            .thenThrow(new ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "Apenas um target permitido"));

        mockMvc.perform(post("/api/v1/favorites")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "destinationId": "%s",
                      "poiId": "%s"
                    }
                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
            .andExpect(status().isBadRequest());
    }
}
