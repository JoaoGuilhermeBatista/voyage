// Testes de CONTRATO para a futura feature de câmbio (frontend).
//
// IMPORTANTE: esta task é TDD "testes primeiro" — nenhum código de produção
// para currency foi criado ainda (sem CurrencyRepository, sem CurrencyRatesModel,
// sem provider, sem tela). Por isso boa parte dos testes abaixo NÃO PODE SER
// COMPILADA contra símbolos que ainda não existem (Dart faz análise estática
// mesmo em testes marcados com `skip: true` — skip só pula a EXECUÇÃO do body,
// não a compilação/análise). A solução adotada: a parte que já compila hoje
// (mocks de ApiClient/Dio existentes, fixtures JSON no shape real do backend)
// é escrita como código Dart de verdade; a parte que dependeria de símbolos
// futuros (CurrencyRepository, CurrencyRatesModel, currencyRatesProvider,
// convert()) fica documentada em comentário dentro do teste `skip`-ado, para
// servir de especificação exata do que a implementação precisa satisfazer.
//
// Shape real confirmado em
// backend/api/voyage/src/main/java/luvoygroup/voyage/features/currency/dto/CurrencyRatesDTO.java
// (endpoint público GET /api/v1/currency/rates, sem autenticação — ver
// SecurityConfig.java linha 39: `.requestMatchers(HttpMethod.GET, "/api/v1/currency/**").permitAll()`):
//
//   {
//     "eurToBrl": 5.90,
//     "gbpToBrl": 6.85,
//     "usdToBrl": 5.55
//   }
//
// (BigDecimal serializado pelo Jackson como número, não string; cache de 24h
// no backend via @Cacheable("currency-rates").)

