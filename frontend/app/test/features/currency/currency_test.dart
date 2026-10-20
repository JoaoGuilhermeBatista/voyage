// Testes de CONTRATO pra feature de câmbio por moeda local do destino
// (item #17 do backlog: "trocar a moeda conforme o local do roteiro").
//
// Shape real confirmado em
// backend/api/voyage/src/main/java/luvoygroup/voyage/features/currency/dto/LocalCurrencyRateDTO.java
// (endpoint público GET /api/v1/currency/rates?country=X, sem autenticação —
// ver SecurityConfig.java: `.requestMatchers(HttpMethod.GET, "/api/v1/currency/**").permitAll()`):
//
//   { "currencyCode": "JPY", "rateToBrl": 0.036 }
//
// 204 sem corpo quando o país não é reconhecido, a moeda local já é BRL, ou
// a API de câmbio não tem essa moeda — nesses casos o repository devolve
// `null` (ver CurrencyRepository.getLocalRate), e _CurrencyRatesBar
// simplesmente não aparece.

import 'package:app/core/services/api_client.dart';
import 'package:app/features/currency/data/currency_rates_model.dart';
import 'package:app/features/currency/data/currency_repository.dart';
import 'package:app/features/currency/presentation/providers/currency_provider.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

class MockApiClient extends Mock implements ApiClient {}

class MockDio extends Mock implements Dio {}

class MockCurrencyRepository extends Mock implements CurrencyRepository {}

void main() {
  late MockApiClient mockApiClient;
  late MockDio mockDio;

  setUpAll(() {
    registerFallbackValue(Options());
  });

  setUp(() {
    mockApiClient = MockApiClient();
    mockDio = MockDio();
    when(() => mockApiClient.dio).thenReturn(mockDio);
  });

  group('CurrencyRepository.getLocalRate()', () {
    test(
      'faz GET em /currency/rates?country=X e retorna LocalCurrencyRateModel parseado do JSON',
      () async {
        when(() => mockDio.get(
              '/currency/rates',
              queryParameters: {'country': 'Japão'},
            )).thenAnswer(
          (_) async => Response(
            data: {'currencyCode': 'JPY', 'rateToBrl': 0.036},
            statusCode: 200,
            requestOptions: RequestOptions(path: '/currency/rates'),
          ),
        );

        final repository = CurrencyRepository(mockApiClient);
        final result = await repository.getLocalRate('Japão');

        verify(() => mockDio.get(
              '/currency/rates',
              queryParameters: {'country': 'Japão'},
            )).called(1);
        expect(result, isA<LocalCurrencyRateModel>());
        expect(result!.currencyCode, 'JPY');
        expect(result.rateToBrl, 0.036);
      },
    );

    test(
      'retorna null quando o backend responde 204 (sem moeda local pra mostrar)',
      () async {
        when(() => mockDio.get(
              '/currency/rates',
              queryParameters: {'country': 'Brasil'},
            )).thenAnswer(
          (_) async => Response(
            data: null,
            statusCode: 204,
            requestOptions: RequestOptions(path: '/currency/rates'),
          ),
        );

        final repository = CurrencyRepository(mockApiClient);
        final result = await repository.getLocalRate('Brasil');

        expect(result, isNull);
      },
    );

    test(
      'propaga erro tratável (DioException) quando a chamada falha',
      () async {
        when(() => mockDio.get(
              '/currency/rates',
              queryParameters: {'country': 'Japão'},
            )).thenThrow(
          DioException(
            requestOptions: RequestOptions(path: '/currency/rates'),
            type: DioExceptionType.connectionError,
            error: 'Falha de conexão',
          ),
        );

        final repository = CurrencyRepository(mockApiClient);
        expect(
          () => repository.getLocalRate('Japão'),
          throwsA(isA<DioException>()),
        );
      },
    );
  });

  group('localCurrencyRateProvider (Riverpod)', () {
    late MockCurrencyRepository mockRepository;

    setUp(() {
      mockRepository = MockCurrencyRepository();
    });

    test('estado inicial é AsyncLoading, depois AsyncData com o model', () async {
      final container = ProviderContainer(overrides: [
        currencyRepositoryProvider.overrideWithValue(mockRepository),
      ]);
      addTearDown(container.dispose);

      when(() => mockRepository.getLocalRate('Japão')).thenAnswer(
        (_) async =>
            const LocalCurrencyRateModel(currencyCode: 'JPY', rateToBrl: 0.036),
      );

      expect(
        container.read(localCurrencyRateProvider('Japão')),
        isA<AsyncLoading>(),
      );

      final result =
          await container.read(localCurrencyRateProvider('Japão').future);
      expect(result!.currencyCode, 'JPY');
      expect(
        container.read(localCurrencyRateProvider('Japão')),
        isA<AsyncData>(),
      );
    });

    test(
      'cache por país — ler o provider duas vezes pro mesmo país não dispara '
      'uma segunda chamada HTTP',
      () async {
        final container = ProviderContainer(overrides: [
          currencyRepositoryProvider.overrideWithValue(mockRepository),
        ]);
        addTearDown(container.dispose);

        when(() => mockRepository.getLocalRate('Japão')).thenAnswer(
          (_) async => const LocalCurrencyRateModel(
            currencyCode: 'JPY',
            rateToBrl: 0.036,
          ),
        );

        await container.read(localCurrencyRateProvider('Japão').future);
        await container.read(localCurrencyRateProvider('Japão').future);

        verify(() => mockRepository.getLocalRate('Japão')).called(1);
      },
    );

    test(
      'fallback gracioso em erro — termina em AsyncError sem travar',
      () async {
        final container = ProviderContainer(overrides: [
          currencyRepositoryProvider.overrideWithValue(mockRepository),
        ]);
        addTearDown(container.dispose);

        when(() => mockRepository.getLocalRate('Japão')).thenThrow(
          DioException(requestOptions: RequestOptions(path: '/currency/rates')),
        );

        await expectLater(
          container.read(localCurrencyRateProvider('Japão').future),
          throwsA(isA<DioException>()),
        );
        expect(
          container.read(localCurrencyRateProvider('Japão')),
          isA<AsyncError>(),
        );
      },
    );
  });
}
