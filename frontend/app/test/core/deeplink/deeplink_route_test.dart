import 'package:flutter_test/flutter_test.dart';
import 'package:app/core/deeplink/deeplink_route.dart';

void main() {
  group('parseDeeplink', () {
    test('reconhece o retorno de sucesso do checkout do Stripe', () {
      final uri = Uri.parse('luvoytravel://checkout-success');
      expect(parseDeeplink(uri), DeeplinkRoute.checkoutSuccess);
    });

    test('reconhece o retorno de cancelamento do checkout do Stripe', () {
      final uri = Uri.parse('luvoytravel://checkout-cancel');
      expect(parseDeeplink(uri), DeeplinkRoute.checkoutCancel);
    });

    test('ignora o link de confirmação de e-mail da Supabase (host '
        'diferente) — quem trata esse é o supabase_flutter', () {
      final uri = Uri.parse(
        'luvoytravel://login-callback?code=abc123-def456',
      );
      expect(parseDeeplink(uri), isNull);
    });

    test('ignora um link de erro de auth da Supabase', () {
      final uri = Uri.parse(
        'luvoytravel://login-callback?error=access_denied&error_code=otp_expired',
      );
      expect(parseDeeplink(uri), isNull);
    });

    test('ignora qualquer host desconhecido dentro do nosso esquema', () {
      final uri = Uri.parse('luvoytravel://algo-nao-mapeado');
      expect(parseDeeplink(uri), isNull);
    });

    test('ignora esquemas diferentes de luvoytravel', () {
      final uri = Uri.parse('https://checkout-success');
      expect(parseDeeplink(uri), isNull);
    });

    test('é indiferente a query params extras no checkout-success', () {
      final uri = Uri.parse(
        'luvoytravel://checkout-success?session_id=cs_test_123',
      );
      expect(parseDeeplink(uri), DeeplinkRoute.checkoutSuccess);
    });
  });
}
