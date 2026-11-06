package luvoygroup.voyage.security;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Acessado via construtor package-private (mesmo padrão de
 * WikimediaCommonsServiceTest) — mocka a cadeia RestClient.get().uri(...)
 * .retrieve().body(String.class) pra simular o JWKS da Supabase, com chaves
 * EC (secp256r1) geradas na hora em vez de uma chave real.
 */
class JwtServiceTest {

    @SuppressWarnings({"unchecked", "rawtypes"})
    private RestClient restClientReturning(String firstJson, String... nextJson) {
        RestClient mockRestClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(mockRestClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(firstJson, nextJson);
        return mockRestClient;
    }

    private KeyPair generateEcKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        return kpg.generateKeyPair();
    }

    /** Converte a coordenada pra 32 bytes big-endian (tamanho fixo do P-256), base64url. */
    private String base64Url(BigInteger value) {
        byte[] raw = value.toByteArray();
        byte[] fixed = new byte[32];
        int srcPos = Math.max(0, raw.length - 32);
        int dstPos = Math.max(0, 32 - raw.length);
        System.arraycopy(raw, srcPos, fixed, dstPos, raw.length - srcPos);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(fixed);
    }

    private String keyEntryJson(String kid, ECPublicKey publicKey) {
        return """
            {"kid": "%s", "kty": "EC", "crv": "P-256", "x": "%s", "y": "%s"}
            """.formatted(
                kid,
                base64Url(publicKey.getW().getAffineX()),
                base64Url(publicKey.getW().getAffineY())
            );
    }

    private String jwksWith(String... keyEntries) {
        return "{\"keys\": [" + String.join(",", keyEntries) + "]}";
    }

    private String signedToken(String kid, KeyPair pair, String subject) {
        return Jwts.builder()
            .setHeaderParam("kid", kid)
            .subject(subject)
            .signWith(pair.getPrivate())
            .compact();
    }

    @Test
    void isValid_returnsTrue_forTokenSignedWithKeyMatchingItsKid() throws Exception {
        KeyPair pair = generateEcKeyPair();
        JwtService service = new JwtService(
            restClientReturning(jwksWith(keyEntryJson("key-1", (ECPublicKey) pair.getPublic())))
        );
        service.init();

        String token = signedToken("key-1", pair, "user-1");

        assertThat(service.isValid(token)).isTrue();
        assertThat(service.extractUserId(token)).isEqualTo("user-1");
    }

    @Test
    void isValid_returnsFalse_whenTokenSignedByADifferentKeyThanItsKidClaims() throws Exception {
        KeyPair realPair = generateEcKeyPair();
        KeyPair attackerPair = generateEcKeyPair();
        JwtService service = new JwtService(
            restClientReturning(jwksWith(keyEntryJson("key-1", (ECPublicKey) realPair.getPublic())))
        );
        service.init();

        String forgedToken = signedToken("key-1", attackerPair, "user-1");

        assertThat(service.isValid(forgedToken)).isFalse();
    }

    @Test
    void resolvesNewKey_byRefetchingJwksOnce_whenTokenKidIsUnknown_simulatingKeyRotation() throws Exception {
        KeyPair oldPair = generateEcKeyPair();
        KeyPair newPair = generateEcKeyPair();
        String onlyOldKey = jwksWith(keyEntryJson("old-key", (ECPublicKey) oldPair.getPublic()));
        String bothKeys = jwksWith(
            keyEntryJson("old-key", (ECPublicKey) oldPair.getPublic()),
            keyEntryJson("new-key", (ECPublicKey) newPair.getPublic())
        );

        JwtService service = new JwtService(restClientReturning(onlyOldKey, bothKeys));
        service.init();

        String tokenFromRotatedKey = signedToken("new-key", newPair, "user-2");

        assertThat(service.isValid(tokenFromRotatedKey)).isTrue();
    }
}
