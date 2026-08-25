import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/services/api_client.dart';
import '../../itinerary/data/itinerary_models.dart';
import 'itinerary_template_models.dart';

/// Segue o mesmo padrão de MapsRepository
/// (lib/features/maps/data/maps_repository.dart): recebe ApiClient no
/// construtor e chama `_apiClient.dio.get/post(...)`. O JWT é anexado
/// automaticamente pelo interceptor de ApiClient quando presente — os dois
/// GETs são públicos no backend (não exigem auth), só o POST /adopt exige
/// JWT (usuário precisa estar logado para criar um roteiro de verdade a
/// partir do template).
class ItineraryTemplateRepository {
  final ApiClient _apiClient;

  ItineraryTemplateRepository(this._apiClient);

  Future<List<ItineraryTemplateSummaryModel>> getTemplatesForDestination(
    String slug,
  ) async {
    final response =
        await _apiClient.dio.get('/destinations/$slug/itinerary-templates');
    return (response.data as List<dynamic>)
        .map((j) => ItineraryTemplateSummaryModel.fromJson(
            j as Map<String, dynamic>))
        .toList();
  }

  Future<ItineraryTemplateDetailModel> getTemplateDetail(
    String slug,
    String templateId,
  ) async {
    final response = await _apiClient.dio
        .get('/destinations/$slug/itinerary-templates/$templateId');
    return ItineraryTemplateDetailModel.fromJson(
        response.data as Map<String, dynamic>);
  }

  /// POST /itinerary-templates/{templateId}/adopt com { "startDate": ... }.
  /// A resposta tem o MESMO shape de GET /itineraries/{id}, por isso reusa
  /// ItineraryModel.fromJson (nenhum model novo para o resultado do adopt).
  Future<ItineraryModel> adoptTemplate(
    String templateId,
    DateTime startDate,
  ) async {
    final response = await _apiClient.dio.post(
      '/itinerary-templates/$templateId/adopt',
      data: {'startDate': _formatDate(startDate)},
    );
    return ItineraryModel.fromJson(response.data as Map<String, dynamic>);
  }

  // Mesma formatação de 3 linhas usada em
  // ItineraryRepository._formatDate (itinerary_repository.dart).
  String _formatDate(DateTime d) =>
      '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';
}

final itineraryTemplateRepositoryProvider =
    Provider<ItineraryTemplateRepository>((ref) {
  return ItineraryTemplateRepository(ref.watch(apiClientProvider));
});
