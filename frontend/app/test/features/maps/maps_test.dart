// Testes da feature "integração com Google Maps" (frontend) — item 5 do
// backlog. Cobre:
//  1. DirectionsModel / EmbedUrlModel — parsing do shape real dos DTOs do
//     backend (DirectionsResponseDTO / EmbedUrlResponseDTO), configured
//     true e false.
//  2. MapsRepository — GET /maps/directions e /maps/embed-url via Dio
//     mockado (mesmo padrão de CurrencyRepository/currency_test.dart).
//  3. Providers Riverpod — directionsProvider/embedUrlProvider expõem
//     AsyncValue<...> (loading → data), cache por chave.
//  4. OpenInMapsButton — deep link SEM API key/backend (item 3 do
//     backlog): tocar o botão chama launchUrl com a URL esperada. Mocka
//     UrlLauncherPlatform.instance (plugin_platform_interface), já que é
//     a primeira vez que url_launcher é usado no projeto — não havia
//     convenção prévia de mock para ele no repo.
//  5. RouteInfoBar — mostra distância/duração quando configured=true;
//     não quebra a tela quando configured=false (mesmo espírito de
//     `_CurrencyRatesBar` em itinerary_screen.dart).
//
// Shape real dos DTOs confirmado em
// backend/api/voyage/.../features/maps/dto/{DirectionsResponseDTO,EmbedUrlResponseDTO}.java:
//   { "configured": true, "distanceMeters": 5200.0, "durationSeconds": 720,
//     "distanceText": "5.2 km", "durationText": "12 mins" }
//   { "configured": false, "distanceMeters": null, "durationSeconds": null,
//     "distanceText": null, "durationText": null }
//   { "configured": true, "embedUrl": "https://www.google.com/maps/embed/..." }
//   { "configured": false, "embedUrl": null }
//
// Endpoints /api/v1/maps/** EXIGEM JWT (diferente de /currency/**, que é
// público) — o JWT é anexado automaticamente pelo interceptor de
// ApiClient (core/services/api_client.dart), então não muda nada do lado
// do repository/provider aqui.

import 'package:app/core/services/api_client.dart';
import 'package:app/features/maps/data/maps_models.dart';
import 'package:app/features/maps/data/maps_repository.dart';
import 'package:app/features/maps/presentation/providers/maps_provider.dart';
import 'package:app/features/maps/presentation/widgets/map_embed_view.dart';
import 'package:app/features/maps/presentation/widgets/open_in_maps_button.dart';
import 'package:app/features/maps/presentation/widgets/poi_map_preview.dart';
import 'package:app/features/maps/presentation/widgets/route_info_bar.dart';
import 'package:app/features/maps/presentation/widgets/route_map_preview.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';
import 'package:url_launcher_platform_interface/url_launcher_platform_interface.dart';

class MockApiClient extends Mock implements ApiClient {}

class MockDio extends Mock implements Dio {}

class MockMapsRepository extends Mock implements MapsRepository {}

class MockUrlLauncherPlatform extends Mock
    with MockPlatformInterfaceMixin
    implements UrlLauncherPlatform {}

