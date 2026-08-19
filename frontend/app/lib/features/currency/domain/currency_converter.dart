/// Converte [amount] de [fromCurrency] para [toCurrency] usando [rates], um
/// mapa moeda→taxa-para-BRL (ex: `{'EUR': 5.90, 'GBP': 6.85, 'USD': 5.55}`).
///
/// BRL é a moeda base implícita: sempre vale 1.0, mesmo que não esteja
/// presente em [rates].
///
/// Regras:
/// - mesma moeda (`fromCurrency == toCurrency`) → retorna [amount] sem
///   alteração, sem consultar [rates].
/// - moedas diferentes → converte para BRL usando a taxa de origem, depois
///   de BRL para a moeda de destino usando a taxa de destino.
/// - moeda desconhecida (não é `'BRL'` e não está em [rates]) → lança
///   [ArgumentError].
double convert(
  double amount, {
  required String fromCurrency,
  required String toCurrency,
  required Map<String, double> rates,
}) {
  if (fromCurrency == toCurrency) return amount;

  double rateToBrl(String currency) {
    if (currency == 'BRL') return 1.0;
    final rate = rates[currency];
    if (rate == null) {
      throw ArgumentError('Moeda desconhecida: $currency');
    }
    return rate;
  }

  final fromRate = rateToBrl(fromCurrency);
  final toRate = rateToBrl(toCurrency);

  final amountInBrl = amount * fromRate;
  return amountInBrl / toRate;
}
