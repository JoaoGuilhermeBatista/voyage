package luvoygroup.voyage.features.currency;

import luvoygroup.voyage.features.currency.dto.LocalCurrencyRateDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CurrencyServiceTest {

    // ─── Teste 1: "Estados Unidos" e "EUA" são o mesmo país (duplicata conhecida
    // na base, ver luvoy-guia-mestre-pdfs) — os dois têm que resolver pra USD,
    // senão destinos como Orlando (country="Estados Unidos") ficam sem cotação
    // enquanto destinos como Nova York (country="EUA") funcionam normalmente ──

    @Test
    void getLocalRate_resolvesEstadosUnidosAndEuaToSameCurrency_whenApiKeyNotConfigured() {
        CurrencyService service = new CurrencyService("not-configured");

        Optional<LocalCurrencyRateDTO> estadosUnidos = service.getLocalRate("Estados Unidos");
        Optional<LocalCurrencyRateDTO> eua = service.getLocalRate("EUA");

        assertThat(estadosUnidos).isPresent();
        assertThat(eua).isPresent();
        assertThat(estadosUnidos.get().currencyCode()).isEqualTo("USD");
        assertThat(eua.get().currencyCode()).isEqualTo("USD");
    }

    // ─── Teste 2: país com espaço sobrando (ex. vindo de input mal higienizado)
    // ainda resolve corretamente — normalização via trim() ──

    @Test
    void getLocalRate_normalizesCountryWhitespace() {
        CurrencyService service = new CurrencyService("not-configured");

        Optional<LocalCurrencyRateDTO> result = service.getLocalRate("  EUA  ");

        assertThat(result).isPresent();
        assertThat(result.get().currencyCode()).isEqualTo("USD");
    }

    // ─── Teste 3: país genuinamente fora do mapa → Optional.empty(), sem lançar
    // exceção (o controller depende disso pra devolver 204) ──

    @Test
    void getLocalRate_returnsEmpty_whenCountryIsNotMapped() {
        CurrencyService service = new CurrencyService("not-configured");

        Optional<LocalCurrencyRateDTO> result = service.getLocalRate("País Inexistente");

        assertThat(result).isEmpty();
    }

    // ─── Teste 4: Brasil nunca tem cotação (moeda local já é BRL) ──

    @Test
    void getLocalRate_returnsEmpty_forBrasil() {
        CurrencyService service = new CurrencyService("not-configured");

        Optional<LocalCurrencyRateDTO> result = service.getLocalRate("Brasil");

        assertThat(result).isEmpty();
    }

    // ─── Teste 5: sem API key configurada, as 3 moedas mais comuns (EUR/GBP/USD)
    // caem no fallback aproximado em vez de desaparecer — rede de segurança
    // equivalente à que existia antes desta classe passar a cobrir qualquer
    // moeda por país ──

    @Test
    void getLocalRate_usesOfflineFallback_forCommonCurrencies_whenApiKeyNotConfigured() {
        CurrencyService service = new CurrencyService("not-configured");

        Optional<LocalCurrencyRateDTO> eur = service.getLocalRate("França");
        Optional<LocalCurrencyRateDTO> gbp = service.getLocalRate("Reino Unido");
        Optional<LocalCurrencyRateDTO> usd = service.getLocalRate("EUA");

        assertThat(eur).isPresent();
        assertThat(eur.get().currencyCode()).isEqualTo("EUR");
        assertThat(eur.get().rateToBrl()).isEqualTo(new BigDecimal("5.90"));

        assertThat(gbp).isPresent();
        assertThat(gbp.get().rateToBrl()).isEqualTo(new BigDecimal("6.85"));

        assertThat(usd).isPresent();
        assertThat(usd.get().rateToBrl()).isEqualTo(new BigDecimal("5.55"));
    }

    // ─── Teste 6: sem API key configurada, uma moeda fora do fallback (ex. JPY)
    // continua sem cotação — não dá pra fabricar uma taxa confiável pras ~40
    // moedas restantes, fica Optional.empty() como antes ──

    @Test
    void getLocalRate_returnsEmpty_forUncommonCurrency_whenApiKeyNotConfiguredAndNoFallback() {
        CurrencyService service = new CurrencyService("not-configured");

        Optional<LocalCurrencyRateDTO> result = service.getLocalRate("Japão");

        assertThat(result).isEmpty();
    }
}
