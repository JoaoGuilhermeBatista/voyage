import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/services/api_client.dart';
import 'unusual_place_models.dart';

/// Mesmo padrão de ItineraryTemplateRepository: recebe ApiClient no
/// construtor, chama `_apiClient.dio.get(...)`. Diferente dos templates
/// (públicos), este endpoint exige JWT — o interceptor de ApiClient já
/// anexa o token automaticamente quando presente.
class UnusualPlaceRepository {
  final ApiClient _apiClient;

  UnusualPlaceRepository(this._apiClient);

  Future<List<UnusualPlaceModel>> getForDestination(String slug) async {
    final response = await _apiClient.dio.get('/unusual-places/$slug');
    return (response.data as List<dynamic>)
        .map((j) => UnusualPlaceModel.fromJson(j as Map<String, dynamic>))
        .toList();
  }
}

final unusualPlaceRepositoryProvider = Provider<UnusualPlaceRepository>((ref) {
  return UnusualPlaceRepository(ref.watch(apiClientProvider));
});
