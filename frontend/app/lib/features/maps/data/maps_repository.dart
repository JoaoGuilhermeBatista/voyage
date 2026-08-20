import '../../../core/services/api_client.dart';
import 'maps_models.dart';

/// Espelha o padrão de CurrencyRepository
/// (lib/features/currency/data/currency_repository.dart): recebe
/// ApiClient no construtor e chama `_apiClient.dio.get(...)`. O JWT é
/// anexado automaticamente pelo interceptor de ApiClient (ver
/// core/services/api_client.dart) — os endpoints /api/v1/maps/** exigem
/// autenticação no backend (ao contrário de /currency/**, que é público).
class MapsRepository {
  final ApiClient _apiClient;

  MapsRepository(this._apiClient);

  Future<DirectionsModel> getDirections({
    required double originLat,
    required double originLng,
    required double destLat,
    required double destLng,
  }) async {
    final response = await _apiClient.dio.get(
      '/maps/directions',
      queryParameters: {
        'originLat': originLat,
        'originLng': originLng,
        'destLat': destLat,
        'destLng': destLng,
      },
    );
    return DirectionsModel.fromJson(response.data as Map<String, dynamic>);
  }

  Future<EmbedUrlModel> getEmbedUrl({
    required double lat,
    required double lng,
  }) async {
    final response = await _apiClient.dio.get(
      '/maps/embed-url',
      queryParameters: {
        'lat': lat,
        'lng': lng,
      },
    );
    return EmbedUrlModel.fromJson(response.data as Map<String, dynamic>);
  }
}
