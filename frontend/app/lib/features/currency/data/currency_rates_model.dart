class CurrencyRatesModel {
  final double eurToBrl;
  final double gbpToBrl;
  final double usdToBrl;

  const CurrencyRatesModel({
    required this.eurToBrl,
    required this.gbpToBrl,
    required this.usdToBrl,
  });

  factory CurrencyRatesModel.fromJson(Map<String, dynamic> json) {
    return CurrencyRatesModel(
      eurToBrl: (json['eurToBrl'] as num).toDouble(),
      gbpToBrl: (json['gbpToBrl'] as num).toDouble(),
      usdToBrl: (json['usdToBrl'] as num).toDouble(),
    );
  }
}
