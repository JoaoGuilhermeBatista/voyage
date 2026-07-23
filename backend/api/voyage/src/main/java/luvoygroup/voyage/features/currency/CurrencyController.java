package luvoygroup.voyage.features.currency;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import luvoygroup.voyage.features.currency.dto.CurrencyRatesDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/currency")
@RequiredArgsConstructor
@Tag(name = "Currency", description = "Taxas de câmbio para exibição de preços no app")
public class CurrencyController {

    private final CurrencyService currencyService;

    @GetMapping("/rates")
    @Operation(summary = "Retorna EUR, GBP e USD convertidos para BRL", description = "Resultado em cache por 24h")
    public CurrencyRatesDTO getRates() {
        return currencyService.getRates();
    }
}
