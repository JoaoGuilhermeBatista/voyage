import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../core/services/api_client.dart';
import '../../data/currency_rates_model.dart';
import '../../data/currency_repository.dart';

final currencyRepositoryProvider = Provider<CurrencyRepository>((ref) {
  return CurrencyRepository(ref.watch(apiClientProvider));
});

/// Busca a moeda local de [country] via GET /api/v1/currency/rates?country=
/// (público, cache de 24h no backend). `.family` cacheia por país — abrir o
/// mesmo roteiro de novo não dispara uma nova chamada HTTP.
final localCurrencyRateProvider =
    FutureProvider.family<LocalCurrencyRateModel?, String>((ref, country) async {
  final repository = ref.watch(currencyRepositoryProvider);
  return repository.getLocalRate(country);
});
