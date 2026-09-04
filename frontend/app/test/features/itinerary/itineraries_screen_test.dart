// Testes da feature "Item B — novo ponto de entrada para gerar roteiro na
// aba Roteiros" (frontend), atualizados pra cobrir a feature multi-destino
// (item 7 do backlog, ver luvoy-pendencias.md): o botão "Criar roteiro"
// (tanto o FAB quanto o do estado vazio) agora abre um pequeno seletor
// ("Roteiro simples" / "Viagem com vários destinos") em vez de ir direto
// pro wizard de um destino só — e a tela passou a consumir tripsProvider
// (lista agrupada) em vez de itinerariesProvider (lista plana).

import 'package:app/features/destinations/presentation/providers/destination_provider.dart';
import 'package:app/features/itinerary/data/itinerary_models.dart';
import 'package:app/features/itinerary/data/itinerary_repository.dart';
import 'package:app/features/itinerary/presentation/providers/itinerary_provider.dart';
import 'package:app/features/itinerary/presentation/screens/itineraries_screen.dart';
import 'package:app/features/itinerary/presentation/widgets/generate_itinerary_sheet.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

class MockItineraryRepository extends Mock implements ItineraryRepository {}

void main() {
  late MockItineraryRepository mockRepo;

  setUp(() {
    mockRepo = MockItineraryRepository();
  });

  ItinerarySummaryModel sampleItinerary() => ItinerarySummaryModel(
    id: 'itin-1',
    title: 'Viagem a Paris',
    destinationName: 'Paris',
    destinationHeroImageUrl: '',
    startDate: DateTime(2026, 9, 1),
    endDate: DateTime(2026, 9, 4),
    dayCount: 3,
  );

  List<TripSummaryModel> sampleTrips() => [
    TripSummaryModel(
      tripGroupId: null,
      title: sampleItinerary().title,
      startDate: sampleItinerary().startDate,
      endDate: sampleItinerary().endDate,
      legs: [sampleItinerary()],
    ),
  ];

  List<Override> commonOverrides() => [
    itineraryRepositoryProvider.overrideWithValue(mockRepo),
    // Preventivo para quando "Criar roteiro" abrir o wizard sem
    // destino pré-definido (modo cidade-primeiro), que passa a
    // observar estes providers.
    destinationsProvider.overrideWith((ref) async => const []),
    destinationSearchProvider.overrideWith((ref, query) async => const []),
  ];

  Widget buildScreen(List<TripSummaryModel> trips) {
    return ProviderScope(
      overrides: [
        tripsProvider.overrideWith((ref) async => trips),
        ...commonOverrides(),
      ],
      child: const MaterialApp(home: ItinerariesScreen()),
    );
  }

  // ── (i) Botão "Criar roteiro" sempre visível → abre o seletor ──────────

  testWidgets(
    'botão "Criar roteiro" existe mesmo com a lista não-vazia, e ao tocar '
    'abre o seletor com as duas opções (roteiro simples / viagem multi-destino)',
    (tester) async {
      await tester.pumpWidget(buildScreen(sampleTrips()));
      await tester.pumpAndSettle();

      expect(find.text('Criar roteiro'), findsOneWidget);

      await tester.tap(find.text('Criar roteiro'));
      await tester.pumpAndSettle();

      expect(find.text('Roteiro simples'), findsOneWidget);
      expect(find.text('Viagem com vários destinos'), findsOneWidget);
    },
  );

  testWidgets(
    'tocar em "Roteiro simples" no seletor abre o wizard sem destino pré-definido',
    (tester) async {
      await tester.pumpWidget(buildScreen(sampleTrips()));
      await tester.pumpAndSettle();

      await tester.tap(find.text('Criar roteiro'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Roteiro simples'));
      await tester.pumpAndSettle();

      expect(find.byType(GenerateItinerarySheet), findsOneWidget);

      final sheet = tester.widget<GenerateItinerarySheet>(
        find.byType(GenerateItinerarySheet),
      );
      expect(sheet.destinationId, isNull);
      expect(sheet.destinationName, isNull);
    },
  );

  // ── (j) Estado vazio ────────────────────────────────────────────────────

  testWidgets('estado vazio mostra o botão "Criar roteiro" (não mais "Explorar '
      'destinos") e tocá-lo abre o mesmo seletor com as duas opções — é '
      'justamente na primeira viagem que a escolha mais importa', (
    tester,
  ) async {
    await tester.pumpWidget(buildScreen(const []));
    await tester.pumpAndSettle();

    expect(find.text('Explorar destinos'), findsNothing);
    expect(find.text('Criar roteiro'), findsOneWidget);

    await tester.tap(find.text('Criar roteiro'));
    await tester.pumpAndSettle();

    expect(find.text('Roteiro simples'), findsOneWidget);
    expect(find.text('Viagem com vários destinos'), findsOneWidget);
  });
}
