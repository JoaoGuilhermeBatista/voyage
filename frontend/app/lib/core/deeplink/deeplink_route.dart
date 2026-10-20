import '../constants/deeplink_constants.dart';

/// O que fazer com um deep link recebido pelo esquema `luvoytravel://`.
enum DeeplinkRoute { checkoutSuccess, checkoutCancel }

/// Interpreta um [Uri] recebido pelo esquema customizado do app.
///
/// Links de confirmação de e-mail/login da Supabase (que carregam
/// `code`/`access_token`/`error` na query ou no fragment) são tratados
/// automaticamente pelo próprio supabase_flutter antes de qualquer outro
/// listener reagir — ver `SupabaseAuth._isAuthCallbackDeeplink` no pacote.
/// Esta função só reconhece os hosts do fluxo de checkout do Stripe, que a
/// Supabase ignora por não ter parâmetros de auth, e retorna `null` para
/// qualquer outro link (deixando a Supabase lidar com ele sozinha).
DeeplinkRoute? parseDeeplink(Uri uri) {
  if (uri.scheme != DeeplinkConstants.scheme) return null;

  switch (uri.host) {
    case 'checkout-success':
      return DeeplinkRoute.checkoutSuccess;
    case 'checkout-cancel':
      return DeeplinkRoute.checkoutCancel;
    default:
      return null;
  }
}
