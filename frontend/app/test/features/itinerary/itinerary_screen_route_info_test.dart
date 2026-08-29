// Testes de integração: RouteInfoBar dentro da tela do roteiro
// (itinerary_screen.dart).
//
// Contexto: RouteInfoBar (lib/features/maps/presentation/widgets/
// route_info_bar.dart) e directionsProvider já têm cobertura própria em
// test/features/maps/maps_test.dart (configured true/false, loading/erro) —
// este arquivo cobre só a INTEGRAÇÃO: entre duas atividades consecutivas do
// mesmo slot (_SlotSection em itinerary_screen.dart), mostrar a distância/
// duração da rota entre os dois POIs, com fallback gracioso quando faltar
// lat/lng em algum dos dois ou quando o slot tiver uma atividade só.
//
// Decisão de escopo: a rota é mostrada entre atividades consecutivas DENTRO
// do mesmo slot (manhã/tarde/noite) — não entre o fim de um slot e o início
// do próximo, para não exigir reestruturar _ItineraryBody.

import 'package:app/features/currency/data/currency_rates_model.dart';
import 'package:app/features/currency/presentation/providers/currency_provider.dart';
import 'package:app/features/itinerary/data/itinerary_models.dart';
import 'package:app/features/itinerary/data/itinerary_repository.dart';
import 'package:app/features/itinerary/presentation/providers/itinerary_provider.dart';
import 'package:app/features/itinerary/presentation/screens/itinerary_screen.dart';
import 'package:app/features/maps/data/maps_models.dart';
import 'package:app/features/maps/presentation/providers/maps_provider.dart';
import 'package:app/features/maps/presentation/widgets/route_info_bar.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

class MockItineraryRepository extends Mock implements ItineraryRepository {}

