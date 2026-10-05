import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/unusual_place_models.dart';
import '../../data/unusual_place_repository.dart';

/// Lugares incomuns de um destino, via GET /unusual-places/{slug}. Family
/// key = slug (mesmo padrão de templatesForDestinationProvider).
final unusualPlacesForDestinationProvider =
    FutureProvider.family<List<UnusualPlaceModel>, String>((ref, slug) {
  return ref.watch(unusualPlaceRepositoryProvider).getForDestination(slug);
});
