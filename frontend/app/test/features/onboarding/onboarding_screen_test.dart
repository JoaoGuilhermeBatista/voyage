// Testes para o wizard de Onboarding — cobre a nova 5ª etapa (Religião):
//
//   1) A etapa de Religião é obrigatória: o botão de avançar/concluir fica
//      desabilitado enquanto nenhuma opção (inclusive "Prefiro não
//      informar") for selecionada.
//   2) O fluxo completo (Orçamento -> Ritmo -> Grupo -> Interesses ->
//      Religião) envia o valor de religião escolhido para
//      UserRepository.updatePreferences.
//
// Estratégia de isolamento (sem tocar no Supabase real): userProvider é
// sobrescrito diretamente (mesmo padrão de edit_preferences_test.dart e
// about_dialog_test.dart), então o corpo real do provider (que depende de
// Supabase.instance.client) nunca executa.

import 'package:app/features/onboarding/presentation/screens/onboarding_screen.dart';
import 'package:app/features/onboarding/presentation/widgets/preference_steps.dart';
import 'package:app/features/profile/data/user_model.dart';
import 'package:app/features/profile/data/user_repository.dart';
import 'package:app/features/profile/presentation/providers/user_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:mocktail/mocktail.dart';

class MockUserRepository extends Mock implements UserRepository {}

const _fakeUser = UserModel(
  id: 'user-1',
  email: 'teste@luvoy.com',
  onboardingCompleted: true,
  isPremium: false,
);

void main() {
  late MockUserRepository mockRepo;
  late GoRouter router;

  setUp(() {
    mockRepo = MockUserRepository();
    when(() => mockRepo.fetchMe()).thenAnswer((_) async => _fakeUser);
  });

  Widget buildTestApp() {
    router = GoRouter(
      initialLocation: '/onboarding',
      routes: [
        GoRoute(
          path: '/onboarding',
          builder: (context, state) => const OnboardingScreen(),
        ),
        GoRoute(
          path: '/home',
          builder: (context, state) => const Scaffold(body: Text('Home Screen')),
        ),
      ],
    );

    return ProviderScope(
      overrides: [
        userRepositoryProvider.overrideWithValue(mockRepo),
        userProvider.overrideWith((ref) async => _fakeUser),
      ],
      child: MaterialApp.router(routerConfig: router),
    );
  }

  Future<void> pumpOnboarding(WidgetTester tester) async {
    await tester.pumpWidget(buildTestApp());
    await tester.pumpAndSettle();
  }

  Future<void> tapContinuar(WidgetTester tester) async {
    await tester.tap(find.text('Continuar'));
    await tester.pumpAndSettle();
  }

  /// Avança da 1ª etapa (Orçamento) até a etapa de Religião (5ª), selecionando
  /// uma opção em cada etapa anterior.
  Future<void> advanceToReligionStep(WidgetTester tester) async {
    await tester.tap(find.text('Econômico')); // Orçamento
    await tester.pumpAndSettle();
    await tapContinuar(tester);

    await tester.tap(find.text('Tranquilo')); // Ritmo
    await tester.pumpAndSettle();
    await tapContinuar(tester);

    await tester.tap(find.text('Solo')); // Grupo
    await tester.pumpAndSettle();
    await tapContinuar(tester);

    await tester.tap(find.text('Cultura')); // Interesses
    await tester.pumpAndSettle();
    await tapContinuar(tester);
  }

  group('OnboardingScreen — etapa de Religião (5ª etapa)', () {
    testWidgets(
      'bloqueia avançar/concluir a partir da 5ª etapa enquanto nenhuma '
      'religião for selecionada',
      (tester) async {
        await pumpOnboarding(tester);

        await advanceToReligionStep(tester);

        expect(find.byType(ReligionStep), findsOneWidget);
        expect(find.text('Começar minha jornada'), findsOneWidget);

        final button =
            tester.widget<ElevatedButton>(find.byType(ElevatedButton));
        expect(button.onPressed, isNull);
      },
    );

    testWidgets(
      'fluxo completo com religião selecionada envia religion correto para '
      'updatePreferences e navega para /home',
      (tester) async {
        when(
          () => mockRepo.updatePreferences(
            budget: any(named: 'budget'),
            rhythm: any(named: 'rhythm'),
            groupType: any(named: 'groupType'),
            interests: any(named: 'interests'),
            religion: any(named: 'religion'),
          ),
        ).thenAnswer((_) async {});

        await pumpOnboarding(tester);

        await advanceToReligionStep(tester);

        await tester.tap(find.text('Evangélica'));
        await tester.pumpAndSettle();

        final button =
            tester.widget<ElevatedButton>(find.byType(ElevatedButton));
        expect(button.onPressed, isNotNull);

        await tester.tap(find.text('Começar minha jornada'));
        await tester.pumpAndSettle();

        verify(
          () => mockRepo.updatePreferences(
            budget: 'ECONOMY',
            rhythm: 'RELAXED',
            groupType: 'SOLO',
            interests: ['cultura'],
            religion: 'EVANGELICAL',
          ),
        ).called(1);

        expect(find.text('Home Screen'), findsOneWidget);
      },
    );
  });
}
