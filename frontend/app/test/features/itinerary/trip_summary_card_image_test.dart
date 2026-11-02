// Teste do card de viagem multi-destino (_TripSummaryCard,
// itineraries_screen.dart) mostrar uma foto de capa de verdade — antes só
// mostrava uma cor sólida de placeholder, mesmo quando o primeiro trecho da
// viagem já tinha `destinationHeroImageUrl` (bug real reportado pelo
// usuário em 2026-11-02, screenshot da tela "Roteiros").

import 'package:app/features/destinations/presentation/providers/destination_provider.dart';
import 'package:app/features/itinerary/data/itinerary_models.dart';
import 'package:app/features/itinerary/data/itinerary_repository.dart';
import 'package:app/features/itinerary/presentation/providers/itinerary_provider.dart';
import 'package:app/features/itinerary/presentation/screens/itineraries_screen.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

class MockItineraryRepository extends Mock implements ItineraryRepository {}

void main() {
  ItinerarySummaryModel leg(String id, String name, String heroImageUrl) {
    return ItinerarySummaryModel(
      id: id,
      title: 'Viagem a $name',
      destinationId: 'dest-$id',
      destinationName: name,
      destinationHeroImageUrl: heroImageUrl,
      startDate: DateTime(2026, 9, 5),
      endDate: DateTime(2026, 9, 8),
      dayCount: 3,
    );
  }

  Widget buildScreen(List<TripSummaryModel> trips) {
    return ProviderScope(
      overrides: [
        itineraryRepositoryProvider.overrideWithValue(
          MockItineraryRepository(),
        ),
        destinationsProvider.overrideWith((ref) async => const []),
        destinationSearchProvider.overrideWith((ref, query) async => const []),
        tripsProvider.overrideWith((ref) async => trips),
      ],
      child: const MaterialApp(home: ItinerariesScreen()),
    );
  }

  testWidgets(
    'viagem multi-destino com heroImageUrl no primeiro trecho mostra a foto '
    'de capa (CachedNetworkImage), não só a cor de placeholder',
    (tester) async {
      final trip = TripSummaryModel(
        tripGroupId: 'trip-1',
        title: 'Paris + Roma + Florença',
        startDate: DateTime(2026, 9, 5),
        endDate: DateTime(2026, 9, 18),
        legs: [
          leg('leg-paris', 'Paris', 'https://images.unsplash.com/paris.jpg'),
          leg('leg-roma', 'Roma', 'https://images.unsplash.com/roma.jpg'),
          leg('leg-floren', 'Florença', ''),
        ],
      );

      await tester.pumpWidget(buildScreen([trip]));
      // Um único pump (sem pumpAndSettle): resolve o FutureProvider e
      // reconstrói com os dados, sem esperar o carregamento real da imagem
      // de rede (que não existe neste ambiente de teste) — mesmo padrão
      // usado em itinerary_poi_edit_test.dart.
      await tester.pump();

      expect(find.text('Paris + Roma + Florença'), findsOneWidget);
      expect(find.text('3 destinos'), findsOneWidget);
      // A imagem usada é a do PRIMEIRO trecho (Paris), não a de Roma/Florença.
      final image = tester.widget<CachedNetworkImage>(
        find.byType(CachedNetworkImage),
      );
      expect(image.imageUrl, 'https://images.unsplash.com/paris.jpg');
    },
  );

  testWidgets(
    'viagem multi-destino sem nenhuma heroImageUrl nos trechos cai no '
    'placeholder de cor (comportamento anterior preservado)',
    (tester) async {
      final trip = TripSummaryModel(
        tripGroupId: 'trip-2',
        title: 'Lisboa + Porto',
        startDate: DateTime(2026, 10, 1),
        endDate: DateTime(2026, 10, 10),
        legs: [leg('leg-lisboa', 'Lisboa', ''), leg('leg-porto', 'Porto', '')],
      );

      await tester.pumpWidget(buildScreen([trip]));
      await tester.pumpAndSettle();

      expect(find.text('Lisboa + Porto'), findsOneWidget);
      expect(find.byType(CachedNetworkImage), findsNothing);
    },
  );
}
