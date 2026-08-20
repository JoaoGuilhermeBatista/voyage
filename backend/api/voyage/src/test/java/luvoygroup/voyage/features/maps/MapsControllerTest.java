package luvoygroup.voyage.features.maps;

import luvoygroup.voyage.TestJwtFactory;
import luvoygroup.voyage.features.maps.dto.DirectionsResponseDTO;
import luvoygroup.voyage.features.maps.dto.EmbedUrlResponseDTO;
import luvoygroup.voyage.security.JwtService;
import luvoygroup.voyage.security.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobre GET /api/v1/maps/directions e GET /api/v1/maps/embed-url.
 *
 * MapsService é um STUB (lança UnsupportedOperationException — ver
 * MapsService.java) neste momento, mas aqui ele é mockado via @MockBean,
 * então isso não afeta estes testes: eles validam o CONTRATO do
 * controller (roteamento, validação de parâmetros, propagação do shape
 * do DTO, exigência de auth), que já é código real e não-stub.
 *
 * Diferente de /api/v1/currency/**, este endpoint proxia uma API paga do
 * Google e por isso EXIGE JWT válido (não está no permitAll() do
 * SecurityConfig) — ver testes de 401 abaixo.
 */
@WebMvcTest(MapsController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class MapsControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean MapsService mapsService;
    @MockBean JwtService jwtService;

    final String validToken = TestJwtFactory.bearer();

    @BeforeEach
    void setupJwt() {
        when(jwtService.isValid(anyString())).thenReturn(true);
        when(jwtService.extractUserId(anyString())).thenReturn(TestJwtFactory.TEST_USER_ID.toString());
        when(jwtService.extractEmail(anyString())).thenReturn(TestJwtFactory.TEST_EMAIL);
    }

    // ─── Teste 40: GET /maps/directions autenticado, service configured=true → 200 ─

    @Test
    void teste40_shouldReturnDirections_whenAuthenticatedAndConfigured() throws Exception {
        when(mapsService.getDirections(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
            .thenReturn(new DirectionsResponseDTO(true, 5200.0, 720, "5.2 km", "12 mins"));

        mockMvc.perform(get("/api/v1/maps/directions")
                .header("Authorization", validToken)
                .param("originLat", "-23.5505")
                .param("originLng", "-46.6333")
                .param("destLat", "-22.9068")
                .param("destLng", "-43.1729"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.configured").value(true))
            .andExpect(jsonPath("$.distanceMeters").value(5200.0))
            .andExpect(jsonPath("$.durationSeconds").value(720))
            .andExpect(jsonPath("$.distanceText").value("5.2 km"))
            .andExpect(jsonPath("$.durationText").value("12 mins"));
    }

    // ─── Teste 41: GET /maps/directions com service configured=false → 200, configured:false ─
    // Chave do Google Maps não configurada no backend: o endpoint continua
    // respondendo 200 (não é um erro), só que com configured=false e os
    // demais campos nulos — o app deve esconder a UI de rota graciosamente.

    @Test
    void teste41_shouldReturn200WithConfiguredFalse_whenGoogleMapsKeyNotConfigured() throws Exception {
        when(mapsService.getDirections(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
            .thenReturn(new DirectionsResponseDTO(false, null, null, null, null));

        mockMvc.perform(get("/api/v1/maps/directions")
                .header("Authorization", validToken)
                .param("originLat", "-23.5505")
                .param("originLng", "-46.6333")
                .param("destLat", "-22.9068")
                .param("destLng", "-43.1729"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.configured").value(false))
            .andExpect(jsonPath("$.distanceMeters").doesNotExist())
            .andExpect(jsonPath("$.durationSeconds").doesNotExist());
    }

    // ─── Teste 42: GET /maps/directions sem um parâmetro obrigatório → 400 ────

    @Test
    void teste42_shouldReturn400_whenDirectionsParamIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/maps/directions")
                .header("Authorization", validToken)
                .param("originLat", "-23.5505")
                .param("originLng", "-46.6333")
                .param("destLat", "-22.9068"))
            // destLng ausente
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    // ─── Teste 43: GET /maps/directions sem token → 401 ───────────────────────

    @Test
    void teste43_shouldReturn401_whenNoTokenProvidedForDirections() throws Exception {
        mockMvc.perform(get("/api/v1/maps/directions")
                .param("originLat", "-23.5505")
                .param("originLng", "-46.6333")
                .param("destLat", "-22.9068")
                .param("destLng", "-43.1729"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    // ─── Teste 44: GET /maps/embed-url autenticado, service configured=true → 200 ─

    @Test
    void teste44_shouldReturnEmbedUrl_whenAuthenticatedAndConfigured() throws Exception {
        when(mapsService.getEmbedUrl(anyDouble(), anyDouble()))
            .thenReturn(new EmbedUrlResponseDTO(
                true,
                "https://www.google.com/maps/embed/v1/place?key=fake-key&q=-23.5505,-46.6333"
            ));

        mockMvc.perform(get("/api/v1/maps/embed-url")
                .header("Authorization", validToken)
                .param("lat", "-23.5505")
                .param("lng", "-46.6333"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.configured").value(true))
            .andExpect(jsonPath("$.embedUrl").value(
                "https://www.google.com/maps/embed/v1/place?key=fake-key&q=-23.5505,-46.6333"));
    }

    // ─── Teste 45: GET /maps/embed-url com service configured=false → 200, configured:false ─

    @Test
    void teste45_shouldReturn200WithConfiguredFalse_whenGoogleMapsKeyNotConfiguredForEmbed() throws Exception {
        when(mapsService.getEmbedUrl(anyDouble(), anyDouble()))
            .thenReturn(new EmbedUrlResponseDTO(false, null));

        mockMvc.perform(get("/api/v1/maps/embed-url")
                .header("Authorization", validToken)
                .param("lat", "-23.5505")
                .param("lng", "-46.6333"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.configured").value(false))
            .andExpect(jsonPath("$.embedUrl").doesNotExist());
    }

    // ─── Teste 46: GET /maps/embed-url sem um parâmetro obrigatório → 400 ─────

    @Test
    void teste46_shouldReturn400_whenEmbedUrlParamIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/maps/embed-url")
                .header("Authorization", validToken)
                .param("lat", "-23.5505"))
            // lng ausente
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    // ─── Teste 47: GET /maps/embed-url sem token → 401 ────────────────────────

    @Test
    void teste47_shouldReturn401_whenNoTokenProvidedForEmbedUrl() throws Exception {
        mockMvc.perform(get("/api/v1/maps/embed-url")
                .param("lat", "-23.5505")
                .param("lng", "-46.6333"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }
}
