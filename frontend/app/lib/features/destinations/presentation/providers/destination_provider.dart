import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/destination_repository.dart';
import '../../data/destination_summary_model.dart';
import '../../data/destination_detail_model.dart';

final destinationsProvider = FutureProvider<List<DestinationSummaryModel>>((ref) {
  return ref.watch(destinationRepositoryProvider).fetchAll();
});

final destinationSearchProvider =
    FutureProvider.family<List<DestinationSummaryModel>, String>((ref, query) {
  return ref.watch(destinationRepositoryProvider).fetchAll(query: query);
});

final destinationDetailProvider =
    FutureProvider.family<DestinationDetailModel, String>((ref, slug) {
  return ref.watch(destinationRepositoryProvider).fetchBySlug(slug);
});
