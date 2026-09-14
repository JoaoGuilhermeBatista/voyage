import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/services/api_client.dart';

/// 3 formas de pagamento (ver luvoy-pendencias.md): assinatura recorrente
/// via cartão, premium avulso de 30 dias via Pix, e crédito avulso de
/// exportação de PDF via Pix. Cada método só pede as URLs de retorno — o
/// backend monta a sessão de checkout e devolve o link do Stripe pronto
/// pra abrir no navegador.
class BillingRepository {
  final ApiClient _apiClient;

  BillingRepository(this._apiClient);

  Future<String> createSubscriptionCheckout({
    required String successUrl,
    required String cancelUrl,
  }) => _checkout('/billing/checkout/subscription', successUrl, cancelUrl);

  Future<String> createPixPremiumCheckout({
    required String successUrl,
    required String cancelUrl,
  }) => _checkout('/billing/checkout/pix-premium', successUrl, cancelUrl);

  Future<String> createPixPdfExportCheckout({
    required String successUrl,
    required String cancelUrl,
  }) => _checkout('/billing/checkout/pix-pdf-export', successUrl, cancelUrl);

  Future<String> _checkout(
    String path,
    String successUrl,
    String cancelUrl,
  ) async {
    final response = await _apiClient.dio.post(
      path,
      data: {'successUrl': successUrl, 'cancelUrl': cancelUrl},
    );
    return response.data['checkoutUrl'] as String;
  }
}

final billingRepositoryProvider = Provider<BillingRepository>((ref) {
  return BillingRepository(ref.watch(apiClientProvider));
});
