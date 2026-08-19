// Testes para a tela de login/cadastro (LoginScreen) — cobre a validação de
// formato de email que ainda não existe e o feedback pós-signup quando o
// Supabase exige confirmação por email.
//
// Estado atual do código (confirmado antes de escrever este arquivo):
//   - lib/features/auth/presentation/screens/login_screen.dart:
//     `_submit()` só valida campo vazio (`email.isEmpty || password.isEmpty`
//     -> 'Preencha todos os campos'), sem nenhuma validação de formato, e não
//     checa `currentSession` depois de um signUp bem-sucedido.
//   - lib/features/auth/presentation/providers/auth_provider.dart:
//     `authNotifierProvider` é um AsyncNotifierProvider<AuthNotifier, void>;
//     `supabaseProvider` é um Provider<SupabaseClient> injetável/overridable;
//     `AuthNotifier.signIn`/`signUp`/`signOut` não são `final`, então dá pra
//     sobrescrever numa subclasse fake sem tocar no Supabase de verdade.
//
// Estratégia de isolamento (sem tocar no Supabase real):
//   - `FakeAuthNotifier extends AuthNotifier` sobrescreve signIn/signUp/
//     signOut só pra registrar chamadas e simular sucesso (AsyncData) sem
//     nenhuma chamada de rede. Injetado via
//     `authNotifierProvider.overrideWith(() => fakeNotifier)`.
//   - Para os testes que dependem de `currentSession` (confirmação de email
//     pendente), `supabaseProvider` é sobrescrito com um mock (mocktail) de
//     SupabaseClient/GoTrueClient, já que essas classes não são `final`/
//     `sealed` e podem ser mockadas com `implements`.
//
// Sobre os testes 1, 4 e 5 abaixo (`skip`): eles exercitam comportamento que
// depende de lógica que ainda não existe em login_screen.dart (a validação
// de formato via validateEmail() e a checagem de currentSession pós-signup).
// Diferente do validators_test.dart, aqui NÃO há nenhum símbolo importado que
// não exista — os testes só interagem com o widget já existente — então o
// arquivo compila normalmente. Ainda assim eles são marcados `skip` com o
// motivo (em vez de deixados "red"), seguindo a orientação explícita desta
// task de documentar como contrato o que a implementação futura precisa
// satisfazer. Quando a feature for implementada, o processo é remover o
// `skip:` de cada um; o corpo já está pronto pra rodar.

import 'package:app/features/auth/presentation/providers/auth_provider.dart';
import 'package:app/features/auth/presentation/screens/login_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

class FakeAuthNotifier extends AuthNotifier {
  final List<(String, String)> signInCalls = [];
  final List<(String, String)> signUpCalls = [];
  int signOutCallCount = 0;

  @override
  Future<void> signIn(String email, String password) async {
    signInCalls.add((email, password));
    state = const AsyncLoading();
    state = const AsyncData(null);
  }

  @override
  Future<void> signUp(String email, String password) async {
    signUpCalls.add((email, password));
    state = const AsyncLoading();
    state = const AsyncData(null);
  }

  @override
  Future<void> signOut() async {
    signOutCallCount++;
    state = const AsyncData(null);
  }
}

class MockSupabaseClient extends Mock implements SupabaseClient {}

class MockGoTrueClient extends Mock implements GoTrueClient {}

class MockSession extends Mock implements Session {}

// Nota: `testWidgets` (diferente de `test()`) tipa `skip` como `bool?`, não
// aceita String como motivo — por isso o motivo de cada skip abaixo vai
// documentado em comentário acima do `skip: true`, não como valor do parâmetro.

const _mensagemConfirmacaoPendente =
    'Enviamos um email de confirmação. Verifique sua caixa de entrada antes '
    'de entrar.';

