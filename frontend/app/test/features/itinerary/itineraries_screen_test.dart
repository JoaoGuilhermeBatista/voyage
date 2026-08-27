// Testes da feature "Item B — novo ponto de entrada para gerar roteiro na
// aba Roteiros" (frontend). Fase TDD "testes primeiro": nenhuma lógica de
// negócio nova foi implementada ainda (botão "Criar roteiro", troca do
// estado vazio) — os testes abaixo documentam o contrato esperado e ficam
// RED de propósito (falha de asserção clara, não erro de compilação) até o
// próximo agente implementar.

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

  List<ItinerarySummaryModel> sampleItineraries() => [
        ItinerarySummaryModel(
          id: 'itin-1',
          title: 'Viagem a Paris',
          destinationName: 'Paris',
          destinationHeroImageUrl: '',
          startDate: DateTime(2026, 9, 1),
          endDate: DateTime(2026, 9, 4),
          dayCount: 3,
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

  Widget buildScreen(List<ItinerarySummaryModel> itineraries) {
    return ProviderScope(
      overrides: [
        itinerariesProvider.overrideWith((ref) async => itineraries),
        ...commonOverrides(),
      ],
      child: const MaterialApp(home: ItinerariesScreen()),
    );
  }

  // ── (i) Botão "Criar roteiro" sempre visível ───────────────────────────

  testWidgets(
    'botão "Criar roteiro" existe mesmo com a lista de roteiros '
    'não-vazia, e ao tocar abre o wizard (GenerateItinerarySheet) sem '
    'destino pré-definido (RED — botão ainda não existe)',
    (tester) async {
      await tester.pumpWidget(buildScreen(sampleItineraries()));
      await tester.pumpAndSettle();

      expect(find.text('Criar roteiro'), findsOneWidget);

      await tester.tap(find.text('Criar roteiro'));
      await tester.pumpAndSettle();

      expect(find.byType(GenerateItinerarySheet), findsOneWidget);

      final sheet =
          tester.widget<GenerateItinerarySheet>(find.byType(GenerateItinerarySheet));
      expect(sheet.destinationId, isNull);
      expect(sheet.destinationName, isNull);
    },
  );

  // ── (j) Estado vazio ────────────────────────────────────────────────────

  testWidgets(
    'estado vazio mostra o botão "Criar roteiro" (não mais "Explorar '
    'destinos") e tocá-lo abre o wizard em vez de navegar para /home '
    '(RED — hoje o botão ainda se chama "Explorar destinos")',
    (tester) async {
      await tester.pumpWidget(buildScreen(const []));
      await tester.pumpAndSettle();

      expect(find.text('Explorar destinos'), findsNothing);
      expect(find.text('Criar roteiro'), findsOneWidget);

      await tester.tap(find.text('Criar roteiro'));
      await tester.pumpAndSettle();

      expect(find.byType(GenerateItinerarySheet), findsOneWidget);
    },
  );
}
