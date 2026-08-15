import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:supabase_flutter/supabase_flutter.dart';
import '../../../../core/services/api_client.dart';
import '../../../../features/auth/presentation/providers/auth_provider.dart';
import '../../data/user_model.dart';
import '../../data/user_repository.dart';

/// Controla se o onboarding foi concluído nesta sessão.
/// Inicializado como false; atualizado pela OnboardingScreen ou ao carregar
/// dados do backend que indiquem onboardingCompleted = true.
final onboardingCompletedProvider = StateProvider<bool>((ref) => false);

final userRepositoryProvider = Provider<UserRepository>((ref) {
  return UserRepository(ref.watch(apiClientProvider));
});

/// Busca os dados do usuário autenticado via GET /api/v1/users/me.
/// Depende de authStateProvider para re-executar automaticamente ao trocar de conta.
/// Retorna null se o usuário não estiver logado.
final userProvider = FutureProvider<UserModel?>((ref) async {
  ref.watch(authStateProvider);
  final session = Supabase.instance.client.auth.currentSession;
  if (session == null) return null;

  final repo = ref.watch(userRepositoryProvider);
  return repo.fetchMe();
});
