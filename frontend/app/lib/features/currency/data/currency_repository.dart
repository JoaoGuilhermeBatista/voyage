import '../../../core/services/api_client.dart';
import 'currency_rates_model.dart';

class CurrencyRepository {
  final ApiClient _apiClient;

  CurrencyRepository(this._apiClient);

  /// `null` quando o backend responde 204 — país sem moeda local mapeada,
  /// moeda local já é BRL, ou a API de câmbio não tem essa moeda.
  Future<LocalCurrencyRateModel?> getLocalRate(String country) async {
    final response = await _apiClient.dio.get(
      '/currency/rates',
      queryParameters: {'country': country},
    );
    if (response.statusCode == 204 || response.data == null) return null;
    return LocalCurrencyRateModel.fromJson(
      response.data as Map<String, dynamic>,
    );
  }
}
