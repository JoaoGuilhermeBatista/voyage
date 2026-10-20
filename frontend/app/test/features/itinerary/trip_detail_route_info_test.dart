// Testes de integração: conector de deslocamento ENTRE CIDADES na tela de
// detalhe de uma viagem multi-destino (trip_detail_screen.dart), item #19
// do backlog ("tempo de deslocamento entre... Cidades").
//
// directionsBetweenDestinationsProvider já tem cobertura própria no backend
// (MapsServiceTest/MapsControllerTest) — este arquivo cobre só a
// INTEGRAÇÃO: entre dois trechos consecutivos de uma viagem, mostrar a
// distância/duração entre as cidades, com fallback gracioso (nada
// renderizado) quando não configurado ou sem rota conhecida.

import 'package:app/features/itinerary/data/itinerary_models.dart';
import 'package:app/features/itinerary/presentation/providers/itinerary_provider.dart';
import 'package:app/features/itinerary/presentation/screens/trip_detail_screen.dart';
import 'package:app/features/maps/data/maps_models.dart';
import 'package:app/features/maps/presentation/providers/maps_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  TripSummaryModel sampleTrip() => TripSummaryModel(
        tripGroupId: 'trip-1',
        title: 'Paris + Roma',
        startDate: DateTime(2026, 9, 1),
        endDate: DateTime(2026, 9, 8),
        legs: [
          ItinerarySummaryModel(
            id: 'leg-paris',
            title: 'Paris',
            destinationId: 'dest-paris',
            destinationName: 'Paris',
            destinationHeroImageUrl: '',
            startDate: DateTime(2026, 9, 1),
            endDate: DateTime(2026, 9, 4),
            dayCount: 4,
          ),
          ItinerarySummaryModel(
            id: 'leg-roma',
            title: 'Roma',
            destinationId: 'dest-roma',
            destinationName: 'Roma',
            destinationHeroImageUrl: '',
            startDate: DateTime(2026, 9, 5),
            endDate: DateTime(2026, 9, 8),
            dayCount: 4,
          ),
        ],
      );

  Future<void> pumpTripDetailScreen(
    WidgetTester tester, {
    required TripSummaryModel trip,
    List<Override> extraOverrides = const [],
  }) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          tripDetailProvider(trip.tripGroupId!).overrideWith((ref) async => trip),
          ...extraOverrides,
        ],
        child: MaterialApp(home: TripDetailScreen(tripGroupId: trip.tripGroupId!)),
      ),
    );
    await tester.pumpAndSettle();
    await tester.pump();
    await tester.pump();
  }

  testWidgets(
    'mostra distância/duração entre as cidades dos dois trechos quando a rota resolve',
    (tester) async {
      await pumpTripDetailScreen(
        tester,
        trip: sampleTrip(),
        extraOverrides: [
          directionsBetweenDestinationsProvider(('dest-paris', 'dest-roma'))
              .overrideWith((ref) async => const DirectionsModel(
                    configured: true,
                    distanceMeters: 1400000.0,
                    durationSeconds: 54000,
                    distanceText: '1.400 km',
                    durationText: '15 h',
                  )),
        ],
      );

      expect(find.textContaining('1.400 km · 15 h'), findsOneWidget);
    },
  );

  testWidgets(
    'não mostra nada quando o Google Maps não está configurado no backend',
    (tester) async {
      await pumpTripDetailScreen(
        tester,
        trip: sampleTrip(),
        extraOverrides: [
          directionsBetweenDestinationsProvider(('dest-paris', 'dest-roma'))
              .overrideWith((ref) async => const DirectionsModel(configured: false)),
        ],
      );

      expect(find.textContaining('até o próximo destino'), findsNothing);
    },
  );

  testWidgets(
    'não mostra nada quando não há rota conhecida entre as duas cidades',
    (tester) async {
      await pumpTripDetailScreen(
        tester,
        trip: sampleTrip(),
        extraOverrides: [
          directionsBetweenDestinationsProvider(('dest-paris', 'dest-roma'))
              .overrideWith((ref) async => const DirectionsModel(configured: true)),
        ],
      );

      expect(find.textContaining('até o próximo destino'), findsNothing);
    },
  );
}
