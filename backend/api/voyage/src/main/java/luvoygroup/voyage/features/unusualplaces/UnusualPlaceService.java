package luvoygroup.voyage.features.unusualplaces;

import luvoygroup.voyage.domain.Destination;
import luvoygroup.voyage.domain.Poi;
import luvoygroup.voyage.domain.UnusualPlace;
import luvoygroup.voyage.domain.User;
import luvoygroup.voyage.exception.ResourceNotFoundException;
import luvoygroup.voyage.features.admin.LlmService;
import luvoygroup.voyage.features.unusualplaces.dto.GeneratedUnusualPlace;
import luvoygroup.voyage.features.unusualplaces.dto.UnusualPlaceDTO;
import luvoygroup.voyage.features.unusualplaces.dto.UnusualPlacesWrapper;
import luvoygroup.voyage.repository.DestinationRepository;
import luvoygroup.voyage.repository.PoiRepository;
import luvoygroup.voyage.repository.UnusualPlaceRepository;
import luvoygroup.voyage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UnusualPlaceService {

    private final LlmService llmService;
    private final DestinationRepository destinationRepo;
    private final PoiRepository poiRepo;
    private final UnusualPlaceRepository unusualPlaceRepo;
    private final UserRepository userRepo;

    /**
     * Gera (ou regenera do zero, substituindo o que existir) a lista de
     * lugares incomuns de um destino via LLM. Admin-triggered, mesmo padrão
     * de {@code AdminService#generateAndSavePoisForDestination} — roda uma
     * vez, salva no banco, nunca chama o LLM de novo numa leitura de usuário
     * final.
     */
    @Transactional
    public int generateForDestination(String slug) {
        Destination destination = destinationRepo.findBySlug(slug)
            .orElseThrow(ResourceNotFoundException::new);

        List<String> existingPoiTitles = poiRepo.findAllByDestinationIdAndActiveTrue(destination.getId()).stream()
            .map(Poi::getTitle)
            .toList();

        UnusualPlacesWrapper wrapper = llmService.generateUnusualPlaces(
            destination.getCity(), destination.getCountry(), existingPoiTitles);

        if (wrapper == null || wrapper.places() == null || wrapper.places().isEmpty())
            throw new IllegalStateException("GPT não retornou lugares incomuns para " + slug);

        unusualPlaceRepo.deleteAll(unusualPlaceRepo.findAllByDestinationIdOrderByOrderIndexAsc(destination.getId()));

        List<GeneratedUnusualPlace> generated = wrapper.places();
        List<UnusualPlace> places = new java.util.ArrayList<>(generated.size());
        for (int i = 0; i < generated.size(); i++) {
            places.add(toEntity(generated.get(i), destination, i));
        }

        unusualPlaceRepo.saveAll(places);
        return places.size();
    }

    private UnusualPlace toEntity(GeneratedUnusualPlace gp, Destination destination, int orderIndex) {
        return UnusualPlace.builder()
            .destination(destination)
            .title(gp.title())
            .description(gp.description())
            .category(gp.category())
            .orderIndex(orderIndex)
            .build();
    }

    /**
     * Leitura pro usuário final — gated por premium
     * ({@link User#hasActivePremium()}): sem premium, só o título vem (teaser
     * de upsell); com premium, conteúdo completo.
     */
    public List<UnusualPlaceDTO> findForUser(String slug, UUID userId) {
        Destination destination = destinationRepo.findBySlug(slug)
            .filter(Destination::isActive)
            .orElseThrow(ResourceNotFoundException::new);

        User user = userRepo.findById(userId).orElseThrow(ResourceNotFoundException::new);
        boolean premium = user.hasActivePremium();

        return unusualPlaceRepo.findAllByDestinationIdOrderByOrderIndexAsc(destination.getId()).stream()
            .map(place -> premium ? UnusualPlaceDTO.unlocked(place) : UnusualPlaceDTO.teaser(place))
            .toList();
    }
}
