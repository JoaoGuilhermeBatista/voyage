import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/services/api_client.dart';
import 'destination_summary_model.dart';
import 'destination_detail_model.dart';

class DestinationRepository {
  final Dio _dio;

  DestinationRepository(this._dio);

  Future<List<DestinationSummaryModel>> fetchAll({String? query}) async {
    final response = await _dio.get(
      '/destinations',
      queryParameters: query != null && query.isNotEmpty ? {'q': query} : null,
    );
    final list = response.data as List<dynamic>;
    return list
        .map((json) => DestinationSummaryModel.fromJson(json as Map<String, dynamic>))
        .toList();
  }

  Future<DestinationDetailModel> fetchBySlug(String slug) async {
    final response = await _dio.get('/destinations/$slug');
    return DestinationDetailModel.fromJson(response.data as Map<String, dynamic>);
  }
}

final destinationRepositoryProvider = Provider<DestinationRepository>((ref) {
  final client = ref.watch(apiClientProvider);
  return DestinationRepository(client.dio);
});
