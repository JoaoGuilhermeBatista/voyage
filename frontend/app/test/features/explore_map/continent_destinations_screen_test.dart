// Teste da tela de destinos por continente (#26): filtra a lista vinda de
// destinationsProvider via continentOf(country) == continent, sem endpoint
// novo nem coluna nova no banco.

import 'package:app/features/destinations/data/destination_summary_model.dart';
import 'package:app/features/destinations/presentation/providers/destination_provider.dart';
import 'package:app/features/explore_map/domain/continent.dart';
import 'package:app/features/explore_map/presentation/screens/continent_destinations_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  DestinationSummaryModel destination(
    String slug,
    String name,
    String country,
  ) {
    return DestinationSummaryModel(
      id: slug,
      slug: slug,
      name: name,
      country: country,
      city: name,
      tagline: 'Tagline de $name',
      heroImageUrl: '',
      tags: const [],
    );
  }

  final paris = destination('paris', 'Paris', 'França');
  final tokyo = destination('tokyo', 'Tóquio', 'Japão');

  Widget buildScreen(Continent continent, List<DestinationSummaryModel> all) {
    return ProviderScope(
      overrides: [destinationsProvider.overrideWith((ref) async => all)],
      child: MaterialApp(
        home: ContinentDestinationsScreen(continent: continent),
      ),
    );
  }

  testWidgets('mostra só os destinos do continente selecionado', (
    tester,
  ) async {
    await tester.pumpWidget(buildScreen(Continent.europa, [paris, tokyo]));
    await tester.pumpAndSettle();

    expect(find.text('Paris'), findsOneWidget);
    expect(find.text('Tóquio'), findsNothing);
  });

  testWidgets('continente sem nenhum destino mostra estado vazio', (
    tester,
  ) async {
    await tester.pumpWidget(buildScreen(Continent.oceania, [paris, tokyo]));
    await tester.pumpAndSettle();

    expect(find.text('Ainda não temos destinos curados aqui'), findsOneWidget);
  });
}
