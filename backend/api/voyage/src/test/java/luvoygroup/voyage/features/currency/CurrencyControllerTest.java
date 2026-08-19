package luvoygroup.voyage.features.currency;

import luvoygroup.voyage.TestJwtFactory;
import luvoygroup.voyage.features.currency.dto.CurrencyRatesDTO;
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

import static org.mockito.ArgumentMatchers.anyString;
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

    // ─── Teste 28: GET /currency/rates com token válido → 200 e shape do DTO ──

    @Test
    void teste28_shouldReturnRates_whenAuthenticated() throws Exception {
        when(currencyService.getRates()).thenReturn(new CurrencyRatesDTO(
            new BigDecimal("5.90"),
            new BigDecimal("6.85"),
            new BigDecimal("5.55")
        ));

        mockMvc.perform(get("/api/v1/currency/rates")
                .header("Authorization", validToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.eurToBrl").value(5.90))
            .andExpect(jsonPath("$.gbpToBrl").value(6.85))
            .andExpect(jsonPath("$.usdToBrl").value(5.55));
    }

    // ─── Teste 29: GET /currency/rates sem autenticação → 200 (endpoint público) ─
    // SecurityConfig.filterChain permite GET /api/v1/currency/** via permitAll(),
    // já que são dados não sensíveis (taxas de câmbio). Sem header Authorization,
    // o request nunca passa pelo JwtAuthFilter de forma bloqueante e chega ao
    // controller normalmente.

    @Test
    void teste29_shouldReturnRates_whenNotAuthenticated() throws Exception {
        when(currencyService.getRates()).thenReturn(new CurrencyRatesDTO(
            new BigDecimal("5.90"),
            new BigDecimal("6.85"),
            new BigDecimal("5.55")
        ));

        mockMvc.perform(get("/api/v1/currency/rates"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.eurToBrl").value(5.90));
    }

    // ─── Teste 30: falha no CurrencyService → 500 tratado pelo GlobalExceptionHandler ─
    // CurrencyService.getRates() lança IllegalStateException quando a resposta da API
    // externa é inválida (ver CurrencyService.java linha 44-46) e não há fallback além
    // do caso "api-key not-configured". O GlobalExceptionHandler.handleGeneric captura
    // qualquer Exception não mapeada e retorna 500 com um ErrorResponse estruturado
    // (não um 500 cru) — este é o comportamento atualmente esperado/documentado.

    @Test
    void teste30_shouldReturn500WithStructuredBody_whenServiceThrows() throws Exception {
        when(currencyService.getRates())
            .thenThrow(new IllegalStateException("Resposta inválida da API de câmbio"));

        mockMvc.perform(get("/api/v1/currency/rates")
                .header("Authorization", validToken))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"))
            .andExpect(jsonPath("$.status").value(500));
    }
}
