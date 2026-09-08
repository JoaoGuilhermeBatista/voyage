// Testes da Home consumindo coleções editoriais reais (GET /collections)
// em vez do filtro fake por tag que existia antes — ver luvoy-pendencias.md.

import 'package:app/features/collections/data/collection_model.dart';
import 'package:app/features/collections/presentation/providers/collection_provider.dart';
import 'package:app/features/destinations/data/destination_summary_model.dart';
import 'package:app/features/destinations/presentation/providers/destination_provider.dart';
import 'package:app/features/home/presentation/screens/home_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  DestinationSummaryModel destination(String slug, String name) {
    return DestinationSummaryModel(
      id: slug,
      slug: slug,
      name: name,
      country: 'França',
      city: name,
      tagline: 'Tagline de $name',
      heroImageUrl: '',
      tags: const [],
    );
  }

  final paris = destination('paris', 'Paris');
  final roma = destination('roma', 'Roma');
  final tokyo = destination('tokyo', 'Tóquio');

  // A Home é um CustomScrollView longo (hero de 480 + header + carrosséis);
  // no tamanho padrão de teste, o conteúdo abaixo do hero fica fora do
  // viewport/cache extent e nem chega a ser construído. Aumentamos a tela
  // de teste pra caber tudo sem precisar de scroll manual em cada teste.
  void useTallSurface(WidgetTester tester) {
    tester.view.physicalSize = const Size(800, 4000);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.reset);
  }

  Widget buildScreen({
    required List<DestinationSummaryModel> destinations,
    required List<CollectionModel> collections,
  }) {
    return ProviderScope(
      overrides: [
        destinationsProvider.overrideWith((ref) async => destinations),
        destinationSearchProvider.overrideWith((ref, query) async => const []),
        collectionsProvider.overrideWith((ref) async => collections),
      ],
      child: const MaterialApp(home: HomeScreen()),
    );
  }

  testWidgets(
    'mostra seções de coleção real (título + eyebrow do backend) para '
    'cada coleção que tenha ao menos 1 destino',
    (tester) async {
      useTallSurface(tester);
      final collection = CollectionModel(
        id: 'c1',
        slug: 'romantic',
        title: 'Destinos românticos',
        eyebrow: 'Coleção',
        orderIndex: 0,
        destinations: [paris, roma],
      );

      await tester.pumpWidget(
        buildScreen(
          destinations: [paris, roma, tokyo],
          collections: [collection],
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Destinos românticos'), findsOneWidget);
    },
  );

  testWidgets('coleção sem destinos (lista vazia) não aparece na Home', (
    tester,
  ) async {
    useTallSurface(tester);
    final empty = CollectionModel(
      id: 'c2',
      slug: 'vazia',
      title: 'Coleção Vazia',
      orderIndex: 0,
      destinations: const [],
    );

    await tester.pumpWidget(
      buildScreen(destinations: [paris], collections: [empty]),
    );
    await tester.pumpAndSettle();

    expect(find.text('Coleção Vazia'), findsNothing);
  });

  testWidgets('sem coleções (lista vazia da API) a Home continua mostrando os '
      'destinos em destaque normalmente', (tester) async {
    useTallSurface(tester);
    await tester.pumpWidget(
      buildScreen(destinations: [paris, roma], collections: const []),
    );
    await tester.pumpAndSettle();

    expect(find.text('Destinos'), findsOneWidget);
    expect(find.text('Paris'), findsWidgets);
  });
}
