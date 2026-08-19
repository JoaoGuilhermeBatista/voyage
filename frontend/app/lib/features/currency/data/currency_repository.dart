import '../../../core/services/api_client.dart';
import 'currency_rates_model.dart';

class CurrencyRepository {
  final ApiClient _apiClient;

  CurrencyRepository(this._apiClient);

  Future<CurrencyRatesModel> getRates() async {
    final response = await _apiClient.dio.get('/currency/rates');
    return CurrencyRatesModel.fromJson(response.data as Map<String, dynamic>);
  }
}