void main() {
  late MockApiClient mockApiClient;
  late MockDio mockDio;

  final directionsJsonConfigured = <String, dynamic>{
    'configured': true,
    'distanceMeters': 5200.0,
    'durationSeconds': 720,
    'distanceText': '5.2 km',
    'durationText': '12 mins',
  };

  final directionsJsonNotConfigured = <String, dynamic>{
    'configured': false,
    'distanceMeters': null,
    'durationSeconds': null,
    'distanceText': null,
    'durationText': null,
  };

  final embedJsonConfigured = <String, dynamic>{
    'configured': true,
    'embedUrl':
        'https://www.google.com/maps/embed/v1/place?key=fake-key&q=-23.5505,-46.6333',
  };

  final embedJsonNotConfigured = <String, dynamic>{
    'configured': false,
    'embedUrl': null,
  };

  setUpAll(() {
    registerFallbackValue(Options());
    registerFallbackValue(const LaunchOptions());
  });

  setUp(() {
    mockApiClient = MockApiClient();
    mockDio = MockDio();
    when(() => mockApiClient.dio).thenReturn(mockDio);
  });

  group('DirectionsModel.fromJson', () {
    test('parseia corretamente quando configured=true', () {
      final result = DirectionsModel.fromJson(directionsJsonConfigured);

      expect(result.configured, true);
      expect(result.distanceMeters, 5200.0);
      expect(result.durationSeconds, 720);
      expect(result.distanceText, '5.2 km');
      expect(result.durationText, '12 mins');
    });

    test('parseia corretamente quando configured=false (campos nulos)', () {
      final result = DirectionsModel.fromJson(directionsJsonNotConfigured);

      expect(result.configured, false);
      expect(result.distanceMeters, isNull);
      expect(result.durationSeconds, isNull);
      expect(result.distanceText, isNull);
      expect(result.durationText, isNull);
    });
  });

  group('EmbedUrlModel.fromJson', () {
    test('parseia corretamente quando configured=true', () {
      final result = EmbedUrlModel.fromJson(embedJsonConfigured);

      expect(result.configured, true);
      expect(
        result.embedUrl,
        'https://www.google.com/maps/embed/v1/place?key=fake-key&q=-23.5505,-46.6333',
      );
    });

    test('parseia corretamente quando configured=false (embedUrl nulo)', () {
      final result = EmbedUrlModel.fromJson(embedJsonNotConfigured);

      expect(result.configured, false);
      expect(result.embedUrl, isNull);
    });
  });

  group('MapsRepository.getDirections()', () {
    test(
      'faz GET em /maps/directions com originLat/originLng/destLat/destLng '
      'e retorna DirectionsModel parseado',
      () async {
        when(() => mockDio.get(
              '/maps/directions',
              queryParameters: {
                'originLat': -23.5505,
                'originLng': -46.6333,
                'destLat': -22.9068,
                'destLng': -43.1729,
              },
            )).thenAnswer((_) async => Response(
              data: directionsJsonConfigured,
              statusCode: 200,
              requestOptions: RequestOptions(path: '/maps/directions'),
            ));

        final repository = MapsRepository(mockApiClient);
        final result = await repository.getDirections(
          originLat: -23.5505,
          originLng: -46.6333,
          destLat: -22.9068,
          destLng: -43.1729,
        );

        verify(() => mockDio.get(
              '/maps/directions',
              queryParameters: {
                'originLat': -23.5505,
                'originLng': -46.6333,
                'destLat': -22.9068,
                'destLng': -43.1729,
              },
            )).called(1);
        expect(result, isA<DirectionsModel>());
        expect(result.configured, true);
        expect(result.distanceMeters, 5200.0);
      },
    );

    test(
      'propaga erro tratável (DioException) quando a chamada falha',
      () async {
        when(() => mockDio.get(
              '/maps/directions',
              queryParameters: any(named: 'queryParameters'),
            )).thenThrow(
          DioException(
            requestOptions: RequestOptions(path: '/maps/directions'),
            type: DioExceptionType.connectionError,
          ),
        );

        final repository = MapsRepository(mockApiClient);
        expect(
          () => repository.getDirections(
            originLat: -23.5505,
            originLng: -46.6333,
            destLat: -22.9068,
            destLng: -43.1729,
          ),
          throwsA(isA<DioException>()),
        );
      },
    );
  });

  group('MapsRepository.getEmbedUrl()', () {
    test(
      'faz GET em /maps/embed-url com lat/lng e retorna EmbedUrlModel parseado',
      () async {
        when(() => mockDio.get(
              '/maps/embed-url',
              queryParameters: {'lat': -23.5505, 'lng': -46.6333},
            )).thenAnswer((_) async => Response(
              data: embedJsonConfigured,
              statusCode: 200,
              requestOptions: RequestOptions(path: '/maps/embed-url'),
            ));

        final repository = MapsRepository(mockApiClient);
        final result =
            await repository.getEmbedUrl(lat: -23.5505, lng: -46.6333);

        verify(() => mockDio.get(
              '/maps/embed-url',
              queryParameters: {'lat': -23.5505, 'lng': -46.6333},
            )).called(1);
        expect(result, isA<EmbedUrlModel>());
        expect(result.configured, true);
        expect(result.embedUrl, isNotNull);
      },
    );

    test(
      'propaga erro tratável (DioException) quando a chamada falha',
      () async {
        when(() => mockDio.get(
              '/maps/embed-url',
              queryParameters: any(named: 'queryParameters'),
            )).thenThrow(
          DioException(
            requestOptions: RequestOptions(path: '/maps/embed-url'),
            type: DioExceptionType.connectionError,
          ),
        );

        final repository = MapsRepository(mockApiClient);
        expect(
          () => repository.getEmbedUrl(lat: -23.5505, lng: -46.6333),
          throwsA(isA<DioException>()),
        );
      },
    );
  });

  group('MapsRepository.getDirectionsEmbedUrl()', () {
    test(
      'faz GET em /maps/embed-url/directions com origem/destino e retorna '
      'EmbedUrlModel parseado',
      () async {
        when(() => mockDio.get(
              '/maps/embed-url/directions',
              queryParameters: {
                'originLat': -23.5505,
                'originLng': -46.6333,
                'destLat': -22.9068,
                'destLng': -43.1729,
              },
            )).thenAnswer((_) async => Response(
              data: embedJsonConfigured,
              statusCode: 200,
              requestOptions: RequestOptions(path: '/maps/embed-url/directions'),
            ));

        final repository = MapsRepository(mockApiClient);
        final result = await repository.getDirectionsEmbedUrl(
          originLat: -23.5505,
          originLng: -46.6333,
          destLat: -22.9068,
          destLng: -43.1729,
        );

        verify(() => mockDio.get(
              '/maps/embed-url/directions',
              queryParameters: {
                'originLat': -23.5505,
                'originLng': -46.6333,
                'destLat': -22.9068,
                'destLng': -43.1729,
              },
            )).called(1);
        expect(result, isA<EmbedUrlModel>());
        expect(result.configured, true);
        expect(result.embedUrl, isNotNull);
      },
    );

    test(
      'propaga erro tratável (DioException) quando a chamada falha',
      () async {
        when(() => mockDio.get(
              '/maps/embed-url/directions',
              queryParameters: any(named: 'queryParameters'),
            )).thenThrow(
          DioException(
            requestOptions: RequestOptions(path: '/maps/embed-url/directions'),
            type: DioExceptionType.connectionError,
          ),
        );

        final repository = MapsRepository(mockApiClient);
        expect(
          () => repository.getDirectionsEmbedUrl(
            originLat: -23.5505,
            originLng: -46.6333,
            destLat: -22.9068,
            destLng: -43.1729,
          ),
          throwsA(isA<DioException>()),
        );
      },
    );
  });

  group('directionsProvider (Riverpod)', () {
    late MockMapsRepository mockRepository;

    setUp(() {
      mockRepository = MockMapsRepository();
    });

    test('estado inicial é AsyncLoading, depois AsyncData com o model', () async {
      final container = ProviderContainer(overrides: [
        mapsRepositoryProvider.overrideWithValue(mockRepository),
      ]);
      addTearDown(container.dispose);

      when(() => mockRepository.getDirections(
            originLat: -23.5505,
            originLng: -46.6333,
            destLat: -22.9068,
            destLng: -43.1729,
          )).thenAnswer((_) async => const DirectionsModel(
            configured: true,
            distanceMeters: 5200.0,
            durationSeconds: 720,
            distanceText: '5.2 km',
            durationText: '12 mins',
          ));

      const key = (-23.5505, -46.6333, -22.9068, -43.1729);
      expect(container.read(directionsProvider(key)), isA<AsyncLoading>());

      final result = await container.read(directionsProvider(key).future);
      expect(result.distanceText, '5.2 km');
      expect(container.read(directionsProvider(key)), isA<AsyncData>());
    });

    test(
      'cache por chave — ler o provider duas vezes com a mesma chave não '
      'dispara uma segunda chamada',
      () async {
        final container = ProviderContainer(overrides: [
          mapsRepositoryProvider.overrideWithValue(mockRepository),
        ]);
        addTearDown(container.dispose);

        when(() => mockRepository.getDirections(
              originLat: -23.5505,
              originLng: -46.6333,
              destLat: -22.9068,
              destLng: -43.1729,
            )).thenAnswer((_) async => const DirectionsModel(
              configured: false,
            ));

        const key = (-23.5505, -46.6333, -22.9068, -43.1729);
        await container.read(directionsProvider(key).future);
        await container.read(directionsProvider(key).future);

        verify(() => mockRepository.getDirections(
              originLat: -23.5505,
              originLng: -46.6333,
              destLat: -22.9068,
              destLng: -43.1729,
            )).called(1);
      },
    );
  });

  group('embedUrlProvider (Riverpod)', () {
    late MockMapsRepository mockRepository;

    setUp(() {
      mockRepository = MockMapsRepository();
    });

    test('estado inicial é AsyncLoading, depois AsyncData com o model', () async {
      final container = ProviderContainer(overrides: [
        mapsRepositoryProvider.overrideWithValue(mockRepository),
      ]);
      addTearDown(container.dispose);

      when(() => mockRepository.getEmbedUrl(lat: -23.5505, lng: -46.6333))
          .thenAnswer((_) async => const EmbedUrlModel(
                configured: true,
                embedUrl: 'https://www.google.com/maps/embed/v1/place?key=k',
              ));

      const key = (-23.5505, -46.6333);
      expect(container.read(embedUrlProvider(key)), isA<AsyncLoading>());

      final result = await container.read(embedUrlProvider(key).future);
      expect(result.configured, true);
      expect(container.read(embedUrlProvider(key)), isA<AsyncData>());
    });
  });

  group('directionsEmbedUrlProvider (Riverpod)', () {
    late MockMapsRepository mockRepository;

    setUp(() {
      mockRepository = MockMapsRepository();
    });

    test('estado inicial é AsyncLoading, depois AsyncData com o model', () async {
      final container = ProviderContainer(overrides: [
        mapsRepositoryProvider.overrideWithValue(mockRepository),
      ]);
      addTearDown(container.dispose);

      when(() => mockRepository.getDirectionsEmbedUrl(
            originLat: -23.5505,
            originLng: -46.6333,
            destLat: -22.9068,
            destLng: -43.1729,
          )).thenAnswer((_) async => const EmbedUrlModel(
                configured: true,
                embedUrl: 'https://www.google.com/maps/embed/v1/directions?key=k',
              ));

      const key = (-23.5505, -46.6333, -22.9068, -43.1729);
      expect(
        container.read(directionsEmbedUrlProvider(key)),
        isA<AsyncLoading>(),
      );

      final result =
          await container.read(directionsEmbedUrlProvider(key).future);
      expect(result.configured, true);
      expect(
        container.read(directionsEmbedUrlProvider(key)),
        isA<AsyncData>(),
      );
    });
  });

  group('OpenInMapsButton', () {
    late MockUrlLauncherPlatform mockUrlLauncher;

    setUp(() {
      mockUrlLauncher = MockUrlLauncherPlatform();
      UrlLauncherPlatform.instance = mockUrlLauncher;
    });

    testWidgets(
      'tocar no botão chama launchUrl com a URL de deep-link esperada '
      '(sem API key, sem chamada ao backend)',
      (tester) async {
        when(() => mockUrlLauncher.launchUrl(any(), any()))
            .thenAnswer((_) async => true);

        await tester.pumpWidget(
          const MaterialApp(
            home: Scaffold(
              body: OpenInMapsButton(lat: -23.5505, lng: -46.6333),
            ),
          ),
        );

        await tester.tap(find.byType(IconButton));
        await tester.pumpAndSettle();

        final captured = verify(
          () => mockUrlLauncher.launchUrl(captureAny(), captureAny()),
        ).captured;
        final launchedUrl = captured[0] as String;
        expect(
          launchedUrl,
          'https://www.google.com/maps/search/?api=1&query=-23.5505,-46.6333',
        );
      },
    );

    testWidgets(
      'mostra snackbar quando launchUrl retorna false (não foi possível abrir)',
      (tester) async {
        when(() => mockUrlLauncher.launchUrl(any(), any()))
            .thenAnswer((_) async => false);

        await tester.pumpWidget(
          const MaterialApp(
            home: Scaffold(
              body: OpenInMapsButton(lat: -23.5505, lng: -46.6333),
            ),
          ),
        );

        await tester.tap(find.byType(IconButton));
        await tester.pumpAndSettle();

        expect(find.byType(SnackBar), findsOneWidget);
      },
    );
  });

  group('RouteInfoBar', () {
    testWidgets(
      'mostra distância/duração quando o provider retorna configured=true',
      (tester) async {
        await tester.pumpWidget(
          ProviderScope(
            overrides: [
              directionsProvider((-23.5505, -46.6333, -22.9068, -43.1729))
                  .overrideWith((ref) async => const DirectionsModel(
                        configured: true,
                        distanceMeters: 5200.0,
                        durationSeconds: 720,
                        distanceText: '5.2 km',
                        durationText: '12 mins',
                      )),
            ],
            child: const MaterialApp(
              home: Scaffold(
                body: RouteInfoBar(
                  originLat: -23.5505,
                  originLng: -46.6333,
                  destLat: -22.9068,
                  destLng: -43.1729,
                ),
              ),
            ),
          ),
        );
        await tester.pump();
        await tester.pump();

        expect(find.text('5.2 km · 12 mins'), findsOneWidget);
      },
    );

    testWidgets(
      'não quebra e mostra nota discreta quando configured=false',
      (tester) async {
        await tester.pumpWidget(
          ProviderScope(
            overrides: [
              directionsProvider((-23.5505, -46.6333, -22.9068, -43.1729))
                  .overrideWith(
                      (ref) async => const DirectionsModel(configured: false)),
            ],
            child: const MaterialApp(
              home: Scaffold(
                body: RouteInfoBar(
                  originLat: -23.5505,
                  originLng: -46.6333,
                  destLat: -22.9068,
                  destLng: -43.1729,
                ),
              ),
            ),
          ),
        );
        await tester.pump();
        await tester.pump();

        expect(find.text('Mapa em breve'), findsOneWidget);
        expect(find.textContaining('km'), findsNothing);
      },
    );

    testWidgets(
      'não renderiza nada (nem quebra) enquanto carrega ou em erro',
      (tester) async {
        await tester.pumpWidget(
          ProviderScope(
            overrides: [
              directionsProvider((-23.5505, -46.6333, -22.9068, -43.1729))
                  .overrideWith((ref) async {
                throw DioException(
                  requestOptions: RequestOptions(path: '/maps/directions'),
                  type: DioExceptionType.connectionError,
                );
              }),
            ],
            child: const MaterialApp(
              home: Scaffold(
                body: RouteInfoBar(
                  originLat: -23.5505,
                  originLng: -46.6333,
                  destLat: -22.9068,
                  destLng: -43.1729,
                ),
              ),
            ),
          ),
        );
        await tester.pump();
        await tester.pump();

        expect(find.byType(RouteInfoBar), findsOneWidget);
        expect(find.textContaining('km'), findsNothing);
      },
    );
  });

  group('PoiMapPreview', () {
    // Rodando em VM (flutter test, não Web), MapEmbedView resolve para a
    // implementação stub (map_embed_view_stub.dart) — o texto abaixo é o
    // que ela renderiza. A implementação real (iframe via
    // HtmlElementView) só existe compilando para Web, ver
    // map_embed_view_web.dart.
    testWidgets(
      'renderiza MapEmbedView quando o provider retorna configured=true',
      (tester) async {
        await tester.pumpWidget(
          ProviderScope(
            overrides: [
              embedUrlProvider((-23.5505, -46.6333)).overrideWith(
                (ref) async => const EmbedUrlModel(
                  configured: true,
                  embedUrl: 'https://www.google.com/maps/embed/v1/place?key=k',
                ),
              ),
            ],
            child: const MaterialApp(
              home: Scaffold(
                body: PoiMapPreview(lat: -23.5505, lng: -46.6333),
              ),
            ),
          ),
        );
        await tester.pump();
        await tester.pump();

        expect(find.byType(MapEmbedView), findsOneWidget);
        expect(
          find.text('Mapa visual disponível na versão web do LUVOY'),
          findsOneWidget,
        );
      },
    );

    testWidgets(
      'não renderiza nada quando configured=false',
      (tester) async {
        await tester.pumpWidget(
          ProviderScope(
            overrides: [
              embedUrlProvider((-23.5505, -46.6333)).overrideWith(
                (ref) async => const EmbedUrlModel(configured: false),
              ),
            ],
            child: const MaterialApp(
              home: Scaffold(
                body: PoiMapPreview(lat: -23.5505, lng: -46.6333),
              ),
            ),
          ),
        );
        await tester.pump();
        await tester.pump();

        expect(find.byType(MapEmbedView), findsNothing);
      },
    );

    testWidgets(
      'não quebra e não renderiza nada quando o provider dá erro',
      (tester) async {
        await tester.pumpWidget(
          ProviderScope(
            overrides: [
              embedUrlProvider((-23.5505, -46.6333)).overrideWith((ref) async {
                throw DioException(
                  requestOptions: RequestOptions(path: '/maps/embed-url'),
                  type: DioExceptionType.connectionError,
                );
              }),
            ],
            child: const MaterialApp(
              home: Scaffold(
                body: PoiMapPreview(lat: -23.5505, lng: -46.6333),
              ),
            ),
          ),
        );
        await tester.pump();
        await tester.pump();

        expect(find.byType(PoiMapPreview), findsOneWidget);
        expect(find.byType(MapEmbedView), findsNothing);
      },
    );
  });

  group('RouteMapPreview', () {
    const key = (-23.5505, -46.6333, -22.9068, -43.1729);

    testWidgets(
      'renderiza MapEmbedView quando o provider retorna configured=true',
      (tester) async {
        await tester.pumpWidget(
          ProviderScope(
            overrides: [
              directionsEmbedUrlProvider(key).overrideWith(
                (ref) async => const EmbedUrlModel(
                  configured: true,
                  embedUrl:
                      'https://www.google.com/maps/embed/v1/directions?key=k',
                ),
              ),
            ],
            child: const MaterialApp(
              home: Scaffold(
                body: RouteMapPreview(
                  originLat: -23.5505,
                  originLng: -46.6333,
                  destLat: -22.9068,
                  destLng: -43.1729,
                ),
              ),
            ),
          ),
        );
        await tester.pump();
        await tester.pump();

        expect(find.byType(MapEmbedView), findsOneWidget);
      },
    );

    testWidgets(
      'não renderiza nada quando configured=false',
      (tester) async {
        await tester.pumpWidget(
          ProviderScope(
            overrides: [
              directionsEmbedUrlProvider(key).overrideWith(
                (ref) async => const EmbedUrlModel(configured: false),
              ),
            ],
            child: const MaterialApp(
              home: Scaffold(
                body: RouteMapPreview(
                  originLat: -23.5505,
                  originLng: -46.6333,
                  destLat: -22.9068,
                  destLng: -43.1729,
                ),
              ),
            ),
          ),
        );
        await tester.pump();
        await tester.pump();

        expect(find.byType(MapEmbedView), findsNothing);
      },
    );
  });
}
