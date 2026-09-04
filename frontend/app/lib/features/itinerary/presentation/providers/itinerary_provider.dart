import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../auth/presentation/providers/auth_provider.dart';
import '../../data/itinerary_models.dart';
import '../../data/itinerary_repository.dart';

final itinerariesProvider = FutureProvider<List<ItinerarySummaryModel>>((
  ref,
) async {
  ref.watch(authStateProvider);
  return ref.read(itineraryRepositoryProvider).fetchAll();
});

final itineraryDetailProvider = FutureProvider.family<ItineraryModel, String>((
  ref,
  id,
) {
  return ref.read(itineraryRepositoryProvider).fetchById(id);
});

final tripsProvider = FutureProvider<List<TripSummaryModel>>((ref) async {
  ref.watch(authStateProvider);
  return ref.read(itineraryRepositoryProvider).fetchAllTrips();
});

final tripDetailProvider = FutureProvider.family<TripSummaryModel, String>((
  ref,
  tripGroupId,
) {
  return ref.read(itineraryRepositoryProvider).fetchTripByGroupId(tripGroupId);
});
