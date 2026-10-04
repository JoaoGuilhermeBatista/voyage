/// Esquema de deep link customizado do app (`luvoytravel://`).
///
/// Sem domínio hospedado ainda, Universal Links/App Links (https://) não são
/// viáveis — dependem de um `apple-app-site-association` servido de
/// verdade. Por isso usamos um esquema custom, que não depende de
/// hospedagem e funciona tanto para o retorno do checkout do Stripe quanto
/// para a confirmação de e-mail da Supabase (ver DeeplinkService e
/// AuthNotifier.signUp).
class DeeplinkConstants {
  static const String scheme = 'luvoytravel';

  /// Usado em `emailRedirectTo` no signUp da Supabase. Precisa estar
  /// cadastrado em Authentication → URL Configuration → Redirect URLs no
  /// painel da Supabase, senão o GoTrue rejeita o redirect com erro.
  static const String authCallback = '$scheme://login-callback';

  /// successUrl/cancelUrl do checkout do Stripe (ver premium_screen.dart).
  /// Reconhecidos por [parseDeeplink]/DeeplinkService.
  static const String checkoutSuccess = '$scheme://checkout-success';
  static const String checkoutCancel = '$scheme://checkout-cancel';
}
