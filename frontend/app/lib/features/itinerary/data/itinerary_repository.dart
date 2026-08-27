import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/services/api_client.dart';
import 'itinerary_models.dart';

class ItineraryRepository {
  final Dio _dio;
  ItineraryRepository(this._dio);

  Future<List<ItinerarySummaryModel>> fetchAll() async {
    final response = await _dio.get('/itineraries');
    return (response.data as List<dynamic>)
        .map((j) =>
            ItinerarySummaryModel.fromJson(j as Map<String, dynamic>))
        .toList();
  }

  Future<ItineraryModel> fetchById(String id) async {
    final response = await _dio.get('/itineraries/$id');
    return ItineraryModel.fromJson(response.data as Map<String, dynamic>);
  }

  Future<ItineraryModel> generate({
    required String destinationId,
    required DateTime startDate,
    required DateTime endDate,
    String? stayArea,
    String? tripVibe,
    String? companions,
    String? tripBudget,
    List<String>? tripInterests,
    String? tripRhythm,
  }) async {
    final body = <String, dynamic>{
      'destinationId': destinationId,
      'startDate': _formatDate(startDate),
      'endDate': _formatDate(endDate),
    };
    if (stayArea != null) body['stayArea'] = stayArea;
    if (tripVibe != null) body['tripVibe'] = tripVibe;
    if (companions != null) body['companions'] = companions;
    if (tripBudget != null) body['tripBudget'] = tripBudget;
    if (tripInterests != null && tripInterests.isNotEmpty) {
      body['tripInterests'] = tripInterests;
    }
    if (tripRhythm != null) body['tripRhythm'] = tripRhythm;

    final response = await _dio.post('/itineraries', data: body);
    return ItineraryModel.fromJson(response.data as Map<String, dynamic>);
  }

  Future<void> delete(String id) async {
    await _dio.delete('/itineraries/$id');
  }

  Future<List<PoiDetailModel>> getAlternatives(
      String itineraryId, String activityId) async {
    final response = await _dio
        .get('/itineraries/$itineraryId/activities/$activityId/alternatives');
    return (response.data as List<dynamic>)
        .map((j) => PoiDetailModel.fromJson(j as Map<String, dynamic>))
        .toList();
  }

  Future<ItineraryActivityModel> substituteActivity(
      String itineraryId, String activityId, String poiId) async {
    final response = await _dio.patch(
      '/itineraries/$itineraryId/activities/$activityId',
      data: {'poiId': poiId},
    );
    return ItineraryActivityModel.fromJson(
        response.data as Map<String, dynamic>);
  }

  String _formatDate(DateTime d) =>
      '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';
}

final itineraryRepositoryProvider = Provider<ItineraryRepository>((ref) {
  return ItineraryRepository(ref.watch(apiClientProvider).dio);
});
