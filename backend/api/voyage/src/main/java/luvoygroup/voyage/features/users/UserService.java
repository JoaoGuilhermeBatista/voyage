package luvoygroup.voyage.features.users;

import luvoygroup.voyage.domain.OnboardingPreferences;
import luvoygroup.voyage.domain.User;
import luvoygroup.voyage.exception.ResourceNotFoundException;
import luvoygroup.voyage.features.users.dto.UpdatePreferencesRequest;
import luvoygroup.voyage.features.users.dto.UserProfileDTO;
import luvoygroup.voyage.repository.OnboardingPreferencesRepository;
import luvoygroup.voyage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OnboardingPreferencesRepository prefsRepository;

    @Transactional
    public UserProfileDTO upsertAndGetProfile(UUID supabaseId, String email) {
        User user = userRepository.findById(supabaseId).orElseGet(() ->
            userRepository.save(User.builder()
                .id(supabaseId)
                .email(email)
                .name(email.split("@")[0])
                .build())
        );
        OnboardingPreferences prefs = prefsRepository.findByUserId(user.getId()).orElse(null);
        return UserProfileDTO.fromUserAndPrefs(user, prefs);
    }

    @Transactional
    public void updatePreferences(UUID userId, UpdatePreferencesRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(ResourceNotFoundException::new);

        OnboardingPreferences prefs = prefsRepository.findByUserId(userId)
            .orElse(OnboardingPreferences.builder().user(user).build());

        prefs.setBudget(request.budget());
        prefs.setRhythm(request.rhythm());
        prefs.setGroupType(request.groupType());
        prefs.setInterests(request.interests());
        prefs.setReligion(request.religion());

        prefsRepository.save(prefs);

        user.setOnboardingCompleted(true);
        userRepository.save(user);
    }
}
