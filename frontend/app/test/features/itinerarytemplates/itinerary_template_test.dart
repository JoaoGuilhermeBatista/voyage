// Testes da feature "templates de roteiro curados" (frontend) — item 1 do
// backlog. O BACKEND já está pronto em produção e testado (72/72 verde):
//   GET  /api/v1/destinations/{slug}/itinerary-templates            (público)
//     -> [{id, title, description, durationDays, budgetTier}]
//   GET  /api/v1/destinations/{slug}/itinerary-templates/{templateId} (público)
//     -> {id, title, description, durationDays, budgetTier, destinationId,
//         destinationName, days: [{dayNumber, activities: [{slot,
//         orderInSlot, poi: PoiDetailDTO}]}]}
//   POST /api/v1/itinerary-templates/{templateId}/adopt (JWT) {"startDate"}
//     -> MESMO shape de GET /itineraries/{id} (reaproveita ItineraryModel).
//
// Esta task é TDD "testes primeiro" para o FRONTEND: só a camada de dados
// (models + repository + providers) é mecânica o bastante (parsing de JSON,
// montagem de URL/body) para ser implementada de verdade nesta fase — segue
// o mesmo precedente de CurrencyRepository.getRates()/MapsRepository em
// currency_test.dart/maps_test.dart, onde chamadas GET/POST + parse simples
// foram escritas e testadas de verdade, não deixadas em stub.
//
// A camada de apresentação (TemplatePreviewScreen inteira, e a renderização
// dos cards em _CuratedTemplatesSection dentro de destination_screen.dart)
// fica em STUB proposital (corpo `throw UnimplementedError()`) porque essa
// combinação (dia-a-dia agrupado por slot, fluxo de date-picker, chamada de
// adopt, navegação, snackbar de erro) é grande demais para esta fase
// somente-testes — ver nota completa em
// lib/features/itinerarytemplates/presentation/screens/template_preview_screen.dart.
// Os testes de UI correspondentes ficam `skip: true`, com o contrato exato
// documentado em comentário dentro de cada um (mesma convenção do teste
// `convert()` skip-ado em currency_test.dart) — Dart analisa/compila o
// arquivo de teste inteiro mesmo com skip: true, então essas seções só
// referenciam símbolos que já existem hoje (find, pumpWidget, os models e
// providers reais), nunca lógica de dentro dos widgets stub.
//
// Exceção: _CuratedTemplatesSliver (o wrapper que decide esconder a seção
// em loading/error/lista-vazia) FOI implementado de verdade, porque:
//  1. é mecânico/trivial — mesmo padrão de fallback gracioso já usado em
//     _CurrencyRatesBar (itinerary_screen.dart) e RouteInfoBar (maps);
//  2. DestinationScreen já está em uso — deixar esse wrapper em stub faria
//     QUALQUER navegação para qualquer destino quebrar (o CustomScrollView
//     builda todos os slivers da lista, não só os visíveis), mesmo para
//     destinos sem nenhum template configurado (o caso comum hoje). Only o
//     branch "lista não-vazia" (que delega para o stub
//     _CuratedTemplatesSection) fica pendente.

import 'package:app/core/services/api_client.dart';
import 'package:app/features/destinations/data/destination_detail_model.dart';
import 'package:app/features/destinations/presentation/providers/destination_provider.dart';
import 'package:app/features/destinations/presentation/screens/destination_screen.dart';
import 'package:app/features/favorites/data/favorite_model.dart';
import 'package:app/features/favorites/presentation/providers/favorite_provider.dart';
import 'package:app/features/itinerary/data/itinerary_models.dart';
import 'package:app/features/itinerarytemplates/data/itinerary_template_models.dart';
import 'package:app/features/itinerarytemplates/data/itinerary_template_repository.dart';
import 'package:app/features/itinerarytemplates/presentation/providers/itinerary_template_provider.dart';
import 'package:app/features/itinerarytemplates/presentation/screens/template_preview_screen.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

class MockApiClient extends Mock implements ApiClient {}

class MockDio extends Mock implements Dio {}

class MockItineraryTemplateRepository extends Mock
    implements ItineraryTemplateRepository {}

// Substitui FavoriteNotifier.build() (que dependeria de authStateProvider /
// FavoriteRepository / ApiClient / Supabase reais) por uma lista vazia
// fixa, só para permitir montar DestinationScreen nestes testes.
class _FakeFavoriteNotifier extends FavoriteNotifier {
  @override
  Future<List<FavoriteModel>> build() async => [];
}

