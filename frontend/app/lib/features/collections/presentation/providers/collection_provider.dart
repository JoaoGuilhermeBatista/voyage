import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/collection_model.dart';
import '../../data/collection_repository.dart';

final collectionsProvider = FutureProvider<List<CollectionModel>>((ref) {
  return ref.watch(collectionRepositoryProvider).fetchAll();
});
