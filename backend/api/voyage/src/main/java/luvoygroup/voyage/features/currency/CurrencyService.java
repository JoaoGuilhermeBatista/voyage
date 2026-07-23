package luvoygroup.voyage.features.currency;

import com.fasterxml.jackson.annotation.JsonProperty;
import luvoygroup.voyage.features.currency.dto.CurrencyRatesDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Map;

@Service
@Slf4j
public class CurrencyService {

    private final RestClient restClient;
    private final String apiKey;

    public CurrencyService(@Value("${exchange-rate.api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.create();
    }

    @Cacheable("currency-rates")
    public CurrencyRatesDTO getRates() {
        if ("not-configured".equals(apiKey)) {
            log.warn("ExchangeRate API key not configured — returning fallback rates");
            return new CurrencyRatesDTO(
                new BigDecimal("5.90"),
                new BigDecimal("6.85"),
                new BigDecimal("5.55")
            );
        }

        log.info("Fetching fresh currency rates from ExchangeRate API");
        ExchangeRateResponse response = restClient.get()
            .uri("https://v6.exchangerate-api.com/v6/{key}/latest/BRL", apiKey)
            .retrieve()
            .body(ExchangeRateResponse.class);

        if (response == null || response.conversionRates() == null) {
            throw new IllegalStateException("Resposta inválida da API de câmbio");
        }

        Map<String, Double> rates = response.conversionRates();
        return new CurrencyRatesDTO(
            invertRate(rates.get("EUR")),
            invertRate(rates.get("GBP")),
            invertRate(rates.get("USD"))
        );
    }

    // conversionRates from BRL base: 1 BRL = X EUR → invert to get 1 EUR = Y BRL
    private BigDecimal invertRate(Double rate) {
        if (rate == null || rate == 0) return BigDecimal.ZERO;
        return BigDecimal.ONE.divide(BigDecimal.valueOf(rate), new MathContext(4));
    }

    private record ExchangeRateResponse(
        String result,
        @JsonProperty("conversion_rates") Map<String, Double> conversionRates
    ) {}
}
