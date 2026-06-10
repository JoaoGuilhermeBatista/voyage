package luvoygroup.voyage.features.auth;

import luvoygroup.voyage.domain.User;
import luvoygroup.voyage.exception.EmailAlreadyExistsException;
import luvoygroup.voyage.exception.InvalidCredentialsException;
import luvoygroup.voyage.features.auth.dto.AuthResponse;
import luvoygroup.voyage.features.auth.dto.UserDTO;
import luvoygroup.voyage.repository.UserRepository;
import luvoygroup.voyage.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse signup(String name, String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }

        User user = User.builder()
            .name(name)
            .email(email)
            .passwordHash(passwordEncoder.encode(password))
            .build();

        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, UserDTO.from(user));
    }

    public AuthResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, UserDTO.from(user));
    }
}
