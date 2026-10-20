package luvoygroup.voyage.features.currency;

import luvoygroup.voyage.TestJwtFactory;
import luvoygroup.voyage.features.currency.dto.LocalCurrencyRateDTO;
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

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CurrencyController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class CurrencyControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean CurrencyService currencyService;
    @MockBean JwtService jwtService;

    final String validToken = TestJwtFactory.bearer();

    @BeforeEach
    void setupJwt() {
        when(jwtService.isValid(anyString())).thenReturn(true);
        when(jwtService.extractUserId(anyString())).thenReturn(TestJwtFactory.TEST_USER_ID.toString());
        when(jwtService.extractEmail(anyString())).thenReturn(TestJwtFactory.TEST_EMAIL);
    }

    // ─── Teste 28: GET /currency/rates?country=X com token válido → 200 e shape do DTO ──

    @Test
    void teste28_shouldReturnLocalRate_whenAuthenticated() throws Exception {
        when(currencyService.getLocalRate(eq("Japão")))
            .thenReturn(Optional.of(new LocalCurrencyRateDTO("JPY", new BigDecimal("0.036"))));

        mockMvc.perform(get("/api/v1/currency/rates")
                .param("country", "Japão")
                .header("Authorization", validToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currencyCode").value("JPY"))
            .andExpect(jsonPath("$.rateToBrl").value(0.036));
    }

    // ─── Teste 29: GET /currency/rates sem autenticação → 200 (endpoint público) ─
    // SecurityConfig.filterChain permite GET /api/v1/currency/** via permitAll(),
    // já que são dados não sensíveis (taxas de câmbio). Sem header Authorization,
    // o request nunca passa pelo JwtAuthFilter de forma bloqueante e chega ao
    // controller normalmente.

    @Test
    void teste29_shouldReturnLocalRate_whenNotAuthenticated() throws Exception {
        when(currencyService.getLocalRate(eq("Japão")))
            .thenReturn(Optional.of(new LocalCurrencyRateDTO("JPY", new BigDecimal("0.036"))));

        mockMvc.perform(get("/api/v1/currency/rates").param("country", "Japão"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currencyCode").value("JPY"));
    }

    // ─── Teste 30: falha no CurrencyService → 500 tratado pelo GlobalExceptionHandler ─
    // CurrencyService.getLocalRate() lança IllegalStateException quando a resposta da
    // API externa é inválida (ver CurrencyService.java) — o GlobalExceptionHandler.
    // handleGeneric captura qualquer Exception não mapeada e retorna 500 com um
    // ErrorResponse estruturado (não um 500 cru) — este é o comportamento atualmente
    // esperado/documentado.

    @Test
    void teste30_shouldReturn500WithStructuredBody_whenServiceThrows() throws Exception {
        when(currencyService.getLocalRate(eq("Japão")))
            .thenThrow(new IllegalStateException("Resposta inválida da API de câmbio"));

        mockMvc.perform(get("/api/v1/currency/rates")
                .param("country", "Japão")
                .header("Authorization", validToken))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"))
            .andExpect(jsonPath("$.status").value(500));
    }

    // ─── Teste 31: país não mapeado / moeda local já é BRL → 204 sem corpo ─

    @Test
    void teste31_shouldReturn204_whenCountryHasNoLocalRateToShow() throws Exception {
        when(currencyService.getLocalRate(eq("Brasil"))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/currency/rates")
                .param("country", "Brasil")
                .header("Authorization", validToken))
            .andExpect(status().isNoContent());
    }
}
