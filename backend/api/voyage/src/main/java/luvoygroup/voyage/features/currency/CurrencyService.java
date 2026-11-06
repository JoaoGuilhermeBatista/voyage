package luvoygroup.voyage.features.currency;

import com.fasterxml.jackson.annotation.JsonProperty;
import luvoygroup.voyage.features.currency.dto.LocalCurrencyRateDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class CurrencyService {

    private final RestClient restClient;
    private final String apiKey;

    public CurrencyService(@Value("${exchange-rate.api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.create();
    }

    /**
     * Nome do país (igual ao campo {@code Destination.country} no banco,
     * em português) → código ISO 4217 da moeda local — cobre os países de
     * todos os destinos ativos hoje. País fora dessa lista, ou cuja moeda
     * já seja BRL (ex.: Brasil — não faz sentido converter BRL pra BRL),
     * não tem cotação local pra mostrar.
     */
    private static final Map<String, String> COUNTRY_TO_CURRENCY = Map.ofEntries(
        Map.entry("África do Sul", "ZAR"),
        Map.entry("Albânia", "ALL"),
        Map.entry("Alemanha", "EUR"),
        Map.entry("Argentina", "ARS"),
        Map.entry("Austrália", "AUD"),
        Map.entry("Áustria", "EUR"),
        Map.entry("Bélgica", "EUR"),
        Map.entry("Bósnia e Herzegovina", "BAM"),
        Map.entry("Brasil", "BRL"),
        Map.entry("Bulgária", "BGN"),
        Map.entry("Chile", "CLP"),
        Map.entry("Chipre", "EUR"),
        Map.entry("Colômbia", "COP"),
        Map.entry("Coreia do Sul", "KRW"),
        Map.entry("Croácia", "EUR"),
        Map.entry("Dinamarca", "DKK"),
        Map.entry("Egito", "EGP"),
        Map.entry("Emirados Árabes", "AED"),
        Map.entry("Escócia", "GBP"),
        Map.entry("Eslováquia", "EUR"),
        Map.entry("Eslovênia", "EUR"),
        Map.entry("Espanha", "EUR"),
        Map.entry("Estados Unidos", "USD"), // mesmo país que "EUA" (duplicata conhecida na base, ver luvoy-guia-mestre-pdfs)
        Map.entry("Estônia", "EUR"),
        Map.entry("EUA", "USD"),
        Map.entry("Finlândia", "EUR"),
        Map.entry("França", "EUR"),
        Map.entry("Grécia", "EUR"),
        Map.entry("Holanda", "EUR"),
        Map.entry("Hungria", "HUF"),
        Map.entry("Indonésia", "IDR"),
        Map.entry("Irlanda", "EUR"),
        Map.entry("Islândia", "ISK"),
        Map.entry("Itália", "EUR"),
        Map.entry("Japão", "JPY"),
        Map.entry("Letônia", "EUR"),
        Map.entry("Lituânia", "EUR"),
        Map.entry("Luxemburgo", "EUR"),
        Map.entry("Malta", "EUR"),
        Map.entry("Marrocos", "MAD"),
        Map.entry("México", "MXN"),
        Map.entry("Mônaco", "EUR"),
        Map.entry("Montenegro", "EUR"),
        Map.entry("Noruega", "NOK"),
        Map.entry("Peru", "PEN"),
        Map.entry("Polônia", "PLN"),
        Map.entry("Portugal", "EUR"),
        Map.entry("Reino Unido", "GBP"),
        Map.entry("República Tcheca", "CZK"),
        Map.entry("Romênia", "RON"),
        Map.entry("Sérvia", "RSD"),
        Map.entry("Singapura", "SGD"),
        Map.entry("Suécia", "SEK"),
        Map.entry("Suíça", "CHF"),
        Map.entry("Tailândia", "THB"),
        Map.entry("Tanzânia", "TZS"),
        Map.entry("Turquia", "TRY"),
        Map.entry("Vietnã", "VND")
    );

    /** Fallback manual só quando a API key não está configurada; não ampliar, fica desatualizado. */
    private static final Map<String, BigDecimal> OFFLINE_FALLBACK_RATES = Map.of(
        "EUR", new BigDecimal("5.90"),
        "GBP", new BigDecimal("6.85"),
        "USD", new BigDecimal("5.55")
    );

    /**
     * Cotação da moeda local de {@code country} convertida pra BRL.
     * {@code Optional.empty()} quando o país não está mapeado, a moeda
     * local já é BRL, ou a API de câmbio não tem essa moeda — nesses
     * casos o chamador simplesmente não mostra a barra de câmbio.
     */
    @Cacheable("currency-rates")
    public Optional<LocalCurrencyRateDTO> getLocalRate(String country) {
        String normalizedCountry = country == null ? "" : country.trim();
        String currencyCode = COUNTRY_TO_CURRENCY.get(normalizedCountry);
        if (currencyCode == null) {
            log.warn("País sem moeda local mapeada em COUNTRY_TO_CURRENCY: \"{}\"", normalizedCountry);
            return Optional.empty();
        }
        if ("BRL".equals(currencyCode)) {
            return Optional.empty();
        }

        if ("not-configured".equals(apiKey)) {
            BigDecimal fallback = OFFLINE_FALLBACK_RATES.get(currencyCode);
            if (fallback != null) {
                log.warn("ExchangeRate API key not configured — usando fallback aproximado para {}", currencyCode);
                return Optional.of(new LocalCurrencyRateDTO(currencyCode, fallback));
            }
            log.warn("ExchangeRate API key not configured e sem fallback para {} — sem cotação local disponível",
                currencyCode);
            return Optional.empty();
        }

        log.info("Fetching fresh currency rate ({}) from ExchangeRate API", currencyCode);
        ExchangeRateResponse response = restClient.get()
            .uri("https://v6.exchangerate-api.com/v6/{key}/latest/BRL", apiKey)
            .retrieve()
            .body(ExchangeRateResponse.class);

        if (response == null || response.conversionRates() == null) {
            throw new IllegalStateException("Resposta inválida da API de câmbio");
        }

        Double rate = response.conversionRates().get(currencyCode);
        if (rate == null) {
            return Optional.empty();
        }
        return Optional.of(new LocalCurrencyRateDTO(currencyCode, invertRate(rate)));
    }

    // conversionRates from BRL base: 1 BRL = X <moeda> → inverte pra 1 <moeda> = Y BRL
    private BigDecimal invertRate(double rate) {
        if (rate == 0) return BigDecimal.ZERO;
        return BigDecimal.ONE.divide(BigDecimal.valueOf(rate), new MathContext(4));
    }

    private record ExchangeRateResponse(
        @JsonProperty("conversion_rates") Map<String, Double> conversionRates
    ) {}
}
