// Reproduz o crash reportado ao fechar o dialog "Sobre o LUVOY":
//
//   You have popped the last page off of the stack, there are no pages
//   left to show. Failed assertion: line 162 pos 7:
//   'currentConfiguration.isNotEmpty'
//
// Causa raiz: ProfileScreen._showAbout usa `builder: (_) => AlertDialog(...)`
// (descartando o context próprio do dialog) e o botão "Fechar" chama
// `Navigator.of(context).pop()` usando o `context` de FORA (o da própria
// ProfileScreen), capturado por closure. Como a ProfileScreen vive dentro
// de um StatefulShellRoute (bottom nav com navigator aninhado por aba), e
// showDialog usa useRootNavigator:true por padrão, `Navigator.of(context)`
// a partir do context externo resolve para o navigator ANINHADO da aba
// (que só tem uma página), não para o navigator raiz onde o dialog foi
// empilhado — popar esse navigator remove a última página da aba,
// derrubando o GoRouterDelegate.
//
// Este teste monta uma árvore mínima com StatefulShellRoute (reproduzindo
// a mesma estrutura de navegação aninhada do app real) — não pumpa o
// LuvoyApp/app_router.dart completo porque seu redirect chama
// Supabase.instance.client de forma síncrona, exigindo
// Supabase.initialize() (inviável no sandbox de teste; mesma limitação já
// documentada em widget_test.dart). O userProvider é sobrescrito
// diretamente, então o corpo real do provider (que depende de Supabase)
// nunca executa.

import 'package:app/features/profile/data/user_model.dart';
import 'package:app/features/profile/presentation/providers/user_provider.dart';
import 'package:app/features/profile/presentation/screens/profile_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

const _fakeUser = UserModel(
  id: 'user-1',
  email: 'teste@luvoy.com',
  onboardingCompleted: true,
  isPremium: false,
);

Widget _buildAppWithNestedShell() {
  final router = GoRouter(
    initialLocation: '/perfil',
    routes: [
      StatefulShellRoute.indexedStack(
        builder: (context, state, navigationShell) => navigationShell,
        branches: [
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

  return ProviderScope(
    overrides: [userProvider.overrideWith((ref) async => _fakeUser)],
    child: MaterialApp.router(routerConfig: router),
  );
}

void main() {
  group('ProfileScreen — dialog "Sobre o LUVOY"', () {
    testWidgets(
      'teste1_fecharDialog_naoDeveDerrubarOGoRouter_quandoDentroDeShellRouteAninhado',
      (tester) async {
        await tester.pumpWidget(_buildAppWithNestedShell());
        await tester.pumpAndSettle();

        await tester.tap(find.text('Sobre o LUVOY'));
        await tester.pumpAndSettle();

        expect(find.text('Fechar'), findsOneWidget);

        await tester.tap(find.text('Fechar'));
        await tester.pumpAndSettle();

        final exception = tester.takeException();
        expect(
          exception,
          isNull,
          reason:
              'Fechar o dialog não deve lançar AssertionError do GoRouterDelegate',
        );
      },
    );

    testWidgets('teste2_perfilContinuaVisivel_apposFecharDialog', (
      tester,
    ) async {
      await tester.pumpWidget(_buildAppWithNestedShell());
      await tester.pumpAndSettle();

      await tester.tap(find.text('Sobre o LUVOY'));
      await tester.pumpAndSettle();

      await tester.tap(find.text('Fechar'));
      await tester.pumpAndSettle();
      tester.takeException(); // não deixa a exceção vazar para o próximo expect

      expect(find.text('Sobre o LUVOY'), findsOneWidget);

      // "Sair" fica abaixo da viewport fixa do teste (a linha "Tema" do
      // seletor de tema empurrou o conteúdo) — o ListView só materializa
      // esse widget depois de rolar até ele.
      await tester.dragUntilVisible(
        find.text('Sair'),
        find.byType(ListView),
        const Offset(0, -100),
      );
      await tester.pumpAndSettle();
      expect(find.text('Sair'), findsOneWidget);
    });
  });
}
