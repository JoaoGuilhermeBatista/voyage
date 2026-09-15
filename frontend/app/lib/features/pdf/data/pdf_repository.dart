import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/services/api_client.dart';

class PdfRepository {
  final ApiClient _apiClient;
  PdfRepository(this._apiClient);

  /// Confirma com o backend que este usuário tem direito a exportar (1
  /// grátis vitalícia, depois premium ilimitado ou 1 crédito avulso
  /// comprado via Pix — ver ItineraryService/PdfService) e incrementa o
  /// contador correspondente. O PDF em si é gerado depois, no cliente, a
  /// partir dos dados do roteiro já carregados na tela (ver
  /// pdf_generator.dart) — o backend não guarda nem serve o arquivo.
  Future<void> confirmExport(String itineraryId) async {
    await _apiClient.dio.post('/pdf/export/$itineraryId');
  }
}

final pdfRepositoryProvider = Provider<PdfRepository>((ref) {
  return PdfRepository(ref.watch(apiClientProvider));
});

/// Mensagem amigável quando o backend recusa por limite do plano gratuito.
/// `null` se o erro não for esse caso, pra quem chamar cair no texto de
/// erro genérico de sempre.
String? pdfLimitMessage(Object error) {
  if (error is DioException && error.response?.statusCode == 402) {
    return 'Você já usou sua exportação gratuita de PDF. Compre uma '
        'exportação avulsa ou assine o Premium para exportar sem limite.';
  }
  return null;
}
