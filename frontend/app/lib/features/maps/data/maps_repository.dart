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

  /// Rota entre as CIDADES de dois destinos (ex.: trechos consecutivos de
  /// uma viagem multi-destino) — diferente de [getDirections], que recebe
  /// lat/lng prontos (POIs).
  Future<DirectionsModel> getDirectionsBetweenDestinations({
    required String originDestinationId,
    required String destDestinationId,
  }) async {
    final response = await _apiClient.dio.get(
      '/maps/directions-between-destinations',
      queryParameters: {
        'originDestinationId': originDestinationId,
        'destDestinationId': destDestinationId,
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

  Future<EmbedUrlModel> getDirectionsEmbedUrl({
    required double originLat,
    required double originLng,
    required double destLat,
    required double destLng,
  }) async {
    final response = await _apiClient.dio.get(
      '/maps/embed-url/directions',
      queryParameters: {
        'originLat': originLat,
        'originLng': originLng,
        'destLat': destLat,
        'destLng': destLng,
      },
    );
    return EmbedUrlModel.fromJson(response.data as Map<String, dynamic>);
  }

  Future<NeighborhoodSuggestionsModel> getNeighborhoodSuggestions({
    required String destinationId,
    required String budgetTier,
  }) async {
    final response = await _apiClient.dio.get(
      '/maps/neighborhood-suggestions',
      queryParameters: {
        'destinationId': destinationId,
        'budgetTier': budgetTier,
      },
    );
    return NeighborhoodSuggestionsModel.fromJson(
        response.data as Map<String, dynamic>);
  }
}
