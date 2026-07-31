package luvoygroup.voyage.features.pdf;

import luvoygroup.voyage.TestJwtFactory;
import luvoygroup.voyage.exception.ForbiddenAccessException;
import luvoygroup.voyage.exception.ResourceNotFoundException;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PdfController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class PdfControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean PdfService pdfService;
    @MockBean JwtService jwtService;

    final String validToken = TestJwtFactory.bearer();

    @BeforeEach
    void setupJwt() {
        when(jwtService.isValid(anyString())).thenReturn(true);
        when(jwtService.extractUserId(anyString())).thenReturn(TestJwtFactory.TEST_USER_ID.toString());
        when(jwtService.extractEmail(anyString())).thenReturn(TestJwtFactory.TEST_EMAIL);
    }

    // ─── Teste 20: exportar roteiro de outro usuário → 403 ───────────────────

    @Test
    void teste20_shouldReturn403_whenExportingAnotherUsersItinerary() throws Exception {
        when(pdfService.export(any(), any()))
            .thenThrow(new ForbiddenAccessException("Acesso não autorizado"));

        mockMvc.perform(post("/api/v1/pdf/export/" + UUID.randomUUID())
                .header("Authorization", validToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    // ─── Teste 21: exportar roteiro inexistente → 404 ────────────────────────

    @Test
    void teste21_shouldReturn404_whenItineraryNotFound() throws Exception {
        when(pdfService.export(any(), any()))
            .thenThrow(new ResourceNotFoundException("Roteiro não encontrado"));

        mockMvc.perform(post("/api/v1/pdf/export/" + UUID.randomUUID())
                .header("Authorization", validToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }
}
