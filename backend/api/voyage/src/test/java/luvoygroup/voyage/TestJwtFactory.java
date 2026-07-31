package luvoygroup.voyage;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

public class TestJwtFactory {

    // Same secret as application-test.yaml
    private static final String SECRET = "bHV2b3ktdGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3Rz";

    public static final UUID TEST_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final String TEST_EMAIL = "test@luvoy.com";

    public static String validToken() {
        return validToken(TEST_USER_ID, TEST_EMAIL);
    }

    public static String validToken(UUID userId, String email) {
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET));
        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 3600_000))
            .signWith(key)
            .compact();
    }

    public static String bearer() {
        return "Bearer " + validToken();
    }

    public static String bearer(UUID userId, String email) {
        return "Bearer " + validToken(userId, email);
    }
}
