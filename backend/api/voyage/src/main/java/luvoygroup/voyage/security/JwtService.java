package luvoygroup.voyage.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Valida JWTs do Supabase Auth contra o JWKS público do projeto. As chaves
 * são indexadas por `kid` (o JWKS pode ter mais de uma durante uma rotação)
 * e, se o `kid` de um token não bater com nenhuma cacheada, busca o JWKS de
 * novo uma vez antes de rejeitar — evita derrubar login em massa só por ter
 * carregado a chave uma única vez no startup.
 */
@Service
@Slf4j
public class JwtService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile Map<String, PublicKey> keysByKid = Map.of();

    @Autowired
    public JwtService() {
        this(RestClient.create());
    }

    /** Construtor package-private pra teste — ver WikimediaCommonsService. */
    JwtService(RestClient restClient) {
        this.restClient = restClient;
    }

    @PostConstruct
    public void init() {
        refreshKeys();
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
            log.warn("[JWT] Validação falhou: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(resolveKey(token))
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private PublicKey resolveKey(String token) {
        String kid = extractKid(token);
        PublicKey key = keysByKid.get(kid);
        if (key == null) {
            refreshKeys();
            key = keysByKid.get(kid);
        }
        if (key == null)
            throw new JwtException("Nenhuma chave do JWKS da Supabase corresponde ao kid '" + kid + "'");
        return key;
    }

    private String extractKid(String token) {
        int firstDot = token.indexOf('.');
        if (firstDot < 0)
            throw new MalformedJwtException("Token sem header JWT");

        String headerJson = new String(
            Base64.getUrlDecoder().decode(token.substring(0, firstDot)), StandardCharsets.UTF_8
        );
        try {
            JsonNode kid = objectMapper.readTree(headerJson).get("kid");
            if (kid == null)
                throw new MalformedJwtException("Token sem campo 'kid' no header");
            return kid.asText();
        } catch (JsonProcessingException e) {
            throw new MalformedJwtException("Header do token não é JSON válido", e);
        }
    }

    private synchronized void refreshKeys() {
        String jwksUrl = supabaseUrl + "/auth/v1/.well-known/jwks.json";
        try {
            String body = restClient.get().uri(jwksUrl).retrieve().body(String.class);
            JsonNode keys = objectMapper.readTree(body).get("keys");

            Map<String, PublicKey> loaded = new HashMap<>();
            for (JsonNode key : keys) {
                loaded.put(key.get("kid").asText(), toEcPublicKey(key));
            }
            if (loaded.isEmpty())
                throw new IllegalStateException("JWKS não retornou nenhuma chave");

            keysByKid = Map.copyOf(loaded);
            log.info("[JWT] {} chave(s) EC carregada(s) do JWKS da Supabase ({})", loaded.size(), jwksUrl);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Supabase public keys from JWKS", e);
        }
    }

    private PublicKey toEcPublicKey(JsonNode key) throws GeneralSecurityException {
        BigInteger x = new BigInteger(1, Base64.getUrlDecoder().decode(key.get("x").asText()));
        BigInteger y = new BigInteger(1, Base64.getUrlDecoder().decode(key.get("y").asText()));

        AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
        parameters.init(new ECGenParameterSpec("secp256r1"));
        ECParameterSpec ecParams = parameters.getParameterSpec(ECParameterSpec.class);

        return KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(new ECPoint(x, y), ecParams));
    }
}
