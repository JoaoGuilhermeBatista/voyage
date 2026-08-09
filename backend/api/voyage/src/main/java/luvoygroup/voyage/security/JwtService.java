package luvoygroup.voyage.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.util.Base64;

@Service
public class JwtService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    private volatile PublicKey publicKey;

    @PostConstruct
    public void init() {
        try {
            String jwksUrl = supabaseUrl + "/auth/v1/.well-known/jwks.json";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(jwksUrl))
                .GET()
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());
            JsonNode key = root.get("keys").get(0);

            byte[] xBytes = Base64.getUrlDecoder().decode(key.get("x").asText());
            byte[] yBytes = Base64.getUrlDecoder().decode(key.get("y").asText());

            BigInteger x = new BigInteger(1, xBytes);
            BigInteger y = new BigInteger(1, yBytes);

            AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
            parameters.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec ecParams = parameters.getParameterSpec(ECParameterSpec.class);

            ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(new ECPoint(x, y), ecParams);
            publicKey = KeyFactory.getInstance("EC").generatePublic(publicKeySpec);

            System.out.println("[JWT] EC public key loaded from Supabase JWKS (" + jwksUrl + ")");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Supabase public key from JWKS", e);
        }
    }

    public String extractUserId(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractEmail(String token) {
        return (String) parseClaims(token).get("email");
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            System.err.println("[JWT] Validation failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(publicKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
