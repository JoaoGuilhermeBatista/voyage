package luvoygroup.voyage.features.favorites;

import luvoygroup.voyage.domain.User;
import luvoygroup.voyage.features.favorites.dto.FavoriteRequest;
import luvoygroup.voyage.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock FavoriteRepository favoriteRepository;
    @Mock UserRepository userRepository;
    @Mock DestinationRepository destinationRepository;
    @Mock PoiRepository poiRepository;
    @Mock ItineraryRepository itineraryRepository;

    @InjectMocks FavoriteService favoriteService;

    final UUID userId = UUID.randomUUID();
    final User user = User.builder().id(userId).email("test@luvoy.com").build();

    // ─── Teste 17: Sem nenhum target → 400 ───────────────────────────────────

    @Test
    void teste17_shouldThrow400_whenFavoriteHasNoTarget() {
        FavoriteRequest request = new FavoriteRequest(null, null, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> favoriteService.create(userId, request));

        assertEquals(400, ex.getStatusCode().value());
    }

    // ─── Teste 18: Dois targets simultâneos → 400 ─────────────────────────────

    @Test
    void teste18_shouldThrow400_whenFavoriteHasMultipleTargets() {
        FavoriteRequest request = new FavoriteRequest(
            UUID.randomUUID(), UUID.randomUUID(), null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> favoriteService.create(userId, request));

        assertEquals(400, ex.getStatusCode().value());
    }

    // ─── Fluxo correto: favoritar destino com sucesso ─────────────────────────

    @Test
    void shouldSucceed_whenFavoritingWithSingleValidDestinationTarget() {
        UUID destinationId = UUID.randomUUID();
        FavoriteRequest request = new FavoriteRequest(destinationId, null, null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(destinationRepository.findById(destinationId))
            .thenReturn(Optional.of(
                luvoygroup.voyage.domain.Destination.builder()
                    .id(destinationId).name("Paris").slug("paris")
                    .country("França").city("Paris").tagline("t").active(true).build()
            ));
        when(favoriteRepository.save(org.mockito.ArgumentMatchers.any()))
            .thenAnswer(inv -> {
                luvoygroup.voyage.domain.Favorite f = inv.getArgument(0);
                f.setId(UUID.randomUUID());
                return f;
            });

        assertDoesNotThrow(() -> favoriteService.create(userId, request));
    }
}
