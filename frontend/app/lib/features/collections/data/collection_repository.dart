import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/services/api_client.dart';
import 'collection_model.dart';

class CollectionRepository {
  final Dio _dio;

  CollectionRepository(this._dio);

  Future<List<CollectionModel>> fetchAll() async {
    final response = await _dio.get('/collections');
    final list = response.data as List<dynamic>;
    return list
        .map((json) => CollectionModel.fromJson(json as Map<String, dynamic>))
        .toList();
  }

}

final collectionRepositoryProvider = Provider<CollectionRepository>((ref) {
  final client = ref.watch(apiClientProvider);
  return CollectionRepository(client.dio);
});
