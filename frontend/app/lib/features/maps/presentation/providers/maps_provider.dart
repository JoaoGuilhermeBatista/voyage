import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../core/services/api_client.dart';
import '../../data/maps_models.dart';
import '../../data/maps_repository.dart';

final mapsRepositoryProvider = Provider<MapsRepository>((ref) {
  return MapsRepository(ref.watch(apiClientProvider));
});

/// Rota entre dois pontos via GET /api/v1/maps/directions. Chave da family
/// é (originLat, originLng, destLat, destLng) — um record posicional do
/// Dart 3, então duas chamadas com as mesmas coordenadas reaproveitam o
/// mesmo Future (mesmo padrão de cache de currencyRatesProvider).
final directionsProvider = FutureProvider.family<
    DirectionsModel, (double originLat, double originLng, double destLat, double destLng)>(
  (ref, coords) async {
    final repository = ref.watch(mapsRepositoryProvider);
    return repository.getDirections(
      originLat: coords.$1,
      originLng: coords.$2,
      destLat: coords.$3,
      destLng: coords.$4,
    );
  },
);

/// Rota entre as CIDADES de dois destinos via GET
/// /api/v1/maps/directions-between-destinations. Chave da family é
/// (originDestinationId, destDestinationId).
final directionsBetweenDestinationsProvider = FutureProvider.family<
    DirectionsModel, (String originDestinationId, String destDestinationId)>(
  (ref, ids) async {
    final repository = ref.watch(mapsRepositoryProvider);
    return repository.getDirectionsBetweenDestinations(
      originDestinationId: ids.$1,
      destDestinationId: ids.$2,
    );
  },
);

/// URL de embed para um ponto via GET /api/v1/maps/embed-url. Chave da
/// family é (lat, lng).
final embedUrlProvider =
    FutureProvider.family<EmbedUrlModel, (double lat, double lng)>(
  (ref, coords) async {
    final repository = ref.watch(mapsRepositoryProvider);
    return repository.getEmbedUrl(lat: coords.$1, lng: coords.$2);
  },
);

/// URL de embed da ROTA entre dois pontos via GET
/// /api/v1/maps/embed-url/directions. Chave da family é (originLat,
/// originLng, destLat, destLng) — mesmo formato de directionsProvider.
final directionsEmbedUrlProvider = FutureProvider.family<EmbedUrlModel,
    (double originLat, double originLng, double destLat, double destLng)>(
  (ref, coords) async {
    final repository = ref.watch(mapsRepositoryProvider);
    return repository.getDirectionsEmbedUrl(
      originLat: coords.$1,
      originLng: coords.$2,
      destLat: coords.$3,
      destLng: coords.$4,
    );
  },
);

/// Sugestões de bairro pra hospedagem via GET /api/v1/maps/neighborhood-suggestions.
/// Chave da family é (destinationId, budgetTier).
final neighborhoodSuggestionsProvider = FutureProvider.family<
    NeighborhoodSuggestionsModel, (String destinationId, String budgetTier)>(
  (ref, params) async {
    final repository = ref.watch(mapsRepositoryProvider);
    return repository.getNeighborhoodSuggestions(
      destinationId: params.$1,
      budgetTier: params.$2,
    );
  },
);