void main() {
  const museu = PoiDetailModel(
    id: 'poi-museu',
    title: 'Museu de Arte Moderna',
    category: 'MUSEUM',
    budgetTier: 'MODERATE',
    latitude: -23.5505,
    longitude: -46.6333,
  );

  const restaurante = PoiDetailModel(
    id: 'poi-restaurante',
    title: 'Restaurante Central',
    category: 'RESTAURANT',
    budgetTier: 'MODERATE',
    latitude: -23.5605,
    longitude: -46.6433,
  );

  const semCoordenadas = PoiDetailModel(
    id: 'poi-sem-coords',
    title: 'Feira de Rua',
    category: 'ACTIVITY',
    budgetTier: 'BUDGET',
  );

  ItineraryModel sampleItinerary(List<ItineraryActivityModel> activities) {
    return ItineraryModel(
      id: 'itin-1',
      title: 'Viagem a São Paulo',
      destinationName: 'São Paulo',
      destinationHeroImageUrl: '',
      startDate: DateTime(2026, 9, 1),
      endDate: DateTime(2026, 9, 3),
      days: [
        ItineraryDayModel(
          dayNumber: 1,
          date: DateTime(2026, 9, 1),
          activities: activities,
        ),
      ],
    );
  }

  Future<void> pumpItineraryScreen(
    WidgetTester tester, {
    required ItineraryModel itinerary,
    List<Override> extraOverrides = const [],
  }) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          itineraryRepositoryProvider
              .overrideWithValue(MockItineraryRepository()),
          itineraryDetailProvider(itinerary.id)
              .overrideWith((ref) async => itinerary),
          currencyRatesProvider.overrideWith(
            (ref) async => const CurrencyRatesModel(
              eurToBrl: 5.90,
              gbpToBrl: 6.85,
              usdToBrl: 5.55,
            ),
          ),
          ...extraOverrides,
        ],
        child: MaterialApp(home: ItineraryScreen(id: itinerary.id)),
      ),
    );
    await tester.pumpAndSettle();
    // directionsProvider resolve em outro microtask além do pumpAndSettle
    // inicial (mesmo padrão de test/features/maps/maps_test.dart).
    await tester.pump();
    await tester.pump();
  }

  testWidgets(
    'mostra RouteInfoBar com distância/duração entre duas atividades '
    'consecutivas do mesmo slot que têm lat/lng',
    (tester) async {
      final itinerary = sampleItinerary([
        const ItineraryActivityModel(
            id: 'act-1', slot: 'MORNING', orderInSlot: 1, poi: museu),
        const ItineraryActivityModel(
            id: 'act-2', slot: 'MORNING', orderInSlot: 2, poi: restaurante),
      ]);

      await pumpItineraryScreen(
        tester,
        itinerary: itinerary,
        extraOverrides: [
          directionsProvider((
            museu.latitude!,
            museu.longitude!,
            restaurante.latitude!,
            restaurante.longitude!,
          )).overrideWith((ref) async => const DirectionsModel(
                configured: true,
                distanceMeters: 1500.0,
                durationSeconds: 900,
                distanceText: '1,5 km',
                durationText: '15 min',
              )),
        ],
      );

      expect(find.byType(RouteInfoBar), findsOneWidget);
      expect(find.text('1,5 km · 15 min'), findsOneWidget);
    },
  );

  testWidgets(
    'não mostra RouteInfoBar quando uma das duas atividades não tem lat/lng',
    (tester) async {
      final itinerary = sampleItinerary([
        const ItineraryActivityModel(
            id: 'act-1', slot: 'MORNING', orderInSlot: 1, poi: museu),
        const ItineraryActivityModel(
            id: 'act-2',
            slot: 'MORNING',
            orderInSlot: 2,
            poi: semCoordenadas),
      ]);

      await pumpItineraryScreen(tester, itinerary: itinerary);

      expect(find.byType(RouteInfoBar), findsNothing);
    },
  );

  testWidgets(
    'não mostra RouteInfoBar quando o slot tem uma única atividade',
    (tester) async {
      final itinerary = sampleItinerary([
        const ItineraryActivityModel(
            id: 'act-1', slot: 'MORNING', orderInSlot: 1, poi: museu),
      ]);

      await pumpItineraryScreen(tester, itinerary: itinerary);

      expect(find.byType(RouteInfoBar), findsNothing);
    },
  );

  testWidgets(
    'com 3 atividades no mesmo slot, mostra 2 RouteInfoBar (entre 1-2 e 2-3)',
    (tester) async {
      const parqueIbirapuera = PoiDetailModel(
        id: 'poi-parque',
        title: 'Parque Ibirapuera',
        category: 'LANDMARK',
        budgetTier: 'BUDGET',
        latitude: -23.5874,
        longitude: -46.6576,
      );

      final itinerary = sampleItinerary([
        const ItineraryActivityModel(
            id: 'act-1', slot: 'MORNING', orderInSlot: 1, poi: museu),
        const ItineraryActivityModel(
            id: 'act-2', slot: 'MORNING', orderInSlot: 2, poi: restaurante),
        const ItineraryActivityModel(
            id: 'act-3',
            slot: 'MORNING',
            orderInSlot: 3,
            poi: parqueIbirapuera),
      ]);

      await pumpItineraryScreen(
        tester,
        itinerary: itinerary,
        extraOverrides: [
          directionsProvider((
            museu.latitude!,
            museu.longitude!,
            restaurante.latitude!,
            restaurante.longitude!,
          )).overrideWith((ref) async => const DirectionsModel(
                configured: true,
                distanceMeters: 1500.0,
                durationSeconds: 900,
                distanceText: '1,5 km',
                durationText: '15 min',
              )),
          directionsProvider((
            restaurante.latitude!,
            restaurante.longitude!,
            parqueIbirapuera.latitude!,
            parqueIbirapuera.longitude!,
          )).overrideWith((ref) async => const DirectionsModel(
                configured: true,
                distanceMeters: 800.0,
                durationSeconds: 480,
                distanceText: '800 m',
                durationText: '8 min',
              )),
        ],
      );

      expect(find.byType(RouteInfoBar), findsNWidgets(2));
      expect(find.text('1,5 km · 15 min'), findsOneWidget);
      expect(find.text('800 m · 8 min'), findsOneWidget);
    },
  );
}
