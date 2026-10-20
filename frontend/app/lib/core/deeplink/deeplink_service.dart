import 'dart:async';

import 'package:app_links/app_links.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'deeplink_route.dart';

/// Ouve os deep links do esquema `luvoytravel://` e reage ao retorno do
/// checkout do Stripe (ver [parseDeeplink]).
///
/// A confirmação de e-mail/login da Supabase NÃO passa por aqui: o próprio
/// supabase_flutter já escuta os mesmos links via sua instância (singleton,
/// ver pacote app_links) de `AppLinks` e completa a sessão sozinho. Esta
/// classe só cuida dos links que a Supabase ignora.
class DeeplinkService {
  final GoRouter _router;
  final GlobalKey<ScaffoldMessengerState> _messengerKey;
  final AppLinks _appLinks;
  StreamSubscription<Uri?>? _sub;

  DeeplinkService(
    this._router,
    this._messengerKey, {
    AppLinks? appLinks,
  }) : _appLinks = appLinks ?? AppLinks();

  void init() {
    _sub = _appLinks.uriLinkStream.listen(_handle);
  }

  void _handle(Uri? uri) {
    if (uri == null) return;
    final route = parseDeeplink(uri);
    if (route == null) return;

    _router.go('/premium');
    final message = switch (route) {
      DeeplinkRoute.checkoutSuccess =>
        'Pagamento confirmado! Atualizando seu plano...',
      DeeplinkRoute.checkoutCancel => 'Pagamento cancelado.',
    };
    _messengerKey.currentState?.showSnackBar(
      SnackBar(content: Text(message)),
    );
  }

  void dispose() {
    unawaited(_sub?.cancel());
  }
}