void main() {
  late MockApiClient mockApiClient;
  late MockDio mockDio;

  // Fixture do PoiDetailDTO (mesmo shape usado em itinerary_poi_edit_test.dart).
  Map<String, dynamic> poiJson({String id = 'poi-1'}) => {
        'id': id,
        'title': 'Museu de Arte Moderna',
        'category': 'MUSEUM',
        'description': 'Um museu incrível no centro da cidade.',
        'address': 'Av. Paulista, 1000',
        'images': <String>[],
        'budgetTier': 'MODERATE',
        'averageDurationMinutes': 90,
      };

  Map<String, dynamic> templateSummaryJson({
    String id = 'tpl-1',
    String title = 'Paris Romântica',
    int durationDays = 3,
    String budgetTier = 'MODERATE',
  }) =>
      {
        'id': id,
        'title': title,
        'description': 'Um roteiro de 3 dias pelos clássicos de Paris.',
        'durationDays': durationDays,
        'budgetTier': budgetTier,
      };

  Map<String, dynamic> templateDetailJson({String id = 'tpl-1'}) => {
        'id': id,
        'title': 'Paris Romântica',
        'description': 'Um roteiro de 3 dias pelos clássicos de Paris.',
        'durationDays': 3,
        'budgetTier': 'MODERATE',
        'destinationId': 'dest-1',
        'destinationName': 'Paris',
        'days': [
          {
            'dayNumber': 1,
            'activities': [
              {'slot': 'MORNING', 'orderInSlot': 2, 'poi': poiJson(id: 'poi-2')},
              {'slot': 'MORNING', 'orderInSlot': 1, 'poi': poiJson(id: 'poi-1')},
              {
                'slot': 'AFTERNOON',
                'orderInSlot': 1,
                'poi': poiJson(id: 'poi-3'),
              },
            ],
          },
        ],
      };

  setUpAll(() {
    registerFallbackValue(Options());
  });

  setUp(() {
    mockApiClient = MockApiClient();
    mockDio = MockDio();
    when(() => mockApiClient.dio).thenReturn(mockDio);
  });

  // ── 1. Models ──────────────────────────────────────────────────────────

  group('ItineraryTemplateSummaryModel.fromJson', () {
    test('parseia id/title/description/durationDays/budgetTier', () {
      final result =
          ItineraryTemplateSummaryModel.fromJson(templateSummaryJson());

      expect(result.id, 'tpl-1');
      expect(result.title, 'Paris Romântica');
      expect(result.description,
          'Um roteiro de 3 dias pelos clássicos de Paris.');
      expect(result.durationDays, 3);
      expect(result.budgetTier, 'MODERATE');
    });
  });

  group('ItineraryTemplateDetailModel.fromJson', () {
    test(
      'parseia o shape completo, incluindo days/activities/poi aninhados',
      () {
        final result =
            ItineraryTemplateDetailModel.fromJson(templateDetailJson());

        expect(result.id, 'tpl-1');
        expect(result.destinationId, 'dest-1');
        expect(result.destinationName, 'Paris');
        expect(result.durationDays, 3);
        expect(result.budgetTier, 'MODERATE');
        expect(result.days, hasLength(1));

        final day = result.days.single;
        expect(day.dayNumber, 1);
        expect(day.activities, hasLength(3));
        expect(day.activities[0].slot, 'MORNING');
        expect(day.activities[0].orderInSlot, 2);
        expect(day.activities[0].poi, isA<PoiDetailModel>());
        expect(day.activities[0].poi.id, 'poi-2');
      },
    );
  });

  group('TemplateActivityModel.fromJson', () {
    test('parseia slot/orderInSlot e delega poi para PoiDetailModel.fromJson',
        () {
      final result = TemplateActivityModel.fromJson({
        'slot': 'EVENING',
        'orderInSlot': 2,
        'poi': poiJson(id: 'poi-9'),
      });

      expect(result.slot, 'EVENING');
      expect(result.orderInSlot, 2);
      expect(result.poi.id, 'poi-9');
      expect(result.poi.title, 'Museu de Arte Moderna');
    });
  });

  group('TemplateDayModel.activitiesForSlot', () {
    test('filtra por slot e ordena por orderInSlot', () {
      final day =
          TemplateDayModel.fromJson(templateDetailJson()['days'][0]);

      final morning = day.activitiesForSlot('MORNING');
      expect(morning, hasLength(2));
      expect(morning[0].poi.id, 'poi-1'); // orderInSlot 1
      expect(morning[1].poi.id, 'poi-2'); // orderInSlot 2

      final evening = day.activitiesForSlot('EVENING');
      expect(evening, isEmpty);
    });
  });

  // ── 2. Repository ─────────────────────────────────────────────────────

  group('ItineraryTemplateRepository.getTemplatesForDestination()', () {
    test(
      'faz GET em /destinations/{slug}/itinerary-templates e retorna a '
      'lista parseada',
      () async {
        when(() => mockDio.get('/destinations/paris/itinerary-templates'))
            .thenAnswer((_) async => Response(
                  data: [templateSummaryJson(id: 'tpl-1'),
                      templateSummaryJson(id: 'tpl-2')],
                  statusCode: 200,
                  requestOptions: RequestOptions(
                      path: '/destinations/paris/itinerary-templates'),
                ));

        final repository = ItineraryTemplateRepository(mockApiClient);
        final result = await repository.getTemplatesForDestination('paris');

        verify(() => mockDio.get('/destinations/paris/itinerary-templates'))
            .called(1);
        expect(result, hasLength(2));
        expect(result[0], isA<ItineraryTemplateSummaryModel>());
        expect(result[0].id, 'tpl-1');
        expect(result[1].id, 'tpl-2');
      },
    );

    test('propaga erro tratável (DioException) quando a chamada falha',
        () async {
      when(() => mockDio.get('/destinations/paris/itinerary-templates'))
          .thenThrow(DioException(
        requestOptions:
            RequestOptions(path: '/destinations/paris/itinerary-templates'),
        type: DioExceptionType.connectionError,
      ));

      final repository = ItineraryTemplateRepository(mockApiClient);
      expect(
        () => repository.getTemplatesForDestination('paris'),
        throwsA(isA<DioException>()),
      );
    });
  });

  group('ItineraryTemplateRepository.getTemplateDetail()', () {
    test(
      'faz GET em /destinations/{slug}/itinerary-templates/{templateId} e '
      'retorna o detalhe parseado',
      () async {
        when(() => mockDio.get(
              '/destinations/paris/itinerary-templates/tpl-1',
            )).thenAnswer((_) async => Response(
              data: templateDetailJson(),
              statusCode: 200,
              requestOptions: RequestOptions(
                  path: '/destinations/paris/itinerary-templates/tpl-1'),
            ));

        final repository = ItineraryTemplateRepository(mockApiClient);
        final result =
            await repository.getTemplateDetail('paris', 'tpl-1');

        verify(() => mockDio.get(
              '/destinations/paris/itinerary-templates/tpl-1',
            )).called(1);
        expect(result, isA<ItineraryTemplateDetailModel>());
        expect(result.id, 'tpl-1');
        expect(result.days, hasLength(1));
      },
    );

    test('propaga erro tratável (DioException) quando a chamada falha',
        () async {
      when(() => mockDio.get(
            '/destinations/paris/itinerary-templates/tpl-1',
          )).thenThrow(DioException(
        requestOptions: RequestOptions(
            path: '/destinations/paris/itinerary-templates/tpl-1'),
        type: DioExceptionType.connectionError,
      ));

      final repository = ItineraryTemplateRepository(mockApiClient);
      expect(
        () => repository.getTemplateDetail('paris', 'tpl-1'),
        throwsA(isA<DioException>()),
      );
    });
  });

  group('ItineraryTemplateRepository.adoptTemplate()', () {
    test(
      'faz POST em /itinerary-templates/{templateId}/adopt com '
      '{"startDate": "YYYY-MM-DD"} e retorna ItineraryModel parseado',
      () async {
        when(() => mockDio.post(
              '/itinerary-templates/tpl-1/adopt',
              data: {'startDate': '2026-09-01'},
            )).thenAnswer((_) async => Response(
              data: {
                'id': 'itin-42',
                'title': 'Viagem a Paris',
                'destinationName': 'Paris',
                'destinationHeroImageUrl': '',
                'startDate': '2026-09-01',
                'endDate': '2026-09-03',
                'days': <dynamic>[],
              },
              statusCode: 200,
              requestOptions:
                  RequestOptions(path: '/itinerary-templates/tpl-1/adopt'),
            ));

        final repository = ItineraryTemplateRepository(mockApiClient);
        final result = await repository.adoptTemplate(
          'tpl-1',
          DateTime(2026, 9, 1),
        );

        verify(() => mockDio.post(
              '/itinerary-templates/tpl-1/adopt',
              data: {'startDate': '2026-09-01'},
            )).called(1);
        expect(result, isA<ItineraryModel>());
        expect(result.id, 'itin-42');
        expect(result.title, 'Viagem a Paris');
      },
    );

    test(
      'formata a data com zero à esquerda (mês/dia de um dígito)',
      () async {
        when(() => mockDio.post(
              '/itinerary-templates/tpl-1/adopt',
              data: {'startDate': '2026-01-05'},
            )).thenAnswer((_) async => Response(
              data: {
                'id': 'itin-42',
                'title': 'Viagem a Paris',
                'destinationName': 'Paris',
                'destinationHeroImageUrl': '',
                'startDate': '2026-01-05',
                'endDate': '2026-01-07',
                'days': <dynamic>[],
              },
              statusCode: 200,
              requestOptions:
                  RequestOptions(path: '/itinerary-templates/tpl-1/adopt'),
            ));

        final repository = ItineraryTemplateRepository(mockApiClient);
        await repository.adoptTemplate('tpl-1', DateTime(2026, 1, 5));

        verify(() => mockDio.post(
              '/itinerary-templates/tpl-1/adopt',
              data: {'startDate': '2026-01-05'},
            )).called(1);
      },
    );

    test('propaga erro tratável (DioException) quando a chamada falha',
        () async {
      when(() => mockDio.post(
            '/itinerary-templates/tpl-1/adopt',
            data: any(named: 'data'),
          )).thenThrow(DioException(
        requestOptions:
            RequestOptions(path: '/itinerary-templates/tpl-1/adopt'),
        type: DioExceptionType.connectionError,
      ));

      final repository = ItineraryTemplateRepository(mockApiClient);
      expect(
        () => repository.adoptTemplate('tpl-1', DateTime(2026, 9, 1)),
        throwsA(isA<DioException>()),
      );
    });
  });

  // ── 3. Providers ──────────────────────────────────────────────────────

  group('templatesForDestinationProvider (Riverpod)', () {
    late MockItineraryTemplateRepository mockRepository;

    setUp(() {
      mockRepository = MockItineraryTemplateRepository();
    });

    test('estado inicial é AsyncLoading, depois AsyncData com a lista',
        () async {
      final container = ProviderContainer(overrides: [
        itineraryTemplateRepositoryProvider.overrideWithValue(mockRepository),
      ]);
      addTearDown(container.dispose);

      when(() => mockRepository.getTemplatesForDestination('paris'))
          .thenAnswer((_) async => [
                ItineraryTemplateSummaryModel.fromJson(
                    templateSummaryJson(id: 'tpl-1')),
              ]);

      expect(container.read(templatesForDestinationProvider('paris')),
          isA<AsyncLoading>());

      final result = await container
          .read(templatesForDestinationProvider('paris').future);
      expect(result, hasLength(1));
      expect(container.read(templatesForDestinationProvider('paris')),
          isA<AsyncData>());
    });
  });

  group('templateDetailProvider (Riverpod)', () {
    late MockItineraryTemplateRepository mockRepository;

    setUp(() {
      mockRepository = MockItineraryTemplateRepository();
    });

    test(
      'estado inicial é AsyncLoading, depois AsyncData com o detalhe, '
      'chaveado por (slug, templateId)',
      () async {
        final container = ProviderContainer(overrides: [
          itineraryTemplateRepositoryProvider
              .overrideWithValue(mockRepository),
        ]);
        addTearDown(container.dispose);

        when(() => mockRepository.getTemplateDetail('paris', 'tpl-1'))
            .thenAnswer((_) async =>
                ItineraryTemplateDetailModel.fromJson(templateDetailJson()));

        const key = ('paris', 'tpl-1');
        expect(
            container.read(templateDetailProvider(key)), isA<AsyncLoading>());

        final result =
            await container.read(templateDetailProvider(key).future);
        expect(result.id, 'tpl-1');
        expect(container.read(templateDetailProvider(key)), isA<AsyncData>());
      },
    );

    test(
      'chaves (slug, templateId) diferentes não compartilham cache — cada '
      'combinação dispara sua própria chamada',
      () async {
        final container = ProviderContainer(overrides: [
          itineraryTemplateRepositoryProvider
              .overrideWithValue(mockRepository),
        ]);
        addTearDown(container.dispose);

        when(() => mockRepository.getTemplateDetail('paris', 'tpl-1'))
            .thenAnswer((_) async => ItineraryTemplateDetailModel.fromJson(
                templateDetailJson(id: 'tpl-1')));
        when(() => mockRepository.getTemplateDetail('roma', 'tpl-2'))
            .thenAnswer((_) async => ItineraryTemplateDetailModel.fromJson(
                templateDetailJson(id: 'tpl-2')));

        final a = await container
            .read(templateDetailProvider(('paris', 'tpl-1')).future);
        final b = await container
            .read(templateDetailProvider(('roma', 'tpl-2')).future);

        expect(a.id, 'tpl-1');
        expect(b.id, 'tpl-2');
        verify(() => mockRepository.getTemplateDetail('paris', 'tpl-1'))
            .called(1);
        verify(() => mockRepository.getTemplateDetail('roma', 'tpl-2'))
            .called(1);
      },
    );
  });

  // ── 4. TemplatePreviewScreen (STUB — contrato documentado, skip: true) ──
  //
  // A implementação de produção (template_preview_screen.dart) lança
  // UnimplementedError propositalmente nesta fase; os testes abaixo
  // documentam o comportamento exato que o próximo agente precisa
  // satisfazer. `skip: true` evita rodar o body (que quebraria), mas o
  // arquivo continua sendo compilado/analisado — por isso só referencia
  // símbolos que já existem hoje (TemplatePreviewScreen, providers, models).

  group('TemplatePreviewScreen (UI)', () {
    testWidgets(
      'renderiza o conteúdo dia-a-dia (título, descrição, budget tier, '
      'seções "Dia 1"/"Dia 2"... com atividades agrupadas por slot em '
      'Manhã/Tarde/Noite) quando templateDetailProvider retorna dados',
      (tester) async {
        final detail =
            ItineraryTemplateDetailModel.fromJson(templateDetailJson());

        await tester.pumpWidget(
          ProviderScope(
            overrides: [
              templateDetailProvider(('paris', 'tpl-1'))
                  .overrideWith((ref) async => detail),
            ],
            child: const MaterialApp(
              home: TemplatePreviewScreen(slug: 'paris', templateId: 'tpl-1'),
            ),
          ),
        );
        await tester.pumpAndSettle();

        expect(find.text('Paris Romântica'), findsOneWidget);
        expect(find.textContaining('Dia 1'), findsOneWidget);
        expect(find.text('Manhã'), findsOneWidget);
        expect(find.text('Tarde'), findsOneWidget);
      },
    );

    testWidgets(
      'tocar em "Usar este roteiro" abre um seletor de data '
      '(showDatePicker / DatePickerDialog)',
      (tester) async {
        final detail =
            ItineraryTemplateDetailModel.fromJson(templateDetailJson());

        await tester.pumpWidget(
          ProviderScope(
            overrides: [
              templateDetailProvider(('paris', 'tpl-1'))
                  .overrideWith((ref) async => detail),
            ],
            child: const MaterialApp(
              home: TemplatePreviewScreen(slug: 'paris', templateId: 'tpl-1'),
            ),
          ),
        );
        await tester.pumpAndSettle();

        await tester.tap(find.text('Usar este roteiro'));
        await tester.pumpAndSettle();

        expect(find.byType(DatePickerDialog), findsOneWidget);
      },
    );

    testWidgets(
      'escolher uma data chama adoptTemplate(templateId, data) e navega '
      'para /roteiro/{itinerary.id} com o roteiro criado',
      (tester) async {
        final detail =
            ItineraryTemplateDetailModel.fromJson(templateDetailJson());
        final mockRepository = MockItineraryTemplateRepository();
        final adopted = ItineraryModel(
          id: 'itin-42',
          title: 'Viagem a Paris',
          destinationName: 'Paris',
          destinationHeroImageUrl: '',
          startDate: DateTime(2026, 9, 1),
          endDate: DateTime(2026, 9, 3),
          days: const [],
        );

        when(() => mockRepository.adoptTemplate('tpl-1', any()))
            .thenAnswer((_) async => adopted);

        await tester.pumpWidget(
          ProviderScope(
            overrides: [
              itineraryTemplateRepositoryProvider
                  .overrideWithValue(mockRepository),
              templateDetailProvider(('paris', 'tpl-1'))
                  .overrideWith((ref) async => detail),
            ],
            child: const MaterialApp(
              home: TemplatePreviewScreen(slug: 'paris', templateId: 'tpl-1'),
            ),
          ),
        );
        await tester.pumpAndSettle();

        // Contrato: tocar "Usar este roteiro" -> escolher uma data no
        // DatePickerDialog -> confirmar -> adoptTemplate é chamado com a
        // data escolhida -> tela navega para /roteiro/itin-42 (decisão: um
        // context.push simples é suficiente, já que não há bottom sheet
        // para dar pop antes, ao contrário de GenerateItinerarySheet).
        //
        // BUG DO TESTE ORIGINAL (corrigido): a versão skip:true desta task
        // não continha nenhuma interação (nem tap em "Usar este roteiro",
        // nem confirmação no DatePickerDialog) antes do verify() abaixo —
        // isso tornaria o teste impossível de satisfazer por uma
        // implementação real dirigida por toque (contradiz o teste anterior,
        // que exige que SÓ o tap no botão abra o seletor). Adicionado o
        // fluxo mínimo real: tocar o botão, aceitar a data pré-selecionada
        // (hoje) tocando "OK" no DatePickerDialog padrão do Material.
        await tester.tap(find.text('Usar este roteiro'));
        await tester.pumpAndSettle();
        await tester.tap(find.text('OK'));
        await tester.pumpAndSettle();

        verify(() => mockRepository.adoptTemplate('tpl-1', any())).called(1);
      },
    );

    testWidgets(
      'erro ao chamar adoptTemplate mostra snackbar e MANTÉM o usuário na '
      'tela de preview (não navega)',
      (tester) async {
        final detail =
            ItineraryTemplateDetailModel.fromJson(templateDetailJson());
        final mockRepository = MockItineraryTemplateRepository();

        when(() => mockRepository.adoptTemplate('tpl-1', any())).thenThrow(
          DioException(
            requestOptions:
                RequestOptions(path: '/itinerary-templates/tpl-1/adopt'),
            type: DioExceptionType.connectionError,
          ),
        );

        await tester.pumpWidget(
          ProviderScope(
            overrides: [
              itineraryTemplateRepositoryProvider
                  .overrideWithValue(mockRepository),
              templateDetailProvider(('paris', 'tpl-1'))
                  .overrideWith((ref) async => detail),
            ],
            child: const MaterialApp(
              home: TemplatePreviewScreen(slug: 'paris', templateId: 'tpl-1'),
            ),
          ),
        );
        await tester.pumpAndSettle();

        // Contrato: erro no adopt -> SnackBar de erro, tela de preview
        // continua montada (find.byType(TemplatePreviewScreen) presente).
        //
        // BUG DO TESTE ORIGINAL (corrigido): mesma lacuna do teste anterior
        // — a versão skip:true não continha nenhuma interação antes do
        // expect(). Adicionado o mesmo fluxo mínimo real (tap no botão +
        // confirmar data) para efetivamente disparar a chamada a
        // adoptTemplate (que aqui está mockada para lançar).
        await tester.tap(find.text('Usar este roteiro'));
        await tester.pumpAndSettle();
        await tester.tap(find.text('OK'));
        await tester.pumpAndSettle();

        expect(find.byType(SnackBar), findsOneWidget);
        expect(find.byType(TemplatePreviewScreen), findsOneWidget);
      },
    );
  });

  // ── 5. DestinationScreen — seção de templates curados ────────────────

  DestinationDetailModel sampleDestination({String slug = 'paris'}) {
    return DestinationDetailModel(
      id: 'dest-1',
      slug: slug,
      name: 'Paris',
      country: 'França',
      city: 'Paris',
      tagline: 'A cidade luz',
      heroImageUrl: '',
      tags: const [],
      sections: const [],
      pois: const [],
    );
  }

  Future<void> pumpDestinationScreen(
    WidgetTester tester, {
    required DestinationDetailModel destination,
    required List<Override> extraOverrides,
  }) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          destinationDetailProvider(destination.slug)
              .overrideWith((ref) async => destination),
          // _HeroAppBar observa destinationFavoriteIdProvider ->
          // favoritesProvider -> FavoriteRepository -> apiClientProvider ->
          // Supabase.instance, que não está inicializado neste ambiente de
          // teste (não faz parte desta feature) — fixado para evitar isso.
          favoritesProvider.overrideWith(() => _FakeFavoriteNotifier()),
          ...extraOverrides,
        ],
        child: MaterialApp(home: DestinationScreen(slug: destination.slug)),
      ),
    );
  }

  group('DestinationScreen — seção de templates curados', () {
    testWidgets(
      'lista vazia em templatesForDestinationProvider -> nenhuma seção '
      'extra é renderizada (comportamento real, não-stub)',
      (tester) async {
        final destination = sampleDestination();

        await pumpDestinationScreen(
          tester,
          destination: destination,
          extraOverrides: [
            templatesForDestinationProvider(destination.slug)
                .overrideWith((ref) async => const []),
          ],
        );
        await tester.pumpAndSettle();

        expect(find.text('ROTEIROS PRONTOS'), findsNothing);
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'loading ou erro em templatesForDestinationProvider não quebra a '
      'tela e não renderiza a seção (comportamento real, não-stub)',
      (tester) async {
        final destination = sampleDestination(slug: 'roma');

        await pumpDestinationScreen(
          tester,
          destination: destination,
          extraOverrides: [
            templatesForDestinationProvider(destination.slug)
                .overrideWith((ref) async {
              throw DioException(
                requestOptions:
                    RequestOptions(path: '/destinations/roma/itinerary-templates'),
                type: DioExceptionType.connectionError,
              );
            }),
          ],
        );
        await tester.pump();
        await tester.pump();

        expect(find.byType(DestinationScreen), findsOneWidget);
        expect(find.text('ROTEIROS PRONTOS'), findsNothing);
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'templatesForDestinationProvider com itens -> cards de template são '
      'renderizados (título, "N dias", chip de budget) e tocar navega para '
      '/destinos/{slug}/roteiros/{templateId} — STUB: _CuratedTemplatesSection '
      'lança UnimplementedError propositalmente, contrato documentado aqui',
      (tester) async {
        final destination = sampleDestination();

        await pumpDestinationScreen(
          tester,
          destination: destination,
          extraOverrides: [
            templatesForDestinationProvider(destination.slug)
                .overrideWith((ref) async => [
                      ItineraryTemplateSummaryModel.fromJson(
                          templateSummaryJson(id: 'tpl-1')),
                    ]),
            // Necessário para o passo de navegação abaixo: ao tocar o card,
            // TemplatePreviewScreen monta de verdade e observa
            // templateDetailProvider(('paris','tpl-1')). Sem este override,
            // ele cairia no repository/ApiClient reais, cujo interceptor
            // acessa Supabase.instance a cada chamada — não inicializado
            // neste ambiente de teste (mesmo motivo documentado acima para
            // favoritesProvider/_FakeFavoriteNotivier; bug de setup do teste
            // original, que faltava este override).
            templateDetailProvider(('paris', 'tpl-1')).overrideWith(
              (ref) async =>
                  ItineraryTemplateDetailModel.fromJson(templateDetailJson()),
            ),
          ],
        );
        await tester.pumpAndSettle();

        expect(find.text('Paris Romântica'), findsOneWidget);
        expect(find.textContaining('3 dias'), findsOneWidget);

        // A seção de templates curados fica abaixo do hero (320px) + header
        // do destino no CustomScrollView — no viewport padrão de teste
        // (800x600) ela nasce fora da área visível/tocável (atrás da
        // bottomNavigationBar fixa "Gerar Roteiro"). Sem este scroll, o tap
        // abaixo acerta a bottomNavigationBar em vez do card (bug do teste
        // original, que faltava essa rolagem antes do tap).
        await tester.drag(find.byType(CustomScrollView), const Offset(0, -600));
        await tester.pumpAndSettle();

        await tester.tap(find.text('Paris Romântica'));
        await tester.pumpAndSettle();

        expect(find.byType(TemplatePreviewScreen), findsOneWidget);
      },
    );
  });
}
