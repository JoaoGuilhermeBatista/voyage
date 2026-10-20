// Testes da feature "editar POI do roteiro" (frontend).
//
// Contexto: dentro de um roteiro já criado, o usuário toca num ícone de
// editar em cada atividade, vê até 3 POIs alternativos da mesma categoria e
// troca o POI da atividade. O BACKEND já está pronto em produção (ver
// backend/api/voyage/.../ItineraryController.java e ItineraryService.java,
// método getAlternatives/substituteActivity) — este arquivo cobre o
// FRONTEND: PoiDetailModel.images, ItineraryRepository.getAlternatives/
// substituteActivity, e a UI de _ActivityTile + bottom sheet de alternativas
// em itinerary_screen.dart.
//
// Decisões de UX confirmadas pelo usuário (implementadas exatamente assim):
// 1. Erro ao buscar alternativas → mensagem de erro DENTRO do bottom sheet
//    (não fecha), com botão "Tentar novamente".
// 2. Seleção de alternativa → o CARD INTEIRO é tocável (tap já seleciona e
//    dispara a troca, sem botão "Escolher" separado).
// 3. Erro ao trocar (substitute) → MANTÉM o sheet aberto, mostra snackbar de
//    erro, permite tentar de novo sem refazer a busca.
//
// Contrato do backend:
//   GET /api/v1/itineraries/{id}/activities/{activityId}/alternatives
//     -> 200, List<PoiDetailDTO> (até 3, mesma categoria, excluindo POIs já
//        usados no roteiro); 404 roteiro; 403 roteiro de outro usuário;
//        404 atividade inexistente.
//   PATCH /api/v1/itineraries/{id}/activities/{activityId}
//     body { "poiId": "<uuid>" } -> 200, ActivityDTO com o novo poi;
//     404/403 roteiro; 404 atividade; 404 poiId inexistente; 403 poiId de
//     destino diferente do roteiro.
//   PoiDetailDTO: id, title, category, description, address, latitude,
//     longitude, images (List<String>), budgetTier, averageDurationMinutes.
//
// Nota sobre acesso a _ActivityTile/_AlternativesSheet: essas classes são
// privadas ao arquivo itinerary_screen.dart (privacidade em Dart é por
// biblioteca/arquivo, não por herança), então os testes de UI (6-8) montam a
// árvore a partir do widget público `ItineraryScreen` (com
// itineraryDetailProvider e itineraryRepositoryProvider sobrescritos) em vez
// de instanciar os widgets privados diretamente — comportamento observável
// idêntico ao descrito no contrato original.

import 'package:app/features/currency/data/currency_rates_model.dart';
import 'package:app/features/currency/presentation/providers/currency_provider.dart';
import 'package:app/features/itinerary/data/itinerary_models.dart';
import 'package:app/features/itinerary/data/itinerary_repository.dart';
import 'package:app/features/itinerary/presentation/providers/itinerary_provider.dart';
import 'package:app/features/itinerary/presentation/screens/itinerary_screen.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

class MockDio extends Mock implements Dio {}

class MockItineraryRepository extends Mock implements ItineraryRepository {}

