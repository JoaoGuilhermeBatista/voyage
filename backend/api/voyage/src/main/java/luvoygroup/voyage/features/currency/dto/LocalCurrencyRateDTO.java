package luvoygroup.voyage.features.currency.dto;

import java.math.BigDecimal;

public record LocalCurrencyRateDTO(String currencyCode, BigDecimal rateToBrl) {}
