import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:supabase_flutter/supabase_flutter.dart';
import '../../features/auth/presentation/providers/auth_provider.dart';
import '../../features/auth/presentation/screens/login_screen.dart';
import '../../features/onboarding/presentation/screens/onboarding_screen.dart';
import '../../features/home/presentation/screens/home_screen.dart';
import '../../features/recommendations/presentation/screens/recommendations_screen.dart';
import '../../features/itinerary/presentation/screens/itineraries_screen.dart';
import '../../features/itinerary/presentation/screens/itinerary_screen.dart';
import '../../features/destinations/presentation/screens/destination_screen.dart';
import '../../features/favorites/presentation/screens/favorites_screen.dart';
import '../../features/profile/presentation/screens/profile_screen.dart';
import '../../features/profile/presentation/screens/edit_preferences_screen.dart';
import '../../features/profile/presentation/providers/user_provider.dart';
import '../../shared/widgets/scaffold_with_nav_bar.dart';

// Adapta um Stream<T> para Listenable, necessário para refreshListenable do GoRouter.
class _StreamListenable extends ChangeNotifier {
  late final StreamSubscription<dynamic> _sub;

  _StreamListenable(Stream<dynamic> stream) {
    _sub = stream.listen((_) => notifyListeners());
  }

  @override
  void dispose() {
    _sub.cancel();
    super.dispose();
  }
}

final appRouterProvider = Provider<GoRouter>((ref) {
  // Observa o estado de auth para que o provider seja descartado quando ele muda,
  // forçando o GoRouter a recriar e reavaliar o redirect.
  ref.watch(authStateProvider);

  // Sincroniza o status de onboarding do backend para o StateProvider local.
  // Quando userProvider carregar com onboardingCompleted = true, atualiza o state.
  ref.listen(userProvider, (_, next) {
    if (next.hasValue &&
        next.value != null &&
        next.value!.onboardingCompleted) {
      ref.read(onboardingCompletedProvider.notifier).state = true;
    }
  });

  final onboardingCompleted = ref.watch(onboardingCompletedProvider);
  final userAsync = ref.watch(userProvider);

  return GoRouter(
    initialLocation: '/home',
    redirect: (context, state) {
      final isLoggedIn =
          Supabase.instance.client.auth.currentSession != null;
      final path = state.matchedLocation;

      // Não logado → sempre vai para login
      if (!isLoggedIn) {
        if (path == '/login') return null;
        return '/login';
      }

      // Logado — determinar se onboarding foi concluído.
      // Quando o backend carregou, ele é a fonte de verdade (evita que o
      // StateProvider de uma sessão anterior confunda um usuário novo).
      // O StateProvider local serve apenas como fallback durante o loading.
      final backendCompleted =
          userAsync.valueOrNull?.onboardingCompleted ?? false;
      final effectiveOnboardingCompleted = backendCompleted ||
          (userAsync.isLoading && onboardingCompleted);

      if (path == '/login') {
        return effectiveOnboardingCompleted ? '/home' : '/onboarding';
      }

      if (!effectiveOnboardingCompleted) {
        // Se o backend ainda está carregando, aguarda sem redirecionar.
        if (userAsync.isLoading) return null;
        // Backend carregou e onboarding não está concluído.
        if (path != '/onboarding') return '/onboarding';
        return null;
      }

      // Onboarding concluído — não deve estar na tela de onboarding.
      if (path == '/onboarding') return '/home';

      return null;
    },
    refreshListenable: _StreamListenable(
      Supabase.instance.client.auth.onAuthStateChange,
    ),
    routes: [
      // Fora do shell — sem bottom nav bar
      GoRoute(
        path: '/login',
        builder: (context, state) => const LoginScreen(),
      ),
      GoRoute(
        path: '/onboarding',
        builder: (context, state) => const OnboardingScreen(),
      ),
      GoRoute(
        path: '/destinos/:slug',
        builder: (context, state) => DestinationScreen(
          slug: state.pathParameters['slug']!,
        ),
      ),
      GoRoute(
        path: '/roteiro/:id',
        builder: (context, state) => ItineraryScreen(
          id: state.pathParameters['id']!,
        ),
      ),
      GoRoute(
        path: '/perfil/preferencias',
        builder: (context, state) => const EditPreferencesScreen(),
      ),

      // Shell — 5 abas com bottom nav bar
      StatefulShellRoute.indexedStack(
        builder: (context, state, navigationShell) =>
            ScaffoldWithNavBar(navigationShell: navigationShell),
        branches: [
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/home',
                builder: (context, state) => const HomeScreen(),
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/para-voce',
                builder: (context, state) => const RecommendationsScreen(),
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/roteiros',
                builder: (context, state) => const ItinerariesScreen(),
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/favoritos',
                builder: (context, state) => const FavoritesScreen(),
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/perfil',
                builder: (context, state) => const ProfileScreen(),
              ),
            ],
          ),
        ],
      ),
    ],
  );
});