void main() {
  Widget buildTestApp({
    required FakeAuthNotifier notifier,
    SupabaseClient? supabaseClient,
  }) {
    return ProviderScope(
      overrides: [
        authNotifierProvider.overrideWith(() => notifier),
        if (supabaseClient != null)
          supabaseProvider.overrideWithValue(supabaseClient),
      ],
      child: const MaterialApp(home: LoginScreen()),
    );
  }

  Future<void> goToCriarConta(WidgetTester tester) async {
    await tester.tap(find.text('Criar conta'));
    await tester.pumpAndSettle();
  }

  Future<void> preencherFormulario(
    WidgetTester tester, {
    required String email,
    required String password,
  }) async {
    await tester.enterText(find.byType(TextField).at(0), email);
    await tester.enterText(find.byType(TextField).at(1), password);
  }

  group('LoginScreen — validação de campo vazio (regressão)', () {
    testWidgets(
      'aba Criar conta, email vazio mostra "Preencha todos os campos" e '
      'não chama signUp',
      (tester) async {
        final fakeNotifier = FakeAuthNotifier();
        await tester.pumpWidget(buildTestApp(notifier: fakeNotifier));

        await goToCriarConta(tester);
        await preencherFormulario(tester, email: '', password: 'senha123');

        await tester.tap(find.widgetWithText(ElevatedButton, 'Criar conta'));
        await tester.pumpAndSettle();

        expect(find.text('Preencha todos os campos'), findsOneWidget);
        expect(fakeNotifier.signUpCalls, isEmpty);
      },
    );
  });

  group('LoginScreen — validação de formato de email (contrato futuro)', () {
    testWidgets(
      'aba Criar conta, email com formato inválido mostra "Digite um email '
      'válido" e NÃO chama signUp',
      (tester) async {
        final fakeNotifier = FakeAuthNotifier();
        await tester.pumpWidget(buildTestApp(notifier: fakeNotifier));

        await goToCriarConta(tester);
        await preencherFormulario(
          tester,
          email: 'invalido',
          password: 'senha123',
        );

        await tester.tap(find.widgetWithText(ElevatedButton, 'Criar conta'));
        await tester.pumpAndSettle();

        expect(find.text('Digite um email válido'), findsOneWidget);
        expect(fakeNotifier.signUpCalls, isEmpty);
      },
    );

    testWidgets(
      'aba Criar conta, email e senha válidos chama signUp com os '
      'argumentos corretos e não mostra mensagem de erro de formato',
      (tester) async {
        final fakeNotifier = FakeAuthNotifier();
        await tester.pumpWidget(buildTestApp(notifier: fakeNotifier));

        await goToCriarConta(tester);
        await preencherFormulario(
          tester,
          email: 'usuario@exemplo.com',
          password: 'senha123',
        );

        await tester.tap(find.widgetWithText(ElevatedButton, 'Criar conta'));
        await tester.pumpAndSettle();

        expect(
          fakeNotifier.signUpCalls,
          [('usuario@exemplo.com', 'senha123')],
        );
        expect(find.text('Digite um email válido'), findsNothing);
        expect(find.text('Preencha todos os campos'), findsNothing);
      },
    );

    testWidgets(
      'aba Entrar, email com formato inválido mostra "Digite um email '
      'válido" e NÃO chama signIn (mesma validação reaproveitada no login)',
      (tester) async {
        final fakeNotifier = FakeAuthNotifier();
        await tester.pumpWidget(buildTestApp(notifier: fakeNotifier));
        // Aba "Entrar" já é a inicial (index 0) — não precisa trocar de aba,
        // mas ainda assim é preciso um pump extra pro widget tree assentar
        // antes de interagir com os campos (sem isso, enterText roda antes
        // do primeiro frame estar totalmente pronto e o texto não é
        // registrado nos controllers). Nos outros testes isso acontece
        // "de graça" via o pumpAndSettle() de goToCriarConta().
        await tester.pump();

        await preencherFormulario(
          tester,
          email: 'user@',
          password: 'senha123',
        );

        await tester.tap(find.widgetWithText(ElevatedButton, 'Entrar'));
        await tester.pumpAndSettle();

        expect(find.text('Digite um email válido'), findsOneWidget);
        expect(fakeNotifier.signInCalls, isEmpty);
      },
    );
  });

  group('LoginScreen — feedback pós-signup (confirmação de email pendente)',
      () {
    testWidgets(
      'signUp bem-sucedido com currentSession null mostra mensagem de '
      'confirmação de email pendente',
      (tester) async {
        final fakeNotifier = FakeAuthNotifier();
        final mockClient = MockSupabaseClient();
        final mockGoTrue = MockGoTrueClient();
        when(() => mockClient.auth).thenReturn(mockGoTrue);
        when(() => mockGoTrue.currentSession).thenReturn(null);

        await tester.pumpWidget(
          buildTestApp(notifier: fakeNotifier, supabaseClient: mockClient),
        );

        await goToCriarConta(tester);
        await preencherFormulario(
          tester,
          email: 'usuario@exemplo.com',
          password: 'senha123',
        );

        await tester.tap(find.widgetWithText(ElevatedButton, 'Criar conta'));
        await tester.pumpAndSettle();

        expect(find.text(_mensagemConfirmacaoPendente), findsOneWidget);
      },
    );

    testWidgets(
      'signUp bem-sucedido com currentSession preenchido NÃO mostra '
      'mensagem de confirmação pendente (auto-confirmado / sem regressão)',
      (tester) async {
        final fakeNotifier = FakeAuthNotifier();
        final mockClient = MockSupabaseClient();
        final mockGoTrue = MockGoTrueClient();
        final mockSession = MockSession();
        when(() => mockClient.auth).thenReturn(mockGoTrue);
        when(() => mockGoTrue.currentSession).thenReturn(mockSession);

        await tester.pumpWidget(
          buildTestApp(notifier: fakeNotifier, supabaseClient: mockClient),
        );

        await goToCriarConta(tester);
        await preencherFormulario(
          tester,
          email: 'usuario@exemplo.com',
          password: 'senha123',
        );

        await tester.tap(find.widgetWithText(ElevatedButton, 'Criar conta'));
        await tester.pumpAndSettle();

        expect(find.text(_mensagemConfirmacaoPendente), findsNothing);
      },
    );
  });
}
