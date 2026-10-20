// Testes de integração: botão "Encerrar viagem" na tela do roteiro
// (itinerary_screen.dart), continuação dos itens #14/#15 do backlog.
//
// Contexto: numa viagem multi-destino A→B→C, o botão "Próximo destino"
// (já existente) só permite ANDAR PRA FRENTE entre os trechos — pra saír
// da viagem o usuário tinha que voltar trecho por trecho pela pilha de
// navegação. Este arquivo cobre o botão novo "Encerrar viagem", que usa
// `go` (não `push`) pra voltar direto pra visão geral da viagem
// (/roteiros/viagem/:tripGroupId), substituindo toda a pilha acumulada
// pelos "Próximo destino" anteriores — ou pra lista de roteiros, se por
// algum motivo o tripGroupId não vier.

import 'package:app/features/currency/data/currency_rates_model.dart';
import 'package:app/features/currency/presentation/providers/currency_provider.dart';
import 'package:app/features/itinerary/data/itinerary_models.dart';
import 'package:app/features/itinerary/data/itinerary_repository.dart';
import 'package:app/features/itinerary/presentation/providers/itinerary_provider.dart';
import 'package:app/features/itinerary/presentation/screens/itinerary_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:mocktail/mocktail.dart';

class MockItineraryRepository extends Mock implements ItineraryRepository {}

void main() {
  ItineraryModel sampleItinerary(String id) {
    return ItineraryModel(
      id: id,
      title: 'Viagem a Paris',
      destinationName: 'Paris',
      destinationHeroImageUrl: '',
      destinationCountry: 'França',
      startDate: DateTime(2026, 9, 1),
      endDate: DateTime(2026, 9, 3),
      days: const [],
    );
  }

  Future<void> pumpScreen(
    WidgetTester tester, {
    required ItineraryModel itinerary,
    required String location,
  }) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          itineraryRepositoryProvider
              .overrideWithValue(MockItineraryRepository()),
          itineraryDetailProvider(itinerary.id)
              .overrideWith((ref) async => itinerary),
          localCurrencyRateProvider.overrideWith(
            (ref, country) async => const LocalCurrencyRateModel(
              currencyCode: 'EUR',
              rateToBrl: 5.90,
            ),
          ),
        ],
        child: MaterialApp.router(
          routerConfig: GoRouter(
            initialLocation: location,
            routes: [
              GoRoute(
                path: '/roteiro/:id',
                builder: (context, state) {
                  final legIdsParam = state.uri.queryParameters['legIds'];
                  final legDayCountsParam =
                      state.uri.queryParameters['legDayCounts'];
                  return ItineraryScreen(
                    id: state.pathParameters['id']!,
                    legIds: legIdsParam == null || legIdsParam.isEmpty
                        ? const []
                        : legIdsParam.split(','),
                    legDayCounts:
                        legDayCountsParam == null || legDayCountsParam.isEmpty
                        ? const []
                        : legDayCountsParam
                              .split(',')
                              .map((s) => int.tryParse(s) ?? 0)
                              .toList(),
                    legIndex: int.tryParse(
                          state.uri.queryParameters['legIndex'] ?? '',
                        ) ??
                        0,
                    tripGroupId: state.uri.queryParameters['tripGroupId'],
                  );
                },
              ),
              GoRoute(
                path: '/roteiros',
                builder: (context, state) =>
                    const Scaffold(body: Text('TELA ROTEIROS')),
              ),
              GoRoute(
                path: '/roteiros/viagem/:tripGroupId',
                builder: (context, state) => Scaffold(
                  body: Text(
                    'TELA VIAGEM ${state.pathParameters['tripGroupId']}',
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  testWidgets(
    'roteiro avulso (sem legIds): não mostra nem "Próximo destino" nem "Encerrar viagem"',
    (tester) async {
      await pumpScreen(
        tester,
        itinerary: sampleItinerary('itin-1'),
        location: '/roteiro/itin-1',
      );

      expect(find.text('Próximo destino'), findsNothing);
      expect(find.text('Encerrar viagem'), findsNothing);
    },
  );

  testWidgets(
    'trecho do meio de uma viagem multi-destino: mostra "Próximo destino" e "Encerrar viagem", e tocar em "Encerrar viagem" vai direto pra visão geral da viagem',
    (tester) async {
      await pumpScreen(
        tester,
        itinerary: sampleItinerary('leg-a'),
        location:
            '/roteiro/leg-a?legIds=leg-a,leg-b,leg-c&legDayCounts=2,2,2'
            '&legIndex=0&tripGroupId=trip-123',
      );

      expect(find.text('Próximo destino'), findsOneWidget);
      expect(find.text('Encerrar viagem'), findsOneWidget);

      await tester.tap(find.text('Encerrar viagem'));
      await tester.pumpAndSettle();

      expect(find.text('TELA VIAGEM trip-123'), findsOneWidget);
    },
  );

  testWidgets(
    'último trecho de uma viagem multi-destino: NÃO mostra "Próximo destino", só "Encerrar viagem"',
    (tester) async {
      await pumpScreen(
        tester,
        itinerary: sampleItinerary('leg-c'),
        location:
            '/roteiro/leg-c?legIds=leg-a,leg-b,leg-c&legDayCounts=2,2,2'
            '&legIndex=2&tripGroupId=trip-123',
      );

      expect(find.text('Próximo destino'), findsNothing);
      expect(find.text('Encerrar viagem'), findsOneWidget);

      await tester.tap(find.text('Encerrar viagem'));
      await tester.pumpAndSettle();

      expect(find.text('TELA VIAGEM trip-123'), findsOneWidget);
    },
  );

  testWidgets(
    'último trecho sem tripGroupId na URL: "Encerrar viagem" cai de volta pra lista de roteiros',
    (tester) async {
      await pumpScreen(
        tester,
        itinerary: sampleItinerary('leg-c'),
        location:
            '/roteiro/leg-c?legIds=leg-a,leg-b,leg-c&legDayCounts=2,2,2&legIndex=2',
      );

      await tester.tap(find.text('Encerrar viagem'));
      await tester.pumpAndSettle();

      expect(find.text('TELA ROTEIROS'), findsOneWidget);
    },
  );
}