import 'package:app/core/services/api_client.dart';
import 'package:app/features/currency/data/currency_rates_model.dart';
import 'package:app/features/currency/data/currency_repository.dart';
import 'package:app/features/currency/domain/currency_converter.dart';
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

  // Fixture no shape exato do CurrencyRatesDTO do backend.
  final ratesJson = <String, dynamic>{
    'eurToBrl': 5.90,
    'gbpToBrl': 6.85,
    'usdToBrl': 5.55,
  };

  setUpAll(() {
    registerFallbackValue(Options());
  });

  setUp(() {
    mockApiClient = MockApiClient();
    mockDio = MockDio();
    when(() => mockApiClient.dio).thenReturn(mockDio);
  });

  group('CurrencyRepository.getRates()', () {
    // ─── Contrato 1: GET /currency/rates via ApiClient/Dio, parse tipado ────
    //
    // Segue o padrão de lib/features/profile/data/user_repository.dart:
    // repository recebe ApiClient no construtor e chama `_apiClient.dio.get(...)`.
    //
    // Setup real (compila e roda hoje, com os mocks já existentes no projeto):
    test(
      'faz GET em /currency/rates e retorna CurrencyRatesModel parseado do JSON',
      () async {
        when(() => mockDio.get('/currency/rates')).thenAnswer(
          (_) async => Response(
            data: ratesJson,
            statusCode: 200,
            requestOptions: RequestOptions(path: '/currency/rates'),
          ),
        );

        final repository = CurrencyRepository(mockApiClient);
        final result = await repository.getRates();

        verify(() => mockDio.get('/currency/rates')).called(1);
        expect(result, isA<CurrencyRatesModel>());
        expect(result.eurToBrl, 5.90);
        expect(result.gbpToBrl, 6.85);
        expect(result.usdToBrl, 5.55);
      },
    );

    // ─── Contrato 2: erro da API é propagado de forma tratável ──────────────
    //
    // Objetivo: o futuro provider precisa conseguir capturar a falha (ex.:
    // via AsyncValue.guard) para cair em algum fallback de UI — esconder a
    // conversão de moeda, não travar a tela. O repository NÃO deve engolir a
    // exceção silenciosamente nem lançar algo não-catchable; repassar a
    // DioException (ou envolvê-la em uma exceção de domínio) é aceitável,
    // desde que seja um erro Dart normal capturável por try/catch.
    test(
      'propaga erro tratável (DioException) quando a chamada falha',
      () async {
        when(() => mockDio.get('/currency/rates')).thenThrow(
          DioException(
            requestOptions: RequestOptions(path: '/currency/rates'),
            type: DioExceptionType.connectionError,
            error: 'Falha de conexão',
          ),
        );

        final repository = CurrencyRepository(mockApiClient);
        expect(
          () => repository.getRates(),
          throwsA(isA<DioException>()), // ou uma exceção de domínio própria
        );

        // Importante: NÃO deve travar a tela nem propagar um erro genérico
        // sem tipo — o provider precisa conseguir distinguir "falhou" de
        // "sucesso" via try/catch ou AsyncValue.guard.
      },
    );
  });

  group('convert() — função pura de conversão de moeda', () {
    // ─── Contrato 3: convert(amount, fromCurrency, toCurrency, rates) ───────
    //
    // Decisão: SKIP (não deixar falhando por erro de compilação/red real).
    // Justificativa: qualquer chamada real a `convert(...)` exige que a
    // função exista em algum arquivo de lib/ importado aqui — mas o Dart
    // analisa e COMPILA o arquivo de teste inteiro antes de decidir o que
    // executar, então `skip: true` não evita um erro de símbolo não
    // encontrado (diferente de, por ex., pytest, onde um teste pode falhar
    // em runtime sem quebrar a coleção dos demais). Se eu importasse
    // `convert` de um arquivo que não existe, TODO o arquivo de teste
    // (incluindo os grupos 1, 2 e 4) deixaria de compilar e o `flutter test`
    // pararia de reportar qualquer resultado útil para este arquivo — pior
    // sinal de "red" do que um teste explicitamente skip-ado com o contrato
    // documentado. Criar um stub vazio de `convert()` só para permitir a
    // compilação seria, na prática, começar a implementar a feature, o que
    // esta task proíbe explicitamente. Portanto: skip, com o teste real
    // pronto em comentário para ser descomentado assim que a função existir
    // (nesse momento ele DEVE falhar/red até a lógica estar correta).
    test(
      'calcula corretamente o valor convertido dado um mapa de taxas',
      () {
        final rates = {'EUR': 5.90, 'GBP': 6.85, 'USD': 5.55}; // X→BRL

        // BRL → EUR
        expect(
          convert(100, fromCurrency: 'BRL', toCurrency: 'EUR', rates: rates),
          closeTo(100 / 5.90, 0.01),
        );

        // EUR → BRL
        expect(
          convert(10, fromCurrency: 'EUR', toCurrency: 'BRL', rates: rates),
          closeTo(59.0, 0.01),
        );

        // mesma moeda → retorna o valor original sem alteração
        expect(
          convert(50, fromCurrency: 'USD', toCurrency: 'USD', rates: rates),
          50,
        );

        // moeda desconhecida no mapa de rates → deve lançar, não
        // retornar silenciosamente um valor errado (ex.: ArgumentError)
        expect(
          () => convert(10, fromCurrency: 'JPY', toCurrency: 'BRL', rates: rates),
          throwsArgumentError,
        );
      },
    );
  });

  group('currencyRatesProvider (Riverpod)', () {
    // ─── Contrato 4: provider expõe AsyncValue<CurrencyRatesModel> ──────────

    late MockCurrencyRepository mockRepository;

    setUp(() {
      mockRepository = MockCurrencyRepository();
    });

    test('estado inicial é AsyncLoading, depois AsyncData com o model', () async {
      final container = ProviderContainer(overrides: [
        currencyRepositoryProvider.overrideWithValue(mockRepository),
      ]);
      addTearDown(container.dispose);

      when(() => mockRepository.getRates()).thenAnswer(
        (_) async => const CurrencyRatesModel(
          eurToBrl: 5.90,
          gbpToBrl: 6.85,
          usdToBrl: 5.55,
        ),
      );

      expect(container.read(currencyRatesProvider), isA<AsyncLoading>());

      final result = await container.read(currencyRatesProvider.future);
      expect(result.eurToBrl, 5.90);
      expect(container.read(currencyRatesProvider), isA<AsyncData>());
    });

    test(
      'cache de sessão — ler o provider duas vezes não dispara uma segunda '
      'chamada HTTP',
      () async {
        final container = ProviderContainer(overrides: [
          currencyRepositoryProvider.overrideWithValue(mockRepository),
        ]);
        addTearDown(container.dispose);

        when(() => mockRepository.getRates()).thenAnswer(
          (_) async => const CurrencyRatesModel(
            eurToBrl: 5.90,
            gbpToBrl: 6.85,
            usdToBrl: 5.55,
          ),
        );

        await container.read(currencyRatesProvider.future);
        await container.read(currencyRatesProvider.future);

        verify(() => mockRepository.getRates()).called(1);
      },
    );

    test(
      'fallback gracioso em erro — termina em AsyncError sem travar',
      () async {
        final container = ProviderContainer(overrides: [
          currencyRepositoryProvider.overrideWithValue(mockRepository),
        ]);
        addTearDown(container.dispose);

        when(() => mockRepository.getRates()).thenThrow(
          DioException(requestOptions: RequestOptions(path: '/currency/rates')),
        );

        await expectLater(
          container.read(currencyRatesProvider.future),
          throwsA(isA<DioException>()),
        );
        expect(container.read(currencyRatesProvider), isA<AsyncError>());
      },
    );
  });
}
