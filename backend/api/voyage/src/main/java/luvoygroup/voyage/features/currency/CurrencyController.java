package luvoygroup.voyage.features.currency;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import luvoygroup.voyage.features.currency.dto.LocalCurrencyRateDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/currency")
@RequiredArgsConstructor
@Tag(name = "Currency", description = "Cotação da moeda local de um destino, convertida para BRL")
public class CurrencyController {

    private final CurrencyService currencyService;

    @GetMapping("/rates")
    @Operation(
        summary = "Retorna a moeda local do país informado convertida para BRL",
        description = "Resultado em cache por 24h. 204 (sem corpo) quando o país não é reconhecido, " +
            "a moeda local já é BRL, ou a API de câmbio não tem essa moeda."
    )
    public ResponseEntity<LocalCurrencyRateDTO> getRates(@RequestParam String country) {
        return currencyService.getLocalRate(country)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
