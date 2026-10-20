class LocalCurrencyRateModel {
  final String currencyCode;
  final double rateToBrl;

  const LocalCurrencyRateModel({
    required this.currencyCode,
    required this.rateToBrl,
  });

  factory LocalCurrencyRateModel.fromJson(Map<String, dynamic> json) {
    return LocalCurrencyRateModel(
      currencyCode: json['currencyCode'] as String,
      rateToBrl: (json['rateToBrl'] as num).toDouble(),
    );
  }
}
