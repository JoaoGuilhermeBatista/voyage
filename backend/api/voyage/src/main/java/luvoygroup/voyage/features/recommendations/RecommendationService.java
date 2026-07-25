package luvoygroup.voyage.features.recommendations;

import luvoygroup.voyage.features.recommendations.dto.RecommendationDTO;
import luvoygroup.voyage.repository.DestinationRepository;
import luvoygroup.voyage.repository.OnboardingPreferencesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final DestinationRepository destinationRepo;
    private final OnboardingPreferencesRepository preferencesRepo;

    @Transactional(readOnly = true)
    public List<RecommendationDTO> recommend(UUID userId) {
        var prefsOpt = preferencesRepo.findByUserId(userId);

        Set<String> interests = prefsOpt
            .map(p -> p.getInterests().stream().map(String::toLowerCase).collect(Collectors.toSet()))
            .orElse(Collections.emptySet());

        return destinationRepo.findAllByActiveTrue().stream()
            .map(d -> {
                int score = (int) d.getTags().stream()
                    .filter(tag -> interests.contains(tag.toLowerCase()))
                    .count();
                return RecommendationDTO.from(d, score);
            })
            .sorted(Comparator.comparingInt(RecommendationDTO::matchScore).reversed())
            .limit(10)
            .toList();
    }
}
