import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/services/api_client.dart';
import 'favorite_model.dart';

class FavoriteRepository {
  final Dio _dio;
  FavoriteRepository(this._dio);

  Future<List<FavoriteModel>> fetchAll() async {
    final response = await _dio.get('/favorites');
    return (response.data as List<dynamic>)
        .map((j) => FavoriteModel.fromJson(j as Map<String, dynamic>))
        .toList();
  }

  Future<void> add(String destinationId) async {
    await _dio.post('/favorites', data: {'destinationId': destinationId});
  }

  Future<void> remove(String favoriteId) async {
    await _dio.delete('/favorites/$favoriteId');
  }
}

final favoriteRepositoryProvider = Provider<FavoriteRepository>((ref) {
  return FavoriteRepository(ref.watch(apiClientProvider).dio);
});
