// Testes para a feature "editar preferências no Perfil".
//
// Cobre:
//   1) A camada de dados (UserRepository.updatePreferences / .fetchMe) que a
//      tela de edição consome.
//   2) O comportamento da EditPreferencesScreen: pré-preenchimento a partir
//      do userProvider, habilitação do botão Salvar apenas quando algo muda,
//      feedback de sucesso/erro e proteção contra duplo submit.

import 'dart:async';

import 'package:app/core/services/api_client.dart';
import 'package:app/features/onboarding/presentation/widgets/preference_steps.dart';
import 'package:app/features/profile/data/user_model.dart';
import 'package:app/features/profile/data/user_repository.dart';
import 'package:app/features/profile/presentation/providers/user_provider.dart';
import 'package:app/features/profile/presentation/screens/edit_preferences_screen.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:mocktail/mocktail.dart';

class MockApiClient extends Mock implements ApiClient {}

class MockDio extends Mock implements Dio {}

class MockUserRepository extends Mock implements UserRepository {}

void main() {
  late MockApiClient mockApiClient;
  late MockDio mockDio;
  late UserRepository repository;

  setUpAll(() {
    // Fallback necessário para o mocktail conseguir usar `any()` com Options.
    registerFallbackValue(Options());
  });

  setUp(() {
    mockApiClient = MockApiClient();
    mockDio = MockDio();
    when(() => mockApiClient.dio).thenReturn(mockDio);
    repository = UserRepository(mockApiClient);
  });

  group('UserRepository.updatePreferences (camada de dados já existente)', () {
    test(
      'teste1_montaPatchCorretoParaUsersPreferences_comBodyEsperado',
      () async {
        when(
          () => mockDio.patch(
            any(),
            data: any(named: 'data'),
          ),
        ).thenAnswer(
          (_) async => Response(
            requestOptions: RequestOptions(path: '/users/preferences'),
            statusCode: 200,
            data: {'message': 'Preferences updated'},
          ),
        );

        await repository.updatePreferences(
          budget: 'MODERATE',
          rhythm: 'RELAXED',
          groupType: 'COUPLE',
          interests: ['praia', 'gastronomia'],
        );

        final captured = verify(
          () => mockDio.patch(
            captureAny(),
            data: captureAny(named: 'data'),
          ),
        ).captured;

        expect(captured[0], '/users/preferences');
        expect(
          captured[1],
          equals({
            'budget': 'MODERATE',
            'rhythm': 'RELAXED',
            'groupType': 'COUPLE',
            'interests': ['praia', 'gastronomia'],
          }),
        );
      },
    );

    test(
      'teste4_incluiReligionNoBody_quandoFornecida',
      () async {
        when(
          () => mockDio.patch(
            any(),
            data: any(named: 'data'),
          ),
        ).thenAnswer(
          (_) async => Response(
            requestOptions: RequestOptions(path: '/users/preferences'),
            statusCode: 200,
            data: {'message': 'Preferences updated'},
          ),
        );

        await repository.updatePreferences(
          budget: 'MODERATE',
          rhythm: 'RELAXED',
          groupType: 'COUPLE',
          interests: ['praia', 'gastronomia'],
          religion: 'CATHOLIC',
        );

        final captured = verify(
          () => mockDio.patch(
            captureAny(),
            data: captureAny(named: 'data'),
          ),
        ).captured;

        expect(captured[0], '/users/preferences');
        expect(
          captured[1],
          equals({
            'budget': 'MODERATE',
            'rhythm': 'RELAXED',
            'groupType': 'COUPLE',
            'interests': ['praia', 'gastronomia'],
            'religion': 'CATHOLIC',
          }),
        );
      },
    );

    test(
      'teste2_propagaExcecao_quandoApiRetornaErro400',
      () async {
        final requestOptions = RequestOptions(path: '/users/preferences');
        when(
          () => mockDio.patch(
            any(),
            data: any(named: 'data'),
          ),
        ).thenThrow(
          DioException(
            requestOptions: requestOptions,
            type: DioExceptionType.badResponse,
            response: Response(
              requestOptions: requestOptions,
              statusCode: 400,
              data: {
                'error': 'BAD_REQUEST',
                'message': 'Budget é obrigatório',
                'status': 400,
              },
            ),
          ),
        );

        expect(
          () => repository.updatePreferences(
            budget: 'MODERATE',
            rhythm: 'RELAXED',
            groupType: 'COUPLE',
            interests: ['praia'],
          ),
          throwsA(isA<DioException>()),
        );
      },
    );
  });

  group('UserRepository.fetchMe (usado para pré-popular a tela de edição)', () {
    test(
      'teste3_retornaUserModelPopulado_apartirDeJsonMockado',
      () async {
        when(() => mockDio.get('/users/me')).thenAnswer(
          (_) async => Response(
            requestOptions: RequestOptions(path: '/users/me'),
            statusCode: 200,
            data: {
              'id': 'user-123',
              'email': 'teste@luvoy.com',
              'avatarUrl': 'https://avatar.example.com/u.png',
              'onboardingCompleted': true,
              'isPremium': false,
              'budget': 'MODERATE',
              'rhythm': 'RELAXED',
              'groupType': 'COUPLE',
              'interests': ['praia', 'gastronomia'],
            },
          ),
        );

        final UserModel user = await repository.fetchMe();

        expect(user.id, 'user-123');
        expect(user.email, 'teste@luvoy.com');
        expect(user.avatarUrl, 'https://avatar.example.com/u.png');
        expect(user.onboardingCompleted, true);
        expect(user.isPremium, false);
        expect(user.budget, 'MODERATE');
        expect(user.rhythm, 'RELAXED');
        expect(user.groupType, 'COUPLE');
        expect(user.interests, ['praia', 'gastronomia']);
      },
    );
  });

  group('UserModel.fromJson', () {
    test(
      'teste5_populaReligion_apartirDeJson',
      () {
        final user = UserModel.fromJson({
          'id': 'user-123',
          'email': 'teste@luvoy.com',
          'onboardingCompleted': true,
          'isPremium': false,
          'budget': 'MODERATE',
          'rhythm': 'RELAXED',
          'groupType': 'COUPLE',
          'interests': ['praia', 'gastronomia'],
          'religion': 'EVANGELICAL',
        });

        expect(user.religion, 'EVANGELICAL');
      },
    );

    test(
      'teste6_religionNula_quandoAusenteNoJson',
      () {
        final user = UserModel.fromJson({
          'id': 'user-123',
          'email': 'teste@luvoy.com',
          'onboardingCompleted': true,
          'isPremium': false,
        });

        expect(user.religion, isNull);
      },
    );
  });

  // ───────────────────────────────────────────────────────────────────────
  // Comportamento da EditPreferencesScreen.
  //
  // Decisão de UX: o botão "Salvar" inicia DESABILITADO e só habilita quando
  // o usuário altera pelo menos um campo em relação ao estado carregado do
  // backend. Justificativa: evita PATCHs desnecessários e comunica
  // claramente ao usuário que não há mudanças pendentes — mesmo padrão
  // comum em telas de "editar perfil".
  // ───────────────────────────────────────────────────────────────────────
  group('EditPreferencesScreen', () {
    const testUser = UserModel(
      id: 'user-123',
      email: 'teste@luvoy.com',
      avatarUrl: 'https://avatar.example.com/u.png',
      onboardingCompleted: true,
      isPremium: false,
      budget: 'MODERATE',
      rhythm: 'RELAXED',
      groupType: 'COUPLE',
      interests: ['gastronomia', 'natureza'],
      religion: 'CATHOLIC',
    );

    late MockUserRepository mockRepo;
    late GoRouter router;

    setUp(() {
      mockRepo = MockUserRepository();
    });

    Widget buildTestApp({UserModel? user = testUser}) {
      router = GoRouter(
        initialLocation: '/perfil',
        routes: [
          GoRoute(
            path: '/perfil',
            builder: (context, state) =>
                const Scaffold(body: Text('Perfil Screen')),
          ),
          GoRoute(
            path: '/perfil/preferencias',
            builder: (context, state) => const EditPreferencesScreen(),
          ),
        ],
      );

      return ProviderScope(
        overrides: [
          userProvider.overrideWith((ref) async => user),
          userRepositoryProvider.overrideWithValue(mockRepo),
        ],
        child: MaterialApp.router(routerConfig: router),
      );
    }

    Future<void> openEditScreen(WidgetTester tester, {UserModel? user = testUser}) async {
      await tester.pumpWidget(buildTestApp(user: user));
      // Deixa o FutureProvider resolver o estado de loading -> data.
      await tester.pumpAndSettle();
      router.push('/perfil/preferencias');
      await tester.pumpAndSettle();
    }

    testWidgets(
      'tela carrega com os valores atuais do usuário pré-selecionados '
      '(budget/rhythm/groupType/interests vindos de userProvider/fetchMe)',
      (tester) async {
        await openEditScreen(tester);

        final budgetStep = tester.widget<BudgetStep>(find.byType(BudgetStep));
        final rhythmStep = tester.widget<RhythmStep>(find.byType(RhythmStep));
        final groupTypeStep =
            tester.widget<GroupTypeStep>(find.byType(GroupTypeStep));
        final interestsStep =
            tester.widget<InterestsStep>(find.byType(InterestsStep));
        final religionStep =
            tester.widget<ReligionStep>(find.byType(ReligionStep));

        expect(budgetStep.selected, 'MODERATE');
        expect(rhythmStep.selected, 'RELAXED');
        expect(groupTypeStep.selected, 'COUPLE');
        expect(interestsStep.selected, {'gastronomia', 'natureza'});
        expect(religionStep.selected, 'CATHOLIC');
      },
    );

    testWidgets(
      'botão Salvar inicia desabilitado e só habilita quando algum campo '
      'for alterado em relação ao valor carregado originalmente',
      (tester) async {
        await openEditScreen(tester);

        ElevatedButton saveButton() =>
            tester.widget<ElevatedButton>(find.byType(ElevatedButton));

        expect(saveButton().onPressed, isNull);

        await tester.tap(find.text('Luxo'));
        await tester.pumpAndSettle();

        expect(saveButton().onPressed, isNotNull);
      },
    );

    testWidgets(
      'botão Salvar habilita ao mudar apenas a religião em relação ao valor '
      'carregado originalmente',
      (tester) async {
        await openEditScreen(tester);

        ElevatedButton saveButton() =>
            tester.widget<ElevatedButton>(find.byType(ElevatedButton));

        expect(saveButton().onPressed, isNull);

        final religionFinder = find.text('Evangélica');
        await tester.ensureVisible(religionFinder);
        await tester.pumpAndSettle();
        await tester.tap(religionFinder);
        await tester.pumpAndSettle();

        expect(saveButton().onPressed, isNotNull);
      },
    );

    testWidgets(
      'ao salvar com sucesso, mostra feedback (snackbar/toast de sucesso) '
      'e navega de volta para a tela de Perfil',
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

        await openEditScreen(tester);

        await tester.tap(find.text('Luxo'));
        await tester.pumpAndSettle();

        await tester.tap(find.byType(ElevatedButton));
        await tester.pumpAndSettle();

        expect(find.text('Preferências atualizadas com sucesso!'), findsOneWidget);
        expect(find.byType(EditPreferencesScreen), findsNothing);
        expect(find.text('Perfil Screen'), findsOneWidget);

        verify(
          () => mockRepo.updatePreferences(
            budget: 'LUXURY',
            rhythm: 'RELAXED',
            groupType: 'COUPLE',
            interests: ['gastronomia', 'natureza'],
            religion: 'CATHOLIC',
          ),
        ).called(1);
      },
    );

    testWidgets(
      'ao mudar a religião e salvar com sucesso, envia o novo valor de '
      'religion para o repositório',
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

        await openEditScreen(tester);

        final religionFinder = find.text('Evangélica');
        await tester.ensureVisible(religionFinder);
        await tester.pumpAndSettle();
        await tester.tap(religionFinder);
        await tester.pumpAndSettle();

        await tester.tap(find.byType(ElevatedButton));
        await tester.pumpAndSettle();

        verify(
          () => mockRepo.updatePreferences(
            budget: 'MODERATE',
            rhythm: 'RELAXED',
            groupType: 'COUPLE',
            interests: ['gastronomia', 'natureza'],
            religion: 'EVANGELICAL',
          ),
        ).called(1);
      },
    );

    testWidgets(
      'ao salvar e a API retornar erro (ex: DioException 400/500), mostra '
      'snackbar de erro, mantém o usuário na tela e preserva os valores '
      'editados no formulário (não reseta para os valores antigos)',
      (tester) async {
        final requestOptions = RequestOptions(path: '/users/preferences');
        when(
          () => mockRepo.updatePreferences(
            budget: any(named: 'budget'),
            rhythm: any(named: 'rhythm'),
            groupType: any(named: 'groupType'),
            interests: any(named: 'interests'),
            religion: any(named: 'religion'),
          ),
        ).thenThrow(
          DioException(
            requestOptions: requestOptions,
            type: DioExceptionType.badResponse,
            response: Response(
              requestOptions: requestOptions,
              statusCode: 400,
              data: {
                'error': 'BAD_REQUEST',
                'message': 'Dados inválidos',
                'status': 400,
              },
            ),
          ),
        );

        await openEditScreen(tester);

        await tester.tap(find.text('Luxo'));
        await tester.pumpAndSettle();

        await tester.tap(find.byType(ElevatedButton));
        await tester.pumpAndSettle();

        expect(
          find.text('Erro ao salvar suas preferências. Tente novamente.'),
          findsOneWidget,
        );
        // Continua na tela de edição, sem navegar de volta.
        expect(find.byType(EditPreferencesScreen), findsOneWidget);

        // Os valores editados continuam preservados no formulário.
        final budgetStep = tester.widget<BudgetStep>(find.byType(BudgetStep));
        expect(budgetStep.selected, 'LUXURY');
      },
    );

    testWidgets(
      'enquanto o PATCH está em andamento, o botão Salvar fica desabilitado '
      '(ou mostra loading) para evitar duplo submit',
      (tester) async {
        final completer = Completer<void>();
        when(
          () => mockRepo.updatePreferences(
            budget: any(named: 'budget'),
            rhythm: any(named: 'rhythm'),
            groupType: any(named: 'groupType'),
            interests: any(named: 'interests'),
            religion: any(named: 'religion'),
          ),
        ).thenAnswer((_) => completer.future);

        await openEditScreen(tester);

        await tester.tap(find.text('Luxo'));
        await tester.pumpAndSettle();

        await tester.tap(find.byType(ElevatedButton));
        // Não usa pumpAndSettle aqui: o PATCH ainda está pendente.
        await tester.pump();

        final saveButton = tester.widget<ElevatedButton>(find.byType(ElevatedButton));
        expect(saveButton.onPressed, isNull);
        expect(find.byType(CircularProgressIndicator), findsOneWidget);

        // Libera o PATCH pendente e deixa o widget assentar para não vazar
        // uma Future pendente entre os testes.
        completer.complete();
        await tester.pumpAndSettle();
      },
    );
  });
}
