import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:supabase_flutter/supabase_flutter.dart';
import 'core/constants/api_constants.dart';
import 'core/deeplink/deeplink_service.dart';
import 'core/router/app_router.dart';
import 'core/themes/app_theme.dart';

/// Usado pelo DeeplinkService para mostrar feedback (ex: confirmação de
/// pagamento) sem depender de um BuildContext de dentro da árvore de telas.
final rootScaffoldMessengerKey = GlobalKey<ScaffoldMessengerState>();

/// O ScrollBehavior padrão do Flutter só arrasta com toque/stylus — mouse
/// (Linux/Windows/macOS desktop) fica sem drag-to-scroll nos carrosséis
/// horizontais sem isso.
class _AppScrollBehavior extends MaterialScrollBehavior {
  @override
  Set<PointerDeviceKind> get dragDevices => {
    ...super.dragDevices,
    PointerDeviceKind.mouse,
  };
}

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  await Supabase.initialize(
    url: ApiConstants.supabaseUrl,
    publishableKey: ApiConstants.supabaseAnonKey,
  );

  runApp(
    const ProviderScope(
      child: LuvoyApp(),
    ),
  );
}

class LuvoyApp extends ConsumerStatefulWidget {
  const LuvoyApp({super.key});

  @override
  ConsumerState<LuvoyApp> createState() => _LuvoyAppState();
}

class _LuvoyAppState extends ConsumerState<LuvoyApp> {
  DeeplinkService? _deeplinkService;

  @override
  void initState() {
    super.initState();
    // Só o retorno do checkout do Stripe passa por aqui — a confirmação de
    // e-mail/login da Supabase é tratada internamente pelo supabase_flutter
    // (ver DeeplinkService).
    final router = ref.read(appRouterProvider);
    _deeplinkService = DeeplinkService(router, rootScaffoldMessengerKey)
      ..init();
  }

  @override
  void dispose() {
    _deeplinkService?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final router = ref.watch(appRouterProvider);
    return MaterialApp.router(
      title: 'Luvoy',
      scaffoldMessengerKey: rootScaffoldMessengerKey,
      theme: AppTheme.lightTheme,
      routerConfig: router,
      scrollBehavior: _AppScrollBehavior(),
      debugShowCheckedModeBanner: false,
    );
  }
}
