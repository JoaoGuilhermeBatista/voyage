package luvoygroup.voyage.features.users;

import luvoygroup.voyage.TestJwtFactory;
import luvoygroup.voyage.domain.enums.BudgetTier;
import luvoygroup.voyage.domain.enums.GroupType;
import luvoygroup.voyage.domain.enums.TravelRhythm;
import luvoygroup.voyage.features.users.dto.UpdatePreferencesRequest;
import luvoygroup.voyage.features.users.dto.UserProfileDTO;
import luvoygroup.voyage.security.JwtService;
import luvoygroup.voyage.security.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Cobre PATCH /api/v1/users/preferences (edição de preferências pós-onboarding,
 * feature "editar preferências no Perfil") e GET /api/v1/users/me.
 *
 * NENHUM código de produção foi alterado para escrever estes testes — eles
 * validam o comportamento já existente de UserController/UserService/
 * UpdatePreferencesRequest, servindo de contrato para a futura tela de edição.
 */
@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean UserService userService;
    @MockBean JwtService jwtService;

    final String validToken = TestJwtFactory.bearer();

    @BeforeEach
    void setupJwt() {
        when(jwtService.isValid(anyString())).thenReturn(true);
        when(jwtService.extractUserId(anyString())).thenReturn(TestJwtFactory.TEST_USER_ID.toString());
        when(jwtService.extractEmail(anyString())).thenReturn(TestJwtFactory.TEST_EMAIL);
    }

    // ─── Teste 22: PATCH /users/preferences com payload válido → 200 ─────────

    @Test
    void teste22_shouldReturn200_whenPreferencesAreValid() throws Exception {
        mockMvc.perform(patch("/api/v1/users/preferences")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "budget": "MODERATE",
                      "rhythm": "RELAXED",
                      "groupType": "COUPLE",
                      "interests": ["praia", "gastronomia"]
                    }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Preferences updated"));

        ArgumentCaptor<UpdatePreferencesRequest> captor = ArgumentCaptor.forClass(UpdatePreferencesRequest.class);
        verify(userService).updatePreferences(eq(TestJwtFactory.TEST_USER_ID), captor.capture());

        UpdatePreferencesRequest sent = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(BudgetTier.MODERATE, sent.budget());
        org.junit.jupiter.api.Assertions.assertEquals(TravelRhythm.RELAXED, sent.rhythm());
        org.junit.jupiter.api.Assertions.assertEquals(GroupType.COUPLE, sent.groupType());
        org.junit.jupiter.api.Assertions.assertEquals(List.of("praia", "gastronomia"), sent.interests());
    }

    // ─── Teste 23: PATCH /users/preferences sem budget → 400 ─────────────────

    @Test
    void teste23_shouldReturn400_whenBudgetIsMissing() throws Exception {
        mockMvc.perform(patch("/api/v1/users/preferences")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "rhythm": "RELAXED",
                      "groupType": "COUPLE",
                      "interests": ["praia"]
                    }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    // ─── Teste 24: PATCH /users/preferences com interests vazio → 400 ────────

    @Test
    void teste24_shouldReturn400_whenInterestsIsEmpty() throws Exception {
        mockMvc.perform(patch("/api/v1/users/preferences")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "budget": "MODERATE",
                      "rhythm": "RELAXED",
                      "groupType": "COUPLE",
                      "interests": []
                    }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    // ─── Teste 25: PATCH /users/preferences com valor de enum inválido → 400 ─
    // Um enum inválido no JSON falha na desserialização do Jackson
    // (HttpMessageNotReadableException), lançada ANTES da Bean Validation
    // rodar. O GlobalExceptionHandler agora possui um
    // @ExceptionHandler(HttpMessageNotReadableException.class) que trata esse
    // caso como 400 BAD_REQUEST, no mesmo formato de erro dos demais handlers.

    @Test
    void teste25_shouldReturn400_whenBudgetEnumValueIsInvalid() throws Exception {
        mockMvc.perform(patch("/api/v1/users/preferences")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "budget": "SUPER_LUXO",
                      "rhythm": "RELAXED",
                      "groupType": "COUPLE",
                      "interests": ["praia"]
                    }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    // ─── Teste 26: PATCH /users/preferences sem token → 401 ──────────────────

    @Test
    void teste26_shouldReturn401_whenNoTokenProvided() throws Exception {
        mockMvc.perform(patch("/api/v1/users/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "budget": "MODERATE",
                      "rhythm": "RELAXED",
                      "groupType": "COUPLE",
                      "interests": ["praia"]
                    }
                """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    // ─── Teste 27: GET /users/me retorna o perfil com as preferências atuais ─

    @Test
    void teste27_shouldReturnUserProfile_withCurrentPreferences_whenAuthenticated() throws Exception {
        UUID userId = TestJwtFactory.TEST_USER_ID;
        UserProfileDTO profile = new UserProfileDTO(
            userId,
            TestJwtFactory.TEST_EMAIL,
            "https://avatar.example.com/u.png",
            true,
            false,
            "MODERATE",
            "RELAXED",
            "COUPLE",
            List.of("praia", "gastronomia")
        );
        when(userService.upsertAndGetProfile(eq(userId), eq(TestJwtFactory.TEST_EMAIL)))
            .thenReturn(profile);

        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", validToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(userId.toString()))
            .andExpect(jsonPath("$.email").value(TestJwtFactory.TEST_EMAIL))
            .andExpect(jsonPath("$.budget").value("MODERATE"))
            .andExpect(jsonPath("$.rhythm").value("RELAXED"))
            .andExpect(jsonPath("$.groupType").value("COUPLE"))
            .andExpect(jsonPath("$.interests[0]").value("praia"))
            .andExpect(jsonPath("$.interests[1]").value("gastronomia"));
    }
}
