import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../auth/presentation/providers/auth_provider.dart';
import '../../data/favorite_model.dart';
import '../../data/favorite_repository.dart';

class FavoriteNotifier extends AsyncNotifier<List<FavoriteModel>> {
  @override
  Future<List<FavoriteModel>> build() async {
    ref.watch(authStateProvider);
    return ref.read(favoriteRepositoryProvider).fetchAll();
  }

  Future<void> add(String destinationId) async {
    await ref.read(favoriteRepositoryProvider).add(destinationId);
    ref.invalidateSelf();
  }

  Future<void> remove(String favoriteId) async {
    await ref.read(favoriteRepositoryProvider).remove(favoriteId);
    ref.invalidateSelf();
  }
}

final favoritesProvider =
    AsyncNotifierProvider<FavoriteNotifier, List<FavoriteModel>>(
  FavoriteNotifier.new,
);

// Returns the favoriteId if the destination is saved, null otherwise.
final destinationFavoriteIdProvider =
    Provider.family<String?, String>((ref, destinationId) {
  return ref.watch(favoritesProvider).valueOrNull
      ?.where((f) => f.destination?.id == destinationId)
      .map((f) => f.id)
      .firstOrNull;
});
