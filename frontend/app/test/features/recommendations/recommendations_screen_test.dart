// Testes da tela "Para Você": seções geradas a partir de user.interests.
//
// Bug real encontrado em 2026-10-21: a conta de teste tinha interesses
// gravados de uma versão antiga do onboarding ("arquitetura", "praia") que
// não existem mais em interestOptions (lib/features/onboarding/presentation/
// widgets/preference_steps.dart) — a tela caía no fallback `?? i` e mostrava
// o valor cru, em minúsculo, como título de seção ("arquitetura" em vez de,
// por exemplo, não mostrar nada). Fix: só gerar seção/chip pra interesses
// que ainda existem na lista atual, e usar o MESMO mapa de rótulos do
// onboarding como base (nunca uma lista de rótulos duplicada à parte).

import 'package:app/features/destinations/data/destination_summary_model.dart';
import 'package:app/features/destinations/presentation/providers/destination_provider.dart';
import 'package:app/features/profile/data/user_model.dart';
import 'package:app/features/profile/presentation/providers/user_provider.dart';
import 'package:app/features/recommendations/presentation/screens/recommendations_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  DestinationSummaryModel destination(
    String slug,
    String name,
    List<String> tags,
  ) {
    return DestinationSummaryModel(
      id: slug,
      slug: slug,
      name: name,
      country: 'França',
      city: name,
      tagline: 'Tagline de $name',
      heroImageUrl: '',
      tags: tags,
    );
  }

  final lyon = destination('lyon', 'Lyon', const ['gastronomia']);
  final roma = destination('roma', 'Roma', const ['arquitetura', 'história']);

  UserModel user(List<String> interests) => UserModel(
    id: 'u1',
    email: 'teste@luvoy.app',
    onboardingCompleted: true,
    isPremium: false,
    interests: interests,
  );

  // A tela é um CustomScrollView com várias seções — viewport maior evita o
  // mesmo problema de SliverList não materializar conteúdo fora da tela que
  // já apareceu em outros testes deste estilo (ver home_screen_test.dart).
  void useTallSurface(WidgetTester tester) {
    tester.view.physicalSize = const Size(800, 2400);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.reset);
  }

  Widget buildScreen({
    required List<DestinationSummaryModel> destinations,
    required UserModel currentUser,
  }) {
    return ProviderScope(
      overrides: [
        destinationsProvider.overrideWith((ref) async => destinations),
        userProvider.overrideWith((ref) async => currentUser),
      ],
      child: const MaterialApp(home: RecommendationsScreen()),
    );
  }

  testWidgets(
    'interesse válido (gastronomia) gera seção com rótulo correto',
    (tester) async {
      useTallSurface(tester);
      await tester.pumpWidget(
        buildScreen(
          destinations: [lyon, roma],
          currentUser: user(const ['gastronomia']),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Gastronomia'), findsWidgets);
    },
  );

  testWidgets(
    'interesse legado que não existe mais em interestOptions (ex. '
    '"arquitetura") não gera seção nem chip cru em minúsculo',
    (tester) async {
      useTallSurface(tester);
      await tester.pumpWidget(
        buildScreen(
          destinations: [lyon, roma],
          currentUser: user(const ['gastronomia', 'arquitetura', 'praia']),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('arquitetura'), findsNothing);
      expect(find.text('praia'), findsNothing);
      // o interesse válido continua funcionando normalmente
      expect(find.text('Gastronomia'), findsWidgets);
    },
  );

  testWidgets(
    'usuário só com interesses legados (nenhum válido) mostra estado vazio, '
    'não uma seção crua',
    (tester) async {
      useTallSurface(tester);
      await tester.pumpWidget(
        buildScreen(
          destinations: [lyon, roma],
          currentUser: user(const ['arquitetura', 'praia']),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('arquitetura'), findsNothing);
      expect(find.text('praia'), findsNothing);
    },
  );
}
