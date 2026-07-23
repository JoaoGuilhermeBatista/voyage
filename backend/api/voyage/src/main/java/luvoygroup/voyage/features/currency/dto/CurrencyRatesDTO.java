package luvoygroup.voyage.features.currency.dto;

import java.math.BigDecimal;

public record CurrencyRatesDTO(
    BigDecimal eurToBrl,
    BigDecimal gbpToBrl,
    BigDecimal usdToBrl
) {}
