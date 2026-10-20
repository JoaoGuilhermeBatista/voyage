package luvoygroup.voyage.features.unusualplaces;

import luvoygroup.voyage.TestJwtFactory;
import luvoygroup.voyage.exception.ResourceNotFoundException;
import luvoygroup.voyage.features.unusualplaces.dto.UnusualPlaceDTO;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UnusualPlaceController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class UnusualPlaceControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean UnusualPlaceService unusualPlaceService;
    @MockBean JwtService jwtService;

    final String validToken = TestJwtFactory.bearer();

    @BeforeEach
    void setupJwt() {
        when(jwtService.isValid(anyString())).thenReturn(true);
        when(jwtService.extractUserId(anyString())).thenReturn(TestJwtFactory.TEST_USER_ID.toString());
        when(jwtService.extractEmail(anyString())).thenReturn(TestJwtFactory.TEST_EMAIL);
    }

    @Test
    void shouldReturnLockedTeaser_whenUserHasNoPremium() throws Exception {
        when(unusualPlaceService.findForUser(eq("paris"), any()))
            .thenReturn(List.of(new UnusualPlaceDTO("Bar Escondido", null, null, true)));

        mockMvc.perform(get("/api/v1/unusual-places/paris")
                .header("Authorization", validToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Bar Escondido"))
            .andExpect(jsonPath("$[0].locked").value(true))
            .andExpect(jsonPath("$[0].description").doesNotExist());
    }

    @Test
    void shouldReturnFullContent_whenUserHasPremium() throws Exception {
        when(unusualPlaceService.findForUser(eq("paris"), any()))
            .thenReturn(List.of(new UnusualPlaceDTO("Bar Escondido", "Descrição completa.", "Bar escondido", false)));

        mockMvc.perform(get("/api/v1/unusual-places/paris")
                .header("Authorization", validToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].locked").value(false))
            .andExpect(jsonPath("$[0].description").value("Descrição completa."));
    }

    @Test
    void shouldReturn401_whenNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/unusual-places/paris"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn404_whenDestinationNotFound() throws Exception {
        when(unusualPlaceService.findForUser(eq("inexistente"), any()))
            .thenThrow(new ResourceNotFoundException());

        mockMvc.perform(get("/api/v1/unusual-places/inexistente")
                .header("Authorization", validToken))
            .andExpect(status().isNotFound());
    }
}