void main() {
  // Fixture no shape real do PoiDetailDTO do backend.
  Map<String, dynamic> poiJson({
    String id = 'poi-1',
    String title = 'Museu de Arte Moderna',
    String category = 'MUSEUM',
    String? description = 'Um museu incrível no centro da cidade.',
    String? address = 'Av. Paulista, 1000',
    List<String>? images = const ['https://example.com/foto.jpg'],
    String budgetTier = 'MODERATE',
    int? averageDurationMinutes = 90,
  }) {
    final json = <String, dynamic>{
      'id': id,
      'title': title,
      'category': category,
      'description': description,
      'address': address,
      'budgetTier': budgetTier,
      'averageDurationMinutes': averageDurationMinutes,
    };
    if (images != null) json['images'] = images;
    return json;
  }

  // Fixture de atividade de roteiro. `images` default vazia de propósito:
  // testes que não são sobre a thumbnail (6 e 7) não devem disparar
  // CachedNetworkImage nenhum, para não depender de rede real.
  ItineraryActivityModel sampleActivity({
    String id = 'act-1',
    String slot = 'MORNING',
    int orderInSlot = 1,
    String title = 'Museu Atual',
    List<String> images = const [],
  }) {
    return ItineraryActivityModel(
      id: id,
      slot: slot,
      orderInSlot: orderInSlot,
      poi: PoiDetailModel.fromJson(poiJson(title: title, images: images)),
    );
  }

  ItineraryModel sampleItinerary({
    String id = 'itin-1',
    required List<ItineraryActivityModel> activities,
  }) {
    return ItineraryModel(
      id: id,
      title: 'Viagem a São Paulo',
      destinationName: 'São Paulo',
      destinationHeroImageUrl: '', // sem hero image: evita CachedNetworkImage extra
      destinationCountry: '',
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

  // Monta ItineraryScreen com itineraryRepositoryProvider e
  // itineraryDetailProvider(itinerary.id) sobrescritos, e
  // currencyRatesProvider fixado (evita tocar em ApiClient/Supabase reais,
  // que não estão inicializados neste ambiente de teste).
  Future<void> pumpItineraryScreen(
    WidgetTester tester, {
    required ItineraryModel itinerary,
    required MockItineraryRepository mockRepo,
  }) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          itineraryRepositoryProvider.overrideWithValue(mockRepo),
          itineraryDetailProvider(itinerary.id)
              .overrideWith((ref) async => itinerary),
          currencyRatesProvider.overrideWith(
            (ref) async => const CurrencyRatesModel(
              eurToBrl: 5.90,
              gbpToBrl: 6.85,
              usdToBrl: 5.55,
            ),
          ),
        ],
        child: MaterialApp(home: ItineraryScreen(id: itinerary.id)),
      ),
    );
    await tester.pumpAndSettle();
  }

  group('PoiDetailModel.fromJson', () {
    test(
      'parseia os campos atualmente suportados (id, title, category, '
      'description, address, budgetTier, averageDurationMinutes)',
      () {
        final result = PoiDetailModel.fromJson(poiJson());

        expect(result.id, 'poi-1');
        expect(result.title, 'Museu de Arte Moderna');
        expect(result.category, 'MUSEUM');
        expect(result.description, 'Um museu incrível no centro da cidade.');
        expect(result.address, 'Av. Paulista, 1000');
        expect(result.budgetTier, 'MODERATE');
        expect(result.averageDurationMinutes, 90);
      },
    );

    test(
      'campos opcionais ausentes no JSON viram null',
      () {
        final json = poiJson()
          ..remove('description')
          ..remove('address')
          ..remove('averageDurationMinutes');

        final result = PoiDetailModel.fromJson(json);

        expect(result.description, isNull);
        expect(result.address, isNull);
        expect(result.averageDurationMinutes, isNull);
      },
    );

    test(
      'inclui images corretamente (lista populada e lista ausente/null → [])',
      () {
        final withImages = PoiDetailModel.fromJson(
          poiJson(images: ['a.jpg', 'b.jpg']),
        );
        expect(withImages.images, ['a.jpg', 'b.jpg']);

        final withoutImagesKey = PoiDetailModel.fromJson(
          poiJson(images: null), // sem a chave 'images' no JSON
        );
        expect(withoutImagesKey.images, <String>[]);

        final withNullImages = PoiDetailModel.fromJson({
          ...poiJson(images: null),
          'images': null,
        });
        expect(withNullImages.images, <String>[]);
      },
    );
  });

  group('ItineraryRepository.getAlternatives()', () {
    test(
      'faz GET em /itineraries/{id}/activities/{activityId}/alternatives '
      'e retorna List<PoiDetailModel> parseado do JSON',
      () async {
        final mockDio = MockDio();
        const itineraryId = 'itin-1';
        const activityId = 'act-1';

        when(() => mockDio.get(
              '/itineraries/$itineraryId/activities/$activityId/alternatives',
            )).thenAnswer((_) async => Response(
              data: [poiJson(id: 'poi-2'), poiJson(id: 'poi-3')],
              statusCode: 200,
              requestOptions: RequestOptions(
                path:
                    '/itineraries/$itineraryId/activities/$activityId/alternatives',
              ),
            ));

        final repository = ItineraryRepository(mockDio);
        final result =
            await repository.getAlternatives(itineraryId, activityId);

        verify(() => mockDio.get(
              '/itineraries/$itineraryId/activities/$activityId/alternatives',
            )).called(1);
        expect(result, hasLength(2));
        expect(result[0], isA<PoiDetailModel>());
        expect(result[0].id, 'poi-2');
      },
    );
  });

  group('ItineraryRepository.substituteActivity()', () {
    test(
      'faz PATCH em /itineraries/{id}/activities/{activityId} com body '
      "{'poiId': ...} e retorna ItineraryActivityModel parseado",
      () async {
        final mockDio = MockDio();
        const itineraryId = 'itin-1';
        const activityId = 'act-1';
        const poiId = 'poi-2';

        when(() => mockDio.patch(
              '/itineraries/$itineraryId/activities/$activityId',
              data: {'poiId': poiId},
            )).thenAnswer((_) async => Response(
              data: {
                'id': activityId,
                'slot': 'MORNING',
                'orderInSlot': 1,
                'poi': poiJson(id: poiId),
              },
              statusCode: 200,
              requestOptions: RequestOptions(
                path: '/itineraries/$itineraryId/activities/$activityId',
              ),
            ));

        final repository = ItineraryRepository(mockDio);
        final result = await repository.substituteActivity(
          itineraryId,
          activityId,
          poiId,
        );

        verify(() => mockDio.patch(
              '/itineraries/$itineraryId/activities/$activityId',
              data: {'poiId': poiId},
            )).called(1);
        expect(result, isA<ItineraryActivityModel>());
        expect(result.poi.id, poiId);
      },
    );
  });

  group('Propagação de erro', () {
    test(
      'erro de rede em getAlternatives/substituteActivity propaga de forma '
      'tratável (não trava a tela)',
      () {
        final mockDio = MockDio();
        final repository = ItineraryRepository(mockDio);

        when(() => mockDio.get(
              '/itineraries/itin-1/activities/act-1/alternatives',
            )).thenThrow(DioException(
          requestOptions: RequestOptions(path: '/x'),
          type: DioExceptionType.connectionError,
        ));
        expect(
          () => repository.getAlternatives('itin-1', 'act-1'),
          throwsA(isA<DioException>()),
        );

        when(() => mockDio.patch(
              '/itineraries/itin-1/activities/act-1',
              data: {'poiId': 'poi-2'},
            )).thenThrow(DioException(
          requestOptions: RequestOptions(path: '/x'),
          type: DioExceptionType.connectionError,
        ));
        expect(
          () => repository.substituteActivity('itin-1', 'act-1', 'poi-2'),
          throwsA(isA<DioException>()),
        );
      },
    );
  });

  group('_ActivityTile — edição de POI (UI)', () {
    testWidgets(
      'tocar no ícone de editar de um _ActivityTile abre o bottom sheet de '
      'alternativas',
      (tester) async {
        final mockRepo = MockItineraryRepository();
        final itinerary =
            sampleItinerary(activities: [sampleActivity(id: 'act-1')]);

        when(() => mockRepo.getAlternatives('itin-1', 'act-1'))
            .thenAnswer((_) async => [
                  PoiDetailModel.fromJson(
                    poiJson(id: 'poi-2', images: const []),
                  ),
                ]);

        await pumpItineraryScreen(
          tester,
          itinerary: itinerary,
          mockRepo: mockRepo,
        );

        await tester.tap(find.byIcon(Icons.edit_outlined));
        await tester.pumpAndSettle();

        expect(find.byType(BottomSheet), findsOneWidget);
        verify(() => mockRepo.getAlternatives('itin-1', 'act-1')).called(1);
      },
    );

    testWidgets(
      'erro ao buscar alternativas mostra mensagem dentro do sheet, com '
      'botão "Tentar novamente" que refaz a chamada',
      (tester) async {
        final mockRepo = MockItineraryRepository();
        final itinerary =
            sampleItinerary(activities: [sampleActivity(id: 'act-1')]);

        var callCount = 0;
        when(() => mockRepo.getAlternatives('itin-1', 'act-1'))
            .thenAnswer((_) async {
          callCount++;
          if (callCount == 1) {
            throw DioException(
              requestOptions: RequestOptions(path: '/x'),
              type: DioExceptionType.connectionError,
            );
          }
          return [PoiDetailModel.fromJson(poiJson(id: 'poi-2', images: const []))];
        });

        await pumpItineraryScreen(
          tester,
          itinerary: itinerary,
          mockRepo: mockRepo,
        );

        await tester.tap(find.byIcon(Icons.edit_outlined));
        await tester.pumpAndSettle();

        // Sheet continua aberto, mostrando erro + botão de retry.
        expect(find.byType(BottomSheet), findsOneWidget);
        expect(find.text('Tentar novamente'), findsOneWidget);

        await tester.tap(find.text('Tentar novamente'));
        await tester.pumpAndSettle();

        expect(find.byType(BottomSheet), findsOneWidget);
        verify(() => mockRepo.getAlternatives('itin-1', 'act-1')).called(2);
      },
    );

    testWidgets(
      'selecionar uma alternativa (tocando no card) chama substituteActivity '
      'e fecha o sheet',
      (tester) async {
        final mockRepo = MockItineraryRepository();
        final itinerary = sampleItinerary(
          activities: [sampleActivity(id: 'act-1', title: 'Museu Atual')],
        );

        when(() => mockRepo.getAlternatives('itin-1', 'act-1'))
            .thenAnswer((_) async => [
                  PoiDetailModel.fromJson(poiJson(
                    id: 'poi-2',
                    title: 'Museu de Arte Moderna',
                    images: const [],
                  )),
                ]);
        when(() => mockRepo.substituteActivity('itin-1', 'act-1', 'poi-2'))
            .thenAnswer((_) async => ItineraryActivityModel.fromJson({
                  'id': 'act-1',
                  'slot': 'MORNING',
                  'orderInSlot': 1,
                  'poi': poiJson(
                    id: 'poi-2',
                    title: 'Museu de Arte Moderna',
                    images: const [],
                  ),
                }));

        await pumpItineraryScreen(
          tester,
          itinerary: itinerary,
          mockRepo: mockRepo,
        );

        await tester.tap(find.byIcon(Icons.edit_outlined));
        await tester.pumpAndSettle();

        await tester.tap(find.text('Museu de Arte Moderna'));
        await tester.pumpAndSettle();

        verify(() =>
                mockRepo.substituteActivity('itin-1', 'act-1', 'poi-2'))
            .called(1);
        expect(find.byType(BottomSheet), findsNothing);
      },
    );

    testWidgets(
      'erro ao trocar (substitute) mantém o sheet aberto, mostra snackbar de '
      'erro e permite tentar de novo',
      (tester) async {
        final mockRepo = MockItineraryRepository();
        final itinerary = sampleItinerary(
          activities: [sampleActivity(id: 'act-1', title: 'Museu Atual')],
        );

        when(() => mockRepo.getAlternatives('itin-1', 'act-1'))
            .thenAnswer((_) async => [
                  PoiDetailModel.fromJson(poiJson(
                    id: 'poi-2',
                    title: 'Museu de Arte Moderna',
                    images: const [],
                  )),
                ]);
        when(() => mockRepo.substituteActivity('itin-1', 'act-1', 'poi-2'))
            .thenThrow(DioException(
          requestOptions: RequestOptions(path: '/x'),
          type: DioExceptionType.connectionError,
        ));

        await pumpItineraryScreen(
          tester,
          itinerary: itinerary,
          mockRepo: mockRepo,
        );

        await tester.tap(find.byIcon(Icons.edit_outlined));
        await tester.pumpAndSettle();

        await tester.tap(find.text('Museu de Arte Moderna'));
        await tester.pumpAndSettle();

        // Sheet permanece aberto (com a lista ainda visível) + snackbar de erro.
        expect(find.byType(BottomSheet), findsOneWidget);
        expect(find.text('Museu de Arte Moderna'), findsOneWidget);
        expect(find.byType(SnackBar), findsOneWidget);

        // Busca não foi refeita: só a chamada inicial de getAlternatives.
        verify(() => mockRepo.getAlternatives('itin-1', 'act-1')).called(1);
      },
    );

    // Nota: cada cenário abaixo faz seu próprio pumpWidget isolado (em vez
    // de reutilizar a mesma árvore/ProviderScope com um segundo pumpWidget)
    // porque o Riverpod não permite trocar o conjunto de overrides de um
    // ProviderScope já montado (assertion "Replaced the override..." ao
    // trocar de itineraryDetailProvider(idA) para itineraryDetailProvider(idB)
    // no mesmo container).
    testWidgets(
      '_ActivityTile mostra a primeira imagem de poi.images quando existir',
      (tester) async {
        final mockRepo = MockItineraryRepository();
        final itinerary = sampleItinerary(
          id: 'itin-with-images',
          activities: [
            sampleActivity(id: 'act-1', images: ['a.jpg', 'b.jpg']),
          ],
        );

        await tester.pumpWidget(
          ProviderScope(
            overrides: [
              itineraryRepositoryProvider.overrideWithValue(mockRepo),
              itineraryDetailProvider(itinerary.id)
                  .overrideWith((ref) async => itinerary),
              currencyRatesProvider.overrideWith(
                (ref) async => const CurrencyRatesModel(
                  eurToBrl: 5.90,
                  gbpToBrl: 6.85,
                  usdToBrl: 5.55,
                ),
              ),
            ],
            child: MaterialApp(home: ItineraryScreen(id: itinerary.id)),
          ),
        );
        // Um único pump (sem pumpAndSettle): resolve o FutureProvider e
        // reconstrói com os dados, sem esperar o carregamento real da
        // imagem de rede (que não existe neste ambiente de teste).
        await tester.pump();
        expect(find.byType(CachedNetworkImage), findsOneWidget);
      },
    );

    testWidgets(
      '_ActivityTile mostra um placeholder quando poi.images está vazia',
      (tester) async {
        final mockRepo = MockItineraryRepository();
        final itinerary = sampleItinerary(
          id: 'itin-without-images',
          activities: [sampleActivity(id: 'act-2', images: const [])],
        );

        await tester.pumpWidget(
          ProviderScope(
            overrides: [
              itineraryRepositoryProvider.overrideWithValue(mockRepo),
              itineraryDetailProvider(itinerary.id)
                  .overrideWith((ref) async => itinerary),
              currencyRatesProvider.overrideWith(
                (ref) async => const CurrencyRatesModel(
                  eurToBrl: 5.90,
                  gbpToBrl: 6.85,
                  usdToBrl: 5.55,
                ),
              ),
            ],
            child: MaterialApp(home: ItineraryScreen(id: itinerary.id)),
          ),
        );
        await tester.pump();
        expect(find.byType(CachedNetworkImage), findsNothing);
        expect(find.byIcon(Icons.image_outlined), findsOneWidget);
      },
    );
  });
}
