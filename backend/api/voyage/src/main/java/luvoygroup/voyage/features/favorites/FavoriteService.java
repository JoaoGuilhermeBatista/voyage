package luvoygroup.voyage.features.favorites;

import luvoygroup.voyage.domain.Favorite;
import luvoygroup.voyage.domain.User;
import luvoygroup.voyage.exception.ForbiddenAccessException;
import luvoygroup.voyage.exception.ResourceNotFoundException;
import luvoygroup.voyage.features.favorites.dto.FavoriteListItemDTO;
import luvoygroup.voyage.features.favorites.dto.FavoriteRequest;
import luvoygroup.voyage.features.favorites.dto.FavoriteResponse;
import luvoygroup.voyage.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final DestinationRepository destinationRepository;
    private final PoiRepository poiRepository;
    private final ItineraryRepository itineraryRepository;

    public FavoriteResponse create(UUID userId, FavoriteRequest request) {
        int targetCount = 0;
        if (request.destinationId() != null) targetCount++;
        if (request.poiId() != null) targetCount++;
        if (request.itineraryId() != null) targetCount++;

        if (targetCount == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nenhum target definido");
        }
        if (targetCount > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Apenas um target permitido por favorito");
        }

        User user = userRepository.findById(userId).orElseThrow(ResourceNotFoundException::new);

        Favorite.FavoriteBuilder builder = Favorite.builder().user(user);

        if (request.destinationId() != null) {
            builder.destination(
                destinationRepository.findById(request.destinationId())
                    .orElseThrow(ResourceNotFoundException::new)
            );
        } else if (request.poiId() != null) {
            builder.poi(
                poiRepository.findById(request.poiId())
                    .orElseThrow(ResourceNotFoundException::new)
            );
        } else {
            builder.itinerary(
                itineraryRepository.findByIdAndDeletedAtIsNull(request.itineraryId())
                    .orElseThrow(ResourceNotFoundException::new)
            );
        }

        return FavoriteResponse.from(favoriteRepository.save(builder.build()));
    }

    public List<FavoriteListItemDTO> findAll(UUID userId) {
        return favoriteRepository.findAllByUserId(userId).stream()
            .map(FavoriteListItemDTO::from)
            .toList();
    }

    public void delete(UUID userId, UUID favoriteId) {
        Favorite favorite = favoriteRepository.findById(favoriteId)
            .orElseThrow(ResourceNotFoundException::new);

        if (!favorite.getUser().getId().equals(userId)) {
            throw new ForbiddenAccessException();
        }

        favoriteRepository.delete(favorite);
    }
}
