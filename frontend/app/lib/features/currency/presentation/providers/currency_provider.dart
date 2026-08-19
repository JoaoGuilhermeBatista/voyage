import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../core/services/api_client.dart';
import '../../data/currency_rates_model.dart';
import '../../data/currency_repository.dart';

final currencyRepositoryProvider = Provider<CurrencyRepository>((ref) {
  return CurrencyRepository(ref.watch(apiClientProvider));
});

/// Busca o câmbio do dia via GET /api/v1/currency/rates (público, cache de
/// 24h no backend). FutureProvider já cacheia o Future por padrão enquanto
/// o container/listener estiver vivo, então uma segunda leitura não dispara
/// uma nova chamada HTTP.
final currencyRatesProvider = FutureProvider<CurrencyRatesModel>((ref) async {
  final repository = ref.watch(currencyRepositoryProvider);
  return repository.getRates();
});
